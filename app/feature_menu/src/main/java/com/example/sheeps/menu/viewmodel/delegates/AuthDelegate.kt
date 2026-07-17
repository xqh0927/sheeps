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
 * 处理登录、登出、验证码发送、存档冲突解决。
 *
 * 持有关系：由 [com.example.sheeps.menu.viewmodel.MenuViewModel] 通过 Hilt 注入并持有，
 * 专门负责认证相关的子状态（登录态、登出、验证码、本地/云端存档冲突）。
 * 线程边界：所有公开方法均接收调用方传入的 [CoroutineScope]（实为 MenuViewModel.viewModelScope），
 * 网络请求在挂起函数内自动切到 IO；作用域随 ViewModel 销毁而取消，委派类本身不创建独立协程作用域。
 */
class AuthDelegate @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val localDao: LocalDao,
    private val syncRepository: SyncRepository
) {
    /**
     * 发送短信验证码。
     * @param scope 调用方协程作用域（MenuViewModel.viewModelScope），退出登录/页面切换时自动取消。
     * @param phone 手机号，长度非 11 位直接在 UI 线程拦截并回调错误 Effect，不发起请求。
     * @param setEffect 向 UI 派发一次性副作用（成功 / 失败 Toast）的回调。
     * 线程边界：参数校验在主线程；[apiService.sendCode] 挂起函数在 IO 线程执行。
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
     * 验证码登录，并检测本地存档与云端的积分/关卡冲突。
     * @param scope 调用方协程作用域（MenuViewModel.viewModelScope）。
     * @param phone 手机号（须 11 位）。
     * @param code 短信验证码（须 6 位）。
     * @param onSuccess 登录成功且无冲突时的回调，携带 [LoginResponse]。
     * @param onConflict 本地与云端数据不一致时的回调，参数依次为 (响应, 本地积分, 本地关卡, 云端积分, 云端关卡)。
     * @param setLoading 控制 UI 加载态的回调。
     * @param setEffect 派发一次性副作用（校验失败 / 验证码错误 Toast）。
     * 线程边界：UI 线程做参数校验与 setLoading(true)；[apiService.login] 在 IO 线程。
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
     * 解决本地/云端存档冲突。
     * @param scope 调用方协程作用域（MenuViewModel.viewModelScope）。
     * @param pendingResponse 之前登录时暂存的 [LoginResponse]，为空则直接返回（无操作）。
     * @param useLocal true=以本地存档为准并标记 dirty 待同步；false=以云端覆盖本地。
     * @param onComplete 处理完成（含成功/失败）后的回调，通常用于重新拉取数据。
     * @param setLoading 控制 UI 加载态。
     * @param setEffect 派发「本地/云端解决成功」或「解决失败」Toast。
     * 线程边界：本地 DB 写入与 [syncRepository.syncDirtyData] 在挂起函数内切 IO；finally 中恢复加载态。
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
     * 处理登出：清空本地数据库与用户偏好，并重置默认关卡进度。
     * @param scope 调用方协程作用域（MenuViewModel.viewModelScope）。
     * @param onComplete 登出完成后的回调（通常由 ViewModel 重新拉取数据刷新 UI）。
     * @param setEffect 派发登出成功 Toast。
     * 线程边界：DB 删除/插入在挂起函数内切 IO；末尾 [kotlinx.coroutines.delay] 仅为了让加载指示器有足够渲染时间。
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
