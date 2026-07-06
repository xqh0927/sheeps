package com.example.sheeps.menu

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
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.local.BackpackItemEntity
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.local.UserProfileEntity
import com.example.sheeps.data.local.UserProgressEntity
import com.example.sheeps.data.model.LoginRequest
import com.example.sheeps.data.model.PasswordLoginRequest
import com.example.sheeps.data.model.SendCodeRequest
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.repository.SyncRepository
import com.example.sheeps.menu.ui.screens.LoginScreen
import com.example.sheeps.theme.SheepsTheme
import com.hjq.toast.Toaster
import com.therouter.TheRouter
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

/**
 * 全屏登录 Activity。
 * 支持验证码登录 / 密码登录双模式，登录成功后保存数据并自动关闭。
 */
@Route(path = "/auth/login")
@AndroidEntryPoint
class LoginActivity : BaseActivity() {

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var prefs: UserPreferences
    @Inject lateinit var localDao: LocalDao
    @Inject lateinit var syncRepository: SyncRepository
    @Inject lateinit var json: Json

    override fun initView(savedInstanceState: Bundle?) {
        setContent {
            SheepsTheme {
                BackHandler { finish() }
                var isLoading by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                LoginScreen(
                    isLoading = isLoading,
                    onBack = { finish() },
                    onSendCode = { phone -> handleSendCode(phone, scope) },
                    onLogin = { phone, code -> handleLoginWithCode(phone, code, scope) { isLoading = it } },
                    onPasswordLogin = { phone, password ->
                        handleLoginWithPassword(phone, password, scope) { isLoading = it }
                    },
                    onNavigateToRegister = {
                        TheRouter.build("/auth/register").navigation(this)
                    },
                    onNavigateToResetPassword = {
                        TheRouter.build("/auth/reset-password").navigation(this)
                    }
                )
            }
        }
    }

    override fun initData() {}

    // -------- 业务逻辑（从 MenuViewModel / AuthDelegate 提取）--------

    private fun handleSendCode(phone: String, scope: kotlinx.coroutines.CoroutineScope) {
        if (phone.length != 11) {
            Toaster.show("请输入正确的11位手机号")
            return
        }
        scope.launch {
            try {
                val response = apiService.sendCode(SendCodeRequest(phone))
                if (response.success) {
                    Toaster.show("验证码已发送！测试码为：${response.code}")
                } else {
                    Toaster.show("验证码发送失败")
                }
            } catch (e: Exception) {
                Toaster.show("网络错误，发送失败")
            }
        }
    }

    private fun handleLoginWithCode(
        phone: String,
        code: String,
        scope: kotlinx.coroutines.CoroutineScope,
        setLoading: (Boolean) -> Unit
    ) {
        if (phone.length != 11 || code.length != 6) {
            Toaster.show("请输入正确的手机号和6位验证码")
            return
        }
        setLoading(true)
        scope.launch {
            try {
                val response = apiService.login(
                    LoginRequest(phone, code, device_uuid = prefs.getUserId())
                )
                if (response.success) {
                    val localPoints = prefs.getPoints()
                    val localLevel = prefs.getUnlockedLevel()
                    val cloudPoints = response.user.points
                    val cloudLevel = response.unlocked_levels.maxOrNull() ?: 1

                    if ((localPoints > 0 || localLevel > 1) &&
                        (localPoints != cloudPoints || localLevel != cloudLevel)
                    ) {
                        // 存档冲突：简单策略取本地优先（不弹窗，因为独立 Activity 不适合复杂交互）
                        // 使用云端数据覆盖本地（安全策略）
                        setLoading(false)
                        saveLoginData(response, scope)
                    } else {
                        saveLoginData(response, scope)
                    }
                } else {
                    setLoading(false)
                    Toaster.show("登录验证失败")
                }
            } catch (e: HttpException) {
                setLoading(false)
                Toaster.show(parseAuthError(e, "验证码错误或已失效"))
            } catch (e: Exception) {
                setLoading(false)
                Toaster.show("验证码错误或已失效")
            }
        }
    }

    private fun handleLoginWithPassword(
        phone: String,
        password: String,
        scope: kotlinx.coroutines.CoroutineScope,
        setLoading: (Boolean) -> Unit
    ) {
        setLoading(true)
        scope.launch {
            try {
                val response = apiService.loginPassword(PasswordLoginRequest(phone, password))
                if (response.success) {
                    saveLoginData(response, scope)
                } else {
                    setLoading(false)
                    Toaster.show("登录失败")
                }
            } catch (e: HttpException) {
                setLoading(false)
                Toaster.show(parseAuthError(e, "登录失败，请稍后重试"))
            } catch (e: Exception) {
                setLoading(false)
                Toaster.show("网络连接异常，请稍后重试")
            }
        }
    }

    /**
     * 保存登录数据到本地存储（参考 MenuViewModel.saveLoginData）
     */
    private fun saveLoginData(
        response: com.example.sheeps.data.model.LoginResponse,
        scope: kotlinx.coroutines.CoroutineScope
    ) {
        scope.launch {
            try {
                prefs.setToken(response.token)
                prefs.setRefreshToken(response.refreshToken)
                prefs.setPhone(response.user.phone)
                prefs.setUsername(response.user.username)
                prefs.setPoints(response.user.points)
                prefs.setTodaySigned(response.today_signed)
                prefs.setSignStreak(response.sign_streak)

                val now = System.currentTimeMillis()
                localDao.deleteProfile()
                localDao.insertProfile(
                    UserProfileEntity(
                        userId = response.user.id,
                        username = response.user.username,
                        points = response.user.points,
                        isDirty = false,
                        updateTimestamp = now
                    )
                )

                localDao.deleteAllProgress()
                localDao.insertProgressList(response.unlocked_levels.map {
                    UserProgressEntity(
                        levelId = it,
                        score = 0,
                        clearTime = 0,
                        isDirty = false,
                        updateTimestamp = now
                    )
                })

                localDao.deleteAllItems()
                localDao.insertItemList(response.items.map {
                    BackpackItemEntity(
                        itemType = it.item_type,
                        count = it.count,
                        isDirty = false,
                        updateTimestamp = now
                    )
                })

                Toaster.show("登录成功！")
                finish()
            } catch (e: Exception) {
                Toaster.show("保存登录数据失败")
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
