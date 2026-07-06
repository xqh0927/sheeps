package com.example.sheeps.core.network

import com.example.sheeps.data.network.ApiService

/**
 * 全局 ApiService 引用持有者
 *
 * 用于在 NetworkModule 中打破循环依赖：
 * tokenRefreshInterceptor 需要 ApiService 来调用 refreshToken，
 * 但 ApiService 的创建又依赖 OkHttpClient（其中包含 tokenRefreshInterceptor）。
 *
 * 解决方案：在 NetworkModule.provideApiService 中设置此引用，
 * tokenRefreshInterceptor 通过此引用来调用 ApiService。
 */
object RefreshTokenProvider {

    @Volatile
    var apiService: ApiService? = null
}
