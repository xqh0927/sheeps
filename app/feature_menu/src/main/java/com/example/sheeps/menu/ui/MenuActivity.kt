package com.example.sheeps.menu.ui

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sheeps.ui.R
import com.example.sheeps.lib_base.base.BaseActivity
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.core.utils.AuthEvent
import com.example.sheeps.core.utils.AuthEventBus
import com.example.sheeps.data.utils.NetworkStatus
import com.example.sheeps.data.model.DailyPopupResponse
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.menu.state.ConflictInfo
import com.example.sheeps.menu.state.MenuViewEffect
import com.example.sheeps.menu.state.MenuViewIntent
import com.example.sheeps.menu.ui.components.MenuBottomNavigation
import com.example.sheeps.menu.ui.components.OfflineWarnBanner
import com.example.sheeps.menu.ui.dialogs.AppUpdateDialog
import com.example.sheeps.menu.ui.dialogs.ConflictDialog
import com.example.sheeps.menu.ui.dialogs.DailyLeaderboardPopupDialog
import com.example.sheeps.menu.ui.dialogs.PrepareGameDialog
import com.example.sheeps.menu.ui.dialogs.SetPasswordDialog
import com.example.sheeps.menu.ui.screens.GameHomeScreen
import com.example.sheeps.menu.ui.screens.PersonalScreen
import com.example.sheeps.menu.ui.screens.ShopScreen
import com.example.sheeps.menu.viewmodel.MenuViewModel
import com.example.sheeps.ui.theme.Overlay_Dark_Medium
import com.example.sheeps.ui.theme.SheepsTheme
import com.example.sheeps.ui.components.ConfirmDialog
import com.example.sheeps.ui.components.SheepsLoading
import com.hjq.toast.Toaster
import com.tencent.mmkv.MMKV
import com.therouter.TheRouter
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.serialization.json.Json

/**
 * 游戏主入口菜单 Activity。
 * 承载了游戏主页、商店、个人中心三个主要 Tab 页。
 * 负责处理多语言切换、登录冲突解决、App 更新检测以及全局弹窗逻辑。
 */
@Route(path = "/menu/main")
@AndroidEntryPoint
class MenuActivity : BaseActivity() {

    private val viewModel: MenuViewModel by viewModels()

    @Inject
    lateinit var json: Json

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var userPrefs: UserPreferences

    @Inject
    lateinit var kv: MMKV

    private var lastBackPressTime = 0L

    override fun initView(savedInstanceState: Bundle?) {
        setContent {
            val state by viewModel.viewState.collectAsState()

            // --- 多语言动态刷新逻辑 ---
            val currentLang = state.language
            val configuration = LocalConfiguration.current
            val context = LocalContext.current
            val localizedContext = remember(currentLang, configuration) {
                if (currentLang.isEmpty()) {
                    context
                } else {
                    val locale = when (currentLang) {
                        "en" -> java.util.Locale.ENGLISH
                        "tw" -> java.util.Locale.TRADITIONAL_CHINESE
                        "ja" -> java.util.Locale.JAPANESE
                        "ko" -> java.util.Locale.KOREAN
                        else -> java.util.Locale.SIMPLIFIED_CHINESE
                    }
                    val config = android.content.res.Configuration(configuration)
                    config.setLocale(locale)
                    val configContext = context.createConfigurationContext(config)
                    configContext.theme.setTo(context.theme)
                    configContext
                }
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                SheepsTheme {
                    // --- 界面控制状态 ---
                    var currentTab by remember { mutableStateOf("game") }
                    var showPrepareDialog by remember { mutableStateOf<Int?>(null) }
                    var showConflictInfo by remember { mutableStateOf<ConflictInfo?>(null) }
                    var showSetPasswordDialog by remember { mutableStateOf(false) }
                    var showDailyPopup by remember { mutableStateOf<DailyPopupResponse?>(null) }
                    var showLogoutConfirm by remember { mutableStateOf(false) }

                    // 登录后检查强制设密 + 每日弹窗
                    LaunchedEffect(state.isLoggedIn) {
                        if (state.isLoggedIn) {
                            // 验证码登录后若未设密码，LoginActivity 会写入 need_set_password flag
                            if (kv.decodeBool("need_set_password", false)) {
                                kv.removeValueForKey("need_set_password")
                                showSetPasswordDialog = true
                            }

                            val token = userPrefs.getToken()
                            val todayStr = java.text.SimpleDateFormat(
                                "yyyy-MM-dd",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date())
                            val lastShown = userPrefs.getLastShownDailyPopupDate()
                            if (lastShown != todayStr && token != null && token.isNotEmpty()) {
                                try {
                                    val response = apiService.getDailyPopup("Bearer $token")
                                    if (response.success) {
                                        userPrefs.setLastShownDailyPopupDate(todayStr)
                                        showDailyPopup = response
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    // --- 监听 Token 失效登录态事件 ---
                    LaunchedEffect(Unit) {
                        AuthEventBus.events.collect { event ->
                            if (event is AuthEvent.Logout) {
                                Toaster.show(localizedContext.getString(R.string.toast_session_expired))
                                TheRouter.build("/auth/login").navigation(this@MenuActivity)
                                finish()
                            }
                        }
                    }

                    // --- 监听 ViewModel 副作用 ---
                    LaunchedEffect(Unit) {
                        viewModel.viewEffect.collect { effect ->
                            when (effect) {
                                is MenuViewEffect.ShowToast -> {
                                    Toaster.show(
                                        if (effect.resId != null) {
                                            localizedContext.getString(
                                                effect.resId,
                                                *effect.formatArgs.toTypedArray()
                                            )
                                        } else {
                                            effect.message
                                        }
                                    )
                                }

                                is MenuViewEffect.ShowLoginActivity -> {
                                    TheRouter.build("/auth/login").navigation(this@MenuActivity)
                                }

                                is MenuViewEffect.NavigateToGame -> {
                                    showPrepareDialog = null
                                    viewModel.sendIntent(MenuViewIntent.ClearCarryItems)
                                    TheRouter.build("/game/play")
                                        .withInt("levelId", effect.levelId)
                                        .withString("carryItemsJson", effect.carryItemsJson)
                                        .navigation()
                                }

                                is MenuViewEffect.ShowConflictDialog -> {
                                    showConflictInfo = ConflictInfo(
                                        localPoints = effect.localPoints,
                                        localLevel = effect.localLevel,
                                        cloudPoints = effect.cloudPoints,
                                        cloudLevel = effect.cloudLevel
                                    )
                                }

                                is MenuViewEffect.ShowSetPasswordDialog -> {
                                    showSetPasswordDialog = true
                                }
                            }
                        }
                    }

                    // --- 页面内容布局 ---
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (state.networkStatus == NetworkStatus.OFFLINE) {
                                OfflineWarnBanner()
                            }
                        },
                        bottomBar = {
                            MenuBottomNavigation(
                                currentTab = currentTab,
                                onTabSelected = { tab ->
                                    currentTab = tab
                                    viewModel.sendIntent(MenuViewIntent.LoadData)
                                }
                            )
                        }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.background,
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                // 标签页切换动画
                                AnimatedContent(
                                    targetState = currentTab,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(
                                            animationSpec = tween(300)
                                        )
                                    },
                                    label = "tabChange"
                                ) { targetTab ->
                                    when (targetTab) {
                                        "game" -> GameHomeScreen(
                                            state = state,
                                            onLevelClick = { lvl ->
                                                if (lvl > 3 && !state.isLoggedIn) {
                                                    TheRouter.build("/auth/login")
                                                        .navigation(this@MenuActivity)
                                                    Toaster.show(this@MenuActivity.getString(R.string.toast_login_required_level))
                                                } else {
                                                    showPrepareDialog = lvl
                                                }
                                            },
                                            onShowLeaderboard = { lvl ->
                                                TheRouter.build("/leaderboard/show")
                                                    .withInt("levelId", lvl)
                                                    .navigation()
                                            },
                                            onNoticeClick = {
                                                TheRouter.build("/menu/notices")
                                                    .withString(
                                                        "noticesJson",
                                                        json.encodeToString(state.notices)
                                                    )
                                                    .navigation()
                                            },
                                            onLoginClick = {
                                                TheRouter.build("/auth/login")
                                                    .navigation(this@MenuActivity)
                                            },
                                            onJoinMatch = {
                                                viewModel.sendIntent(
                                                    MenuViewIntent.JoinMatch(
                                                        state.phone
                                                    )
                                                )
                                            },
                                            onLeaveMatch = {
                                                viewModel.sendIntent(
                                                    MenuViewIntent.LeaveMatch(
                                                        state.phone
                                                    )
                                                )
                                            },
                                            onResetMatch = { viewModel.sendIntent(MenuViewIntent.ResetMatchStatus) },
                                            onNavigateToDuel = { gId, pId, levelId, seed ->
                                                TheRouter.build("/game/duel")
                                                    .withString("gameId", gId)
                                                    .withString("playerId", pId)
                                                    .withInt("levelId", levelId)
                                                    .withInt("seed", seed)
                                                    .navigation()
                                            }
                                        )

                                        "shop" -> ShopScreen(
                                            state = state,
                                            onLoginClick = {
                                                TheRouter.build("/auth/login")
                                                    .navigation(this@MenuActivity)
                                            },
                                            onExchangeClick = { itemId, count ->
                                                viewModel.sendIntent(
                                                    MenuViewIntent.ExchangeShopItem(
                                                        itemId,
                                                        count
                                                    )
                                                )
                                            },
                                            onChangeSkin = { skin ->
                                                viewModel.sendIntent(
                                                    MenuViewIntent.ChangeSkin(
                                                        skin
                                                    )
                                                )
                                            }
                                        )

                                        "me" -> PersonalScreen(
                                            state = state,
                                            onLoginClick = {
                                                TheRouter.build("/auth/login")
                                                    .navigation(this@MenuActivity)
                                            },
                                            onLogoutClick = {
                                                showLogoutConfirm = true
                                            },
                                            onSignInClick = {
                                                viewModel.sendIntent(
                                                    MenuViewIntent.SignIn
                                                )
                                            },
                                            onClaimTask = { taskId ->
                                                viewModel.sendIntent(
                                                    MenuViewIntent.ClaimTask(taskId)
                                                )
                                            },
                                            onApplySkin = { skin ->
                                                viewModel.sendIntent(
                                                    MenuViewIntent.ChangeSkin(skin)
                                                )
                                            },
                                            onGoToPlay = { currentTab = "game" }
                                        )
                                    }
                                }

                                // 备战弹窗：选择携带道具
                                if (showPrepareDialog != null) {
                                    PrepareGameDialog(
                                        levelId = showPrepareDialog!!,
                                        state = state,
                                        onDismiss = {
                                            showPrepareDialog = null
                                            viewModel.sendIntent(MenuViewIntent.ClearCarryItems)
                                        },
                                        onConfirm = { lvl ->
                                            val carryJson =
                                                json.encodeToString(state.selectedCarryItems)
                                            viewModel.sendIntent(MenuViewIntent.ClearCarryItems)
                                            viewModel.sendIntent(
                                                MenuViewIntent.GoToGame(
                                                    lvl,
                                                    carryJson
                                                )
                                            )
                                        },
                                        onUpdateItem = { itemType, change ->
                                            viewModel.sendIntent(
                                                MenuViewIntent.UpdateCarryItem(
                                                    itemType,
                                                    change
                                                )
                                            )
                                        },
                                        onUnlock = { lvl ->
                                            viewModel.sendIntent(
                                                MenuViewIntent.UnlockLevelWithPoints(
                                                    lvl
                                                )
                                            )
                                        }
                                    )
                                }

                                // 强制设置密码对话框
                                if (showSetPasswordDialog) {
                                    SetPasswordDialog(
                                        onSetPassword = { password ->
                                            viewModel.sendIntent(
                                                MenuViewIntent.SetPassword(
                                                    password
                                                )
                                            )
                                            showSetPasswordDialog = false
                                        }
                                    )
                                }

                                // 存档冲突处理弹窗
                                if (showConflictInfo != null) {
                                    ConflictDialog(
                                        info = showConflictInfo!!,
                                        onChooseLocal = {
                                            viewModel.sendIntent(
                                                MenuViewIntent.ResolveConflict(
                                                    useLocal = true
                                                )
                                            )
                                            showConflictInfo = null
                                        },
                                        onChooseCloud = {
                                            viewModel.sendIntent(
                                                MenuViewIntent.ResolveConflict(
                                                    useLocal = false
                                                )
                                            )
                                            showConflictInfo = null
                                        }
                                    )
                                }

                                // App 更新弹窗
                                state.appUpdateInfo?.let { updateInfo ->
                                    if (updateInfo.has_update) {
                                        AppUpdateDialog(
                                            updateInfo = updateInfo,
                                            onDismiss = {
                                                viewModel.sendIntent(MenuViewIntent.DismissUpdate)
                                            }
                                        )
                                    }
                                }

                                // 每日排行榜奖励公示弹窗
                                if (showDailyPopup != null) {
                                    DailyLeaderboardPopupDialog(
                                        data = showDailyPopup!!,
                                        onDismiss = { showDailyPopup = null }
                                    )
                                }

                                // 退出登录确认弹窗
                                if (showLogoutConfirm) {
                                    ConfirmDialog(
                                        onDismissRequest = { showLogoutConfirm = false },
                                        title = stringResource(R.string.dialog_tip_title),
                                        message = stringResource(R.string.dialog_logout_confirm_message),
                                        confirmText = stringResource(R.string.btn_confirm),
                                        dismissText = stringResource(R.string.btn_cancel),
                                        onConfirm = {
                                            showLogoutConfirm = false
                                            viewModel.sendIntent(MenuViewIntent.Logout)
                                        },
                                        onDismiss = { showLogoutConfirm = false }
                                    )
                                }

                                // 全屏加载指示器（带手势拦截）
                                if (state.isLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Overlay_Dark_Medium)
                                            .pointerInput(Unit) {
                                                awaitEachGesture {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        event.changes.forEach { it.consume() }
                                                    }
                                                }
                                            }
                                            .clickable(enabled = false, onClick = {}),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(
                                                    androidx.compose.foundation.shape.RoundedCornerShape(
                                                        16.dp
                                                    )
                                                )
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(
                                                        alpha = 0.92f
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SheepsLoading(size = 44.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    override fun initData() {
        // ViewModel 在创建时自动加载初始数据
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000) {
                // 第二次按下：禁用自身再派发，让系统执行默认返回（无其他回调时即 finish）
                isEnabled = false
                this@MenuActivity.onBackPressedDispatcher.onBackPressed()
            } else {
                lastBackPressTime = now
                Toaster.show(this@MenuActivity.getString(R.string.toast_press_back_exit))
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // 检测从 SettingsActivity 返回后的主题变更
        // ThemeManager 是全局 StateFlow，SheepsTheme 通过 collectAsState 自动响应，无需重建
        if (kv.decodeBool("theme_changed_in_settings", false)) {
            kv.removeValueForKey("theme_changed_in_settings")
        }

        // 检测从 SettingsActivity 返回后的语言变更
        // 直接更新 ViewModel state 触发 remember(currentLang) 重建 localizedContext
        if (kv.decodeBool("language_changed_in_settings", false)) {
            kv.removeValueForKey("language_changed_in_settings")
            viewModel.sendIntent(MenuViewIntent.ChangeLanguage(userPrefs.getLanguage()))
            // handleLoadData 已在 ChangeLanguage 中调用，无需再发 LoadData
            return
        }

        // 每次回到前台刷新一次数据（如积分、体力等）
        viewModel.sendIntent(MenuViewIntent.LoadData)

        // 每次回到前台重新拉取游戏模式开关状态，确保管理后台变更即时生效
        viewModel.refreshGameModes()
    }
}
