package com.example.sheeps.game.viewmodel.delegates

import android.content.Context
import com.example.sheeps.data.local.PendingStageScore
import com.example.sheeps.data.repository.StageScoreRepository
import com.example.sheeps.game.state.GameViewEffect
import com.example.sheeps.worker.ScoreUploadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class ScoreDelegate @Inject constructor(
    private val scores: StageScoreRepository,
    @ApplicationContext private val context: Context
) {
    fun scheduleUpload(ownerId: String, key: String) {
        try {
            ScoreUploadWorker.schedule(context, ownerId, key)
        } catch (error: IllegalStateException) {
            android.util.Log.w("ScoreUpload", "Upload will be scheduled on next foreground", error)
        }
    }

    suspend fun record(result: PendingStageScore) = scores.record(result)

    suspend fun upload(
        result: PendingStageScore,
        getLocalizedString: (String, String, String, String, String) -> String,
        setEffect: (GameViewEffect) -> Unit
    ) {
        scheduleUpload(result.ownerId, result.runId)
        val synced = try {
            scores.flush(result.ownerId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false // The committed Room row remains available to the upload worker.
        }
        if (result.won && scores.currentOwner() == result.ownerId) {
            val message = if (synced) {
                getLocalizedString("通关成功！进度已安全存储！", "Cleared! Progress saved!", "通關成功！進度已安全儲存！", "クリア成功！セーブしました！", "클리어 성공! 저장되었습니다!")
            } else {
                getLocalizedString("通关成功！已离线保存进度，恢复连接后自动同步", "Cleared! Result saved for upload", "通關成功！已離線儲存進度，恢復連線後自動同步", "クリア成功！接続回復後に同期します", "클리어 성공! 연결되면 동기화합니다")
            }
            setEffect(GameViewEffect.ShowToast(message))
        }
    }
}
