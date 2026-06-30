package com.example.sheeps.menu.viewmodel.delegates

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
import kotlinx.coroutines.launch
import javax.inject.Inject
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
            try {
                // 1. 本地先行：增加预估积分
                val currentPoints = prefs.getPoints() + 20
                updateLocalPoints(currentPoints, true)
                prefs.setPoints(currentPoints)

                // 2. 异步同步云端
                val response = apiService.signIn("Bearer $token")
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast("签到成功！连续签到第 ${response.streak} 天，获得 ${response.reward_points} 积分！"))
                    updateLocalPoints(response.current_points, false)
                    prefs.setPoints(response.current_points)
                    prefs.setTodaySigned(true)
                    prefs.setSignStreak(response.streak)
                } else {
                    setEffect(MenuViewEffect.ShowToast("签到失败"))
                }
                onComplete()
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast("连接异常，积分已本地入账，后续将自动同步"))
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
        val item = shopItems.find { it.id == shopItemId } ?: return setEffect(MenuViewEffect.ShowToast("道具未找到"))

        val totalCost = item.points_price * count
        if (prefs.getPoints() < totalCost) {
            setEffect(MenuViewEffect.ShowToast("兑换失败，积分余额不足"))
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
                val response = apiService.exchangeItem("Bearer $token", ExchangeRequest(shopItemId, count))
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast("兑换成功，已加入背包！"))
                    localDao.clearProfileDirty(prefs.getUserId())
                    localDao.clearItemDirty(item.item_type)
                } else {
                    setEffect(MenuViewEffect.ShowToast("云端兑换失败，回滚本地扣减"))
                    onComplete()
                }
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast("连接受限，积分本地扣减，已记录入背包，稍后将自动同步"))
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
                    setEffect(MenuViewEffect.ShowToast("任务奖励领取成功！积分已增加"))
                    localDao.clearProfileDirty(prefs.getUserId())
                    onComplete()
                } else {
                    setEffect(MenuViewEffect.ShowToast("领奖失败"))
                    onComplete()
                }
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast("网络连通受限，积分已加入本地，稍后同步"))
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
        
        val cost = when(levelId) {
            2 -> 50
            3 -> 100
            else -> 200
        }
        
        if (prefs.getPoints() < cost) {
            setEffect(MenuViewEffect.ShowToast("解锁失败，积分余额不足"))
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
                    UserProgressEntity(levelId = levelId, score = 0, clearTime = 0, isDirty = true, updateTimestamp = now)
                )

                val response = apiService.unlockLevel("Bearer $token", UnlockLevelRequest(levelId))
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast("积分子系统扣除成功！第 ${levelId} 关已解锁"))
                    localDao.clearProfileDirty(prefs.getUserId())
                    localDao.clearProgressDirty(levelId)
                } else {
                    setEffect(MenuViewEffect.ShowToast("解锁失败，云端同步拒绝"))
                    onComplete()
                }
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast("连接不畅，已扣减积分本地解锁，后续有网自动同步"))
            }
        }
    }

    private suspend fun updateLocalPoints(points: Int, isDirty: Boolean, timestamp: Long = System.currentTimeMillis()) {
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
