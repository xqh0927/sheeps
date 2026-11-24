package com.example.sheeps.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CarryRunTest {
    @Test
    fun restoringAnAcknowledgedRunAfterDatabaseReopenDoesNotChargeAgain() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = "carry-${UUID.randomUUID()}.db"
        val request = mapOf("UNDO" to 4)
        suspend fun open(block: suspend (LocalDao) -> Unit) {
            val db = Room.databaseBuilder(context, AppDatabase::class.java, file).build()
            try { block(db.localDao()) } finally { db.close() }
        }
        try {
            open { dao ->
                dao.insertProfile(UserProfileEntity("alice", "Alice", 0, false, 1))
                dao.insertItem(BackpackItemEntity("UNDO", 2, false, 1))
                assertEquals(mapOf("UNDO" to 2), dao.consumeForRun("first", "alice", request))
                dao.acknowledgeConsumptions("alice", dao.pendingConsumptions("alice").map { it.operationId })
                dao.insertItem(BackpackItemEntity("UNDO", 5, false, 2))
            }
            open { dao ->
                assertEquals(mapOf("UNDO" to 2), dao.consumeForRun("first", "alice", request))
                assertEquals(5, dao.getAllItems().single().count)
                assertEquals(0, dao.pendingConsumptions("alice").size)
                assertEquals(request, dao.consumeForRun("replay", "alice", request))
                assertEquals(1, dao.getAllItems().single().count)
                assertEquals(1, dao.pendingConsumptions("alice").size)
            }
        } finally { context.deleteDatabase(file) }
        Unit
    }

    @Test
    fun concurrentGuestRestoresShareOneAllocation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.localDao()
            dao.insertItem(BackpackItemEntity("HINT", 3, false, 1))
            val requested = mapOf("HINT" to 1)
            val allocations = List(2) { async { dao.consumeForRun("guest-run", null, requested) } }.awaitAll()
            assertEquals(listOf(requested, requested), allocations)
            assertEquals(2, dao.getAllItems().single().count)
            assertEquals(emptyMap<String, Int>(), dao.consumeForRun("guest-run", "alice", requested))
            assertEquals(2, dao.getAllItems().single().count)
        } finally { db.close() }
        Unit
    }
}
