package com.example.sheeps.data.result

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/**
 * 统一错误闸门：所有网络/挂起调用必须且仅经此入口。
 *
 * 结果与错误码约定：
 * - 成功 → [ApiResult.Success]
 * - [CancellationException] → **重抛**（绝不吞掉协程取消）
 * - [BusinessException] → [ApiResult.Error](code = 40000)
 * - [HttpException] → [ApiResult.Error](code = HTTP 状态码，如 401/403/500)
 * - [IOException] → [ApiResult.Error](code = -1，网络层错误)
 * - 其它 [Exception] → [ApiResult.Error](code = -2，未知错误)
 *
 * 鉴权重试：当 [ApiResult.Error.code] == 401 且提供了 [onAuthError] 回调时，
 * 触发一次刷新（回调返回 true 表示刷新成功），成功后**重试 [block] 一次（最多 1 次）**；
 * 刷新失败或仍返回 401 则降级为 [ApiResult.Error](code = 401)。
 *
 * @param block 实际挂起调用（如 `apiService.xxx(...)`）。
 * @param onAuthError 可选的鉴权错误处理回调，返回 true 表示已成功刷新凭据。
 */
suspend fun <T> safeApiCall(
    block: suspend () -> T,
    onAuthError: (suspend (code: Int) -> Boolean)? = null
): ApiResult<T> {
    val result = runCatchingNetwork(block)
    if (result is ApiResult.Error && result.code == 401 && onAuthError != null) {
        // 触发一次刷新（回调内部异常也视为刷新失败）
        val refreshed = runCatching { onAuthError(result.code) }.getOrElse { false }
        if (refreshed) return runCatchingNetwork(block) // 重试一次，不二次重试
    }
    return result
}

/**
 * 内部：执行 [block] 并归一化异常为 [ApiResult]。
 * 注意 [CancellationException] 必须**重抛**，不可被下方通用 [Exception] 分支吞掉。
 */
private suspend fun <T> runCatchingNetwork(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: CancellationException) {
    throw e // 必须重抛，保证协程取消可向上传播
} catch (e: BusinessException) {
    ApiResult.Error(code = 40000, message = e.message ?: "业务失败", cause = e)
} catch (e: HttpException) {
    ApiResult.Error(code = e.code(), message = e.message ?: "HTTP ${e.code()}", cause = e)
} catch (e: IOException) {
    ApiResult.Error(code = -1, message = "网络异常：${e.message}", cause = e)
} catch (e: Exception) {
    ApiResult.Error(code = -2, message = e.message ?: "未知错误", cause = e)
}
