package com.example.sheeps.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StageSaveTransactionTest {
    @Test fun failedReplacementPreservesPreviousBoardInventoryAndBothOutboxes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val stages = db.stageSaveDao()
            val inventory = db.localDao()
            stages.begin(StageSaveEntity("guest", "old", 4, byteArrayOf(1)))
            inventory.insertItem(BackpackItemEntity("UNDO", 2, false, 1))
            val abandoned = PendingStageScore("guest", "old", 4, 0, 245, 1, false, 950, abandoned = true)
            suspend fun prepare() {
                inventory.recordStageResult(abandoned, true)
                inventory.consumeForRun("new", null, mapOf("UNDO" to 1))
                inventory.enqueueStageStart(PendingStageStart("guest", "new", 5, "published-1", 1000))
            }

            val failure = runCatching {
                stages.replaceRun { prepare(); error("Interrupted before the new snapshot") }
            }
            assertTrue(failure.isFailure)
            assertEquals("old", stages.latest("guest")!!.id)
            assertEquals(2, inventory.getAllItems().single().count)
            assertNull(inventory.carryReceipt("new"))
            assertTrue(inventory.pendingScores("guest").isEmpty())
            assertTrue(inventory.pendingStageStarts("guest").isEmpty())

            stages.replaceRun {
                prepare()
                StageSaveEntity("guest", "new", 5, byteArrayOf(2))
            }
            assertEquals("new", stages.latest("guest")!!.id)
            assertEquals(1, inventory.getAllItems().single().count)
            assertEquals(abandoned, inventory.pendingScores("guest").single())
            assertEquals("new", inventory.pendingStageStarts("guest").single().runId)
            val result = PendingStageScore("guest", "new", 5, 100, 5000, 0, true, 1200)
            inventory.recordStageResult(result, true)
            stages.checkpoint("guest", "new", byteArrayOf(3))
            assertNull(stages.latest("guest"))
            assertEquals(result, inventory.pendingScores("guest").last())
        } finally { db.close() }
    }
}
