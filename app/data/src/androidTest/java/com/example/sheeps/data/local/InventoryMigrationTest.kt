package com.example.sheeps.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sheeps.data.model.UserItem
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryMigrationTest {
    @Test
    fun upgradeThenRetryConsumptionWithAnotherAccountAndNewLocalUse() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4).build()
        suspend fun withDatabase(block: suspend (AppDatabase) -> Unit) {
            val db = open()
            try { block(db) } finally { db.close() }
        }
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
                old.execSQL("""CREATE TABLE user_progress (levelId INTEGER NOT NULL PRIMARY KEY,
                    score INTEGER NOT NULL, clearTime INTEGER NOT NULL, isDirty INTEGER NOT NULL, updateTimestamp INTEGER NOT NULL)""")
                old.execSQL("""CREATE TABLE backpack_items (itemType TEXT NOT NULL PRIMARY KEY,
                    count INTEGER NOT NULL, isDirty INTEGER NOT NULL, updateTimestamp INTEGER NOT NULL)""")
                old.execSQL("""CREATE TABLE user_profile (userId TEXT NOT NULL PRIMARY KEY,
                    username TEXT NOT NULL, points INTEGER NOT NULL, isDirty INTEGER NOT NULL, updateTimestamp INTEGER NOT NULL)""")
                old.execSQL("INSERT INTO user_profile VALUES ('device-uuid', 'Alice', 37, 0, 1)")
                old.execSQL("INSERT INTO backpack_items VALUES ('UNDO', 3, 0, 1)")
                old.version = 1
            }
            withDatabase { db ->
                val dao = db.localDao()
                assertEquals(37, dao.getProfile()!!.points)
                val start = PendingStageStart("alice", "paused", 7, "generated-v5", 1000L)
                dao.enqueueStageStart(start)
                dao.enqueueStageStart(start.copy(startedAt = 9000L))
                db.stageSaveDao().begin(StageSaveEntity("alice", "paused", 7, byteArrayOf(1, 2)))
                for (level in 1..3) {
                    dao.recordStageResult(PendingStageScore("alice", "run-$level", level, 100, 1000, 0, true, 1000L + level), true)
                }
                dao.recordStageResult(PendingStageScore("alice", "run-1", 1, 100, 1000, 0, true, 1001), true)
                assertEquals(mapOf("UNDO" to 1), dao.consumeCarryItems("alice", mapOf("UNDO" to 1), "device-uuid"))
                assertTrue(dao.hasProfile("alice"))
                dao.insertProfile(UserProfileEntity("device-uuid", "Alice", 51, true, 5))
                dao.repairLegacyProfile("alice", "device-uuid")
                dao.renameProfile("alice", "renamed")
                assertEquals(51, dao.profileById("alice")!!.points)
                assertTrue(dao.profileById("alice")!!.isDirty)
                assertEquals("renamed", dao.profileById("alice")!!.username)
                assertEquals(2, dao.getAllItems().single().count)
            }
            withDatabase { db ->
                val dao = db.localDao()
                assertEquals(1000L, dao.pendingStageStarts("alice").single().startedAt)
                assertTrue(dao.pendingStageStarts("bob").isEmpty())
                assertEquals("paused", db.stageSaveDao().latest("alice")?.id)
                assertEquals(listOf("alice"), dao.pendingUploadOwners())
                assertEquals(listOf("run-1", "run-2", "run-3"), dao.pendingScores("alice").map { it.runId })
                assertTrue(dao.pendingScores("bob").isEmpty())
                dao.replaceCloudProgress("alice", emptyList())
                assertEquals(setOf(1, 2, 3, 4), dao.getAllProgress().map { it.levelId }.toSet())
                dao.acknowledgeScore("bob", "run-1")
                assertEquals(3, dao.pendingScores("alice").size)
                dao.acknowledgeScore("alice", "run-1")
                assertEquals(2, dao.pendingScores("alice").size)
                val first = dao.pendingConsumptions("alice").single().operationId
                dao.deleteProfile()
                dao.insertProfile(UserProfileEntity("bob", "Bob", 0, false, 1))
                dao.replaceCloudInventory("bob", listOf(UserItem("UNDO", 5)))
                dao.replaceCloudInventory("alice", listOf(UserItem("UNDO", 9)), listOf(first))
                assertEquals(5, dao.getAllItems().single().count)
                assertTrue(dao.pendingConsumptions("bob").isEmpty())
                assertEquals(first, dao.pendingConsumptions("alice").single().operationId)

                dao.deleteProfile()
                dao.insertProfile(UserProfileEntity("alice", "Alice", 37, false, 1))
                dao.replaceCloudInventory("alice", listOf(UserItem("UNDO", 3)))
                assertEquals(2, dao.getAllItems().single().count)
                dao.consumeCarryItems("alice", mapOf("UNDO" to 1))
                val second = dao.pendingConsumptions("alice").last().operationId
                // A different device bought two items. The server has acknowledged only the first local use.
                repeat(2) { dao.replaceCloudInventory("alice", listOf(UserItem("UNDO", 4)), listOf(first)) }
                assertEquals(3, dao.getAllItems().single().count)
                assertEquals(listOf(second), dao.pendingConsumptions("alice").map { it.operationId })
                dao.replaceCloudInventory("alice", listOf(UserItem("UNDO", 3)), listOf(second))
                assertEquals(3, dao.getAllItems().single().count)
                assertTrue(dao.pendingConsumptions("alice").isEmpty())
                assertEquals(mapOf("UNDO" to 3), dao.consumeCarryItems("alice", mapOf("UNDO" to 8)))
                assertEquals(0, dao.getAllItems().single().count)
            }
        } finally {
            context.deleteDatabase(name)
        }
        Unit
    }
}
