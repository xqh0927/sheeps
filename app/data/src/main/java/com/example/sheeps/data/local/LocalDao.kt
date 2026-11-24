package com.example.sheeps.data.local

import androidx.room.*
import com.example.sheeps.data.model.UserItem
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

/**
 * 本地数据访问对象（Room DAO）。
 *
 * 提供三块数据（关卡进度 / 道具背包 / 用户资料）的查询与写入，统一以 `isDirty` 字段标记
 * 离线脏数据，配合 [com.example.sheeps.data.repository.SyncRepository] 做端云增量同步。
 *
 * 线程约束：所有 `suspend` 方法由 Room 自动切换到 [kotlinx.coroutines.Dispatchers.IO] 执行，
 * 非 suspend 的 `observe*` 方法返回 [kotlinx.coroutines.flow.Flow]，在收集方所在线程发射数据，
 * 通常于主线程通过 `collectAsState`/`lifecycleScope` 收集（@MainThread）。
 */
@Dao
@JvmSuppressWildcards
interface LocalDao {

    // --- User Progress ---
    @Query("SELECT * FROM user_progress")
    /** 以 Flow 持续观察全部关卡进度（数据变更自动刷新 UI）。 */
    fun observeAllProgress(): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress")
    /** 一次性获取全部关卡进度（用于构建同步请求体）。 */
    suspend fun getAllProgress(): List<UserProgressEntity>

    @Query("SELECT * FROM user_progress WHERE isDirty = 1")
    /** 获取所有待同步的脏关卡进度（isDirty = 1）。 */
    suspend fun getDirtyProgress(): List<UserProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    /** 写入/替换单条关卡进度（冲突策略 REPLACE，按 levelId 覆盖）。 */
    suspend fun insertProgress(progress: UserProgressEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    /** 批量写入关卡进度（云端拉取覆盖时调用），返回各条 rowId。 */
    suspend fun insertProgressList(progressList: List<UserProgressEntity>): List<Long>

    @Query("UPDATE user_progress SET isDirty = 0 WHERE levelId = :levelId")
    /** 同步成功后清除指定关卡的脏标记（isDirty = 0）。 */
    suspend fun clearProgressDirty(levelId: Int): Int

    @Query("DELETE FROM user_progress")
    /** 清空全部关卡进度（云端覆盖前先删后插，保证本地与服务端一致）。 */
    suspend fun deleteAllProgress(): Int

    @Transaction
    suspend fun replaceProgress(progress: List<UserProgressEntity>) {
        deleteAllProgress()
        insertProgressList(progress)
    }

    // --- Backpack Items ---
    @Query("SELECT * FROM backpack_items")
    /** 以 Flow 持续观察全部道具背包。 */
    fun observeAllItems(): Flow<List<BackpackItemEntity>>

    @Query("SELECT * FROM backpack_items")
    /** 一次性获取全部道具背包。 */
    suspend fun getAllItems(): List<BackpackItemEntity>

    @Query("SELECT * FROM backpack_items WHERE isDirty = 1")
    /** 获取所有待同步的脏道具（isDirty = 1）。 */
    suspend fun getDirtyItems(): List<BackpackItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    /** 写入/替换单条道具存量。 */
    suspend fun insertItem(item: BackpackItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    /** 批量写入道具背包（云端拉取覆盖时调用）。 */
    suspend fun insertItemList(items: List<BackpackItemEntity>): List<Long>

    @Query("DELETE FROM backpack_items")
    /** 清空全部道具背包（云端覆盖前先删后插）。 */
    suspend fun deleteAllItems(): Int

    @Query("SELECT * FROM pending_item_consumptions WHERE userId = :userId ORDER BY rowid")
    suspend fun pendingConsumptions(userId: String): List<PendingItemConsumption>

    @Insert
    suspend fun enqueueConsumption(op: PendingItemConsumption)

    @Query("DELETE FROM pending_item_consumptions WHERE userId = :userId AND operationId IN (:ids)")
    suspend fun acknowledgeConsumptions(userId: String, ids: List<String>)

    // Version 1 could save a signed-in profile under the device UUID after a rename or a local score.
    @Query("""UPDATE OR IGNORE user_profile SET userId = :owner WHERE userId = :deviceId
               AND NOT EXISTS (SELECT 1 FROM user_profile WHERE userId != :deviceId)""")
    suspend fun adoptLegacyProfile(owner: String, deviceId: String)

    @Query("""DELETE FROM user_profile WHERE userId = :deviceId AND userId != :owner
               AND EXISTS (SELECT 1 FROM user_profile WHERE userId = :owner)""")
    suspend fun deleteLegacyProfile(owner: String, deviceId: String)

    @Transaction
    suspend fun repairLegacyProfile(owner: String, deviceId: String) {
        if (owner == deviceId) return
        val device = profileById(deviceId) ?: return
        val account = profileById(owner)
        if (account != null && device.isDirty && device.updateTimestamp > account.updateTimestamp) {
            insertProfile(account.copy(points = device.points, isDirty = true, updateTimestamp = device.updateTimestamp))
        }
        adoptLegacyProfile(owner, deviceId)
        deleteLegacyProfile(owner, deviceId)
    }

    @Query("SELECT * FROM carry_run_receipts WHERE runId = :runId")
    suspend fun carryReceipt(runId: String): CarryRunReceipt?

    @Insert
    suspend fun insertCarryReceipt(receipt: CarryRunReceipt)

    @Transaction
    suspend fun consumeForRun(
        runId: String, userId: String?, requested: Map<String, Int>, legacyDeviceId: String? = null
    ): Map<String, Int> {
        if (requested.isEmpty()) return emptyMap()
        val receipt = carryReceipt(runId)
        if (receipt != null) {
            return if (receipt.userId == userId) Json.decodeFromString(receipt.carriedJson) else emptyMap()
        }
        val carried = consumeCarryItems(userId, requested, legacyDeviceId)
        insertCarryReceipt(CarryRunReceipt(runId, userId, Json.encodeToString(carried)))
        return carried
    }

    @Transaction
    suspend fun consumeCarryItems(userId: String?, requested: Map<String, Int>, legacyDeviceId: String? = null): Map<String, Int> {
        if (userId != null && legacyDeviceId != null) repairLegacyProfile(userId, legacyDeviceId)
        if (userId != null && !hasProfile(userId)) return emptyMap()
        val stock = getAllItems().associateBy { it.itemType }
        val carried = mutableMapOf<String, Int>()
        for ((type, wanted) in requested) {
            val item = stock[type] ?: continue
            val quantity = minOf(wanted, item.count)
            if (quantity <= 0) continue
            insertItem(item.copy(count = item.count - quantity, isDirty = true, updateTimestamp = System.currentTimeMillis()))
            if (userId != null) enqueueConsumption(PendingItemConsumption(userId, UUID.randomUUID().toString(), type, quantity))
            carried[type] = quantity
        }
        return carried
    }

    @Transaction
    suspend fun replaceCloudInventory(userId: String, items: List<UserItem>, acknowledged: List<String> = emptyList()) {
        if (!hasProfile(userId)) return
        if (acknowledged.isNotEmpty()) acknowledgeConsumptions(userId, acknowledged)
        val pending = pendingConsumptions(userId).groupBy { it.itemType }
        val now = System.currentTimeMillis()
        val inventory = items.map { item ->
            val outstanding = pending[item.item_type].orEmpty().sumOf { it.quantity.toLong() }
            val count = (item.count - outstanding).coerceAtLeast(0).toInt()
            BackpackItemEntity(item.item_type, count, outstanding > 0, now)
        }
        deleteAllItems()
        insertItemList(inventory)
    }

    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    suspend fun profileById(userId: String): UserProfileEntity?

    @Query("UPDATE user_profile SET username = :name WHERE userId = :userId")
    suspend fun renameProfile(userId: String, name: String)

    @Query("SELECT EXISTS(SELECT 1 FROM user_profile WHERE userId = :userId)")
    suspend fun hasProfile(userId: String): Boolean

    // --- User Profile ---
    @Query("SELECT * FROM user_profile LIMIT 1")
    /** 以 Flow 持续观察当前用户资料（最多一条，取 LIMIT 1）。 */
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    /** 一次性获取当前用户资料（可能为 null）。 */
    suspend fun getProfile(): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE isDirty = 1")
    /** 获取所有待同步的脏用户资料（isDirty = 1）。 */
    suspend fun getDirtyProfiles(): List<UserProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    /** 写入/替换用户资料。 */
    suspend fun insertProfile(profile: UserProfileEntity): Long

    @Query("UPDATE user_profile SET isDirty = 0 WHERE userId = :userId")
    /** 同步成功后清除指定用户的资料脏标记。 */
    suspend fun clearProfileDirty(userId: String): Int

    @Query("DELETE FROM user_profile")
    /** 清空用户资料表（云端覆盖前先删后插）。 */
    suspend fun deleteProfile(): Int
}
