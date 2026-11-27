package com.example.sheeps.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.sheeps.data.repository.StageScoreRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScoreUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scores: StageScoreRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val owner = inputData.getString("owner") ?: return Result.success()
        val resultsSent = scores.flush(owner)
        val startsSent = scores.flushStarts(owner)
        return if (resultsSent && startsSent) Result.success() else Result.retry()
    }

    companion object {
        fun schedule(context: Context, owner: String, key: String) {
            val upload = OneTimeWorkRequestBuilder<ScoreUploadWorker>()
                .setInputData(workDataOf("owner" to owner))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("stage-upload:$owner:$key", ExistingWorkPolicy.KEEP, upload)
        }
    }
}
