package com.example.sheeps.core.di;

import android.content.Context;
import androidx.room.Room;
import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.local.AppDatabase;
import com.example.sheeps.data.local.LocalDao;
import com.tencent.mmkv.MMKV;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@dagger.Module()
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c7\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\b\u0010\u0004\u001a\u00020\u0005H\u0007J\u001a\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u00052\b\b\u0001\u0010\t\u001a\u00020\nH\u0007J\u0012\u0010\u000b\u001a\u00020\f2\b\b\u0001\u0010\t\u001a\u00020\nH\u0007J\u0010\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\fH\u0007\u00a8\u0006\u0010"}, d2 = {"Lcom/example/sheeps/core/di/StorageModule;", "", "<init>", "()V", "provideMmkv", "Lcom/tencent/mmkv/MMKV;", "provideUserPreferences", "Lcom/example/sheeps/core/preference/UserPreferences;", "mmkv", "context", "Landroid/content/Context;", "provideDatabase", "Lcom/example/sheeps/data/local/AppDatabase;", "provideLocalDao", "Lcom/example/sheeps/data/local/LocalDao;", "db", "core_release"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public final class StorageModule {
    @org.jetbrains.annotations.NotNull()
    public static final com.example.sheeps.core.di.StorageModule INSTANCE = null;
    
    private StorageModule() {
        super();
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.tencent.mmkv.MMKV provideMmkv() {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.core.preference.UserPreferences provideUserPreferences(@org.jetbrains.annotations.NotNull()
    com.tencent.mmkv.MMKV mmkv, @dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.data.local.AppDatabase provideDatabase(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.data.local.LocalDao provideLocalDao(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.local.AppDatabase db) {
        return null;
    }
}