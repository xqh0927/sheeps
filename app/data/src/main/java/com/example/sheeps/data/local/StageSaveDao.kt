package com.example.sheeps.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import androidx.room.Query
import com.example.sheeps.data.repository.ResumableStage
import kotlinx.coroutines.flow.Flow

@Dao
interface StageSaveDao {
    @Query("SELECT runId AS id, levelId AS level FROM stage_saves WHERE ownerId = :owner")
    fun observeLatest(owner: String): Flow<ResumableStage?>

    @Query("SELECT runId AS id, levelId AS level FROM stage_saves WHERE ownerId = :owner")
    suspend fun latest(owner: String): ResumableStage?

    @Query("SELECT * FROM stage_saves WHERE ownerId = :owner AND runId = :id")
    suspend fun read(owner: String, id: String): StageSaveEntity?

    // Run allocation, prior result and opening receipt are all in the same Room database.
    @Transaction
    suspend fun replaceRun(create: suspend () -> StageSaveEntity) {
        begin(create())
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun begin(row: StageSaveEntity)

    @Query("UPDATE stage_saves SET payload = :payload WHERE ownerId = :owner AND runId = :id")
    suspend fun checkpoint(owner: String, id: String, payload: ByteArray)

    @Query("DELETE FROM stage_saves WHERE ownerId = :owner AND runId = :id")
    suspend fun discard(owner: String, id: String)
}
