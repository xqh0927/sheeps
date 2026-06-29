package com.example.sheeps.menu.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.sheeps.core.base.BaseMviViewModel
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.core.utils.NetworkMonitor
import com.example.sheeps.core.utils.NetworkStatus
import com.example.sheeps.data.local.*
import com.example.sheeps.data.model.*
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.repository.SyncRepository
import com.example.sheeps.menu.state.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.lang.Math.max
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val json: Json,
    private val localDao: LocalDao,
    private val syncRepository: SyncRepository,
    private val networkMonitor: NetworkMonitor
) : BaseMviViewModel<MenuViewState, MenuViewIntent, MenuViewEffect>(MenuViewState()) {

    private var pendingLoginResponse: LoginResponse? = null

    init {
        // Observe reactive database updates as single source of truth
        viewModelScope.launch {
            localDao.observeProfile().collectLatest { profileEntity ->
                if (profileEntity != null) {
                    updateState {
                        copy(
                            username = profileEntity.username,
                            points = profileEntity.points
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            localDao.observeAllProgress().collectLatest { progressList ->
                val maxUnlocked = progressList.map { it.levelId }.maxOrNull() ?: 1
                updateState {
                    copy(unlockedLevel = maxUnlocked)
                }
            }
        }

        viewModelScope.launch {
            localDao.observeAllItems().collectLatest { itemList ->
                val items = itemList.map { UserItem(it.itemType, it.count) }
                updateState { copy(backpackItems = items) }
            }
        }

        // Observe network changes
        viewModelScope.launch {
            networkMonitor.status.collectLatest { status ->
                updateState { copy(networkStatus = status) }
            }
        }

        // Initial data loading
        updateState { 
            copy(
                language = prefs.getLanguage(),
                currentSkin = prefs.getCurrentSkin()
            ) 
        }
        sendIntent(MenuViewIntent.LoadData)
        checkAppUpdateOnce()
    }

    override fun handleIntent(intent: MenuViewIntent) {
        when (intent) {
            is MenuViewIntent.LoadData -> handleLoadData()
            is MenuViewIntent.SendSmsCode -> handleSendSmsCode(intent.phone)
            is MenuViewIntent.LoginWithCode -> handleLoginWithCode(intent.phone, intent.code)
            is MenuViewIntent.Logout -> handleLogout()
            is MenuViewIntent.SignIn -> handleSignIn()
            is MenuViewIntent.ExchangeShopItem -> handleExchangeShopItem(intent.shopItemId, intent.count)
            is MenuViewIntent.ClaimTask -> handleClaimTask(intent.taskId)
            is MenuViewIntent.UnlockLevelWithPoints -> handleUnlockLevelWithPoints(intent.levelId)
            is MenuViewIntent.UpdateCarryItem -> handleUpdateCarryItem(intent.itemType, intent.change)
            is MenuViewIntent.ClearCarryItems -> handleClearCarryItems()
            is MenuViewIntent.GoToGame -> handleGoToGame(intent.levelId, intent.carryItemsJson)
            is MenuViewIntent.ResolveConflict -> handleResolveConflict(intent.useLocal)
            is MenuViewIntent.ChangeLanguage -> handleChangeLanguage(intent.lang)
            is MenuViewIntent.DismissUpdate -> updateState { copy(appUpdateInfo = null) }
            is MenuViewIntent.ChangeSkin -> handleChangeSkin(intent.skin)
        }
    }

    private fun handleChangeSkin(skin: String) {
        prefs.setCurrentSkin(skin)
        updateState { copy(currentSkin = skin) }
    }

    private fun checkAppUpdateOnce() {
        viewModelScope.launch {
            try {
                if (networkMonitor.isOnline()) {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode
                    }
                    val appUpdate = apiService.checkUpdate(versionCode)
                    updateState { copy(appUpdateInfo = appUpdate) }
                }
            } catch (e: Exception) {
                // Ignore update fetch errors silently
            }
        }
    }

    private fun handleChangeLanguage(lang: String) {
        prefs.setLanguage(lang)
        updateState { copy(language = lang) }
        handleLoadData()
    }

    private fun handleLoadData() {
        if (currentState.shopItems.isEmpty()) {
            updateState { copy(isLoading = true) }
        }
        val isLoggedIn = prefs.isLoggedIn()

        viewModelScope.launch {
            try {
                // Fetch public shop items & notices
                val shopItems = try {
                    apiService.getShopItems()
                } catch (e: Exception) {
                    currentState.shopItems.ifEmpty { emptyList() }
                }

                val notices = try {
                    apiService.getNotices()
                } catch (e: Exception) {
                    currentState.notices.ifEmpty { emptyList() }
                }



                if (isLoggedIn && networkMonitor.isOnline()) {
                    val authHeader = "Bearer ${prefs.getToken()}"
                    
                    // 1. 先将本地离线脏进度推到云端
                    syncRepository.syncDirtyData()
                    
                    // 2. 再拉取最新的云端进度覆盖本地，防止拉取过快覆盖导致本地关卡解锁丢失
                    syncRepository.pullCloudProfile()

                    val dailyTasks = try {
                        apiService.getDailyTasks(authHeader)
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val pointsHistory = try {
                        apiService.getPointsHistory(authHeader)
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val exchangeHistory = try {
                        apiService.getExchangeHistory(authHeader)
                    } catch (e: Exception) {
                        emptyList()
                    }

                    updateState {
                        copy(
                            isLoading = false,
                            isLoggedIn = true,
                            phone = prefs.getPhone() ?: "",
                            shopItems = shopItems,
                            notices = notices,
                            dailyTasks = dailyTasks,
                            pointsHistory = pointsHistory,
                            exchangeHistory = exchangeHistory
                        )
                    }
                } else {
                    // Offline or Guest view update
                    updateState {
                        copy(
                            isLoading = false,
                            isLoggedIn = isLoggedIn,
                            phone = prefs.getPhone() ?: "",
                            shopItems = shopItems,
                            notices = notices,
                            dailyTasks = emptyList(),
                            pointsHistory = emptyList(),
                            exchangeHistory = emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                setEffect(MenuViewEffect.ShowToast("连接服务异常，已载入本地离线数据"))
            }
        }
    }

    private fun handleSendSmsCode(phone: String) {
        if (phone.length != 11) {
            setEffect(MenuViewEffect.ShowToast("请输入正确的11位手机号"))
            return
        }
        viewModelScope.launch {
            try {
                val response = apiService.sendCode(SendCodeRequest(phone))
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast("验证码已发送！测试码为：${response.code}"))
                } else {
                    setEffect(MenuViewEffect.ShowToast("验证码发送失败"))
                }
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast("网络错误，发送失败"))
            }
        }
    }

    private fun handleLoginWithCode(phone: String, code: String) {
        if (phone.length != 11 || code.length != 6) {
            setEffect(MenuViewEffect.ShowToast("请输入正确的手机号和6位验证码"))
            return
        }
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // Pass guest Device UUID for backend merging
                val response = apiService.login(LoginRequest(phone, code, device_uuid = prefs.getUserId()))
                if (response.success) {
                    val localPoints = prefs.getPoints()
                    val localLevel = prefs.getUnlockedLevel()
                    val cloudPoints = response.user.points
                    val cloudLevel = response.unlocked_levels.maxOrNull() ?: 1

                    // Conflict detection: if local progress has points or level > 1 and differs from cloud
                    if ((localPoints > 0 || localLevel > 1) && (localPoints != cloudPoints || localLevel != cloudLevel)) {
                        pendingLoginResponse = response
                        updateState { copy(isLoading = false) }
                        setEffect(MenuViewEffect.ShowConflictDialog(
                            localPoints = localPoints,
                            localLevel = localLevel,
                            cloudPoints = cloudPoints,
                            cloudLevel = cloudLevel
                        ))
                    } else {
                        // Directly resolve login
                        saveLoginData(response)
                        setEffect(MenuViewEffect.ShowToast("登录成功！"))
                    }
                } else {
                    updateState { copy(isLoading = false) }
                    setEffect(MenuViewEffect.ShowToast("登录验证失败"))
                }
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                setEffect(MenuViewEffect.ShowToast("验证码错误或已失效"))
            }
        }
    }

    private fun handleResolveConflict(useLocal: Boolean) {
        val response = pendingLoginResponse ?: return
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // 1. Save credentials
                prefs.setToken(response.token)
                prefs.setRefreshToken(response.refreshToken)
                prefs.setPhone(response.user.phone)

                val now = System.currentTimeMillis()

                if (useLocal) {
                    // Mark existing local Room data as dirty to sync up
                    val localProfile = localDao.getProfile()
                    localDao.deleteProfile() // 删掉旧游客 Profile
                    if (localProfile != null) {
                        localDao.insertProfile(UserProfileEntity(
                            userId = response.user.id,
                            username = localProfile.username,
                            points = localProfile.points,
                            isDirty = true,
                            updateTimestamp = now
                        ))
                    } else {
                        localDao.insertProfile(UserProfileEntity(
                            userId = response.user.id,
                            username = prefs.getUsername(),
                            points = prefs.getPoints(),
                            isDirty = true,
                            updateTimestamp = now
                        ))
                    }

                    val localProgress = localDao.getAllProgress()
                    localDao.insertProgressList(localProgress.map { it.copy(isDirty = true, updateTimestamp = now) })

                    val localItems = localDao.getAllItems()
                    localDao.insertItemList(localItems.map { it.copy(isDirty = true, updateTimestamp = now) })

                    // Trigger immediate silent sync upload
                    syncRepository.syncDirtyData()
                    setEffect(MenuViewEffect.ShowToast("已保留本地进度并同步至云端！"))
                } else {
                    // Load and write Cloud progress directly, clearing local differences
                    prefs.setUsername(response.user.username)
                    prefs.setPoints(response.user.points)
                    
                    localDao.deleteProfile() // 删掉旧游客 Profile
                    localDao.insertProfile(UserProfileEntity(
                        userId = response.user.id,
                        username = response.user.username,
                        points = response.user.points,
                        isDirty = false,
                        updateTimestamp = now
                    ))

                    localDao.deleteAllProgress()
                    localDao.insertProgressList(response.unlocked_levels.map {
                        UserProgressEntity(levelId = it, score = 0, clearTime = 0, isDirty = false, updateTimestamp = now)
                    })

                    localDao.deleteAllItems()
                    localDao.insertItemList(response.items.map {
                        BackpackItemEntity(itemType = it.item_type, count = it.count, isDirty = false, updateTimestamp = now)
                    })

                    setEffect(MenuViewEffect.ShowToast("已成功加载云端存档进度！"))
                }

                pendingLoginResponse = null
                handleLoadData()
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast("存档冲突解决异常，请检查网络"))
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }

    private suspend fun saveLoginData(response: LoginResponse) {
        prefs.setToken(response.token)
        prefs.setRefreshToken(response.refreshToken)
        prefs.setPhone(response.user.phone)
        prefs.setUsername(response.user.username)
        prefs.setPoints(response.user.points)

        val now = System.currentTimeMillis()
        localDao.deleteProfile() // 删掉旧游客 Profile
        localDao.insertProfile(UserProfileEntity(
            userId = response.user.id,
            username = response.user.username,
            points = response.user.points,
            isDirty = false,
            updateTimestamp = now
        ))

        localDao.deleteAllProgress()
        localDao.insertProgressList(response.unlocked_levels.map {
            UserProgressEntity(levelId = it, score = 0, clearTime = 0, isDirty = false, updateTimestamp = now)
        })

        localDao.deleteAllItems()
        localDao.insertItemList(response.items.map {
            BackpackItemEntity(itemType = it.item_type, count = it.count, isDirty = false, updateTimestamp = now)
        })

        handleLoadData()
    }

    private fun handleLogout() {
        viewModelScope.launch {
            // Clear local DB and local prefs
            localDao.deleteProfile()
            localDao.deleteAllProgress()
            localDao.deleteAllItems()
            prefs.logout()

            // Initialize default local state in database
            val now = System.currentTimeMillis()
            localDao.insertProfile(UserProfileEntity(
                userId = prefs.getUserId(),
                username = prefs.getUsername(),
                points = 0,
                isDirty = false,
                updateTimestamp = now
            ))
            localDao.insertProgress(UserProgressEntity(levelId = 1, score = 0, clearTime = 0, isDirty = false, updateTimestamp = now))

            setEffect(MenuViewEffect.ShowToast("已退出登录，清除本地缓存"))
            handleLoadData()
        }
    }

    private fun handleSignIn() {
        val token = prefs.getToken()
        if (token == null) {
            setEffect(MenuViewEffect.ShowLoginDialog)
            return
        }

        viewModelScope.launch {
            try {
                // 1. Locally update first
                val currentPoints = prefs.getPoints() + 20 // Estimate default daily rewards 20
                localDao.insertProfile(UserProfileEntity(
                    userId = prefs.getUserId(),
                    username = prefs.getUsername(),
                    points = currentPoints,
                    isDirty = true,
                    updateTimestamp = System.currentTimeMillis()
                ))
                prefs.setPoints(currentPoints)

                // 2. Network request in background
                val response = apiService.signIn("Bearer $token")
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast("签到成功！连续签到第 ${response.streak} 天，获得 ${response.reward_points} 积分！"))
                    
                    // Overwrite with actual points returned by server
                    localDao.insertProfile(UserProfileEntity(
                        userId = prefs.getUserId(),
                        username = prefs.getUsername(),
                        points = response.current_points,
                        isDirty = false,
                        updateTimestamp = System.currentTimeMillis()
                    ))
                    prefs.setPoints(response.current_points)
                } else {
                    setEffect(MenuViewEffect.ShowToast("签到失败"))
                }
                handleLoadData()
            } catch (e: Exception) {
                // If offline / network error, points already credited locally. It will sync later.
                setEffect(MenuViewEffect.ShowToast("连接异常，积分已本地入账，后续将自动同步"))
            }
        }
    }

    private fun handleExchangeShopItem(shopItemId: Int, count: Int) {
        val token = prefs.getToken()
        if (token == null) {
            setEffect(MenuViewEffect.ShowLoginDialog)
            return
        }

        val item = currentState.shopItems.find { it.id == shopItemId }
        if (item == null) {
            setEffect(MenuViewEffect.ShowToast("道具未找到"))
            return
        }

        val totalCost = item.points_price * count
        if (prefs.getPoints() < totalCost) {
            setEffect(MenuViewEffect.ShowToast("兑换失败，积分余额不足"))
            return
        }

        viewModelScope.launch {
            try {
                // 1. Local-First deduct points and credit item
                val currentPoints = prefs.getPoints() - totalCost
                prefs.setPoints(currentPoints)

                val localItem = localDao.getAllItems().find { it.itemType == item.item_type }
                val newCount = (localItem?.count ?: 0) + count
                
                localDao.insertProfile(UserProfileEntity(
                    userId = prefs.getUserId(),
                    username = prefs.getUsername(),
                    points = currentPoints,
                    isDirty = true,
                    updateTimestamp = System.currentTimeMillis()
                ))
                localDao.insertItem(BackpackItemEntity(
                    itemType = item.item_type,
                    count = newCount,
                    isDirty = true,
                    updateTimestamp = System.currentTimeMillis()
                ))

                // 2. Sync to cloud in background
                val response = apiService.exchangeItem("Bearer $token", ExchangeRequest(shopItemId, count))
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast("兑换成功，已加入背包！"))
                    // Mark as clean on success
                    localDao.clearProfileDirty(prefs.getUserId())
                    localDao.clearItemDirty(item.item_type)
                } else {
                    setEffect(MenuViewEffect.ShowToast("云端兑换失败，回滚本地扣减"))
                    handleLoadData()
                }
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast("连接受限，积分本地扣减，已记录入背包，稍后将自动同步"))
            }
        }
    }

    private fun handleClaimTask(taskId: String) {
        val token = prefs.getToken()
        if (token == null) {
            setEffect(MenuViewEffect.ShowLoginDialog)
            return
        }

        val task = currentState.dailyTasks.find { it.task_id == taskId }
        if (task == null || !task.is_completed || task.is_rewarded) return

        viewModelScope.launch {
            try {
                // Local-First points claim
                val currentPoints = prefs.getPoints() + task.points_reward
                prefs.setPoints(currentPoints)

                localDao.insertProfile(UserProfileEntity(
                    userId = prefs.getUserId(),
                    username = prefs.getUsername(),
                    points = currentPoints,
                    isDirty = true,
                    updateTimestamp = System.currentTimeMillis()
                ))

                val response = apiService.claimTaskReward("Bearer $token", TaskClaimRequest(taskId))
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast("任务奖励领取成功！积分已增加"))
                    localDao.clearProfileDirty(prefs.getUserId())
                    handleLoadData()
                } else {
                    setEffect(MenuViewEffect.ShowToast("领奖失败"))
                    handleLoadData()
                }
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast("网络连通受限，积分已加入本地，稍后同步"))
            }
        }
    }

    private fun handleUnlockLevelWithPoints(levelId: Int) {
        val token = prefs.getToken()
        if (token == null) {
            setEffect(MenuViewEffect.ShowLoginDialog)
            return
        }

        val cost = if (levelId == 2) 50 else if (levelId == 3) 100 else 200
        if (prefs.getPoints() < cost) {
            setEffect(MenuViewEffect.ShowToast("解锁失败，积分余额不足"))
            return
        }

        viewModelScope.launch {
            try {
                // Local-First unlock
                val currentPoints = prefs.getPoints() - cost
                prefs.setPoints(currentPoints)
                prefs.setUnlockedLevel(max(prefs.getUnlockedLevel(), levelId))

                val now = System.currentTimeMillis()
                localDao.insertProfile(UserProfileEntity(
                    userId = prefs.getUserId(),
                    username = prefs.getUsername(),
                    points = currentPoints,
                    isDirty = true,
                    updateTimestamp = now
                ))

                localDao.insertProgress(UserProgressEntity(
                    levelId = levelId,
                    score = 0,
                    clearTime = 0,
                    isDirty = true,
                    updateTimestamp = now
                ))

                val response = apiService.unlockLevel("Bearer $token", UnlockLevelRequest(levelId))
                if (response.success) {
                    setEffect(MenuViewEffect.ShowToast("积分子系统扣除成功！第 ${levelId} 关已解锁"))
                    localDao.clearProfileDirty(prefs.getUserId())
                    localDao.clearProgressDirty(levelId)
                } else {
                    setEffect(MenuViewEffect.ShowToast("解锁失败，云端同步拒绝"))
                    handleLoadData()
                }
            } catch (e: Exception) {
                setEffect(MenuViewEffect.ShowToast("连接不畅，已扣减积分本地解锁，后续有网自动同步"))
            }
        }
    }

    private fun handleUpdateCarryItem(itemType: String, change: Int) {
        val state = currentState
        val backpackStock = state.backpackItems.find { it.item_type == itemType }?.count ?: 0
        val currentSelect = state.selectedCarryItems[itemType] ?: 0
        val target = currentSelect + change

        if (target < 0) return
        if (target > backpackStock) {
            setEffect(MenuViewEffect.ShowToast("携带数量超出库存上限"))
            return
        }

        val updatedMap = state.selectedCarryItems.toMutableMap()
        if (target == 0) {
            updatedMap.remove(itemType)
        } else {
            updatedMap[itemType] = target
        }
        updateState { copy(selectedCarryItems = updatedMap) }
    }

    private fun handleClearCarryItems() {
        updateState { copy(selectedCarryItems = emptyMap()) }
    }

    private fun handleGoToGame(levelId: Int, carryItemsJson: String) {
        setEffect(MenuViewEffect.NavigateToGame(levelId, carryItemsJson))
    }
}
