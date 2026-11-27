package com.example.sheeps.data.repository

import android.content.Context
import com.example.sheeps.data.game.GameLevelGenerator
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.network.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class StageBoard(val tiles: List<Tile>, val offline: Boolean = false, val version: String? = null) {
    val healthVersion: String get() = version ?: "unknown"
    val hasBlind: Boolean get() = tiles.any { it.isBlind }
    val hasSeals: Boolean get() = tiles.any { it.sealedCount > 0 }
}

/** Pins the board seen in loadout for the lifetime of a run. A new run checks for publication changes. */
@Singleton
class StageBoards(
    private val api: ApiService,
    private val json: Json,
    private val generator: GameLevelGenerator,
    private val directory: File
) {
    @Inject constructor(api: ApiService, json: Json, generator: GameLevelGenerator, @ApplicationContext context: Context) :
        this(api, json, generator, File(context.cacheDir, "stage-boards"))

    private val mutex = Mutex()
    private var lastRun: Pair<File, StageBoard>? = null

    suspend fun resolve(level: Int, runId: String, userId: Int): StageBoard = withContext(Dispatchers.IO) {
        val key = UUID.nameUUIDFromBytes(runId.toByteArray(Charsets.UTF_8))
        val run = File(directory, "run-$level-$key")
        val prepared = mutex.withLock { pinned(run) }
        if (prepared != null) return@withContext prepared

        val seed = Math.floorMod(runId.hashCode(), 1_000_000) + 1
        val response = try {
            withTimeoutOrNull(2_000L) { api.getStageBoard(level, seed) }?.takeIf { it.isSuccessful }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        ensureActive()
        val published = File(directory, "published-$level")
        val remote = response?.body()
        val board = if (!remote.isNullOrEmpty()) {
            StageBoard(remote, version = response.headers()["X-Stage-Version"])
        } else {
            read(published)?.copy(offline = true)
                ?: StageBoard(generator.generateSolvableLevelLocal(level, seed.toLong(), userId), offline = true, version = "generated-local-v5")
        }
        mutex.withLock {
            ensureActive()
            pinned(run)?.let { return@withLock it }
            if (!board.offline) {
                when (response?.headers()?.get("X-Stage-Source")) {
                    "published" -> write(published, board)
                    "generated" -> published.delete()
                }
            }
            write(run, board)
            lastRun = run to board
            directory.listFiles { f -> f.name.startsWith("run-") && f != run }
                ?.sortedByDescending { it.lastModified() }?.drop(15)?.forEach { it.delete() }
            board
        }
    }

    private fun pinned(file: File): StageBoard? = lastRun?.takeIf { it.first == file }?.second ?: read(file)

    private fun read(file: File): StageBoard? = try {
        if (file.isFile) json.decodeFromString<StageBoard>(file.readText()).takeIf { it.tiles.isNotEmpty() } else null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: IOException) {
        null
    }

    private fun write(file: File, board: StageBoard) {
        // Storage is best effort. The in-memory copy still pins a preview if the cache is full.
        try {
            directory.mkdirs()
            val temp = File.createTempFile("board-", ".tmp", directory)
            try {
                temp.writeText(json.encodeToString(board))
                temp.renameTo(file)
            } finally {
                temp.delete()
            }
        } catch (_: IOException) {
            return
        }
    }
}
