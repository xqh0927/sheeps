package com.example.sheeps.menu.viewmodel.delegates

import com.apkfuns.logutils.LogUtils
import com.example.sheeps.core.R
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.local.BackpackItemEntity
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.local.UserProfileEntity
import com.example.sheeps.data.local.UserProgressEntity
import com.example.sheeps.data.model.ExchangeRequest
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.data.model.TaskClaimRequest
import com.example.sheeps.data.model.UnlockLevelRequest
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.menu.state.MenuViewEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max

/**
 * 社交与任务操作逻辑委派类
 * 处理签到、商店兑换、任务领奖、关卡解锁
 */
class SocialActionDelegate @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val localDao: LocalDao
) {
    /**
     * 处理每日签到
     */
    fun handleSignIn(
        scope: CoroutineScope,
        onComplete: () -> Unit,
        setEffect: (MenuViewEffect) -> Unit
    ) {
        val token = prefs.getToken() ?: return setEffect(MenuViewEffect.ShowLoginDialog)

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

                // 第2阶段：正常的 API 调用（绝不能用 NonCancellable 包裹，要允许网络取消！）
                val response = apiService.signIn("Bearer $token")

                if (!response.success) {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_sign_in_failed))
                    return@launch // 直接返回，会触发 finally 里的回滚
                }

                // 第3阶段：服务端签到成功，本地持久化关键数据（这里的极短写文件操作才使用 NonCancellable）
                withContext(NonCancellable) {
                    isConfirmedByServer = true

                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_sign_in_success, formatArgs = listOf(response.streak, response.reward_points)))
                    updateLocalPoints(response.current_points, false)
                    prefs.setPoints(response.current_points)
                    prefs.setTodaySigned(true)
                    prefs.setSignStreak(response.streak)
                }

                // 第4阶段：刷新页面（非关键操作，移出 NonCancellable）
                onComplete()

            } catch (e: CancellationException) {
                // 【遵循 Kotlin 协程规约】协程取消异常必须再次抛出，绝不能被普通的 Exception 吞掉
                LogUtils.w("SignIn cancelled by user/lifecycle")
                throw e
            } catch (e: Exception) {
                LogUtils.e("SignIn failed: ${e::class.simpleName} — ${e.message}", e)
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_sign_in_failed_retry))
            } finally {
                // 【核心安全网】如果走到这里时，服务端没有确认成功（网络报错、接口返回false、或者用户关闭页面导致协程取消）
                // 必须使用 NonCancellable 将预扣的 20 积分完美回滚！
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
     * 处理道具兑换
     */
    fun handleExchangeShopItem(
        scope: CoroutineScope,
        shopItemId: Int,
        count: Int,
        shopItems: List<ShopItem>,
        onComplete: () -> Unit,
        setEffect: (MenuViewEffect) -> Unit
    ) {
        val token = prefs.getToken() ?: return setEffect(MenuViewEffect.ShowLoginDialog)
        val item = shopItems.find { it.id == shopItemId }
            ?: return setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_item_not_found))

        val totalCost = item.points_price * count
        if (prefs.getPoints() < totalCost) {
            setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_points_insufficient))
            return
        }

        scope.launch {
            try {
                // 1. 本地扣减
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

                // 2. 同步云端
                val response =
                    apiService.exchangeItem("Bearer $token", ExchangeRequest(shopItemId, count))
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_exchange_success))
                    localDao.clearProfileDirty(prefs.getUserId())
                    localDao.clearItemDirty(item.item_type)
                } else {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_exchange_failed_cloud))
                    onComplete()
                }
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_exchange_offline))
            }
        }
    }

    /**
     * 处理任务领奖
     */
    fun handleClaimTask(
        scope: CoroutineScope,
        taskId: String,
        dailyTasks: List<com.example.sheeps.data.model.DailyTask>,
        onComplete: () -> Unit,
        setEffect: (MenuViewEffect) -> Unit
    ) {
        val token = prefs.getToken() ?: return setEffect(MenuViewEffect.ShowLoginDialog)
        val task = dailyTasks.find { it.task_id == taskId } ?: return

        if (!task.is_completed || task.is_rewarded) return

        scope.launch {
            try {
                // 本地积分入账
                val currentPoints = prefs.getPoints() + task.points_reward
                prefs.setPoints(currentPoints)
                updateLocalPoints(currentPoints, true)

                val response = apiService.claimTaskReward("Bearer $token", TaskClaimRequest(taskId))
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_task_claim_success))
                    localDao.clearProfileDirty(prefs.getUserId())
                    onComplete()
                } else {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_task_claim_failed))
                    onComplete()
                }
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_task_claim_offline))
            }
        }
    }

    /**
     * 使用积分手动解锁关卡
     */
    fun handleUnlockLevelWithPoints(
        scope: CoroutineScope,
        levelId: Int,
        onComplete: () -> Unit,
        setEffect: (MenuViewEffect) -> Unit
    ) {
        val token = prefs.getToken() ?: return setEffect(MenuViewEffect.ShowLoginDialog)

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
            try {
                // 本地解锁
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

                val response = apiService.unlockLevel("Bearer $token", UnlockLevelRequest(levelId))
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_unlock_success_level, formatArgs = listOf(levelId)))
                    localDao.clearProfileDirty(prefs.getUserId())
                    localDao.clearProgressDirty(levelId)
                } else {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_unlock_failed_cloud))
                    onComplete()
                }
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_unlock_offline))
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
