package com.example.sheeps.data.result

/**
 * 统一网络结果类型（唯一网络结果出口）。
 *
 * - [Success]：请求成功，携带业务数据 [data]。
 * - [Error]：请求失败，携带错误码 [code]、可读文案 [message] 与可选的底层异常 [cause]。
 *
 * 调用方**禁止**自行构造 [ApiResult]，只能通过 [safeApiCall] 或 Repository 方法获得；
 * 调用方通过 `when(result) { is ApiResult.Success -> ... is ApiResult.Error -> ... }`
 * 或 `.onSuccess { }.onFailure { }` 分流，不得再 `try/catch` 网络异常。
 */
sealed interface ApiResult<out T> {
    /** 请求成功，携带数据。 */
    data class Success<out T>(val data: T) : ApiResult<T>

    /**
     * 请求失败。
     * @param code 错误码：网络层 IOException→-1；协程取消→重抛不封装；未知异常→-2；
     *             HTTP 错误→实际状态码（401/403/500…）；业务失败(!success)→40000。
     * @param message 可读错误文案。
     * @param cause 底层异常（若有），便于排查。
     */
    data class Error(
        val code: Int,
        val message: String,
        val cause: Throwable? = null
    ) : ApiResult<Nothing>
}

/**
 * 业务失败异常：Repository 在 `!response.success` 时抛出，
 * 由 [safeApiCall] 捕获并归一为 [ApiResult.Error](code = 40000)。
 */
class BusinessException(message: String) : Exception(message)
