package com.example.sheeps.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface LocalDao {

    // --- User Progress ---
    @Query("SELECT * FROM user_progress")
    fun observeAllProgress(): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress")
    suspend fun getAllProgress(): List<UserProgressEntity>

    @Query("SELECT * FROM user_progress WHERE isDirty = 1")
    suspend fun getDirtyProgress(): List<UserProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: UserProgressEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressList(progressList: List<UserProgressEntity>): List<Long>

    @Query("UPDATE user_progress SET isDirty = 0 WHERE levelId = :levelId")
    suspend fun clearProgressDirty(levelId: Int): Int

    @Query("DELETE FROM user_progress")
    suspend fun deleteAllProgress(): Int

    // --- Backpack Items ---
    @Query("SELECT * FROM backpack_items")
    fun observeAllItems(): Flow<List<BackpackItemEntity>>

    @Query("SELECT * FROM backpack_items")
    suspend fun getAllItems(): List<BackpackItemEntity>

    @Query("SELECT * FROM backpack_items WHERE isDirty = 1")
    suspend fun getDirtyItems(): List<BackpackItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: BackpackItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemList(items: List<BackpackItemEntity>): List<Long>

    @Query("UPDATE backpack_items SET isDirty = 0 WHERE itemType = :itemType")
    suspend fun clearItemDirty(itemType: String): Int

    @Query("DELETE FROM backpack_items")
    suspend fun deleteAllItems(): Int

    // --- User Profile ---
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE isDirty = 1")
    suspend fun getDirtyProfiles(): List<UserProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity): Long

    @Query("UPDATE user_profile SET isDirty = 0 WHERE userId = :userId")
    suspend fun clearProfileDirty(userId: String): Int

    @Query("DELETE FROM user_profile")
    suspend fun deleteProfile(): Int
}
