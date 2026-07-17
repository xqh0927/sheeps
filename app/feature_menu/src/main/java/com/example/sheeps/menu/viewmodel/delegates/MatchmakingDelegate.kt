package com.example.sheeps.menu.viewmodel.delegates

import com.example.sheeps.data.model.MatchJoinRequest
import com.example.sheeps.data.repository.MatchRepository
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.menu.state.MenuViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 匹配系统逻辑委派类
 * 处理在线匹配的加入、取消以及状态轮询。
 *
 * 重构要点（方案 2 / 统一错误闸门）：
 * - 改注入 [MatchRepository]，不再直注 [com.example.sheeps.data.network.ApiService]。
 * - 所有网络结果经 [ApiResult] 分发（[ApiResult.Success] / [ApiResult.Error]），
 *   **不再出现包裹网络调用的 `try/catch(Exception)`**。
 * - 轮询中单次网络错误**不再静默吞**：累计失败达到阈值后置 `matchStatus = "error"`，使其可见。
 *
 * 持有关系：由 [com.example.sheeps.menu.viewmodel.MenuViewModel] 注入并持有。
 * 线程边界：所有公开方法接收调用方 [CoroutineScope]（MenuViewModel.viewModelScope），
 * 网络请求在挂起函数内自动切 IO；轮询通过作用域取消而终止，不会泄漏。
 */
class MatchmakingDelegate @Inject constructor(
    private val matchRepository: MatchRepository
) {
    /**
     * 处理加入匹配。
     *
     * @param scope 协程作用域（MenuViewModel.viewModelScope），进入匹配 / 轮询期间若 ViewModel 销毁则自动取消。
     * @param playerId 玩家 ID，用于发起加入与后续状态轮询。
     * @param currentState 当前 [MenuViewState]，本委派只读不持有，避免捕获过期状态（轮询中用可变变量跟踪）。
     * @param updateState 状态更新回调，用于写回匹配中/已匹配/错误等子状态。
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
            when (val result = matchRepository.joinMatch(MatchJoinRequest(playerId))) {
                is ApiResult.Success -> {
                    val joinResponse = result.data
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
                        pollMatchStatus(playerId, updateState)
                    }
                }
                is ApiResult.Error -> updateState { copy(matchStatus = "error") }
            }
        }
    }

    /**
     * 轮询匹配状态（加入后未立即匹配时调用）。
     *
     * 内存/协程安全：通过 `repeat(MAX_POLL_ROUNDS)` 上限 + 每次 [delay] 实现，不会无限循环；
     * 运行于调用方 scope 内，ViewModel 销毁时 [scope] 取消即终止。
     * 使用局部可变变量 `matched` 跟踪结果，避免捕获的 [currentState] 参数不可变导致死循环。
     *
     * 可见化改造：单次网络错误不再静默忽略，而是累计失败次数；连续失败达到
     * [MAX_POLL_FAILURES] 次后置 `matchStatus = "error"`，使错误暴露给 UI。
     */
    private suspend fun pollMatchStatus(
        playerId: String,
        updateState: (MenuViewState.() -> MenuViewState) -> Unit
    ) {
        var matched = false
        var failedCount = 0

        repeat(MAX_POLL_ROUNDS) {
            if (matched) return@repeat
            delay(POLL_INTERVAL_MS)

            when (val result = matchRepository.getMatchStatus(playerId)) {
                is ApiResult.Success -> {
                    failedCount = 0 // 成功一轮，重置连续失败计数
                    val statusResponse = result.data
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
                }
                is ApiResult.Error -> {
                    // 轮询单次错误：跳过本轮（退避），但累计失败，连续过多则暴露错误
                    failedCount++
                    if (failedCount >= MAX_POLL_FAILURES) {
                        matched = true
                        updateState { copy(matchStatus = "error") }
                    }
                }
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
     * 线程边界：[matchRepository.leaveMatch] 在 IO 执行；非关键路径，成功/失败均忽略。
     */
    fun handleLeaveMatch(scope: CoroutineScope, playerId: String) {
        scope.launch {
            when (matchRepository.leaveMatch(MatchJoinRequest(playerId))) {
                is ApiResult.Success -> { /* 非关键路径，忽略 */ }
                is ApiResult.Error -> { /* 离开匹配失败不影响本地状态，忽略 */ }
            }
        }
    }

    companion object {
        /** 轮询最大轮数（与原始实现一致）。 */
        private const val MAX_POLL_ROUNDS = 20

        /** 单次轮询间隔（毫秒）。 */
        private const val POLL_INTERVAL_MS = 1500L

        /** 连续失败达到该次数则判定为错误并暴露给 UI。 */
        private const val MAX_POLL_FAILURES = 3
    }
}
