package com.example.sheeps.game.viewmodel.delegates

import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.model.ScoreRequest
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.game.state.EndlessViewEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

/**
 * 无尽生存模式成绩上传委派。
 *
 * 复用 [com.example.sheeps.game.viewmodel.delegates.ScoreDelegate] 的 sha256 签名与提交范式，
 * 但组装带 `game_mode = 1` 的 [ScoreRequest]。
 *
 * 防作弊签名：`sha256("${userId}_${levelId}_${elapsedMs}_folklore")`（与服务端一致）。
 * 同时本地存历史最高分（SharedPreferences key = `endless_best`）。
 */
class EndlessScoreDelegate @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences
) {

    /**
     * 提交无尽成绩到云端并本地存档最高分。
     *
     * @param scope 提交作用域（通常用 viewModelScope，保证页面关闭仍可提交）
     * @param finalScore 本局最终得分
     * @param elapsedMs 存活时长（毫秒）
     * @param isDaily 是否每日挑战
     * @param getLocalizedString 多语言取词回调 (zh, en, tw, ja, ko)
     * @param setEffect 副作用回调（用于提示 Toast）
     */
    fun submitEndlessScore(
        scope: CoroutineScope,
        finalScore: Int,
        elapsedMs: Long,
        isDaily: Boolean,
        getLocalizedString: (String, String, String, String, String) -> String,
        setEffect: (EndlessViewEffect) -> Unit
    ) {
        // 本地先存最高分（即便提交失败也不丢纪录）
        val prevBest = prefs.getEndlessBest()
        if (finalScore > prevBest) prefs.setEndlessBest(finalScore)

        scope.launch(Dispatchers.IO) {
            try {
                val userId = prefs.getUserId()
                val token = prefs.getToken()
                val authHeader = token?.let { "Bearer $it" }
                val levelId = 0 // 无尽模式用 0 占位
                val sign = sha256("${userId}_${levelId}_${elapsedMs}_folklore")

                apiService.submitScore(
                    auth = authHeader,
                    request = ScoreRequest(
                        user_id = userId,
                        level_id = levelId,
                        score = finalScore,
                        clear_time_ms = elapsedMs,
                        sign = sign,
                        game_mode = 1
                    )
                )

                setEffect(
                    EndlessViewEffect.ShowToast(
                        getLocalizedString(
                            "成绩已提交！",
                            "Score submitted!",
                            "成績已提交！",
                            "スコア送信完了！",
                            "점수 제출 완료!"
                        )
                    )
                )
            } catch (e: Exception) {
                setEffect(
                    EndlessViewEffect.ShowToast(
                        getLocalizedString(
                            "成绩已离线保存",
                            "Score saved offline",
                            "成績已離線保存",
                            "オフライン保存しました",
                            "오프라인 저장됨"
                        )
                    )
                )
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
