package com.example.sheeps.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room 数据库定义（App 本地持久化根）。
 *
 * 包含三张表：关卡进度 [UserProgressEntity]、道具背包 [BackpackItemEntity]、用户资料 [UserProfileEntity]。
 * 所有写操作均通过 [LocalDao] 暴露，且统一在 [kotlinx.coroutines.Dispatchers.IO] 执行
 * （DAO 的 `suspend` 方法由 Room 自动切到 IO 线程）。
 *
 * 注意：`version = 1` 且 `exportSchema = false`，后续如需变更表结构须提升版本号并实现
 * Migration，否则会抛 IllegalStateException。单实例由 Hilt / DI 提供（见 [com.example.sheeps.core.di.StorageModule]）。
 */
@Database(
    entities = [
        UserProgressEntity::class,
        BackpackItemEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localDao(): LocalDao
}
