package com.example.sheeps.menu.ui

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.sheeps.core.base.BaseActivity
import com.example.sheeps.data.model.ResetPasswordRequest
import com.example.sheeps.data.model.SendCodeRequest
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.menu.ui.screens.ResetPasswordScreen
import com.example.sheeps.core.R
import com.example.sheeps.theme.SheepsTheme
import com.hjq.toast.Toaster
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

/**
 * 全屏找回密码 Activity。
 * 重置成功后 toast 提示并自动关闭，回到登录页。
 *
 * 生命周期与内存说明：
 * - 注入的 apiService 为 Hilt 单例，Activity 销毁即释放，不长期持有界面。
 * - 重置请求使用 [androidx.lifecycle.lifecycleScope]，保证 finish() 后请求仍可完成；
 *   lifecycleScope 随 Activity 销毁自动取消，无泄漏。
 */
@Route(path = "/auth/reset-password")
@AndroidEntryPoint
class ResetPasswordActivity : BaseActivity() {

    @Inject lateinit var apiService: ApiService

    override fun initView(savedInstanceState: Bundle?) {
        setContent {
            SheepsTheme {
                BackHandler { finish() }
                var isLoading by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                ResetPasswordScreen(
                    isLoading = isLoading,
                    onBack = { finish() },
                    onSendCode = { phone -> handleSendCode(phone, scope) },
                    onReset = { phone, code, newPassword ->
                        handleReset(phone, code, newPassword) { isLoading = it }
                    },
                    onResetSuccess = {
                        // 导航由 handleReset 在 API 成功后处理；
                        // ResetPasswordScreen 中此回调紧随 onReset 触发，这里做 no-op
                    }
                )
            }
        }
    }

    override fun initData() {}

    // -------- 业务逻辑 --------

    private fun handleSendCode(phone: String, scope: kotlinx.coroutines.CoroutineScope) {
        if (phone.length != 11) {
            Toaster.show(getString(R.string.err_invalid_phone))
            return
        }
        scope.launch {
            try {
                val response = apiService.sendCode(SendCodeRequest(phone))
                if (response.success) {
                    Toaster.show(getString(R.string.toast_send_code_success, response.code))
                } else {
                    Toaster.show(getString(R.string.toast_send_code_failed))
                }
            } catch (e: Exception) {
                Toaster.show(getString(R.string.toast_send_code_network_error))
            }
        }
    }

    private fun handleReset(
        phone: String,
        code: String,
        newPassword: String,
        setLoading: (Boolean) -> Unit
    ) {
        setLoading(true)
        // 使用 lifecycleScope 确保在 finish() 之后协程不会被立即取消
        // ⚠️ 内存隐患（已规避）：lifecycleScope 绑定 Activity 生命周期，销毁即取消；
        // 此处有意「先请求后 finish」，避免请求被提前打断。请勿改成 GlobalScope。
        lifecycleScope.launch {
            try {
                val response = apiService.resetPassword(
                    ResetPasswordRequest(phone, code, newPassword)
                )
                if (response.success) {
                    Toaster.show(getString(R.string.toast_reset_password_success))
                    finish()
                } else {
                    setLoading(false)
                    Toaster.show(getString(R.string.toast_reset_password_failed))
                }
            } catch (e: HttpException) {
                setLoading(false)
                Toaster.show(parseAuthError(e, getString(R.string.toast_reset_password_failed_retry)))
            } catch (e: Exception) {
                setLoading(false)
                Toaster.show(getString(R.string.toast_network_error_retry))
            }
        }
    }

    private fun parseAuthError(e: HttpException, fallback: String): String {
        return try {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            val errorJson = JSONObject(errorBody)
            val error = errorJson.optString("error", "")
            error.ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }
}
