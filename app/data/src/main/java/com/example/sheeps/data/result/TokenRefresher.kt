package com.example.sheeps.data.result

import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.model.RefreshResponse
import com.example.sheeps.data.network.ApiService
import javax.inject.Inject

/**
 * 最小实现的自动 Token 刷新器。
 *
 * 自身调用 [safeApiCall] 时**不传 [safeApiCall.onAuthError]**（即不传递刷新回调），
 * 从而避免 401 → 刷新 → 再 401 → 再刷新的递归死循环。
 *
 * 刷新成功：写回新双 Token 并返回 [ApiResult.Success]。
 * 刷新失败（无刷新令牌 / 业务失败 / 网络异常）：清除本地 Token 与 RefreshToken（强制重新登录），
 * 并返回对应的 [ApiResult.Error]。
 */
class TokenRefresher @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences
) {
    suspend fun refreshToken(): ApiResult<RefreshResponse> {
        val refreshToken = prefs.getRefreshToken()
            ?: return ApiResult.Error(code = 401, message = "无刷新令牌")

        return safeApiCall({ // 无 onAuthError → 不递归重试
            val resp = apiService.refreshToken(mapOf("refreshToken" to refreshToken))
            if (!resp.success) {
                // 刷新失败：清除本地凭据，迫使重新登录
                prefs.setToken(null)
                prefs.setRefreshToken(null)
                throw BusinessException(resp.error ?: "refresh failed")
            }
            prefs.setToken(resp.token)
            prefs.setRefreshToken(resp.refreshToken)
            resp
        })
    }
}
