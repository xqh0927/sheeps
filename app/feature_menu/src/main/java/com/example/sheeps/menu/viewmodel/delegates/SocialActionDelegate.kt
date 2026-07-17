package com.example.sheeps.menu.viewmodel.delegates

import com.example.sheeps.ui.R
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.local.BackpackItemEntity
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.local.UserProfileEntity
import com.example.sheeps.data.local.UserProgressEntity
import com.example.sheeps.data.model.ExchangeRequest
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.data.model.TaskClaimRequest
import com.example.sheeps.data.model.UnlockLevelRequest
import com.example.sheeps.data.repository.UserRepository
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.data.result.ErrorMessageMapper
import com.example.sheeps.menu.state.MenuViewEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max

/**
 * 社交与任务操作逻辑委派类
 * 处理签到、商店兑换、任务领奖、关卡解锁。
 *
 * 重构要点（方案 2 / 统一错误闸门）：
 * - 改注入 [UserRepository]，不再直注 [com.example.sheeps.data.network.ApiService]。
 * - 网络结果统一经 [ApiResult] 分发（`when(result)`），**删除原 `catch(CancellationException)` 与
 *   `catch(Exception)`**；仅 [handleSignIn] 保留 `try/finally` 做积分回滚（本地资源清理，不吞网络异常）。
 * - 鉴权头由 [UserRepository] 内部统一构造，delegate 不再手写 `"Bearer $token"`。
 * - 错误文案统一经 [ErrorMessageMapper.toResId]，delegate 不再硬编码 Toast 字符串。
 *
 * 持有关系：由 [com.example.sheeps.menu.viewmodel.MenuViewModel] 注入并持有。
 * 一致性：多处采用「乐观更新 + 失败回滚」策略，保证本地积分与服务端最终一致。
 */
class SocialActionDelegate @Inject constructor(
    private val userRepository: UserRepository,
    private val prefs: UserPreferences,
    private val localDao: LocalDao
) {
    /**
     * 处理每日签到（乐观更新 + 失败回滚）。
     * @param scope 协程作用域（MenuViewModel.viewModelScope）。
     * @param onComplete 服务端确认成功后刷新页面的回调。
     * @param setEffect 派发签到成功/失败 Toast。
     *
     * 回滚不变量（与原实现等价）：
     * - 成功确认（[ApiResult.Success] 且服务端成功）→ [kotlinx.coroutines.withContext]([NonCancellable]) 持久化，
     *   [isConfirmedByServer] = true，finally 不回滚。
     * - 业务/网络失败（[ApiResult.Error]）→ [isConfirmedByServer] 仍为 false，finally 回滚。
     * - 协程被取消（[CancellationException]）→ [safeApiCall] 重抛，穿透 `when` 直达 finally 回滚，
     *   且取消异常继续向上传播（不吞）。
     */
    fun handleSignIn(
        scope: CoroutineScope,
        onComplete: () -> Unit,
        setEffect: (MenuViewEffect) -> Unit
    ) {
        val token = prefs.getToken() ?: return setEffect(MenuViewEffect.ShowLoginActivity)

        scope.launch {
            // 1. 记录原始积分，用于发生异常或协程被取消时回滚
            val originalPoints = prefs.getPoints()
            val optimisticPoints = originalPoints + 20

            // 标记服务端是否已成功处理
            var isConfirmedByServer = false

            try {
                // 第1阶段：本地预扣（乐观更新 UI）
                updateLocalPoints(optimisticPoints, true)
                prefs.setPoints(optimisticPoints)

                // 第2阶段：统一的网络请求（绝不包裹 try/catch；取消会被 safeApiCall 重抛）
                when (val result = userRepository.signIn()) {
                    is ApiResult.Success -> {
                        val response = result.data
                        // Repository 已把 !success 转为 ApiResult.Error，此处 response 必为成功态
                        // 第3阶段：服务端签到成功，本地持久化关键数据（用 NonCancellable 防取消导致积分丢失）
                        withContext(NonCancellable) {
                            isConfirmedByServer = true

                            setEffect(
                                MenuViewEffect.ShowToast(
                                    resId = R.string.toast_sign_in_success,
                                    formatArgs = listOf(response.streak, response.reward_points)
                                )
                            )
                            updateLocalPoints(response.current_points, false)
                            prefs.setPoints(response.current_points)
                            prefs.setTodaySigned(true)
                            prefs.setSignStreak(response.streak)
                        }

                        // 第4阶段：刷新页面（非关键操作，移出 NonCancellable）
                        onComplete()
                    }
                    is ApiResult.Error -> {
                        setEffect(MenuViewEffect.ShowToast(resId = ErrorMessageMapper.toResId(result.code)))
                    }
                }
            } finally {
                // 【核心安全网】服务端未确认成功（网络报错 / 业务失败 / 协程取消）→ 完美回滚预扣积分
                if (!isConfirmedByServer) {
                    withContext(NonCancellable) {
                        updateLocalPoints(originalPoints, false)
                        prefs.setPoints(originalPoints)
                    }
                }
            }
        }
    }

    /**
     * 处理道具兑换（本地先扣积分与入库，再同步云端）。
     * @param scope 协程作用域（MenuViewModel.viewModelScope）。
     * @param shopItemId 商城道具 ID。
     * @param count 兑换数量。
     * @param shopItems 当前商城列表（用于按 ID 查找单价与 item_type）。
     * @param onComplete 云端失败时刷新页面的回调。
     * @param setEffect 派发兑换成功/失败/积分不足 Toast。
     */
    fun handleExchangeShopItem(
        scope: CoroutineScope,
        shopItemId: Int,
        count: Int,
        shopItems: List<ShopItem>,
        onComplete: () -> Unit,
        setEffect: (MenuViewEffect) -> Unit
    ) {
        val token = prefs.getToken() ?: return setEffect(MenuViewEffect.ShowLoginActivity)
        val item = shopItems.find { it.id == shopItemId }
            ?: return setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_item_not_found))

        val totalCost = item.points_price * count
        if (prefs.getPoints() < totalCost) {
            setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_points_insufficient))
            return
        }

        scope.launch {
            // 1. 本地扣减（乐观更新，均为本地 IO）
            val currentPoints = prefs.getPoints() - totalCost
            prefs.setPoints(currentPoints)

            val localItem = localDao.getAllItems().find { it.itemType == item.item_type }
            val newCount = (localItem?.count ?: 0) + count

            updateLocalPoints(currentPoints, true)
            localDao.insertItem(
                BackpackItemEntity(
                    itemType = item.item_type,
                    count = newCount,
                    isDirty = true,
                    updateTimestamp = System.currentTimeMillis()
                )
            )

            // 2. 同步云端（统一错误闸门，不再手写 "Bearer $token"）
            when (val result = userRepository.exchangeItem(ExchangeRequest(shopItemId, count))) {
                is ApiResult.Success -> {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_exchange_success))
                    localDao.clearProfileDirty(prefs.getUserId())
                    localDao.clearItemDirty(item.item_type)
                }
                is ApiResult.Error -> {
                    // 云端失败/网络异常：保留本地乐观扣减（离线优先），提示并刷新
                    setEffect(MenuViewEffect.ShowToast(resId = ErrorMessageMapper.toResId(result.code)))
                    onComplete()
                }
            }
        }
    }

    /**
     * 处理任务领奖（本地先加分，再同步云端）。
     * @param scope 协程作用域（MenuViewModel.viewModelScope）。
     * @param taskId 任务 ID。
     * @param dailyTasks 当前任务列表（用于定位任务并校验已完成/未领奖）。
     * @param onComplete 完成后的刷新回调。
     * @param setEffect 派发领奖成功/失败 Toast。
     */
    fun handleClaimTask(
        scope: CoroutineScope,
        taskId: String,
        dailyTasks: List<com.example.sheeps.data.model.DailyTask>,
        onComplete: () -> Unit,
        setEffect: (MenuViewEffect) -> Unit
    ) {
        val token = prefs.getToken() ?: return setEffect(MenuViewEffect.ShowLoginActivity)
        val task = dailyTasks.find { it.task_id == taskId } ?: return

        if (!task.is_completed || task.is_rewarded) return

        scope.launch {
            // 本地积分入账（乐观更新，本地 IO）
            val currentPoints = prefs.getPoints() + task.points_reward
            prefs.setPoints(currentPoints)
            updateLocalPoints(currentPoints, true)

            // 同步云端（统一错误闸门）
            when (val result = userRepository.claimTaskReward(TaskClaimRequest(taskId))) {
                is ApiResult.Success -> {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_task_claim_success))
                    localDao.clearProfileDirty(prefs.getUserId())
                    onComplete()
                }
                is ApiResult.Error -> {
                    setEffect(MenuViewEffect.ShowToast(resId = ErrorMessageMapper.toResId(result.code)))
                    onComplete()
                }
            }
        }
    }

    /**
     * 使用积分手动解锁关卡（本地先扣积分并解锁，再同步云端）。
     * @param scope 协程作用域（MenuViewModel.viewModelScope）。
     * @param levelId 目标关卡 ID，费用按 ID 阶梯计算（2→50，3→100，其余→200）。
     * @param onComplete 云端失败时刷新回调。
     * @param setEffect 派发解锁成功/失败/积分不足 Toast。
     */
    fun handleUnlockLevelWithPoints(
        scope: CoroutineScope,
        levelId: Int,
        onComplete: () -> Unit,
        setEffect: (MenuViewEffect) -> Unit
    ) {
        val token = prefs.getToken() ?: return setEffect(MenuViewEffect.ShowLoginActivity)

        val cost = when (levelId) {
            2 -> 50
            3 -> 100
            else -> 200
        }

        if (prefs.getPoints() < cost) {
            setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_unlock_failed_insufficient))
            return
        }

        scope.launch {
            // 本地解锁（乐观更新，本地 IO）
            val currentPoints = prefs.getPoints() - cost
            prefs.setPoints(currentPoints)
            prefs.setUnlockedLevel(max(prefs.getUnlockedLevel(), levelId))

            val now = System.currentTimeMillis()
            updateLocalPoints(currentPoints, true, now)
            localDao.insertProgress(
                UserProgressEntity(
                    levelId = levelId,
                    score = 0,
                    clearTime = 0,
                    isDirty = true,
                    updateTimestamp = now
                )
            )

            // 同步云端（统一错误闸门）
            when (val result = userRepository.unlockLevel(UnlockLevelRequest(levelId))) {
                is ApiResult.Success -> {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_unlock_success_level, formatArgs = listOf(levelId)))
                    localDao.clearProfileDirty(prefs.getUserId())
                    localDao.clearProgressDirty(levelId)
                }
                is ApiResult.Error -> {
                    setEffect(MenuViewEffect.ShowToast(resId = ErrorMessageMapper.toResId(result.code)))
                    onComplete()
                }
            }
        }
    }

    private suspend fun updateLocalPoints(
        points: Int,
        isDirty: Boolean,
        timestamp: Long = System.currentTimeMillis()
    ) {
        localDao.insertProfile(
            UserProfileEntity(
                userId = prefs.getUserId(),
                username = prefs.getUsername(),
                points = points,
                isDirty = isDirty,
                updateTimestamp = timestamp
            )
        )
    }
}
