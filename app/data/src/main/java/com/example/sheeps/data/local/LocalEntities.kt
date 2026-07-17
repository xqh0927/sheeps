package com.example.sheeps.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 关卡进度本地实体（离线优先）。
 *
 * 每次通关 / 解锁都会写入此表并标记 [isDirty] = true，由 [com.example.sheeps.data.repository.SyncRepository]
 * 在前后台切换或网络恢复时增量同步至云端；同步成功后再清空脏标记。
 */
@Entity(tableName = "user_progress")
data class UserProgressEntity(
    /** 关卡编号（主键）。下一个待解锁关卡也以 `levelId+1` 形式占位写入。 */
    @PrimaryKey val levelId: Int,
    /** 该关卡历史最高分 */
    val score: Int,
    /** 通关清盘用时（毫秒） */
    val clearTime: Long,
    /** 是否为待同步的脏数据（本地变更尚未上传云端） */
    val isDirty: Boolean,
    /** 最近一次写入的时间戳（毫秒），用于冲突排序 */
    val updateTimestamp: Long
)

/**
 * 道具背包本地实体（离线优先）。
 *
 * [itemType] 为道具标识符（如 UNDO / SHUFFLE），[isDirty] 标记本地存量变更待同步。
 */
@Entity(tableName = "backpack_items")
data class BackpackItemEntity(
    /** 道具类型标识符（主键，大写） */
    @PrimaryKey val itemType: String,
    /** 当前持有数量 */
    val count: Int,
    /** 是否为待同步的脏数据 */
    val isDirty: Boolean,
    /** 最近一次写入时间戳（毫秒） */
    val updateTimestamp: Long
)

/**
 * 用户资料本地实体（离线优先）。
 *
 * 与 MMKV 中的偏好（[com.example.sheeps.data.preference.UserPreferences]）互为冗余缓存，
 * 用于无网络时展示；[isDirty] 标记待上传的资料变更。
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    /** 用户唯一 ID（主键） */
    @PrimaryKey val userId: String,
    /** 昵称 */
    val username: String,
    /** 当前总积分 */
    val points: Int,
    /** 是否为待同步的脏数据 */
    val isDirty: Boolean,
    /** 最近一次写入时间戳（毫秒） */
    val updateTimestamp: Long
)
