package com.example.sheeps.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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

    @Query("UPDATE backpack_items SET isDirty = 0 WHERE itemType = :itemType")
    /** 同步成功后清除指定道具的脏标记。 */
    suspend fun clearItemDirty(itemType: String): Int

    @Query("DELETE FROM backpack_items")
    /** 清空全部道具背包（云端覆盖前先删后插）。 */
    suspend fun deleteAllItems(): Int

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
