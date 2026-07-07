package com.example.sheeps.menu.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.sheeps.core.R
import com.example.sheeps.core.base.BaseMviViewModel
import com.example.sheeps.core.game.SkinConstants
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.core.utils.NetworkMonitor
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.model.LoginResponse
import com.example.sheeps.data.model.RenameRequest
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.data.model.UserItem
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.repository.SyncRepository
import com.example.sheeps.menu.state.MenuViewEffect
import com.example.sheeps.menu.state.MenuViewIntent
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.viewmodel.delegates.AuthDelegate
import com.example.sheeps.menu.viewmodel.delegates.MatchmakingDelegate
import com.example.sheeps.menu.viewmodel.delegates.SocialActionDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

/**
 * 菜单主界面 ViewModel
 * 负责状态流转、核心业务调度。具体业务逻辑已委派至各 Delegate 实现，保持此类简洁。
 */
@HiltViewModel
class MenuViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val context: android.content.Context,
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val json: Json,
    private val localDao: LocalDao,
    private val syncRepository: SyncRepository,
    private val networkMonitor: NetworkMonitor,
    // 业务委派
    private val authDelegate: AuthDelegate,
    private val socialActionDelegate: SocialActionDelegate,
    private val matchmakingDelegate: MatchmakingDelegate
) : BaseMviViewModel<MenuViewState, MenuViewIntent, MenuViewEffect>(MenuViewState()) {

    private var pendingLoginResponse: LoginResponse? = null

    init {
        setupObservers()
        initData()
    }

    private fun setupObservers() {
        // 观察数据库用户信息变化
        viewModelScope.launch {
            localDao.observeProfile().collectLatest { profile ->
                profile?.let { updateState { copy(username = it.username, points = it.points) } }
            }
        }
        // 观察关卡进度
        viewModelScope.launch {
            localDao.observeAllProgress().collectLatest { list ->
                updateState { copy(unlockedLevel = list.map { it.levelId }.maxOrNull() ?: 1) }
            }
        }
        // 观察背包物品
        viewModelScope.launch {
            localDao.observeAllItems().collectLatest { list ->
                updateState { copy(backpackItems = list.map { UserItem(it.itemType, it.count) }) }
            }
        }
        // 观察网络状态
        viewModelScope.launch {
            networkMonitor.status.collectLatest { status -> updateState { copy(networkStatus = status) } }
        }
    }

    private fun initData() {
        updateState {
            copy(
                language = prefs.getLanguage(),
                currentSkin = prefs.getCurrentSkin(),
                todaySigned = prefs.getTodaySigned(),
                signStreak = prefs.getSignStreak(),
                highestLevelCleared = prefs.getHighestLevelCleared(),
                avatarUrl = prefs.getAvatarUrl()
            )
        }
        handleLoadData()
        checkAppUpdateOnce()
    }

    override fun handleIntent(intent: MenuViewIntent) {
        when (intent) {
            is MenuViewIntent.LoadData -> handleLoadData()
            is MenuViewIntent.SendSmsCode -> authDelegate.handleSendSmsCode(viewModelScope, intent.phone, ::setEffect)
            is MenuViewIntent.LoginWithCode -> authDelegate.handleLoginWithCode(
                viewModelScope, intent.phone, intent.code,
                onSuccess = { saveLoginData(it) },
                onConflict = { res, lp, ll, cp, cl ->
                    pendingLoginResponse = res
                    updateState { copy(isLoading = false) }
                    setEffect(MenuViewEffect.ShowConflictDialog(lp, ll, cp, cl))
                },
                setLoading = { updateState { copy(isLoading = it) } },
                setEffect = ::setEffect
            )
            is MenuViewIntent.LoginWithPassword -> handleLoginWithPassword(intent.phone, intent.password)
            is MenuViewIntent.Register -> handleRegister(intent.phone, intent.password, intent.code)
            is MenuViewIntent.ResetPassword -> handleResetPassword(intent.phone, intent.code, intent.newPassword)
            is MenuViewIntent.SetPassword -> handleSetPassword(intent.password)
            is MenuViewIntent.CheckPassword -> handleCheckPassword()
            is MenuViewIntent.Logout -> {
                updateState { copy(isLoading = true) }
                authDelegate.handleLogout(viewModelScope, {
                    updateState { copy(isLoading = false) }
                    handleLoadData()
                }, ::setEffect)
            }
            is MenuViewIntent.ResolveConflict -> authDelegate.handleResolveConflict(
                viewModelScope, pendingLoginResponse, intent.useLocal,
                onComplete = { pendingLoginResponse = null; handleLoadData() },
                setLoading = { updateState { copy(isLoading = it) } },
                setEffect = ::setEffect
            )
            is MenuViewIntent.SignIn -> socialActionDelegate.handleSignIn(viewModelScope, { handleLoadData() }, ::setEffect)
            is MenuViewIntent.ExchangeShopItem -> socialActionDelegate.handleExchangeShopItem(
                viewModelScope, intent.shopItemId, intent.count, currentState.shopItems,
                { handleLoadData() }, ::setEffect
            )
            is MenuViewIntent.ClaimTask -> socialActionDelegate.handleClaimTask(
                viewModelScope, intent.taskId, currentState.dailyTasks, { handleLoadData() }, ::setEffect
            )
            is MenuViewIntent.UnlockLevelWithPoints -> socialActionDelegate.handleUnlockLevelWithPoints(
                viewModelScope, intent.levelId, { handleLoadData() }, ::setEffect
            )
            is MenuViewIntent.JoinMatch -> matchmakingDelegate.handleJoinMatch(viewModelScope, intent.playerId, currentState, ::updateState)
            is MenuViewIntent.LeaveMatch -> matchmakingDelegate.handleLeaveMatch(viewModelScope, intent.playerId)
            
            is MenuViewIntent.UpdateCarryItem -> handleUpdateCarryItem(intent.itemType, intent.change)
            is MenuViewIntent.ClearCarryItems -> updateState { copy(selectedCarryItems = emptyMap()) }
            is MenuViewIntent.GoToGame -> setEffect(MenuViewEffect.NavigateToGame(intent.levelId, intent.carryItemsJson))
            is MenuViewIntent.ChangeLanguage -> handleChangeLanguage(intent.lang)
            is MenuViewIntent.ChangeSkin -> handleChangeSkin(intent.skin)
            is MenuViewIntent.ChangeAvatar -> handleChangeAvatar(intent.imageBytes, intent.fileName)
            is MenuViewIntent.UpdateNickname -> handleUpdateNickname(intent.nickname)
            is MenuViewIntent.DismissUpdate -> updateState { copy(appUpdateInfo = null) }
            is MenuViewIntent.ResetMatchStatus -> updateState { copy(matchStatus = "none") }
        }
    }

    private fun handleLoadData() {
        if (currentState.shopItems.isEmpty()) updateState { copy(isLoading = true) }
        val isLoggedIn = prefs.isLoggedIn()

        viewModelScope.launch {
            try {
                coroutineScope {
                    // 并行发起所有独立请求
                    val shopItemsDef = async { fetchShopItems() }
                    val noticesDef = async { try { apiService.getNotices() } catch (e: Exception) { currentState.notices } }

                    if (isLoggedIn && networkMonitor.isOnline()) {
                        val authHeader = "Bearer ${prefs.getToken()}"

                        // Sync（先推后拉，必须顺序）+ 拉取头像，合并为一条协程
                        val profileDef = async {
                            syncRepository.syncDirtyData()
                            syncRepository.pullCloudProfile()
                            try { apiService.getUserProfile(authHeader).avatarUrl ?: "" }
                            catch (e: Exception) { prefs.getAvatarUrl() }
                        }
                        val dailyDef = async { try { apiService.getDailyTasks(authHeader) } catch (e: Exception) { emptyList() } }
                        val pointsDef = async { try { apiService.getPointsHistory(authHeader) } catch (e: Exception) { emptyList() } }
                        val exchangeDef = async { try { apiService.getExchangeHistory(authHeader) } catch (e: Exception) { emptyList() } }

                        // 等待全部完成
                        val shopItems = shopItemsDef.await()
                        val notices = noticesDef.await()
                        val dailyTasks = dailyDef.await()
                        val pointsHistory = pointsDef.await()
                        val exchangeHistory = exchangeDef.await()
                        val profileAvatarUrl = profileDef.await()

                        if (profileAvatarUrl.isNotEmpty()) {
                            prefs.setAvatarUrl(profileAvatarUrl)
                        }

                        updateState {
                            copy(
                                isLoading = false, isLoggedIn = true, phone = prefs.getPhone() ?: "",
                                username = prefs.getUsername(),
                                shopItems = shopItems, notices = notices, dailyTasks = dailyTasks,
                                pointsHistory = pointsHistory, exchangeHistory = exchangeHistory,
                                todaySigned = prefs.getTodaySigned(), signStreak = prefs.getSignStreak(),
                                highestLevelCleared = prefs.getHighestLevelCleared(),
                                avatarUrl = prefs.getAvatarUrl()
                            )
                        }
                    } else {
                        val shopItems = shopItemsDef.await()
                        val notices = noticesDef.await()
                        updateState {
                            copy(
                                isLoading = false, isLoggedIn = false, username = "", phone = "",
                                points = 0,
                                shopItems = shopItems, notices = notices, dailyTasks = emptyList(),
                                pointsHistory = emptyList(), exchangeHistory = emptyList(),
                                todaySigned = false, signStreak = 0,
                                highestLevelCleared = 0, avatarUrl = ""
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_offline_fallback))
            }
        }
    }

    private suspend fun fetchShopItems(): List<ShopItem> {
        return try {
            val remoteItems = apiService.getShopItems()
            val remoteTypes = remoteItems.map { it.item_type }.toSet()

            // 本地注册动画系列皮肤（客户端生成，无需服务端配置）
            val localAnimatedSkins = listOf(
                ShopItem(
                    id = -100,
                    name = context.getString(com.example.sheeps.core.R.string.item_skin_keai),
                    description = context.getString(com.example.sheeps.core.R.string.item_skin_keai_desc),
                    image_url = "",
                    item_type = "SKIN_KEAI",
                    points_price = 200,
                    stock = 9999
                ),
                ShopItem(
                    id = -101,
                    name = context.getString(com.example.sheeps.core.R.string.item_skin_daimeng),
                    description = context.getString(com.example.sheeps.core.R.string.item_skin_daimeng_desc),
                    image_url = "",
                    item_type = "SKIN_DAIMENG",
                    points_price = 200,
                    stock = 9999
                )
            )
            val animatedTypes = localAnimatedSkins.map { it.item_type }.toSet()
            val allExistingTypes = remoteTypes + animatedTypes

            val gourmetSkins = SkinConstants.provinces.mapIndexed { index, province ->
                ShopItem(
                    1000 + index,
                    context.getString(R.string.province_skin_title, province.name),
                    context.getString(R.string.province_skin_desc, province.name),
                    "",
                    "SKIN_${province.id.uppercase()}",
                    200,
                    9999
                )
            }.filter { it.item_type !in allExistingTypes }
            remoteItems + localAnimatedSkins + gourmetSkins
        } catch (e: Exception) {
            currentState.shopItems
        }
    }

    private fun handleLoginWithPassword(phone: String, password: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                val response = apiService.loginPassword(
                    com.example.sheeps.data.model.PasswordLoginRequest(phone, password)
                )
                if (response.success) {
                    saveLoginData(response)
                } else {
                    updateState { copy(isLoading = false) }
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_login_failed))
                }
            } catch (e: HttpException) {
                updateState { copy(isLoading = false) }
                val errorMsg = parseAuthError(e)
                if (errorMsg != null) {
                    setEffect(MenuViewEffect.ShowToast(message = errorMsg))
                } else {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_login_failed_retry))
                }
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_network_error_retry))
            }
        }
    }

    /**
     * 解析认证接口错误响应体，直接提取服务端返回的 error 字段
     */
    private fun parseAuthError(e: HttpException): String? {
        return try {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            val errorJson = JSONObject(errorBody)
            val error = errorJson.optString("error", "")
            error.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun handleRegister(phone: String, password: String, code: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                val response = apiService.registerAuth(
                    com.example.sheeps.data.model.RegisterAuthRequest(phone, password, code)
                )
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_register_success))
                    updateState { copy(isLoading = false) }
                } else {
                    updateState { copy(isLoading = false) }
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_register_failed))
                }
            } catch (e: HttpException) {
                updateState { copy(isLoading = false) }
                val errorMsg = parseAuthError(e)
                if (errorMsg != null) {
                    setEffect(MenuViewEffect.ShowToast(message = errorMsg))
                } else {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_register_failed_retry))
                }
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_network_error_retry))
            }
        }
    }

    private fun handleResetPassword(phone: String, code: String, newPassword: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                val response = apiService.resetPassword(
                    com.example.sheeps.data.model.ResetPasswordRequest(phone, code, newPassword)
                )
                updateState { copy(isLoading = false) }
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_reset_password_success))
                } else {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_reset_password_failed))
                }
            } catch (e: HttpException) {
                updateState { copy(isLoading = false) }
                val errorMsg = parseAuthError(e)
                if (errorMsg != null) {
                    setEffect(MenuViewEffect.ShowToast(message = errorMsg))
                } else {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_reset_password_failed_retry))
                }
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_network_error_retry))
            }
        }
    }

    private fun handleSetPassword(password: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                val authHeader = "Bearer ${prefs.getToken()}"
                val response = apiService.setPassword(
                    authHeader,
                    com.example.sheeps.data.model.SetPasswordRequest(password)
                )
                updateState { copy(isLoading = false) }
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_set_password_success))
                    handleLoadData()
                } else {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_set_password_failed))
                }
            } catch (e: HttpException) {
                updateState { copy(isLoading = false) }
                val errorMsg = parseAuthError(e)
                if (errorMsg != null) {
                    setEffect(MenuViewEffect.ShowToast(message = errorMsg))
                } else {
                    setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_set_password_failed_retry))
                }
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_network_error_retry))
            }
        }
    }

    private fun handleCheckPassword() {
        viewModelScope.launch {
            try {
                val authHeader = "Bearer ${prefs.getToken()}"
                val response = apiService.checkPassword(authHeader)
                if (!response.hasPassword) {
                    setEffect(MenuViewEffect.ShowSetPasswordDialog)
                }
            } catch (_: Exception) { /* 静默失败 */ }
        }
    }

    private fun saveLoginData(response: com.example.sheeps.data.model.LoginResponse) {
        viewModelScope.launch {
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
                    com.example.sheeps.data.local.UserProfileEntity(
                        userId = response.user.id,
                        username = response.user.username,
                        points = response.user.points,
                        isDirty = false,
                        updateTimestamp = now
                    )
                )

                localDao.deleteAllProgress()
                localDao.insertProgressList(response.unlocked_levels.map {
                    com.example.sheeps.data.local.UserProgressEntity(
                        levelId = it,
                        score = 0,
                        clearTime = 0,
                        isDirty = false,
                        updateTimestamp = now
                    )
                })

                localDao.deleteAllItems()
                localDao.insertItemList(response.items.map {
                    com.example.sheeps.data.local.BackpackItemEntity(
                        itemType = it.item_type,
                        count = it.count,
                        isDirty = false,
                        updateTimestamp = now
                    )
                })

                handleLoadData()
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_login_success))

                // 登录后检查是否已设置密码，若未设置则弹出强制设密对话框
                if (!response.hasPassword) {
                    setEffect(MenuViewEffect.ShowSetPasswordDialog)
                }
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_save_login_data_failed))
            }
        }
    }

    private fun handleUpdateCarryItem(itemType: String, change: Int) {
        val state = currentState
        val stock = state.backpackItems.find { it.item_type == itemType }?.count ?: 0
        val current = state.selectedCarryItems[itemType] ?: 0
        val target = current + change

        if (target < 0) return
        if (target > stock) return setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_carry_limit_exceeded))
        if (change > 0 && current == 0 && state.selectedCarryItems.count { it.value > 0 } >= 5) {
            return setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_carry_limit_items))
        }

        val updated = state.selectedCarryItems.toMutableMap()
        if (target == 0) updated.remove(itemType) else updated[itemType] = target
        updateState { copy(selectedCarryItems = updated) }
    }

    private fun handleChangeLanguage(lang: String) {
        prefs.setLanguage(lang)
        updateState { copy(language = lang) }
        handleLoadData()
    }

    private fun handleChangeSkin(skin: String) {
        prefs.setCurrentSkin(skin)
        updateState { copy(currentSkin = skin) }
    }

    /**
     * 处理头像变更请求
     * Luban 压缩后的字节数组直接以 multipart/form-data 上传至 R2
     */
    private fun handleChangeAvatar(imageBytes: ByteArray, fileName: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                val authHeader = "Bearer ${prefs.getToken()}"
                val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("avatar", fileName, requestBody)
                val uploadResponse = apiService.uploadAvatar(authHeader, part)
                val url = uploadResponse.avatarUrl ?: ""
                prefs.setAvatarUrl(url)
                updateState { copy(avatarUrl = url, isLoading = false) }
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_avatar_update_success))
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_avatar_upload_failed))
            }
        }
    }

    private fun handleUpdateNickname(nickname: String) {
        viewModelScope.launch {
            try {
                apiService.rename(RenameRequest(nickname))
                updateState { copy(username = nickname) }
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_nickname_update_success))
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast(resId = R.string.toast_nickname_update_failed))
            }
        }
    }

    private fun checkAppUpdateOnce() {
        viewModelScope.launch {
            try {
                if (networkMonitor.isOnline()) {
                    val info = context.packageManager.getPackageInfo(context.packageName, 0)
                    val vCode = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else info.versionCode
                    val appUpdate = apiService.checkUpdate(vCode)
                    updateState { copy(appUpdateInfo = appUpdate) }
                }
            } catch (e: Exception) {}
        }
    }
}