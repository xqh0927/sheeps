package com.example.sheeps.menu.viewmodel.delegates

import com.example.sheeps.core.R
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.local.BackpackItemEntity
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.local.UserProfileEntity
import com.example.sheeps.data.local.UserProgressEntity
import com.example.sheeps.data.model.LoginRequest
import com.example.sheeps.data.model.LoginResponse
import com.example.sheeps.data.model.SendCodeRequest
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.repository.SyncRepository
import com.example.sheeps.menu.state.MenuViewEffect
import com.apkfuns.logutils.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户认证逻辑委派类
 * 处理登录、登出、验证码发送、存档冲突解决
 */
class AuthDelegate @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val localDao: LocalDao,
    private val syncRepository: SyncRepository
) {
    /**
     * 发送短信验证码
     */
    fun handleSendSmsCode(
        scope: CoroutineScope,
        phone: String,
        setEffect: (MenuViewEffect) -> Unit
    ) {
        if (phone.length != 11) {
            setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_phone_invalid))
            return
        }
        scope.launch {
            try {
                val response = apiService.sendCode(SendCodeRequest(phone))
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_send_code_success, formatArgs = listOf(response.code ?: "")))
                } else {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_send_code_failed))
                }
            } catch (e: Exception) {
                LogUtils.e("sendCode失败", e)
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_send_code_network_error))
            }
        }
    }

    /**
     * 处理登录逻辑
     */
    fun handleLoginWithCode(
        scope: CoroutineScope,
        phone: String,
        code: String,
        onSuccess: (LoginResponse) -> Unit,
        onConflict: (LoginResponse, Int, Int, Int, Int) -> Unit,
        setLoading: (Boolean) -> Unit,
        setEffect: (MenuViewEffect) -> Unit
    ) {
        if (phone.length != 11 || code.length != 6) {
            setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_phone_code_invalid))
            return
        }
        setLoading(true)
        scope.launch {
            try {
                val response = apiService.login(LoginRequest(phone, code, device_uuid = prefs.getUserId()))
                if (response.success) {
                    val localPoints = prefs.getPoints()
                    val localLevel = prefs.getUnlockedLevel()
                    val cloudPoints = response.user.points
                    val cloudLevel = response.unlocked_levels.maxOrNull() ?: 1

                    if ((localPoints > 0 || localLevel > 1) && (localPoints != cloudPoints || localLevel != cloudLevel)) {
                        onConflict(response, localPoints, localLevel, cloudPoints, cloudLevel)
                    } else {
                        onSuccess(response)
                    }
                } else {
                    setLoading(false)
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_login_verification_failed))
                }
            } catch (e: Exception) {
                setLoading(false)
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_code_invalid_or_expired))
            }
        }
    }

    /**
     * 解决存档冲突
     */
    fun handleResolveConflict(
        scope: CoroutineScope,
        pendingResponse: LoginResponse?,
        useLocal: Boolean,
        onComplete: () -> Unit,
        setLoading: (Boolean) -> Unit,
        setEffect: (MenuViewEffect) -> Unit
    ) {
        val response = pendingResponse ?: return
        setLoading(true)
        scope.launch {
            try {
                prefs.setToken(response.token)
                prefs.setRefreshToken(response.refreshToken)
                prefs.setPhone(response.user.phone)

                val now = System.currentTimeMillis()
                if (useLocal) {
                    resolveWithLocal(response, now)
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_conflict_resolved_local))
                } else {
                    resolveWithCloud(response, now)
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_conflict_resolved_cloud))
                }
                onComplete()
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_conflict_resolve_error))
            } finally {
                setLoading(false)
            }
        }
    }

    private suspend fun resolveWithLocal(response: LoginResponse, now: Long) {
        val localProfile = localDao.getProfile()
        localDao.deleteProfile()
        localDao.insertProfile(
            UserProfileEntity(
                userId = response.user.id,
                username = localProfile?.username ?: prefs.getUsername(),
                points = localProfile?.points ?: prefs.getPoints(),
                isDirty = true,
                updateTimestamp = now
            )
        )

        localDao.insertProgressList(localDao.getAllProgress().map {
            it.copy(isDirty = true, updateTimestamp = now)
        })

        localDao.insertItemList(localDao.getAllItems().map {
            it.copy(isDirty = true, updateTimestamp = now)
        })

        syncRepository.syncDirtyData()
    }

    private suspend fun resolveWithCloud(response: LoginResponse, now: Long) {
        prefs.setUsername(response.user.username)
        prefs.setPoints(response.user.points)

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
            UserProgressEntity(levelId = it, score = 0, clearTime = 0, isDirty = false, updateTimestamp = now)
        })

        localDao.deleteAllItems()
        localDao.insertItemList(response.items.map {
            BackpackItemEntity(itemType = it.item_type, count = it.count, isDirty = false, updateTimestamp = now)
        })
    }

    /**
     * 处理登出
     */
    fun handleLogout(scope: CoroutineScope, onComplete: () -> Unit, setEffect: (MenuViewEffect) -> Unit) {
        scope.launch {
            localDao.deleteProfile()
            localDao.deleteAllProgress()
            localDao.deleteAllItems()
            prefs.logout()

            // 重新插入默认进度（仅等级 1，不插入 profile 以避免 observer 覆盖已清除的 state）
            val now = System.currentTimeMillis()
            localDao.insertProgress(
                UserProgressEntity(levelId = 1, score = 0, clearTime = 0, isDirty = false, updateTimestamp = now)
            )

            setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_logout_success))
            // 确保加载指示器有足够渲染时间
            kotlinx.coroutines.delay(400L)
            onComplete()
        }
    }
}
