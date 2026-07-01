package com.example.sheeps.core;

/**
 * 全局配置文件，用于定义应用范围内的常量和配置信息。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u000e\u0010\u0004\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/example/sheeps/core/AppConfig;", "", "<init>", "()V", "BASE_URL", "", "WS_URL", "core_release"})
public final class AppConfig {
    
    /**
     * 后端服务接口的基础请求地址
     */
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String BASE_URL = "https://xqh.cc.cd/";
    
    /**
     * 后端服务实时同步的 WebSocket 地址
     */
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String WS_URL = "wss://xqh.cc.cd/api/ws";
    @org.jetbrains.annotations.NotNull()
    public static final com.example.sheeps.core.AppConfig INSTANCE = null;
    
    private AppConfig() {
        super();
    }
}