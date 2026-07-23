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
import com.example.sheeps.lib_base.base.BaseActivity
import com.example.sheeps.data.model.RegisterAuthRequest
import com.example.sheeps.data.model.SendCodeRequest
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.menu.ui.screens.RegisterScreen
import com.example.sheeps.ui.R
import com.example.sheeps.ui.theme.SheepsTheme
import com.hjq.toast.Toaster
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

/**
 * 全屏注册 Activity。
 * 注册成功后 toast 提示并自动关闭，回到登录页。*/
import com.example.sheeps.lib_base.router.RouterPath

/**
 * 注册 Activity。
 */
@Route(path = RouterPath.Auth.REGISTER)
@AndroidEntryPoint
class RegisterActivity : BaseActivity() {

    @Inject lateinit var apiService: ApiService

    override fun initView(savedInstanceState: Bundle?) {
        setContent {
            SheepsTheme {
                BackHandler { finish() }
                var isLoading by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                RegisterScreen(
                    isLoading = isLoading,
                    onBack = { finish() },
                    onSendCode = { phone -> handleSendCode(phone, scope) },
                    onRegister = { phone, password, code ->
                        handleRegister(phone, password, code) { isLoading = it }
                    },
                    onRegisterSuccess = {
                        // 导航由 handleRegister 在 API 成功后处理；
                        // RegisterScreen 中此回调紧随 onRegister 触发，这里做 no-op
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

    private fun handleRegister(
        phone: String,
        password: String,
        code: String,
        setLoading: (Boolean) -> Unit
    ) {
        setLoading(true)
        // 使用 lifecycleScope 确保在 finish() 之后协程不会被立即取消
        // ⚠️ 内存隐患（已规避）：lifecycleScope 绑定 Activity 生命周期，Activity 销毁即取消；
        // 此处为「先发请求再 finish」的有意设计，避免请求被组合移除提前打断。请勿改成 GlobalScope。
        lifecycleScope.launch {
            try {
                val response = apiService.registerAuth(
                    RegisterAuthRequest(phone, password, code)
                )
                if (response.success) {
                    Toaster.show(getString(R.string.toast_register_success))
                    finish()
                } else {
                    setLoading(false)
                    Toaster.show(getString(R.string.toast_register_failed))
                }
            } catch (e: HttpException) {
                setLoading(false)
                Toaster.show(parseAuthError(e, getString(R.string.toast_register_failed_retry)))
            } catch (e: Exception) {
                setLoading(false)
                Toaster.show(getString(com.example.sheeps.data.R.string.toast_network_error_retry))
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
