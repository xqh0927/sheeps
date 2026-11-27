package com.example.sheeps.data.repository

import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.model.StageStartRequest
import com.example.sheeps.data.model.GenericResponse
import com.example.sheeps.data.local.PendingStageScore
import com.example.sheeps.data.model.ScoreRequest
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.data.result.TokenRefresher
import com.example.sheeps.data.utils.NetworkMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class StageScoreRepository @Inject constructor(
    private val dao: LocalDao,
    private val api: ApiService,
    private val prefs: UserPreferences,
    private val network: NetworkMonitor,
    private val sessions: TokenRefresher
) {
    private val uploading = Mutex()
    private val confirmations = java.util.concurrent.atomic.AtomicLong()
    val confirmationCount: Long get() = confirmations.get()

    fun currentOwner(): String = prefs.accountId() ?: prefs.getUserId()

    suspend fun record(result: PendingStageScore) {
        val active = currentOwner() == result.ownerId
        dao.recordStageResult(result, active)
        if (active && currentOwner() == result.ownerId && result.won && result.levelId == prefs.getUnlockedLevel()) {
            prefs.setUnlockedLevel(result.levelId + 1)
        }
    }

    /** Inactive accounts retain their rows until they next sign in. False asks the worker to retry. */
    suspend fun flush(owner: String): Boolean = uploading.withLock {
        if (owner != currentOwner()) return@withLock true
        val pending = dao.pendingScores(owner)
        if (pending.isEmpty()) return@withLock true
        if (!network.isOnline()) return@withLock false
        for (row in pending) {
            val token = prefs.getToken()
            val current = if (token == null) prefs.getUserId() else prefs.accountId(token)
            if (current != owner) return@withLock true
            val signature = MessageDigest.getInstance("SHA-256")
                .digest("${owner}_${row.levelId}_${row.clearTimeMs}_folklore".toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            val request = ScoreRequest(owner, row.levelId, row.score, row.clearTimeMs, signature,
                items_used = row.itemsUsed, is_win = if (row.won) 1 else 0, run_id = row.runId, completed_at = row.completedAt, abandoned = row.abandoned)
            if (!acknowledged(token, row.runId) { api.submitScoreOnce(it, request) }) return@withLock false
            dao.acknowledgeScore(owner, row.runId)
            confirmations.incrementAndGet()
        }
        true
    }

    suspend fun flushStarts(owner: String): Boolean = uploading.withLock {
        if (owner != currentOwner()) return@withLock true
        for (start in dao.pendingStageStarts(owner)) {
            val token = prefs.getToken()
            val current = if (token == null) prefs.getUserId() else prefs.accountId(token)
            if (current != owner) return@withLock true
            if (!network.isOnline()) return@withLock false
            val request = StageStartRequest(owner, start.runId, start.levelId, start.layoutVersion, start.startedAt)
            if (!acknowledged(token, start.runId) { api.recordStageStart(it, request) }) return@withLock false
            dao.acknowledgeStageStart(owner, start.runId)
        }
        true
    }

    private suspend fun acknowledged(token: String?, runId: String, send: suspend (String) -> GenericResponse): Boolean {
        val receipt = if (token != null) {
            when (val result = sessions.authenticated(token, send)) {
                is ApiResult.Success -> result.data
                is ApiResult.Error -> return false
            }
        } else {
            // Explicit empty auth keeps the interceptor from adopting a concurrently signed-in account.
            try { send("") }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: IOException) { return false }
            catch (_: HttpException) { return false }
        }
        return receipt.success && receipt.run_id == runId
    }
}
