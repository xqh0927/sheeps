package com.example.sheeps.core.di;

import com.example.sheeps.data.network.ApiService;
import com.example.sheeps.core.AppConfig;
import com.example.sheeps.core.preference.UserPreferences;
import com.apkfuns.logutils.LogUtils;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

/**
 * 客户端网络配置 Hilt 依赖注入模块
 * 负责提供全局唯一的 Json 序列化解析器、配置五大核心拦截器的 OkHttpClient、以及 Retrofit 服务代理。
 */
@dagger.Module()
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c7\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\b\u0010\u0004\u001a\u00020\u0005H\u0007J\u0018\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u0005H\u0007J\u0018\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u00072\u0006\u0010\n\u001a\u00020\u0005H\u0007J\u0010\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\fH\u0007\u00a8\u0006\u0011"}, d2 = {"Lcom/example/sheeps/core/di/NetworkModule;", "", "<init>", "()V", "provideJson", "Lkotlinx/serialization/json/Json;", "provideOkHttpClient", "Lokhttp3/OkHttpClient;", "prefs", "Lcom/example/sheeps/core/preference/UserPreferences;", "json", "provideRetrofit", "Lretrofit2/Retrofit;", "okHttpClient", "provideApiService", "Lcom/example/sheeps/data/network/ApiService;", "retrofit", "core_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public final class NetworkModule {
    @org.jetbrains.annotations.NotNull()
    public static final com.example.sheeps.core.di.NetworkModule INSTANCE = null;
    
    private NetworkModule() {
        super();
    }
    
    /**
     * 提供全局共享的 Json 序列化配置，容忍未知字段
     */
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.serialization.json.Json provideJson() {
        return null;
    }
    
    /**
     * 构造并配置搭载五大拦截器的 OkHttpClient
     */
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final okhttp3.OkHttpClient provideOkHttpClient(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.preference.UserPreferences prefs, @org.jetbrains.annotations.NotNull()
    kotlinx.serialization.json.Json json) {
        return null;
    }
    
    /**
     * 提供全局唯一的 Retrofit 实例
     */
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final retrofit2.Retrofit provideRetrofit(@org.jetbrains.annotations.NotNull()
    okhttp3.OkHttpClient okHttpClient, @org.jetbrains.annotations.NotNull()
    kotlinx.serialization.json.Json json) {
        return null;
    }
    
    /**
     * 提供网络 API 接口访问代理实现
     */
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.data.network.ApiService provideApiService(@org.jetbrains.annotations.NotNull()
    retrofit2.Retrofit retrofit) {
        return null;
    }
}