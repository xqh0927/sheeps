package com.example.sheeps.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val levelId: Int,
    val score: Int,
    val clearTime: Long,
    val isDirty: Boolean,
    val updateTimestamp: Long
)

@Entity(tableName = "backpack_items")
data class BackpackItemEntity(
    @PrimaryKey val itemType: String,
    val count: Int,
    val isDirty: Boolean,
    val updateTimestamp: Long
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val points: Int,
    val isDirty: Boolean,
    val updateTimestamp: Long
)
