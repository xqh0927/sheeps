package com.example.sheeps.game.viewmodel.delegates

import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.model.ScoreRequest
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.repository.SyncRepository
import com.example.sheeps.game.state.GameViewEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

/**
 * 得分上传与同步逻辑委派类
 */
class ScoreDelegate @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val localDao: LocalDao,
    private val syncRepository: SyncRepository
) {
    /**
     * 提交关卡成绩到云端并同步本地
     */
    fun submitScoreOnline(
        scope: CoroutineScope,
        levelId: Int,
        finalScore: Int,
        clearTimeMs: Long,
        itemsUsed: Int,
        isWin: Int,
        getLocalizedString: (String, String, String, String, String) -> String,
        setEffect: (GameViewEffect) -> Unit
    ) {
        // 更新本地解锁状态
        if (isWin == 1 && levelId == prefs.getUnlockedLevel()) {
            prefs.setUnlockedLevel(levelId + 1)
        }

        // 线程边界：外部传入的 scope 通常为 viewModelScope；launch(Dispatchers.IO) 将网络/本地写入切到 IO 线程，
        // 回调 setEffect 切换回主线程驱动 UI。⚠️ 若该 scope 为匿名未绑定 VM 的 CoroutineScope，存在泄漏风险。
        // 使用独立的 IO 作用域提交，防止页面关闭导致提交中断
        scope.launch(Dispatchers.IO) {
            try {
                // 1. 本地存储进度（仅成功通关时）
                if (isWin == 1) {
                    syncRepository.saveProgressAndPointsLocally(
                        levelId = levelId,
                        score = finalScore,
                        clearTime = clearTimeMs,
                        pointsGained = 0
                    )
                }

                // 2. 云端排行榜同步
                val userId = prefs.getUserId()
                val token = prefs.getToken()
                val authHeader = token?.let { "Bearer $it" }
                val sign = sha256("${userId}_${levelId}_${clearTimeMs}_folklore")

                apiService.submitScore(
                    auth = authHeader,
                    request = ScoreRequest(
                        user_id = userId,
                        level_id = levelId,
                        score = finalScore,
                        clear_time_ms = clearTimeMs,
                        sign = sign,
                        game_mode = 0,
                        items_used = itemsUsed,
                        is_win = isWin
                    )
                )

                if (isWin == 1) {
                    setEffect(GameViewEffect.ShowToast(
                        getLocalizedString("通关成功！进度已安全存储！", "Cleared! Progress saved!", "通關成功！進度已安全存儲！", "クリア成功！セーブしました！", "클리어 성공! 저장되었습니다!")
                    ))
                }
            } catch (e: Exception) {
                if (isWin == 1) {
                    setEffect(GameViewEffect.ShowToast(
                        getLocalizedString("通关成功！已离线保存进度，恢复连接后自动同步", "Cleared! Progress saved offline, will sync when reconnected", "通關成功！已離線保存進度，恢復連接後自動同步", "クリア成功！オフラインで保存しました。再接続時に同期されます", "클리어 성공! 오프라인으로 저장되었으며, 재연결 시 동기화됩니다")
                    ))
                }
            }
        }
    }

    private fun sha256(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
