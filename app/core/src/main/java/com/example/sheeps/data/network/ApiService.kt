package com.example.sheeps.data.network

import com.example.sheeps.data.model.*
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {

    @POST("/api/register")
    suspend fun register(@Body request: RegisterRequest): GenericResponse

    @POST("/api/user/rename")
    suspend fun rename(@Body request: RenameRequest): GenericResponse

    @GET("/api/level")
    suspend fun getLevel(@Query("id") levelId: Int): List<Tile>

    @POST("/api/score/submit")
    suspend fun submitScore(
        @Header("Authorization") auth: String?,
        @Body request: ScoreRequest
    ): GenericResponse

    @GET("/api/leaderboard")
    suspend fun getLeaderboard(
        @Query("level_id") levelId: Int,
        @Query("limit") limit: Int = 50
    ): LeaderboardResponse

    // --- New APIs ---

    @POST("/api/auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequest): SendCodeResponse

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/api/auth/refresh")
    suspend fun refreshToken(@Body request: Map<String, String>): RefreshResponse

    @POST("/api/user/sync")
    suspend fun syncData(
        @Header("Authorization") auth: String,
        @Body request: SyncRequest
    ): SyncResponse

    @POST("/api/level/unlock")
    suspend fun unlockLevel(
        @Header("Authorization") auth: String,
        @Body request: UnlockLevelRequest
    ): UnlockLevelResponse

    @GET("/api/shop/items")
    suspend fun getShopItems(): List<ShopItem>

    @POST("/api/shop/exchange")
    suspend fun exchangeItem(
        @Header("Authorization") auth: String,
        @Body request: ExchangeRequest
    ): ExchangeResponse

    @POST("/api/sign/today")
    suspend fun signIn(
        @Header("Authorization") auth: String
    ): SignResponse

    @GET("/api/task/daily")
    suspend fun getDailyTasks(
        @Header("Authorization") auth: String
    ): List<DailyTask>

    @POST("/api/task/claim")
    suspend fun claimTaskReward(
        @Header("Authorization") auth: String,
        @Body request: TaskClaimRequest
    ): TaskClaimResponse

    @GET("/api/notice/list")
    suspend fun getNotices(): List<Notice>

    @GET("/api/user/profile")
    suspend fun getUserProfile(
        @Header("Authorization") auth: String
    ): UserProfileResponse

    @GET("/api/user/points-history")
    suspend fun getPointsHistory(
        @Header("Authorization") auth: String
    ): List<PointRecord>

    @GET("/api/user/exchange-history")
    suspend fun getExchangeHistory(
        @Header("Authorization") auth: String
    ): List<ExchangeRecord>

    @GET("/api/leaderboard")
    suspend fun getLeaderboardPaged(
        @Query("level_id") levelId: Int,
        @Query("type") type: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 20
    ): LeaderboardResponse
}
