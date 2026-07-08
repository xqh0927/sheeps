package com.example.sheeps.data.network

import com.example.sheeps.data.model.AppUpdateResponse
import com.example.sheeps.data.model.CheckPasswordResponse
import com.example.sheeps.data.model.DailyTask
import com.example.sheeps.data.model.DailyPopupResponse
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
import com.example.sheeps.data.model.PasswordLoginRequest
import com.example.sheeps.data.model.PointRecord
import com.example.sheeps.data.model.RefreshResponse
import com.example.sheeps.data.model.RegisterAuthRequest
import com.example.sheeps.data.model.RegisterRequest
import com.example.sheeps.data.model.RenameRequest
import com.example.sheeps.data.model.ResetPasswordRequest
import com.example.sheeps.data.model.ScoreRequest
import com.example.sheeps.data.model.SendCodeRequest
import com.example.sheeps.data.model.SendCodeResponse
import com.example.sheeps.data.model.SetPasswordRequest
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
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {

    /**
     * 用户注册 — 通过手机号注册新账号
     *
     * @param request 注册请求体，包含手机号等注册信息
     * @return GenericResponse 通用响应，包含 success 状态
     */
    @POST("/api/register")
    suspend fun register(@Body request: RegisterRequest): GenericResponse

    /**
     * 用户改名 — 修改用户昵称
     *
     * @param request 改名请求体，包含用户ID和新昵称
     * @return GenericResponse 通用响应，包含 success 状态
     */
    @POST("/api/user/rename")
    suspend fun rename(@Body request: RenameRequest): GenericResponse

    /**
     * 获取关卡布局数据 — 根据关卡ID和随机种子生成可解的游戏棋盘
     *
     * @param levelId 关卡编号
     * @param seed 可选，随机种子（不传则由服务端随机生成）
     * @return List<Tile> 关卡瓦片（麻将牌）布局列表
     */
    @GET("/api/level")
    suspend fun getLevel(
        @Query("id") levelId: Int,
        @Query("seed") seed: Int? = null
    ): List<Tile>

    /**
     * 提交通关成绩 — 将玩家通关耗时与分数提交至服务端校验并存档
     *
     * @param auth 可选的 Authorization 请求头（Bearer Token）
     * @param request 成绩请求体，包含用户ID、关卡ID、通关耗时（毫秒）及防作弊签名
     * @return GenericResponse 通用响应，包含 success 状态
     */
    @POST("/api/score/submit")
    suspend fun submitScore(
        @Header("Authorization") auth: String?,
        @Body request: ScoreRequest
    ): GenericResponse

    /**
     * 获取排行榜（基础版） — 查询指定关卡的积分排行
     *
     * @param levelId 关卡编号
     * @param limit 返回条目数量上限，默认 50
     * @return LeaderboardResponse 包含排行榜列表的响应
     */
    @GET("/api/leaderboard")
    suspend fun getLeaderboard(
        @Query("level_id") levelId: Int,
        @Query("limit") limit: Int = 50,
        @Query("game_mode") gameMode: Int = 0
    ): LeaderboardResponse

    /**
     * 发送验证码 — 向指定手机号发送6位数字登录验证码
     *
     * @param request 发送验证码请求体，包含手机号
     * @return SendCodeResponse 包含 success 状态及验证码（调试用）
     */
    @POST("/api/auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequest): SendCodeResponse

    /**
     * 用户登录 — 通过手机号+验证码完成登录或自动注册
     *
     * @param request 登录请求体，包含手机号、验证码、可选的设备UUID（用于游客数据合并）
     * @return LoginResponse 包含 token、refreshToken、用户信息、已解锁关卡、道具列表、签到状态
     */
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    /**
     * 密码登录 — 通过手机号+密码完成登录
     *
     * @param request 密码登录请求体，包含手机号和密码
     * @return LoginResponse 包含 token、refreshToken、用户信息等
     */
    @POST("/api/auth/login-password")
    suspend fun loginPassword(@Body request: PasswordLoginRequest): LoginResponse

    /**
     * 用户注册 — 通过手机号+密码+验证码注册新账号
     *
     * @param request 注册请求体，包含手机号、密码、验证码
     * @return GenericResponse 通用响应，包含 success 状态
     */
    @POST("/api/auth/register")
    suspend fun registerAuth(@Body request: RegisterAuthRequest): GenericResponse

    /**
     * 重置密码 — 通过手机号+验证码+新密码重置
     *
     * @param request 重置密码请求体
     * @return GenericResponse 通用响应
     */
    @POST("/api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): GenericResponse

    /**
     * 检查是否已设置密码
     *
     * @param auth Authorization 请求头（Bearer Token）
     * @return CheckPasswordResponse 包含 hasPassword 字段
     */
    @GET("/api/auth/check-password")
    suspend fun checkPassword(
        @Header("Authorization") auth: String
    ): CheckPasswordResponse

    /**
     * 设置密码 — 登录后设置密码（奖励 50 积分）
     *
     * @param auth Authorization 请求头（Bearer Token）
     * @param request 设置密码请求体
     * @return GenericResponse 通用响应
     */
    @POST("/api/auth/set-password")
    suspend fun setPassword(
        @Header("Authorization") auth: String,
        @Body request: SetPasswordRequest
    ): GenericResponse

    /**
     * 刷新Token — 使用 Refresh Token 静默换取新的双 Token
     *
     * @param request Map 包含 refreshToken 字段
     * @return RefreshResponse 包含新的 token 和 refreshToken
     */
    @POST("/api/auth/refresh")
    suspend fun refreshToken(@Body request: Map<String, String>): RefreshResponse

    /**
     * 端云数据同步 — 将本地 Room 脏数据增量同步至云端，并返回权威云端数据
     *
     * @param auth Authorization 请求头（Bearer Token）
     * @param request 同步请求体，包含积分、已解锁关卡列表、道具背包数据
     * @return SyncResponse 返回同步后的用户信息、已解锁关卡、道具列表
     */
    @POST("/api/user/sync")
    suspend fun syncData(
        @Header("Authorization") auth: String,
        @Body request: SyncRequest
    ): SyncResponse

    /**
     * 解锁关卡 — 消耗积分解锁指定编号的新关卡
     *
     * @param auth Authorization 请求头（Bearer Token）
     * @param request 解锁请求体，包含目标关卡编号
     * @return UnlockLevelResponse 包含 success 状态及当前剩余积分
     */
    @POST("/api/level/unlock")
    suspend fun unlockLevel(
        @Header("Authorization") auth: String,
        @Body request: UnlockLevelRequest
    ): UnlockLevelResponse

    /**
     * 获取积分商城商品列表 — 查询所有可兑换的道具商品（支持多语言）
     *
     * @return List<ShopItem> 商品列表，包含名称、描述、图片、所需积分、库存
     */
    @GET("/api/shop/items")
    suspend fun getShopItems(): List<ShopItem>

    /**
     * 积分兑换道具 — 消耗积分兑换指定的法宝道具并写入背包
     *
     * @param auth Authorization 请求头（Bearer Token）
     * @param request 兑换请求体，包含商品ID和兑换数量
     * @return ExchangeResponse 包含兑换后的道具类型、新数量和剩余积分
     */
    @POST("/api/shop/exchange")
    suspend fun exchangeItem(
        @Header("Authorization") auth: String,
        @Body request: ExchangeRequest
    ): ExchangeResponse

    /**
     * 每日签到 — 完成当天签到并领取递增的连签积分奖励
     *
     * @param auth Authorization 请求头（Bearer Token）
     * @return SignResponse 包含连续签到天数、本次奖励积分、当前总积分
     */
    @POST("/api/sign/today")
    suspend fun signIn(
        @Header("Authorization") auth: String
    ): SignResponse

    /**
     * 获取每日任务列表 — 查询当天的任务模板及当前玩家的完成进度
     *
     * @param auth Authorization 请求头（Bearer Token）
     * @return List<DailyTask> 每日任务列表，包含任务名称、目标次数、当前进度、是否完成/已领取
     */
    @GET("/api/task/daily")
    suspend fun getDailyTasks(
        @Header("Authorization") auth: String
    ): List<DailyTask>

    /**
     * 领取任务奖励 — 对已完成且未领取的任务领取积分奖励
     *
     * @param auth Authorization 请求头（Bearer Token）
     * @param request 领取请求体，包含任务ID
     * @return TaskClaimResponse 包含 success 状态及当前总积分
     */
    @POST("/api/task/claim")
    suspend fun claimTaskReward(
        @Header("Authorization") auth: String,
        @Body request: TaskClaimRequest
    ): TaskClaimResponse

    /**
     * 获取公告列表 — 查询系统公告（支持多语言）
     *
     * @return List<Notice> 公告列表，包含标题、内容、类型、发布时间
     */
    @GET("/api/notice/list")
    suspend fun getNotices(): List<Notice>

    /**
     * 获取用户Profile — 查询用户完整资料、背包道具、签到进度及最高通关记录
     *
     * @param auth Authorization 请求头（Bearer Token）
     * @return UserProfileResponse 包含用户信息、已解锁关卡、道具、签到状态、最高关卡、头像URL
     */
    @GET("/api/user/profile")
    suspend fun getUserProfile(
        @Header("Authorization") auth: String
    ): UserProfileResponse

    /**
     * 上传头像 — 将头像图片通过 multipart/form-data 上传至 R2 存储
     *
     * @param auth Authorization 请求头（Bearer Token）
     * @param avatar multipart 头像图片 Part
     * @return GenericResponse 通用响应，包含 avatarUrl 字段
     */
    @Multipart
    @POST("/api/user/avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") auth: String,
        @Part avatar: MultipartBody.Part
    ): GenericResponse

    /**
     * 获取积分流水 — 查询个人积分收支历史明细（最近50条）
     *
     * @param auth Authorization 请求头（Bearer Token）
     * @return List<PointRecord> 积分记录列表，包含类型、金额、来源、余额、时间
     */
    @GET("/api/user/points-history")
    suspend fun getPointsHistory(
        @Header("Authorization") auth: String
    ): List<PointRecord>

    /**
     * 获取兑换记录 — 查询个人道具兑换历史明细（最近50条）
     *
     * @param auth Authorization 请求头（Bearer Token）
     * @return List<ExchangeRecord> 兑换记录列表，包含商品ID、道具类型、数量、花费积分、时间
     */
    @GET("/api/user/exchange-history")
    suspend fun getExchangeHistory(
        @Header("Authorization") auth: String
    ): List<ExchangeRecord>

    /**
     * 获取排行榜（分页版） — 按类型（日榜/周榜/总榜）分页查询指定关卡的排行榜
     *
     * @param levelId 关卡编号
     * @param type 排行榜类型："daily"（日榜）、"weekly"（周榜）、"history"（总榜）
     * @param page 页码，从1开始
     * @param limit 每页条目数，默认 20
     * @return LeaderboardResponse 包含分页排行榜列表的响应
     */
    @GET("/api/leaderboard")
    suspend fun getLeaderboardPaged(
        @Query("level_id") levelId: Int,
        @Query("type") type: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 20
    ): LeaderboardResponse

    /**
     * 检查App更新 — 对比当前客户端版本号与服务端最新版本
     *
     * @param versionCode 当前客户端版本号（整数）
     * @return AppUpdateResponse 包含是否有更新、最新版本号、下载地址、更新说明
     */
    @GET("/api/app/check-update")
    suspend fun checkUpdate(
        @Query("version_code") versionCode: Int
    ): AppUpdateResponse

    /**
     * 加入匹配队列 — 进入多人实时对决匹配池，等待或立即配对对手
     *
     * @param request 匹配请求体，包含玩家ID
     * @return MatchStatusResponse 包含匹配状态（waiting/matched）、对局ID、对手ID、对决关卡
     */
    @POST("/api/match/join")
    suspend fun joinMatch(@Body request: MatchJoinRequest): MatchStatusResponse

    /**
     * 轮询匹配状态 — 查询当前玩家的匹配队列状态
     *
     * @param playerId 玩家ID
     * @return MatchStatusResponse 包含匹配状态（waiting/matched/not_in_queue）、对局信息
     */
    @GET("/api/match/status")
    suspend fun getMatchStatus(@Query("playerId") playerId: String): MatchStatusResponse

    /**
     * 离开匹配队列 — 主动取消匹配并移出等待队列
     *
     * @param request 离开请求体，包含玩家ID
     * @return GenericResponse 通用响应，包含 status: "left"
     */
    @POST("/api/match/leave")
    suspend fun leaveMatch(@Body request: MatchJoinRequest): GenericResponse

    /**
     * 每日弹窗数据 — 获取昨日积分榜前三名及当前玩家的昨日排名
     *
     * @param auth Authorization 请求头（Bearer Token）
     * @return DailyPopupResponse 包含前三名玩家列表和当前用户昨日排名
     */
    @GET("/api/leaderboard/daily-popup")
    suspend fun getDailyPopup(
        @Header("Authorization") auth: String
    ): DailyPopupResponse
}