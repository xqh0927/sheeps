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
