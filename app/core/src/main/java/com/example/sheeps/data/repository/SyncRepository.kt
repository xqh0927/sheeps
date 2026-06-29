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

@Singleton
class SyncRepository @Inject constructor(
    private val apiService: ApiService,
    private val localDao: LocalDao,
    private val userPreferences: UserPreferences,
    private val networkMonitor: NetworkMonitor
) {

    // Get current token for manual passing if needed (or rely on authInterceptor)
    private fun getAuthHeader(): String {
        val token = userPreferences.getToken()
        return if (token != null) "Bearer $token" else ""
    }

    /**
     * Sync local dirty data to the cloud.
     */
    suspend fun syncDirtyData() = withContext(Dispatchers.IO) {
        if (!userPreferences.isLoggedIn()) return@withContext
        if (!networkMonitor.isOnline()) return@withContext

        try {
            val dirtyProgress = localDao.getDirtyProgress()
            val dirtyItems = localDao.getDirtyItems()
            val dirtyProfiles = localDao.getDirtyProfiles()

            if (dirtyProgress.isEmpty() && dirtyItems.isEmpty() && dirtyProfiles.isEmpty()) {
                return@withContext
            }

            LogUtils.d("Syncing dirty local data: ${dirtyProgress.size} progress, ${dirtyItems.size} items, ${dirtyProfiles.size} profiles")

            // 1. Compile request
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

            // 2. Call cloud sync api
            val response = apiService.syncData(
                auth = getAuthHeader(),
                request = request
            )

            if (response.success) {
                // 3. Clear dirty flags locally
                dirtyProgress.forEach { localDao.clearProgressDirty(it.levelId) }
                dirtyItems.forEach { localDao.clearItemDirty(it.itemType) }
                if (dirtyProfiles.isNotEmpty()) {
                    localDao.clearProfileDirty(userPreferences.getUserId())
                }
                LogUtils.d("Sync successfully completed, cleared local dirty flags.")
            }
        } catch (e: Exception) {
            LogUtils.e("Sync failed: ${e.message}", e)
        }
    }

    /**
     * Save level clear score locally (first) and trigger sync background.
     */
    suspend fun saveProgressAndPointsLocally(levelId: Int, score: Int, clearTime: Long, pointsGained: Int) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        
        // 1. Insert progress
        val progress = UserProgressEntity(
            levelId = levelId,
            score = score,
            clearTime = clearTime,
            isDirty = true,
            updateTimestamp = now
        )
        localDao.insertProgress(progress)

        // 2. Unlock next level locally
        val nextLevelProgress = UserProgressEntity(
            levelId = levelId + 1,
            score = 0,
            clearTime = 0,
            isDirty = true,
            updateTimestamp = now
        )
        localDao.insertProgress(nextLevelProgress)

        // 3. Update points
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

        // 4. Trigger sync in background
        syncDirtyData()
    }

    /**
     * Save item count locally (first) and trigger sync background.
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

        // Trigger sync
        syncDirtyData()
    }

    /**
     * Download the entire cloud state and populate local database (overwriting existing).
     */
    suspend fun pullCloudProfile(): UserProfileResponse? = withContext(Dispatchers.IO) {
        if (!userPreferences.isLoggedIn()) return@withContext null
        try {
            val response = apiService.getUserProfile(getAuthHeader())
            if (response.success && response.user != null) {
                val now = System.currentTimeMillis()
                
                // Write user details
                userPreferences.setPoints(response.user.points)
                userPreferences.setUsername(response.user.username)
                
                val profile = UserProfileEntity(
                    userId = response.user.id,
                    username = response.user.username,
                    points = response.user.points,
                    isDirty = false,
                    updateTimestamp = now
                )
                localDao.insertProfile(profile)

                // Write levels
                val progressList = response.unlocked_levels.map {
                    UserProgressEntity(
                        levelId = it,
                        score = 0, // placeholder since leaderboard is fetched separately
                        clearTime = 0,
                        isDirty = false,
                        updateTimestamp = now
                    )
                }
                localDao.deleteAllProgress()
                localDao.insertProgressList(progressList)

                // Write items
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
            LogUtils.e("Failed to pull cloud profile: ${e.message}", e)
        }
        null
    }
}
