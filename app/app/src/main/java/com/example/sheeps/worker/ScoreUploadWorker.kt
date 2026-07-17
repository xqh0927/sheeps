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

/**
 * 成绩上传后台任务。
 * 即使应用处于后台或由于网络波动上传失败，此任务也会通过 WorkManager 保证可靠地将关卡成绩同步至云端。
 *
 * 失败返回 [androidx.work.ListenableWorker.Result.retry]，按退避策略自动重试。
 * 线程约束：[doWork] 由 `CoroutineWorker` 默认在后台线程（@WorkerThread）执行，不阻塞主线程；
 * 使用 applicationContext 语义的 `context` 注入，避免持有 Activity/View，无内存泄漏风险。
 */
@HiltWorker
class ScoreUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) : CoroutineWorker(context, workerParams) {

    /**
     * 执行一次成绩上传。
     *
     * 从 [androidx.work.WorkerParameters.getInputData] 读取 `score` / `level_id` / `clear_time`，
     * 用与服务端一致的算法计算防作弊签名后提交；[pendingScore] < 0 视为无效直接失败。
     * 返回 [androidx.work.ListenableWorker.Result.success] / [androidx.work.ListenableWorker.Result.retry] / failure。
     */
    override suspend fun doWork(): Result {
        return try {
            // 获取任务输入参数
            val userId = userPreferences.getUserId()
            val pendingScore = inputData.getInt("score", -1)
            val levelId = inputData.getInt("level_id", 1)
            val clearTime = inputData.getLong("clear_time", 0L)

            if (pendingScore < 0) return Result.failure()

            // 签名计算（与服务端逻辑一致）
            val sign = sha256("${userId}_${levelId}_${clearTime}_folklore")

            val token = userPreferences.getToken()
            val authHeader = if (token != null) "Bearer $token" else null

            // 调用接口提交
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
            logcat("ScoreUploadWorker") { "Score uploaded successfully: $pendingScore for level $levelId" }
            Result.success()
        } catch (e: Exception) {
            logcat("ScoreUploadWorker") { "Upload failed: ${e.message}, retrying based on backoff policy..." }
            // 失败时触发重试策略
            Result.retry()
        }
    }

    /** 计算 SHA-256 十六进制摘要（与服务端一致的防作弊签名算法）。 */
    private fun sha256(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).fold("") { str, it -> str + "%02x".format(it) }
    }
}
