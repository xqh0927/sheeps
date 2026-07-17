package com.example.sheeps.core.di

import android.content.Context
import androidx.room.Room
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.local.AppDatabase
import com.example.sheeps.data.local.LocalDao
import com.tencent.mmkv.MMKV
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 本地存储 Hilt 依赖注入模块。
 *
 * 提供进程级单例的 MMKV 实例、[UserPreferences]、Room 数据库 [AppDatabase] 及其 DAO。
 * 所有 @Provides 均标注 @Singleton，随应用进程生命周期存在。
 */
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    /** 提供全局默认 MMKV 实例（单例）。 */
    @Provides
    @Singleton
    fun provideMmkv(): MMKV = MMKV.defaultMMKV()

    /** 提供 UserPreferences 单例，注入 MMKV 与应用 Context。 */
    @Provides
    @Singleton
    fun provideUserPreferences(mmkv: MMKV, @ApplicationContext context: Context): UserPreferences = UserPreferences(mmkv, context)

    /** 提供 Room 数据库单例（sheeps_local.db），迁移采用 fallbackToDestructiveMigration。 */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sheeps_local.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    /** 提供本地数据访问 DAO 单例。 */
    @Provides
    @Singleton
    fun provideLocalDao(db: AppDatabase): LocalDao = db.localDao()
}
