package com.example.sheeps.data.result

/**
 * [ApiResult] 的便利扩展函数。
 *
 * 这些扩展与 `when(result)` 等价，供偏好链式调用的调用方使用；
 * delegate / ViewModel 中两种方式任选其一，但**不得**用其替代错误码归一化。
 */

/** 成功时执行 [action]，返回原 [ApiResult] 以便链式调用。 */
inline fun <T> ApiResult<T>.onSuccess(action: (value: T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) action(data)
    return this
}

/** 失败时执行 [action]，返回原 [ApiResult] 以便链式调用。 */
inline fun <T> ApiResult<T>.onFailure(
    action: (code: Int, message: String, cause: Throwable?) -> Unit
): ApiResult<T> {
    if (this is ApiResult.Error) action(code, message, cause)
    return this
}

/** 成功时返回数据，否则返回 null。 */
fun <T> ApiResult<T>.getOrNull(): T? = if (this is ApiResult.Success) data else null

/** 成功时返回数据，失败时由 [onFailure] 提供兜底值。 */
inline fun <T> ApiResult<T>.getOrElse(onFailure: (error: ApiResult.Error) -> T): T = when (this) {
    is ApiResult.Success -> data
    is ApiResult.Error -> onFailure(this)
}
