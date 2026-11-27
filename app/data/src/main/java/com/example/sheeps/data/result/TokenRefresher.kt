package com.example.sheeps.data.result

import com.example.sheeps.data.model.RefreshResponse
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.preference.UserPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRefresher @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences
) {
    private val inFlight = mutableMapOf<String, CompletableDeferred<ApiResult<RefreshResponse>>>()
    @Volatile
    private var expiredRefreshToken: String? = null

    /** Share a renewed token across parallel requests which were rejected with the same access token. */
    suspend fun refreshToken(rejected: String? = prefs.getToken()): ApiResult<RefreshResponse> {
        if (rejected == null) return ApiResult.Error(401, "请重新登录")
        val completion = CompletableDeferred<ApiResult<RefreshResponse>>()
        val shared = synchronized(inFlight) { inFlight.getOrPut(rejected) { completion } }
        if (shared !== completion) return shared.await()
        try {
            // Closing one screen must not cancel the refresh awaited by other screens.
            val result = withContext(NonCancellable) { renew(rejected) }
            completion.complete(result)
            return result
        } catch (e: Throwable) {
            completion.completeExceptionally(e)
            throw e
        } finally {
            synchronized(inFlight) { inFlight.remove(rejected) }
        }
    }

    private suspend fun renew(rejected: String): ApiResult<RefreshResponse> {
        val current = prefs.getToken()
        val owner = prefs.accountId(rejected)
        if (owner == null || current == null || prefs.accountId(current) != owner) {
            return ApiResult.Error(401, "账号已切换")
        }
        val refresh = prefs.getRefreshToken() ?: return ApiResult.Error(401, "请重新登录")
        if (current != rejected) return ApiResult.Success(RefreshResponse(true, current, refresh))
        if (refresh == expiredRefreshToken) return ApiResult.Error(401, "请重新登录")

        val result = safeApiCall({ apiService.refreshToken(mapOf("refreshToken" to refresh)) })
        if (result is ApiResult.Error) {
            if (result.code == 401) expiredRefreshToken = refresh
            return result
        }
        val response = (result as ApiResult.Success).data
        val access = response.token
        val renewed = response.refreshToken
        if (!response.success || access.isNullOrBlank() || renewed.isNullOrBlank() || prefs.accountId(access) != owner) {
            return ApiResult.Error(401, "请重新登录")
        }
        if (!prefs.replaceTokens(current, refresh, access, renewed)) {
            return ApiResult.Error(401, "账号已切换")
        }
        expiredRefreshToken = null
        return result
    }

    /** Refresh only on 401 and retry once, using the token belonging to the original caller. */
    suspend fun <T> authenticated(token: String, request: suspend (String) -> T): ApiResult<T> {
        val result = safeApiCall({ request("Bearer $token") })
        if (result !is ApiResult.Error || result.code != 401) return result
        return when (val renewal = refreshToken(token)) {
            is ApiResult.Error -> renewal
            is ApiResult.Success -> safeApiCall({ request("Bearer ${renewal.data.token}") })
        }
    }
}
