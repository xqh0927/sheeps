package com.example.sheeps.data.local

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressReplacementTest {
    @Test
    fun failedCloudReplacementKeepsLastDisplayedLevels() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.localDao()
            val previous = UserProgressEntity(68, 0, 0, false, 1)
            dao.insertProgress(previous)
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_update BEFORE INSERT ON user_progress " +
                    "WHEN NEW.levelId = 70 BEGIN SELECT RAISE(ABORT, 'interrupted update'); END"
            )
            try {
                dao.replaceProgress(listOf(previous.copy(levelId = 69), previous.copy(levelId = 70)))
                fail("Expected the interrupted replacement to roll back")
            } catch (_: SQLiteException) {
                assertEquals(listOf(previous), dao.getAllProgress())
            }
            dao.replaceProgress(emptyList())
            assertEquals(emptyList<UserProgressEntity>(), dao.getAllProgress())
        } finally {
            db.close()
        }
    }
}
