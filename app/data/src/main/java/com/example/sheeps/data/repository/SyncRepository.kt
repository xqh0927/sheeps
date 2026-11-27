package com.example.sheeps.data.repository

import com.apkfuns.logutils.LogUtils
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.utils.NetworkMonitor
import com.example.sheeps.data.local.*
import com.example.sheeps.data.model.*
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.data.result.TokenRefresher
import com.example.sheeps.data.network.ApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val networkMonitor: NetworkMonitor,
    private val tokenRefresher: TokenRefresher,
    private val stageScores: StageScoreRepository
) {

    private val cloudInventory = Mutex()
    val confirmedStageScores: Long get() = stageScores.confirmationCount

    /**
     * 将本地标记为脏数据 (isDirty = true) 的离线操作同步上传至云端 D1 数据库。
     * 同步成功后，会自动清除本地 Room 对应的脏数据标记，保证端云最终一致性。
     */
    suspend fun syncDirtyData() = cloudInventory.withLock {
        stageScores.flush(stageScores.currentOwner())
        val token = userPreferences.getToken() ?: return@withLock
        val owner = userPreferences.accountId(token) ?: return@withLock
        localDao.repairLegacyProfile(owner, userPreferences.getUserId())
        if (!networkMonitor.isOnline() || !localDao.hasProfile(owner)) return@withLock
        val dirtyProgress = localDao.getDirtyProgress()
        val dirtyProfiles = localDao.getDirtyProfiles().filter { it.userId == owner }
        val pending = localDao.pendingConsumptions(owner)
        if (pending.isEmpty() && dirtyProgress.isEmpty() && dirtyProfiles.isEmpty() &&
            localDao.getDirtyItems().isEmpty()) return@withLock

        try {
            val batches = pending.chunked(40).ifEmpty { listOf(emptyList()) }
            for ((index, ops) in batches.withIndex()) {
                val request = SyncRequest(
                    points = if (index == 0 && dirtyProfiles.isNotEmpty()) userPreferences.getPoints() else 0,
                    unlocked_levels = if (index == 0) dirtyProgress.map { it.levelId } else emptyList(),
                    consumptions = ops.map { ItemConsumption(it.operationId, it.itemType, it.quantity) }
                )
                val result = when (val call = tokenRefresher.authenticated(token) { auth -> apiService.syncData(auth, request) }) {
                    is ApiResult.Success -> call.data
                    is ApiResult.Error -> return@withLock
                }
                if (!result.success || result.user.id != owner || userPreferences.accountId() != owner) return@withLock
                val confirmed = result.acknowledged_consumptions.intersect(ops.map { it.operationId }.toSet())
                if (ops.isNotEmpty() && confirmed.isEmpty()) return@withLock // Older Worker: keep the durable operations.
                localDao.replaceCloudInventory(owner, result.items, confirmed.toList())
                if (index == 0) {
                    dirtyProgress.forEach { localDao.clearProgressDirty(it.levelId) }
                    dirtyProfiles.forEach { localDao.clearProfileDirty(it.userId) }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            LogUtils.e("端云数据同步失败: ${e.message}")
        }
    }

    /**
     * 从云端下载最新权威的用户资料并完全覆盖刷新本地 Room 数据库（常用于重新登录或网络切换时的最终对齐）。
     * 
     * @return 返回获取到的最新 UserProfileResponse 对象，失败时返回 null
     */
    suspend fun pullCloudProfile(): UserProfileResponse? = cloudInventory.withLock {
        val token = userPreferences.getToken() ?: return@withLock null
        val owner = userPreferences.accountId(token) ?: return@withLock null
        try {
            val response = when (val call = tokenRefresher.authenticated(token, apiService::getUserProfile)) {
                is ApiResult.Success -> call.data
                is ApiResult.Error -> return@withLock null
            }
            if (response.success && response.user?.id == owner && userPreferences.accountId() == owner) {
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
                localDao.replaceCloudProgress(owner, progressList)

                // A missing acknowledgement may mean that D1 already applied the debit.
                // Preserve the local view until sync retries the exact same operations.
                if (localDao.pendingConsumptions(owner).isEmpty()) {
                    localDao.replaceCloudInventory(owner, response.items)
                }

                return@withLock response
            }
        } catch (e: Exception) {
            LogUtils.e("拉取云端用户资料并对齐失败: ${e.message}", e)
        }
        null
    }
}
