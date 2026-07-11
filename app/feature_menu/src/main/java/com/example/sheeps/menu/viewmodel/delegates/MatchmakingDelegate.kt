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
 * 处理在线匹配的加入、取消以及状态轮询。
 *
 * 持有关系：由 [com.example.sheeps.menu.viewmodel.MenuViewModel] 注入并持有，
 * 负责匹配相关的子状态（matchStatus / matchedGameId / matchedOpponentId / duelLevel / gameSeed）。
 * 线程边界：所有公开方法接收调用方 [CoroutineScope]（MenuViewModel.viewModelScope），
 * 网络请求在挂起函数内自动切 IO；轮询通过作用域取消而终止，不会泄漏。
 */
class MatchmakingDelegate @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * 处理加入匹配。
     *
     * @param scope 协程作用域（MenuViewModel.viewModelScope），进入匹配 / 轮询期间若 ViewModel 销毁则自动取消。
     * @param playerId 玩家 ID，用于发起加入与后续状态轮询。
     * @param currentState 当前 [MenuViewState]，本委派只读不持有，避免捕获过期状态（轮询中用可变变量跟踪）。
     * @param updateState 状态更新回调，用于写回匹配中/已匹配/错误等子状态。
     * 线程边界：先在主线程写初始 searching 态；[apiService.joinMatch] 在 IO；
     * 若服务端返回非 matched 则进入 [pollMatchStatus] 周期性轮询。
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
     * 轮询匹配状态（加入后未立即匹配时调用）。
     * ⚠️ 内存/协程隐患（已规避）：轮询通过 `repeat(20)` 上限 + 每次 [delay] 实现，
     * 不会无限循环；且运行于调用方 scope 内，ViewModel 销毁时 [scope] 取消即终止，不会泄漏或空转。
     * 使用局部可变变量 `matched` 跟踪结果，避免捕获的 [currentState] 参数不可变导致死循环。
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
     * 处理离开匹配：通知服务端取消排队。
     * @param scope 协程作用域（MenuViewModel.viewModelScope）。
     * @param playerId 玩家 ID。
     * 线程边界：[apiService.leaveMatch] 在 IO 执行；失败静默忽略（离开请求非关键路径）。
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
