package com.example.sheeps.data.repository

import com.example.sheeps.data.local.StageSaveDao
import com.example.sheeps.data.local.StageSaveEntity
import com.example.sheeps.data.model.Tile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class ResumableStage(val id: String, val level: Int)

@Serializable
data class StagePosition(
    val boardTiles: List<Tile>,
    val slotTiles: List<Tile>,
    val movedOutTiles: List<Tile>,
    val sealedClearCount: Int,
    val sealedUnlockedIds: Set<String>,
    val score: Int? = null // Older history entries did not store the score.
)

@Serializable
data class StageSnapshot(
    val position: StagePosition,
    val history: List<StagePosition>,
    val carry: Map<String, Int>,
    val sealOrder: List<String>,
    val sealThreshold: Int,
    val highlighted: Set<String>,
    val doublePoints: Boolean,
    val score: Int,
    val itemsUsed: Int,
    val elapsedMs: Long,
    val awaitingRevive: Boolean
)

class StageSaveSession(val owner: String, val id: String, val level: Int) {
    @Volatile var current: Boolean = true
        internal set
}

/** Serialize checkpoints and ownership changes, including Activity instances sharing one run. */
@Singleton
@OptIn(ExperimentalSerializationApi::class)
class StageSaves @Inject constructor(private val dao: StageSaveDao, private val json: Json) {
    private val mutex = Mutex()
    private val writer = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessions = mutableMapOf<String, StageSaveSession>()

    private val settling = MutableStateFlow<Map<String, String>>(emptyMap())

    fun observeLatest(owner: String) = dao.observeLatest(owner).combine(settling) { row, finished ->
        row?.takeUnless { finished[owner] == it.id }
    }.distinctUntilChanged()

    suspend fun latest(owner: String) = mutex.withLock {
        dao.latest(owner)?.takeUnless { settling.value[owner] == it.id }
    }

    /** Queue the local result after every earlier move and before any new opening for this owner. */
    fun settle(
        session: StageSaveSession,
        record: suspend () -> Unit,
        onSaved: suspend () -> Unit,
        onFailure: (StageSnapshot?) -> Unit
    ) {
        if (!session.current) return
        settling.update { it + (session.owner to session.id) }
        writer.launch(start = CoroutineStart.UNDISPATCHED) {
            var checkpoint: StageSnapshot? = null
            val saved = mutex.withLock {
                if (sessions[session.owner] !== session) return@launch
                try {
                    withContext(Dispatchers.IO) { record() }
                    true
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    try {
                        // If the Room transaction committed before a later preferences update
                        // failed, its score is already durable and the run row is gone.
                        val row = dao.read(session.owner, session.id) ?: return@withLock true
                        checkpoint = decode(row.payload)
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // The menu can retry the disk read after the storage problem clears.
                    } finally {
                        settling.update { pending ->
                            if (pending[session.owner] == session.id) pending - session.owner else pending
                        }
                    }
                    false
                }
            }
            if (saved) onSaved() else onFailure(checkpoint)
        }
    }

    private fun claim(session: StageSaveSession) {
        val previous = sessions.put(session.owner, session)
        if (previous !== session) previous?.current = false
    }

    suspend fun begin(
        session: StageSaveSession,
        onReplace: suspend (ResumableStage, StageSnapshot) -> Unit,
        prepare: suspend () -> StageSnapshot
    ): StageSnapshot = mutex.withLock {
        lateinit var snapshot: StageSnapshot
        dao.replaceRun {
            val old = dao.latest(session.owner)
            if (old != null && old.id != session.id && old.id != settling.value[session.owner]) {
                val row = dao.read(session.owner, old.id)
                val previous = row?.let { decode(it.payload) }
                if (previous != null) onReplace(old, previous)
            }
            snapshot = prepare()
            StageSaveEntity(session.owner, session.id, session.level, encode(snapshot))
        }
        claim(session)
        settling.update { it - session.owner }
        snapshot
    }

    suspend fun restore(session: StageSaveSession): StageSnapshot? = mutex.withLock {
        if (settling.value[session.owner] == session.id) return@withLock null
        val row = dao.read(session.owner, session.id) ?: return@withLock null
        require(row.levelId == session.level)
        val snapshot = decode(row.payload)
        if (snapshot == null) {
            dao.discard(session.owner, session.id)
            throw IllegalStateException("Invalid stage checkpoint")
        }
        if (settling.value[session.owner] == session.id) return@withLock null
        claim(session)
        snapshot
    }

    // Acquire the FIFO mutex before returning to the caller. A late pause can never overwrite
    // a newer Activity's moves, resurrect a settled game, or overwrite a different run.
    fun checkpoint(session: StageSaveSession, snapshot: StageSnapshot, onFailure: () -> Unit) {
        writer.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                mutex.withLock {
                    if (sessions[session.owner] === session) {
                        dao.checkpoint(session.owner, session.id, encode(snapshot))
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onFailure()
            }
        }
    }

    private suspend fun decode(payload: ByteArray): StageSnapshot? = withContext(Dispatchers.Default) {
        try {
            GZIPInputStream(payload.inputStream()).use { json.decodeFromStream<StageSnapshot>(it) }
        } catch (_: java.io.IOException) {
            null
        } catch (_: kotlinx.serialization.SerializationException) {
            null
        }
    }

    private suspend fun encode(snapshot: StageSnapshot) = withContext(Dispatchers.Default) {
        val bytes = ByteArrayOutputStream()
        GZIPOutputStream(bytes).use { json.encodeToStream(snapshot, it) }
        bytes.toByteArray()
    }
}
