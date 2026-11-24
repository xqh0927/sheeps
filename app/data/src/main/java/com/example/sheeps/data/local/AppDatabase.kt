package com.example.sheeps.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProgressEntity::class,
        BackpackItemEntity::class,
        UserProfileEntity::class,
        PendingItemConsumption::class,
        CarryRunReceipt::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localDao(): LocalDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS pending_item_consumptions (
                    userId TEXT NOT NULL, operationId TEXT NOT NULL, itemType TEXT NOT NULL,
                    quantity INTEGER NOT NULL, PRIMARY KEY(userId, operationId))""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS carry_run_receipts (
                    runId TEXT NOT NULL PRIMARY KEY, userId TEXT, carriedJson TEXT NOT NULL)""")
            }
        }
    }
}
