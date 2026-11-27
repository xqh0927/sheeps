package com.example.sheeps.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stage_saves")
data class StageSaveEntity(
    @PrimaryKey val ownerId: String,
    val runId: String,
    val levelId: Int,
    val payload: ByteArray
)
