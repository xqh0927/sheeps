package com.example.sheeps.data.repository

import com.apkfuns.logutils.LogUtils
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.core.utils.NetworkMonitor
import com.example.sheeps.data.local.*
import com.example.sheeps.data.model.*
import com.example.sheeps.data.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 离线优先同步数据仓库 (SyncRepository)
 * 负责本地 Room 数据库的增量保存、脏数据标记、前后台静默同步至云端 D1 以及拉取覆盖云端权威状态。
 */
@Singleton
class SyncRepository @Inject constructor(
    private val apiService: ApiService,
    private val localDao: LocalDao,
    private val userPreferences: UserPreferences,
    private val networkMonitor: NetworkMonitor
) {

    /**
     * 获取当前用户的 Bearer 授权请求头
     * @return 返回格式化的 Authorization 请求头字符串，若未登录则返回空串
     */
    private fun getAuthHeader(): String {
        val token = userPreferences.getToken()
        return if (token != null) "Bearer $token" else ""
    }

    /**
     * 将本地标记为脏数据 (isDirty = true) 的离线操作同步上传至云端 D1 数据库。
     * 同步成功后，会自动清除本地 Room 对应的脏数据标记，保证端云最终一致性。
     */
    suspend fun syncDirtyData() = withContext(Dispatchers.IO) {
        if (!userPreferences.isLoggedIn()) return@withContext
        if (!networkMonitor.isOnline()) return@withContext

        try {
            // 获取所有本地发生变更但尚未同步的脏数据条目
            val dirtyProgress = localDao.getDirtyProgress()
            val dirtyItems = localDao.getDirtyItems()
            val dirtyProfiles = localDao.getDirtyProfiles()

            if (dirtyProgress.isEmpty() && dirtyItems.isEmpty() && dirtyProfiles.isEmpty()) {
                return@withContext
            }

            LogUtils.d("开始同步本地离线脏数据: ${dirtyProgress.size} 条关卡进度, ${dirtyItems.size} 个道具, ${dirtyProfiles.size} 个个人资料")

            // 1. 构建云端同步请求体
            val currentPoints = userPreferences.getPoints()
            val allProgress = localDao.getAllProgress()
            val unlockedLevels = allProgress.map { it.levelId }

            val allLocalItems = localDao.getAllItems()
            val userItemsList = allLocalItems.map { UserItem(it.itemType, it.count) }

            val request = SyncRequest(
                points = currentPoints,
                unlocked_levels = unlockedLevels,
                items = userItemsList
            )

            // 2. 调用云端 API 发送同步数据负载
            val response = apiService.syncData(
                auth = getAuthHeader(),
                request = request
            )

            if (response.success) {
                // 3. 成功后，原子清除本地的脏数据标记 (isDirty 置为 false)
                dirtyProgress.forEach { localDao.clearProgressDirty(it.levelId) }
                dirtyItems.forEach { localDao.clearItemDirty(it.itemType) }
                if (dirtyProfiles.isNotEmpty()) {
                    localDao.clearProfileDirty(userPreferences.getUserId())
                }
                LogUtils.d("端云数据同步成功，已清除本地 Room 脏数据标记。")
            }
        } catch (e: Exception) {
            LogUtils.e("端云数据同步失败: ${e.message}", e)
        }
    }

    /**
     * 将玩家最新的关卡结算成绩与积分先本地安全写入 Room（离线优先），随后发起后台静默同步。
     * 
     * @param levelId 通关的关卡 ID
     * @param score 本次通关积分/分数
     * @param clearTime 关卡清盘通关用时 (毫秒)
     * @param pointsGained 本次通关奖励的积分数
     */
    suspend fun saveProgressAndPointsLocally(levelId: Int, score: Int, clearTime: Long, pointsGained: Int) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        
        // 1. 写入通关关卡记录
        val progress = UserProgressEntity(
            levelId = levelId,
            score = score,
            clearTime = clearTime,
            isDirty = true,
            updateTimestamp = now
        )
        localDao.insertProgress(progress)

        // 2. 本地自动解锁下一关卡
        val nextLevelProgress = UserProgressEntity(
            levelId = levelId + 1,
            score = 0,
            clearTime = 0,
            isDirty = true,
            updateTimestamp = now
        )
        localDao.insertProgress(nextLevelProgress)

        // 3. 累加并同步更新本地用户总积分
        val currentPoints = userPreferences.getPoints() + pointsGained
        userPreferences.setPoints(currentPoints)
        
        val profile = UserProfileEntity(
            userId = userPreferences.getUserId(),
            username = userPreferences.getUsername(),
            points = currentPoints,
            isDirty = true,
            updateTimestamp = now
        )
        localDao.insertProfile(profile)

        // 4. 发起后台异步网络数据同步
        syncDirtyData()
    }

    /**
     * 将玩家最新的法宝道具存量写入本地 Room，并触发后台同步。
     * 
     * @param itemType 道具的标识符 (如 'UNDO', 'SHUFFLE')
     * @param newCount 扣减或增加后最新的道具拥有量
     */
    suspend fun saveItemLocally(itemType: String, newCount: Int) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val item = BackpackItemEntity(
            itemType = itemType,
            count = newCount,
            isDirty = true,
            updateTimestamp = now
        )
        localDao.insertItem(item)

        // 触发数据同步
        syncDirtyData()
    }

    /**
     * 从云端下载最新权威的用户资料并完全覆盖刷新本地 Room 数据库（常用于重新登录或网络切换时的最终对齐）。
     * 
     * @return 返回获取到的最新 UserProfileResponse 对象，失败时返回 null
     */
    suspend fun pullCloudProfile(): UserProfileResponse? = withContext(Dispatchers.IO) {
        if (!userPreferences.isLoggedIn()) return@withContext null
        try {
            val response = apiService.getUserProfile(getAuthHeader())
            if (response.success && response.user != null) {
                val now = System.currentTimeMillis()
                
                // 1. 同步覆盖本地 SharedPreferences (MMKV) 用户状态数据
                userPreferences.setPoints(response.user.points)
                userPreferences.setUsername(response.user.username)
                userPreferences.setTodaySigned(response.today_signed)
                userPreferences.setSignStreak(response.sign_streak)
                userPreferences.setHighestLevelCleared(response.highest_level_cleared)
                
                val profile = UserProfileEntity(
                    userId = response.user.id,
                    username = response.user.username,
                    points = response.user.points,
                    isDirty = false,
                    updateTimestamp = now
                )
                localDao.insertProfile(profile)

                // 2. 同步覆盖本地已解锁的关卡列表
                val progressList = response.unlocked_levels.map {
                    UserProgressEntity(
                        levelId = it,
                        score = 0, // 占位符，关卡高分排行榜将在独立界面按需查询
                        clearTime = 0,
                        isDirty = false,
                        updateTimestamp = now
                    )
                }
                localDao.deleteAllProgress()
                localDao.insertProgressList(progressList)

                // 3. 同步覆盖本地背包道具余量
                val itemList = response.items.map {
                    BackpackItemEntity(
                        itemType = it.item_type,
                        count = it.count,
                        isDirty = false,
                        updateTimestamp = now
                    )
                }
                localDao.deleteAllItems()
                localDao.insertItemList(itemList)

                return@withContext response
            }
        } catch (e: Exception) {
            LogUtils.e("拉取云端用户资料并对齐失败: ${e.message}", e)
        }
        null
    }
}
