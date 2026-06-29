package com.example.sheeps.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.model.ScoreRequest
import com.example.sheeps.data.network.ApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import logcat.logcat
import java.security.MessageDigest

@HiltWorker
class ScoreUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val userId = userPreferences.getUserId()
            val pendingScore = inputData.getInt("score", -1)
            val levelId = inputData.getInt("level_id", 1)
            val clearTime = inputData.getLong("clear_time", 0L)

            if (pendingScore < 0) return Result.failure()

            val sign = sha256("${userId}_${levelId}_${clearTime}_folklore")

            val token = userPreferences.getToken()
            val authHeader = if (token != null) "Bearer $token" else null

            apiService.submitScore(
                auth = authHeader,
                request = ScoreRequest(
                    user_id = userId,
                    level_id = levelId,
                    score = pendingScore,
                    clear_time_ms = clearTime,
                    sign = sign
                )
            )
            logcat("ScoreUploadWorker") { "Score uploaded: $pendingScore for level $levelId" }
            Result.success()
        } catch (e: Exception) {
            logcat("ScoreUploadWorker") { "Upload failed: ${e.message}, retrying..." }
            Result.retry()
        }
    }

    private fun sha256(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).fold("") { str, it -> str + "%02x".format(it) }
    }
}
