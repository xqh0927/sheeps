package com.example.sheeps.menu.viewmodel.delegates

import com.example.sheeps.data.model.MatchJoinRequest
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.menu.state.MenuViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 匹配系统逻辑委派类
 * 处理在线匹配的加入、取消以及状态轮询
 */
class MatchmakingDelegate @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * 处理加入匹配
     * 
     * @param scope 协程作用域
     * @param playerId 玩家ID
     * @param currentState 当前界面状态
     * @param updateState 状态更新回调
     */
    fun handleJoinMatch(
        scope: CoroutineScope,
        playerId: String,
        currentState: MenuViewState,
        updateState: (MenuViewState.() -> MenuViewState) -> Unit
    ) {
        updateState {
            copy(
                matchStatus = "searching",
                matchedGameId = null,
                matchedOpponentId = null,
                duelLevel = 2,
                gameSeed = 0
            )
        }
        
        scope.launch {
            try {
                val joinResponse = apiService.joinMatch(MatchJoinRequest(playerId))
                if (joinResponse.status == "matched") {
                    updateState {
                        copy(
                            matchStatus = "matched",
                            matchedGameId = joinResponse.gameId,
                            matchedOpponentId = joinResponse.opponentId,
                            duelLevel = joinResponse.duelLevel ?: 2,
                            gameSeed = joinResponse.gameSeed ?: 0
                        )
                    }
                } else {
                    // 开始轮询匹配状态
                    pollMatchStatus(scope, playerId, currentState, updateState)
                }
            } catch (e: Exception) {
                updateState { copy(matchStatus = "error") }
            }
        }
    }

    /**
     * 轮询匹配状态
     */
    private suspend fun pollMatchStatus(
        scope: CoroutineScope,
        playerId: String,
        currentState: MenuViewState,
        updateState: (MenuViewState.() -> MenuViewState) -> Unit
    ) {
        // 用可变引用跟踪匹配状态，避免捕获的 currentState 参数值不变导致的死循环
        var matched = false
        repeat(20) {
            if (matched) return@repeat
            delay(1500)

            try {
                val statusResponse = apiService.getMatchStatus(playerId)
                when (statusResponse.status) {
                    "matched" -> {
                        matched = true
                        updateState {
                            copy(
                                matchStatus = "matched",
                                matchedGameId = statusResponse.gameId,
                                matchedOpponentId = statusResponse.opponentId,
                                duelLevel = statusResponse.duelLevel ?: 2,
                                gameSeed = statusResponse.gameSeed ?: 0
                            )
                        }
                    }
                    "not_in_queue" -> {
                        matched = true
                        updateState { copy(matchStatus = "error") }
                    }
                    // "waiting" → 继续轮询
                }
            } catch (e: Exception) {
                // 轮询中的单次错误暂不中断
            }
        }

        // 超时未匹配到
        if (!matched) {
            updateState { copy(matchStatus = "error") }
        }
    }

    /**
     * 处理离开匹配
     */
    fun handleLeaveMatch(scope: CoroutineScope, playerId: String) {
        scope.launch {
            try {
                apiService.leaveMatch(MatchJoinRequest(playerId))
            } catch (e: Exception) {
                // 忽略离开匹配的错误
            }
        }
    }
}
