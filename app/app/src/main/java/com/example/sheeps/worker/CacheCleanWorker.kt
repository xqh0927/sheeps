package com.example.sheeps.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import logcat.logcat

/**
 * Coil 图片缓存清理后台任务。
 *
 * 由 WorkManager 调度（通常配合定期/一次性任务），在后台线程清空 Coil 的磁盘缓存与内存缓存，
 * 用于释放存储空间或排查图片缓存导致的显示异常。
 *
 * 线程约束：[doWork] 由 `CoroutineWorker` 默认在后台线程（非主线程，@WorkerThread）执行，
 * 内部 `diskCache?.clear()` / `memoryCache?.clear()` 为 I/O 与内存操作，均在此后台线程完成，
 * 不阻塞 UI。使用 [android.content.Context.getApplicationContext]（applicationContext）避免持有
 * Activity/View 引用，无内存泄漏风险。
 */
@HiltWorker
class CacheCleanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @OptIn(ExperimentalCoilApi::class)
    override suspend fun doWork(): Result {
        return try {
            Coil.imageLoader(applicationContext).diskCache?.clear()
            Coil.imageLoader(applicationContext).memoryCache?.clear()
            logcat("CacheCleanWorker") { "Coil cache cleared successfully" }
            Result.success()
        } catch (e: Exception) {
            logcat("CacheCleanWorker") { "Cache clean failed: ${e.message}" }
            Result.failure()
        }
    }
}
