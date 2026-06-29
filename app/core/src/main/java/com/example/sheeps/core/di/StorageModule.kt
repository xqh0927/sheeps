package com.example.sheeps.core.di

import android.content.Context
import androidx.room.Room
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.local.AppDatabase
import com.example.sheeps.data.local.LocalDao
import com.tencent.mmkv.MMKV
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideMmkv(): MMKV = MMKV.defaultMMKV()

    @Provides
    @Singleton
    fun provideUserPreferences(mmkv: MMKV, @ApplicationContext context: Context): UserPreferences = UserPreferences(mmkv, context)

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

    @Provides
    @Singleton
    fun provideLocalDao(db: AppDatabase): LocalDao = db.localDao()
}
