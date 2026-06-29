package com.example.sheeps.core.di

import com.example.sheeps.data.network.ApiService
import com.example.sheeps.core.AppConfig
import com.example.sheeps.core.preference.UserPreferences
import com.apkfuns.logutils.LogUtils
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

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(prefs: UserPreferences, json: Json): OkHttpClient {
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
            level = HttpLoggingInterceptor.Level.BODY
        }

        val languageInterceptor = Interceptor { chain ->
            val request = chain.request()
            val lang = prefs.getLanguage().ifEmpty { java.util.Locale.getDefault().language }
            val newRequest = request.newBuilder()
                .header("Accept-Language", lang)
                .build()
            chain.proceed(newRequest)
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request()
            val token = prefs.getToken()
            val builder = request.newBuilder()
            if (token != null && request.header("Authorization") == null) {
                builder.header("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }

        // Silent Double-Token Refresh Interceptor with Lock
        val tokenRefreshInterceptor = Interceptor { chain ->
            val request = chain.request()
            var response = chain.proceed(request)

            if (response.code == 401 && !request.url.toString().contains("/api/auth/refresh")) {
                synchronized(this) {
                    val currentToken = prefs.getToken()
                    val requestToken = request.header("Authorization")?.removePrefix("Bearer ")

                    // If another request thread has already updated the token, retry immediately
                    if (currentToken != requestToken && currentToken != null) {
                        response.close()
                        val newRequest = request.newBuilder()
                            .header("Authorization", "Bearer $currentToken")
                            .build()
                        response = chain.proceed(newRequest)
                        return@Interceptor response
                    }

                    val refreshToken = prefs.getRefreshToken()
                    if (refreshToken == null) {
                        prefs.logout()
                        com.example.sheeps.core.utils.AuthEventBus.post(com.example.sheeps.core.utils.AuthEvent.Logout)
                        return@Interceptor response
                    }

                    // Call refresh API synchronously
                    val client = OkHttpClient()
                    val mediaType = "application/json".toMediaType()
                    val requestBody =
                        "{\"refreshToken\":\"$refreshToken\"}".toRequestBody(mediaType)

                    val refreshUrl =
                        request.url.newBuilder().encodedPath("/api/auth/refresh").build()
                    val refreshRequest = okhttp3.Request.Builder()
                        .url(refreshUrl)
                        .method("POST", requestBody)
                        .build()

                    try {
                        val refreshResponse = client.newCall(refreshRequest).execute()
                        if (refreshResponse.isSuccessful) {
                            val bodyStr = refreshResponse.body?.string() ?: ""
                            val refreshRes =
                                json.decodeFromString<com.example.sheeps.data.model.RefreshResponse>(
                                    bodyStr
                                )
                            if (refreshRes.success && refreshRes.token != null) {
                                prefs.setToken(refreshRes.token)
                                prefs.setRefreshToken(refreshRes.refreshToken)

                                response.close()
                                val newRequest = request.newBuilder()
                                    .header("Authorization", "Bearer ${refreshRes.token}")
                                    .build()
                                response = chain.proceed(newRequest)
                                return@Interceptor response
                            }
                        }
                    } catch (e: Exception) {
                        LogUtils.e( "Failed to refresh token", e)
                    }

                    // If refresh failed, perform logout
                    prefs.logout()
                    com.example.sheeps.core.utils.AuthEventBus.post(com.example.sheeps.core.utils.AuthEvent.Logout)
                }
            }
            response
        }

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
                        response.close()
                    } else {
                        return@Interceptor response
                    }
                } catch (e: IOException) {
                    lastException = e
                }

                if (attempt < 3) {
                    try {
                        Thread.sleep(attempt * 1000L) // Wait 1s, 2s
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Retry interrupted", e)
                    }
                }
                attempt++
            }
            if (response != null) return@Interceptor response
            throw lastException ?: IOException("Request failed after 3 attempts")
        }

        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(languageInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(tokenRefreshInterceptor)
            .addInterceptor(weakNetworkRetryInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
