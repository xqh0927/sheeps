package com.example.sheeps.data.result

import com.example.sheeps.ui.R

/**
 * 统一错误文案映射：将 [ApiResult.Error.code] 映射为用户可见的 `R.string.*` 资源 ID。
 *
 * 设计目标：消除 delegate 内各自硬编码的 Toast 字符串，保证同一错误码在全应用
 * 映射为同一文案。delegate 只负责 `setEffect(ShowToast(resId = ErrorMessageMapper.toResId(code)))`。
 *
 * 映射规则（与 [safeApiCall] 错误码约定一致）：
 * - -1      → 网络异常
 * - 401     → 登录失效
 * - 40000   → 业务失败（!success）
 * - 500..599→ 服务端错误
 * - 其它    → 通用网络重试提示
 */
object ErrorMessageMapper {
    fun toResId(code: Int): Int = when (code) {
        -1 -> R.string.toast_network_error
        401 -> R.string.toast_login_expired
        40000 -> R.string.toast_business_failed
        in 500..599 -> R.string.toast_server_error
        else -> R.string.toast_network_error_retry
    }
}
