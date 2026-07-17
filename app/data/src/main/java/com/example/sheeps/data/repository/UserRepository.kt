package com.example.sheeps.data.repository

import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.model.ExchangeRequest
import com.example.sheeps.data.model.LoginRequest
import com.example.sheeps.data.model.LoginResponse
import com.example.sheeps.data.model.SendCodeRequest
import com.example.sheeps.data.model.SendCodeResponse
import com.example.sheeps.data.model.SignResponse
import com.example.sheeps.data.model.ExchangeResponse
import com.example.sheeps.data.model.TaskClaimRequest
import com.example.sheeps.data.model.TaskClaimResponse
import com.example.sheeps.data.model.UnlockLevelRequest
import com.example.sheeps.data.model.UnlockLevelResponse
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.data.result.BusinessException
import com.example.sheeps.data.result.TokenRefresher
import com.example.sheeps.data.result.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户相关数据仓库（统一错误闸门）。
 *
 * 组合 [ApiService] + [UserPreferences] + [LocalDao] + [SyncRepository] + [TokenRefresher]，
 * 所有对外方法均返回 [ApiResult]，并在内部把 `!response.success` 的业务失败
 * 归一为 [ApiResult.Error](code = 40000)；调用方（delegate / ViewModel）不再读取 `success` 字段。
 *
 * 鉴权头由内部 [auth] 统一构造（"Bearer $token"），调用方无需手写。
 * 401 由 [safeApiCall] 拦截并触发 [TokenRefresher.refreshToken] 重试一次。
 */
@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val localDao: LocalDao,
    private val syncRepository: SyncRepository,
    private val tokenRefresher: TokenRefresher
) {
    /** 统一构造 Authorization 头；未登录时返回空串（由后端按接口决定是否拒绝）。 */
    private fun auth() = prefs.getToken()?.let { "Bearer $it" } ?: ""

    /**
     * 统一入口：经 [safeApiCall] 包装，并挂载 401 自动刷新（最多 1 次）。
     * 刷新成功（返回 [ApiResult.Success]）才会重试 [block]。
     */
    private suspend fun <T> call(block: suspend () -> T): ApiResult<T> =
        safeApiCall(block) { code -> code == 401 && tokenRefresher.refreshToken() is ApiResult.Success }

    suspend fun sendCode(phone: String): ApiResult<SendCodeResponse> = call {
        apiService.sendCode(SendCodeRequest(phone))
            .also { if (!it.success) throw BusinessException("验证码发送失败") }
    }

    suspend fun login(req: LoginRequest): ApiResult<LoginResponse> = call {
        apiService.login(req)
            .also { if (!it.success) throw BusinessException("登录失败") }
    }

    suspend fun signIn(): ApiResult<SignResponse> = call {
        apiService.signIn(auth())
            .also { if (!it.success) throw BusinessException("签到失败") }
    }

    suspend fun exchangeItem(req: ExchangeRequest): ApiResult<ExchangeResponse> = call {
        apiService.exchangeItem(auth(), req)
            .also { if (!it.success) throw BusinessException("兑换失败") }
    }

    suspend fun claimTaskReward(req: TaskClaimRequest): ApiResult<TaskClaimResponse> = call {
        apiService.claimTaskReward(auth(), req)
            .also { if (!it.success) throw BusinessException("任务领奖失败") }
    }

    suspend fun unlockLevel(req: UnlockLevelRequest): ApiResult<UnlockLevelResponse> = call {
        apiService.unlockLevel(auth(), req)
            .also { if (!it.success) throw BusinessException("解锁关卡失败") }
    }

    /**
     * 端云脏数据同步（Q4：保留 [SyncRepository]，由本仓库委托聚合，消除 delegate 直注特例）。
     * 失败以 [ApiResult.Error] 返回，由调用方（如 [AuthDelegate.handleResolveConflict]）分流处理。
     */
    suspend fun syncDirtyData(): ApiResult<Unit> = call { syncRepository.syncDirtyData() }
}
