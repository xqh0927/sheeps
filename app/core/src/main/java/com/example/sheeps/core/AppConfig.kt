package com.example.sheeps.core

/**
 * 全局配置文件，用于定义应用范围内的常量和配置信息。
 */
object AppConfig {
    /**
     * 后端服务接口的基础请求地址
     */
    const val BASE_URL = "https://api.xqh.cc.cd/"

    /**
     * 后端服务实时同步的 WebSocket 地址
     */
    const val WS_URL = "wss://api.xqh.cc.cd/api/ws"
}
