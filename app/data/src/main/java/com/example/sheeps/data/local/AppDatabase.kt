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
        CarryRunReceipt::class,
        PendingStageStart::class,
        StageSaveEntity::class,
        PendingStageScore::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localDao(): LocalDao

    abstract fun stageSaveDao(): StageSaveDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_stage_scores ADD COLUMN abandoned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""CREATE TABLE IF NOT EXISTS pending_stage_starts (
                    ownerId TEXT NOT NULL, runId TEXT NOT NULL, levelId INTEGER NOT NULL,
                    layoutVersion TEXT NOT NULL, startedAt INTEGER NOT NULL, PRIMARY KEY(ownerId, runId))""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS stage_saves (
                    ownerId TEXT NOT NULL PRIMARY KEY, runId TEXT NOT NULL, levelId INTEGER NOT NULL, payload BLOB NOT NULL)""")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS pending_stage_scores (
                    ownerId TEXT NOT NULL, runId TEXT NOT NULL, levelId INTEGER NOT NULL, score INTEGER NOT NULL,
                    clearTimeMs INTEGER NOT NULL, itemsUsed INTEGER NOT NULL, won INTEGER NOT NULL,
                    completedAt INTEGER NOT NULL, PRIMARY KEY(ownerId, runId))""")
            }
        }

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
