package com.example.sheeps.core.di

import com.example.sheeps.data.network.ApiService
import com.example.sheeps.lib_network.AppConfig
import com.example.sheeps.core.BuildConfig
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.lib_network.network.EncryptionInterceptor
import com.apkfuns.logutils.LogUtils
import com.example.sheeps.core.utils.AuthEvent
import com.example.sheeps.core.utils.AuthEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 客户端网络配置 Hilt 依赖注入模块
 * 负责提供全局唯一的 Json 序列化解析器、配置五大核心拦截器的 OkHttpClient、以及 Retrofit 服务代理。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * 提供全局共享的 Json 序列化配置，容忍未知字段
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * 构造并配置搭载五大拦截器的 OkHttpClient
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(prefs: UserPreferences, json: Json): OkHttpClient {
        // 1. 基于 LogUtils 包装的日志拦截器：通过 StringBuilder 缓冲单次 HTTP 往返日志，避免日志控制台被交错多线程打碎
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            private val threadLocalBuffer = object : ThreadLocal<StringBuilder>() {
                override fun initialValue(): StringBuilder = StringBuilder()
            }

            override fun log(message: String) {
                val buffer = threadLocalBuffer.get() ?: StringBuilder()
                buffer.append(message).append("\n")
                if (message.startsWith("--> END") || message.startsWith("<-- END") || message.startsWith("<-- HTTP FAILED")) {
                    LogUtils.d(buffer.toString().trimEnd())
                    buffer.setLength(0)
                }
            }
        }).apply {
            // 生产环境降级为 NONE：避免将解密后的完整请求/响应体写入日志（隐私泄漏 + 性能开销），
            // 仅 DEBUG 构建保留 BODY 级别以便本地排查网络问题。
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        // 2. 国际化语言头拦截器：自动根据首选项或系统语言，为所有 API 请求附带 Accept-Language 请求头
        val languageInterceptor = Interceptor { chain ->
            val request = chain.request()
            val lang = prefs.getLanguage().ifEmpty { java.util.Locale.getDefault().language }
            val newRequest = request.newBuilder()
                .header("Accept-Language", lang)
                .build()
            chain.proceed(newRequest)
        }

        // 3. 鉴权 Token 拦截器：如果用户已登录，且当前请求未手动带上 Authorization，则自动填充 Bearer AccessToken
        val authInterceptor = Interceptor { chain ->
            val request = chain.request()
            val token = prefs.getToken()
            val builder = request.newBuilder()
            if (token != null && request.header("Authorization") == null) {
                builder.header("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }

        // 5. 加密拦截器：请求体加密 + 响应体解密
        val encryptionInterceptor = EncryptionInterceptor()

        // 6. 弱网/服务器 5xx 故障自动指数退避重试拦截器：
        // 针对网络异常或后端服务器偶发性 502/504 故障，在 1~3 秒内进行退避重试，最大尝试 3 次，大幅提高不稳网络环境下的请求成功率。
        // 收窄：写接口（POST）遇 5xx 直接返回、不重试，避免重复提交/重复发放道具；连接级 IOException 仍可重试 POST；GET 维持原逻辑。
        val weakNetworkRetryInterceptor = Interceptor { chain ->
            val request = chain.request()
            var attempt = 1
            var response: Response? = null
            var lastException: IOException? = null

            while (attempt <= 3) {
                try {
                    response = chain.proceed(request)
                    if (response.isSuccessful) {
                        return@Interceptor response
                    } else if (response.code >= 500) {
                        // 写接口（POST）遇 5xx 不重试：避免重复提交/重复发放道具（score/submit、shop/exchange 等）。
                        // 注意：仅连接级 IOException（见下方 catch 分支）仍可重试 POST；GET 维持原重试逻辑不变。
                        if (request.method == "POST") {
                            return@Interceptor response
                        }
                        response.close()
                    } else {
                        return@Interceptor response
                    }
                } catch (e: IOException) {
                    lastException = e
                }

                if (attempt < 3) {
                    try {
                        // 线程切换点：此代码运行于 OkHttp 拦截器所在的工作线程（Dispatcher 线程池），
                        // Thread.sleep 会阻塞该工作线程；因重试次数 ≤3 且为后端故障场景，阻塞可接受。
                        Thread.sleep(attempt * 1000L) // 分别等待 1秒、2秒 再次重试
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("重试过程被中断", e)
                    }
                }
                attempt++
            }
            if (response != null) return@Interceptor response
            throw lastException ?: IOException("重试 3 次后请求依然失败")
        }

        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .dispatcher(okhttp3.Dispatcher().apply {
                maxRequests = 64
                maxRequestsPerHost = 20
            })
            .addInterceptor(languageInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(encryptionInterceptor)
            .addInterceptor(weakNetworkRetryInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * 提供全局唯一的 Retrofit 实例
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    /**
     * 提供网络 API 接口访问代理实现
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
