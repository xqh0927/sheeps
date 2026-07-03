package com.example.sheeps.menu.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.sheeps.core.base.BaseMviViewModel
import com.example.sheeps.core.game.SkinConstants
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.core.utils.NetworkMonitor
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.model.LoginResponse
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
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
                highestLevelCleared = prefs.getHighestLevelCleared()
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
            is MenuViewIntent.Logout -> authDelegate.handleLogout(viewModelScope, { handleLoadData() }, ::setEffect)
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
            is MenuViewIntent.DismissUpdate -> updateState { copy(appUpdateInfo = null) }
            is MenuViewIntent.ResetMatchStatus -> updateState { copy(matchStatus = "none") }
        }
    }

    private fun handleLoadData() {
        if (currentState.shopItems.isEmpty()) updateState { copy(isLoading = true) }
        val isLoggedIn = prefs.isLoggedIn()

        viewModelScope.launch {
            try {
                val shopItems = fetchShopItems()
                val notices = try { apiService.getNotices() } catch (e: Exception) { currentState.notices }

                if (isLoggedIn && networkMonitor.isOnline()) {
                    val authHeader = "Bearer ${prefs.getToken()}"
                    syncRepository.syncDirtyData()
                    syncRepository.pullCloudProfile()

                    val dailyTasks = try { apiService.getDailyTasks(authHeader) } catch (e: Exception) { emptyList() }
                    val pointsHistory = try { apiService.getPointsHistory(authHeader) } catch (e: Exception) { emptyList() }
                    val exchangeHistory = try { apiService.getExchangeHistory(authHeader) } catch (e: Exception) { emptyList() }

                    updateState {
                        copy(
                            isLoading = false, isLoggedIn = true, phone = prefs.getPhone() ?: "",
                            shopItems = shopItems, notices = notices, dailyTasks = dailyTasks,
                            pointsHistory = pointsHistory, exchangeHistory = exchangeHistory,
                            todaySigned = prefs.getTodaySigned(), signStreak = prefs.getSignStreak(),
                            highestLevelCleared = prefs.getHighestLevelCleared()
                        )
                    }
                } else {
                    updateState {
                        copy(
                            isLoading = false, isLoggedIn = isLoggedIn, phone = prefs.getPhone() ?: "",
                            shopItems = shopItems, notices = notices, dailyTasks = emptyList(),
                            pointsHistory = emptyList(), exchangeHistory = emptyList(),
                            todaySigned = prefs.getTodaySigned(), signStreak = prefs.getSignStreak(),
                            highestLevelCleared = prefs.getHighestLevelCleared()
                        )
                    }
                }
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                setEffect(MenuViewEffect.ShowToast("连接服务异常，已载入本地离线数据"))
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
                ShopItem(1000 + index, "${province.name}美食", "解锁${province.name}省特色美食图标皮肤", "", "SKIN_${province.id.uppercase()}", 200, 9999)
            }.filter { it.item_type !in allExistingTypes }
            remoteItems + localAnimatedSkins + gourmetSkins
        } catch (e: Exception) {
            currentState.shopItems
        }
    }

    private fun saveLoginData(response: LoginResponse) {
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
                setEffect(MenuViewEffect.ShowToast("登录成功！"))
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast("保存登录数据失败"))
            }
        }
    }

    private fun handleUpdateCarryItem(itemType: String, change: Int) {
        val state = currentState
        val stock = state.backpackItems.find { it.item_type == itemType }?.count ?: 0
        val current = state.selectedCarryItems[itemType] ?: 0
        val target = current + change

        if (target < 0) return
        if (target > stock) return setEffect(MenuViewEffect.ShowToast("携带数量超出库存上限"))
        if (change > 0 && current == 0 && state.selectedCarryItems.count { it.value > 0 } >= 5) {
            return setEffect(MenuViewEffect.ShowToast("最多只能选择5种道具"))
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
