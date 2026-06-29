package com.example.sheeps.data.network

import com.example.sheeps.data.model.AppUpdateResponse
import com.example.sheeps.data.model.DailyTask
import com.example.sheeps.data.model.ExchangeRecord
import com.example.sheeps.data.model.ExchangeRequest
import com.example.sheeps.data.model.ExchangeResponse
import com.example.sheeps.data.model.GenericResponse
import com.example.sheeps.data.model.LeaderboardResponse
import com.example.sheeps.data.model.LoginRequest
import com.example.sheeps.data.model.LoginResponse
import com.example.sheeps.data.model.MatchJoinRequest
import com.example.sheeps.data.model.MatchStatusResponse
import com.example.sheeps.data.model.Notice
import com.example.sheeps.data.model.PointRecord
import com.example.sheeps.data.model.RefreshResponse
import com.example.sheeps.data.model.RegisterRequest
import com.example.sheeps.data.model.RenameRequest
import com.example.sheeps.data.model.ScoreRequest
import com.example.sheeps.data.model.SendCodeRequest
import com.example.sheeps.data.model.SendCodeResponse
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.data.model.SignResponse
import com.example.sheeps.data.model.SyncRequest
import com.example.sheeps.data.model.SyncResponse
import com.example.sheeps.data.model.TaskClaimRequest
import com.example.sheeps.data.model.TaskClaimResponse
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.UnlockLevelRequest
import com.example.sheeps.data.model.UnlockLevelResponse
import com.example.sheeps.data.model.UserProfileResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

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

    @GET("/api/app/check-update")
    suspend fun checkUpdate(
        @Query("version_code") versionCode: Int
    ): AppUpdateResponse

    @POST("/api/match/join")
    suspend fun joinMatch(@Body request: MatchJoinRequest): MatchStatusResponse

    @GET("/api/match/status")
    suspend fun getMatchStatus(@Query("playerId") playerId: String): MatchStatusResponse

    @POST("/api/match/leave")
    suspend fun leaveMatch(@Body request: MatchJoinRequest): GenericResponse
}
