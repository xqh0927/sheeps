package com.example.sheeps.menu.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import coil.Coil
import coil.request.ImageRequest
 import coil.size.Scale
import com.example.sheeps.core.R
import com.example.sheeps.core.base.BaseMviViewModel
import com.example.sheeps.core.cache.ShopCache
import com.example.sheeps.core.game.TileIconProvider
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.size.Size
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

/**
 * MVI 架构与持有关系说明：
 * - State：[MenuViewState] 为单一不可变状态源，所有变更均通过 [updateState]（复制式 copy）完成；
 * - Intent：[MenuViewIntent]，UI 事件统一在 [handleIntent] 中按密封类型分发；
 * - Effect：[MenuViewEffect]，一次性副作用（弹窗 / 导航）通过 [setEffect] 派发，不进入状态树。
 * 认证 / 社交 / 匹配等子状态逻辑分别委派给 AuthDelegate / SocialActionDelegate / MatchmakingDelegate，
 * 三者由 Hilt 注入并由本 ViewModel 持有，复用同一 viewModelScope。
 * 注意：注入的 context 标注 @ApplicationContext，为 Application 级 Context，不持有 Activity/View 引用，无内存泄漏风险。
 */
@HiltViewModel
class MenuViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val context: android.content.Context,
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val json: Json,
    private val shopCache: ShopCache,
    private val localDao: LocalDao,
    private val syncRepository: SyncRepository,
    private val networkMonitor: NetworkMonitor,
    // 业务委派
    private val authDelegate: AuthDelegate,
    private val socialActionDelegate: SocialActionDelegate,
    private val matchmakingDelegate: MatchmakingDelegate
) : BaseMviViewModel<MenuViewState, MenuViewIntent, MenuViewEffect>(MenuViewState()) {

    // 暂存登录响应，供存档冲突对话框「解决冲突」时回放；为 ViewModel 级字段且仅持有数据对象，不引用 Activity/View，无泄漏风险。
    private var pendingLoginResponse: LoginResponse? = null

    // ⚠️ 内存隐患：ProcessLifecycleOwner 为进程级生命周期（贯穿整个 App 而非单个 Activity）。
    // 若忘记在 onCleared 中 removeObserver，该匿名 DefaultLifecycleObserver 会长期持有本 ViewModel 导致泄漏。
    // 安全：本对象为 ViewModel 内部匿名类，不引用 Activity；且已在 onCleared() 反注册（见下方）。
    /** 切回前台 / 定时静默刷新的进程级生命周期观察者，onCleared 时移除，避免泄漏。 */
    private val shopRefreshObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            // 忽略进程首次启动（initData 已拉取），仅当切回前台时静默刷新
            if (firstForegroundPassed) {
                refreshShopItems()
            } else {
                firstForegroundPassed = true
            }
        }
    }

    /** 进程首次 onStart 不触发刷新（initData 已覆盖启动拉取）。 */
    private var firstForegroundPassed = false

    /** 定时刷新协程句柄，onCleared 时取消。 */
    private var periodicRefreshJob: Job? = null

    init {
        setupObservers()
        initData()
        // 切回前台 / 定时（约 30min）静默刷新商城（多语言动态下发 + 本地缓存变更检测）
        // 线程边界：addObserver / startPeriodicShopRefresh 在主线程调用，内部刷新逻辑经 viewModelScope 切到 IO（Retrofit 挂起函数主安全）。
        ProcessLifecycleOwner.get().lifecycle.addObserver(shopRefreshObserver)
        startPeriodicShopRefresh()
    }

    override fun onCleared() {
        super.onCleared()
        // 资源释放：反注册进程级生命周期观察者 + 取消定时刷新 Job，避免 ViewModel 销毁后仍被引用或空跑协程。
        ProcessLifecycleOwner.get().lifecycle.removeObserver(shopRefreshObserver)
        periodicRefreshJob?.cancel()
    }

    private fun setupObservers() {
        // ⚠️ 内存隐患（已规避）：以下 4 处均通过 viewModelScope.launch { flow.collectLatest { } } 收集本地数据库 / 网络状态流。
        // viewModelScope 由 ViewModel 生命周期托管，ViewModel 被清除（onCleared）时作用域自动取消，
        // collectLatest 随之解除收集，不会因匿名收集器持有引用而泄漏；切勿改用 GlobalScope 或自行创建的 CoroutineScope。
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
                avatarUrl = prefs.getAvatarUrl(),
                // 首屏先用本地缓存快照，避免空白；后续 handleLoadData 会拉取并比对刷新
                shopItems = shopCache.getCachedItems()
            )
        }
        handleLoadData()
        checkAppUpdateOnce()
    }

    /**
     * MVI 的 Intent 分发中枢。
     * 接收 UI 发来的 [MenuViewIntent]，按密封类型路由到对应的私有处理函数或委派给各 Delegate；
     * 调用线程：与主线程（UI 调用方）一致，内部耗时操作一律通过 viewModelScope.launch 切到后台。
     * 注意：本方法应保持「只分发、不写业务」，具体状态/副作用变更交给被调用方处理。
     */
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

    /**
     * 拉取并聚合首屏 / 刷新所需数据（商城、公告、用户资料、任务、积分/兑换历史）。
     * 线程边界：外层 viewModelScope.launch 运行于主线程（Dispatchers.Main.immediate），
     * 内部 apiService.* 与 syncRepository.* 均为 Retrofit/Room 挂起函数（主安全，自动切 IO），无需手动切线程。
     * 并行策略：通过 coroutineScope + async 并发独立请求，统一 await 后一次性合并写入 State。
     */
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

                        // Sync：先拉后推，避免本地旧数据覆盖服务器新数据（管理后台修改后）
                        val profileDef = async {
                            syncRepository.pullCloudProfile()
                            syncRepository.syncDirtyData()
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

    /**
     * 拉取商城列表（方案 A：多语言动态下发 + 本地缓存 + 变更检测）。
     *
     *  - 直接信任服务端 /api/shop/items 返回的（已按请求语言本地化的）[ShopItem] 列表，
     *    不再在客户端用 context.getString(R.string.xxx) 本地拼装皮肤 / 道具文案。
     *  - 注入 [TileIconProvider] URL 注册表（图片下发核心），保持卡面渲染能力。
     *  - 通过 [ShopCache.saveIfChanged] 做内容比对：仅当服务端内容相对本地缓存有变化时才写盘；
     *    返回结果始终交给 UI，用户完全无感。
     *  - 离线 / 首启 / 请求失败时回退到 [ShopCache.getCachedItems] 上次缓存快照，保证有内容可显示。
     */
    private suspend fun fetchShopItems(): List<ShopItem> {
        return try {
            val remoteItems = apiService.getShopItems()
            // 注入 URL 注册表：建立 skin_tiles / item_icons 映射与分组数据（v2 图片下发核心）
            TileIconProvider.setShopItems(remoteItems)
            // 保底预热：首次商城数据就绪后，预载当前皮肤与默认皮肤（解决进对局首张慢）
            preloadSkin(prefs.getCurrentSkin())
            preloadSkin("shuang")
            // 本地缓存 + 变更检测：仅内容变化时写盘（返回是否有变化，调用方据此决定是否刷新）
            shopCache.saveIfChanged(remoteItems)
            remoteItems
        } catch (e: Exception) {
            // 离线 / 首启 / 请求失败：回退到上次缓存快照（MMKV 持久化）
            shopCache.getCachedItems()
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
        preloadSkin(skin)
    }

    /**
     * 换肤后后台预加载新皮肤的 12 张卡面图片（Coil 内存/磁盘缓存），
     * 避免进入对局后首张牌面加载缓慢。enqueue 为非阻塞调用，使用 viewModelScope 默认调度。
     */
    private fun preloadSkin(skinKey: String) {
        viewModelScope.launch {
            val px = (48f * context.resources.displayMetrics.density).toInt()
            TileIconProvider.getSkinTileUrls(skinKey).forEach { url ->
                Coil.imageLoader(context).enqueue(
                    ImageRequest.Builder(context)
                        .data(url)
                        .size(Size(px, px))
                        .scale(Scale.FIT)
                        .build()
                )
            }
        }
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
        // 记录起始时间，避免紧随其后的 onResume 立即重复请求
        lastGameModesRefreshMs = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                if (networkMonitor.isOnline()) {
                    val info = context.packageManager.getPackageInfo(context.packageName, 0)
                    val vCode = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else info.versionCode
                    val appUpdate = apiService.checkUpdate(vCode)
                    updateState {
                        copy(
                            appUpdateInfo = appUpdate,
                            gameModes = appUpdate.game_modes
                        )
                    }
                }
            } catch (e: Exception) {}
        }
    }

    /**
     * 公开方法：供 Activity onResume 等前台回归场景调用，刷新游戏模式开关状态。
     * 仅回写 [MenuViewState.gameModes]，绝不触碰 [MenuViewState.appUpdateInfo]，
     * 以避免覆盖用户已 dismiss 的更新提示。带 60s 节流，避免冷启动 / 快速切页时的重复请求。
     */
    fun refreshGameModes() {
        val now = System.currentTimeMillis()
        if (now - lastGameModesRefreshMs < GAME_MODES_REFRESH_THROTTLE_MS) return
        lastGameModesRefreshMs = now
        viewModelScope.launch {
            try {
                if (networkMonitor.isOnline()) {
                    val info = context.packageManager.getPackageInfo(context.packageName, 0)
                    val vCode = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else info.versionCode
                    val appUpdate = apiService.checkUpdate(vCode)
                    updateState { copy(gameModes = appUpdate.game_modes) }
                }
            } catch (e: Exception) {}
        }
    }

    /**
     * 启动定时（约 30 分钟）静默刷新协程；仅在 ViewModel 存活期间运行，onCleared 时取消。
     */
    private fun startPeriodicShopRefresh() {
        periodicRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(SHOP_REFRESH_INTERVAL_MS)
                refreshShopItems()
            }
        }
    }

    /**
     * 静默刷新商城（切回前台 / 定时触发）。
     * 直接信任服务端已本地化字段；仅当内容相对本地缓存有变化时才更新 UI，用户完全无感。
     */
    private fun refreshShopItems() {
        viewModelScope.launch {
            val remoteItems = try {
                apiService.getShopItems()
            } catch (e: Exception) {
                return@launch
            }
            // 注入 URL 注册表（图片下发核心）
            TileIconProvider.setShopItems(remoteItems)
            // 仅内容变化时回写缓存并刷新 UI
            if (shopCache.saveIfChanged(remoteItems)) {
                updateState { copy(shopItems = remoteItems) }
            }
        }
    }

    companion object {
        /** 商城定时静默刷新间隔：30 分钟。 */
        private const val SHOP_REFRESH_INTERVAL_MS = 30L * 60 * 1000
        /** 游戏模式刷新节流：同一前台回归若间隔小于该值则跳过，避免重复请求。 */
        private const val GAME_MODES_REFRESH_THROTTLE_MS = 60L * 1000
    }

    /** 上次刷新游戏模式的时间戳（毫秒），用于 onResume 节流。 */
    private var lastGameModesRefreshMs = 0L
}