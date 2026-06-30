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
        levelStartTime: Long,
        itemsUsedCount: Int,
        isDoublePointsActive: Boolean,
        getLocalizedString: (String, String, String, String, String) -> String,
        setEffect: (GameViewEffect) -> Unit
    ) {
        val clearTime = System.currentTimeMillis() - levelStartTime

        // 更新本地解锁状态
        if (levelId == prefs.getUnlockedLevel()) {
            prefs.setUnlockedLevel(levelId + 1)
        }

        // 计算成绩积分
        val timeInSeconds = clearTime / 1000L
        val difficultyCoeff = when (levelId) {
            1 -> 1
            2 -> 2
            3 -> 3
            else -> 4
        }
        val baseScore = (maxOf(100L, (1000L - timeInSeconds * 2) - itemsUsedCount * 50L) * difficultyCoeff).toInt()
        val finalScore = if (isDoublePointsActive) baseScore * 2 else baseScore

        // 使用独立的 IO 作用域提交，防止页面关闭导致提交中断
        scope.launch(Dispatchers.IO) {
            try {
                // 1. 本地存储进度
                val currentProgress = localDao.getAllProgress().find { it.levelId == levelId && it.score > 0 }
                val pointsReward = if (currentProgress == null) 50 else 0
                
                syncRepository.saveProgressAndPointsLocally(
                    levelId = levelId,
                    score = finalScore,
                    clearTime = clearTime,
                    pointsGained = pointsReward
                )

                // 2. 云端排行榜同步
                val userId = prefs.getUserId()
                val token = prefs.getToken()
                val authHeader = token?.let { "Bearer $it" }
                val sign = sha256("${userId}_${levelId}_${clearTime}_folklore")

                apiService.submitScore(
                    auth = authHeader,
                    request = ScoreRequest(
                        user_id = userId,
                        level_id = levelId,
                        score = finalScore,
                        clear_time_ms = clearTime,
                        sign = sign
                    )
                )

                setEffect(GameViewEffect.ShowToast(
                    if (pointsReward > 0) {
                        getLocalizedString("恭喜通关！首次通关获得50积分，进度已安全存储！", "Congratulations! First clear rewarded 50 points, progress saved!", "恭喜通關！首次通關獲得50積分，進度已安全存儲！", "クリアおめでとうございます！初回クリアで50ポイント獲得、セーブしました！", "클리어를 축하합니다! 최초 클리어로 50포인트를 획득했으며, 저장되었습니다!")
                    } else {
                        getLocalizedString("通关成功！进度已安全存储！", "Cleared! Progress saved!", "通關成功！進度已安全存儲！", "クリア成功！セーブしました！", "클리어 성공! 저장되었습니다!")
                    }
                ))
            } catch (e: Exception) {
                setEffect(GameViewEffect.ShowToast(
                    getLocalizedString("通关成功！已离线保存进度，恢复连接后自动同步", "Cleared! Progress saved offline, will sync when reconnected", "通關成功！已離線保存進度，恢復連接後自動同步", "クリア成功！オフラインで保存しました。再接続時に同期されます", "클리어 성공! 오프라인으로 저장되었으며, 재연결 시 동기화됩니다")
                ))
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
