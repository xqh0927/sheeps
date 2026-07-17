package com.example.sheeps.data.repository

import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.model.GenericResponse
import com.example.sheeps.data.model.MatchJoinRequest
import com.example.sheeps.data.model.MatchStatusResponse
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.data.result.TokenRefresher
import com.example.sheeps.data.result.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 匹配相关数据仓库（统一错误闸门）。
 *
 * 封装加入匹配 / 轮询状态 / 离开匹配接口，均返回 [ApiResult]。
 * 匹配接口本身不携带 Authorization 头，不会触发 401 重试（符合预期）。
 *
 * 注：构造中保留 [prefs] 仅为与全局鉴权依赖图一致；匹配接口无需鉴权头。
 */
@Singleton
class MatchRepository @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val tokenRefresher: TokenRefresher
) {
    private suspend fun <T> call(block: suspend () -> T): ApiResult<T> =
        safeApiCall(block) { code -> code == 401 && tokenRefresher.refreshToken() is ApiResult.Success }

    suspend fun joinMatch(req: MatchJoinRequest): ApiResult<MatchStatusResponse> =
        call { apiService.joinMatch(req) }

    suspend fun getMatchStatus(playerId: String): ApiResult<MatchStatusResponse> =
        call { apiService.getMatchStatus(playerId) }

    suspend fun leaveMatch(req: MatchJoinRequest): ApiResult<GenericResponse> =
        call { apiService.leaveMatch(req) }
}
