package com.example.sheeps.menu

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.base.BaseActivity
import com.example.sheeps.core.preference.UserPreferences
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
import com.example.sheeps.menu.ui.dialogs.LoginDialog
import com.example.sheeps.menu.ui.dialogs.PrepareGameDialog
import com.example.sheeps.menu.ui.dialogs.SetPasswordDialog
import com.example.sheeps.menu.ui.screens.GameHomeScreen
import com.example.sheeps.menu.ui.screens.NoticeListScreen
import com.example.sheeps.menu.ui.screens.PersonalScreen
import com.example.sheeps.menu.ui.screens.ShopScreen
import com.example.sheeps.menu.viewmodel.MenuViewModel
import com.example.sheeps.theme.Overlay_Dark_Medium
import com.example.sheeps.theme.SheepsTheme
import com.example.sheeps.ui.components.SheepsLoading
import com.hjq.toast.Toaster
import com.therouter.TheRouter
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.zibin.luban.api.compressTo

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

    override fun initView(savedInstanceState: Bundle?) {
        setContent {
            val state by viewModel.viewState.collectAsState()

            // --- 多语言动态刷新逻辑 ---
            // 通过监听 State 中的 language 变化，动态创建并提供经过语言配置修正的 Context
            val currentLang = state.language
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val context = androidx.compose.ui.platform.LocalContext.current
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

            // 使用 CompositionLocalProvider 将修正后的 Context 注入 Compose 树
            CompositionLocalProvider(androidx.compose.ui.platform.LocalContext provides localizedContext) {
                SheepsTheme {
                    // --- 界面控制状态 ---
                    var currentTab by remember { mutableStateOf("game") } // 当前选中的标签页：game, shop, me
                    var showLoginDialog by remember { mutableStateOf(false) } // 登录弹窗显示控制
                    var showPrepareDialog by remember { mutableStateOf<Int?>(null) } // 备战弹窗（传入关卡ID）
                    var showConflictInfo by remember { mutableStateOf<ConflictInfo?>(null) } // 存档冲突信息
                    var showNoticeListScreen by remember { mutableStateOf(false) } // 公告列表全屏显示
                    var showSetPasswordDialog by remember { mutableStateOf(false) }
                    var showDailyPopup by remember { mutableStateOf<DailyPopupResponse?>(null) } // 每日弹窗内容

                    val scope = rememberCoroutineScope()

                    // 【修改处 2】头像选择器（使用 Luban 2 同步压缩 → bytes → 直接上传 R2）
                    val pickAvatarLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let { imageUri ->
                            scope.launch(Dispatchers.IO) {
                                try {
                                    // 使用 Luban 2 的 Kotlin 扩展函数直接压缩（自动处理临时目录与缓存）
                                    val result = imageUri.compressTo(context)
                                    val file = result.getOrNull()

                                    if (file != null && file.exists() && file.canRead()) {
                                        val bytes = file.readBytes()
                                      withContext(Dispatchers.Main) {
                                            viewModel.sendIntent(
                                                MenuViewIntent.ChangeAvatar(
                                                    bytes,
                                                    file.name
                                                )
                                            )
                                        }
                                    } else {
                                        val errorMsg =
                                            result.exceptionOrNull()?.message ?: "图片处理失败"
                                      withContext(Dispatchers.Main) {
                                            Toaster.show(errorMsg)
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toaster.show("图片处理失败")
                                    }
                                }
                            }
                        }
                    }

                    // 登录后检查每日弹窗
                    LaunchedEffect(state.isLoggedIn) {
                        if (state.isLoggedIn) {
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

                    // --- 监听 ViewModel 副作用 ---
                    LaunchedEffect(Unit) {
                        viewModel.viewEffect.collect { effect ->
                            when (effect) {
                                is MenuViewEffect.ShowToast -> {
                                    Toaster.show(effect.message)
                                }

                                is MenuViewEffect.ShowLoginDialog -> {
                                    showLoginDialog = true
                                }

                                is MenuViewEffect.NavigateToGame -> {
                                    showPrepareDialog = null
                                    viewModel.sendIntent(MenuViewIntent.ClearCarryItems)
                                    // 路由跳转至单机游戏
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
                    if (showNoticeListScreen) {
                        NoticeListScreen(
                            notices = state.notices,
                            onBack = { showNoticeListScreen = false }
                        )
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                if (state.networkStatus == com.example.sheeps.core.utils.NetworkStatus.OFFLINE) {
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
                                                        showLoginDialog = true
                                                        Toaster.show("第四关及后续关卡需要登录解锁，请先登录！")
                                                    } else {
                                                        showPrepareDialog = lvl
                                                    }
                                                },
                                                onShowLeaderboard = { lvl ->
                                                    TheRouter.build("/leaderboard/show")
                                                        .withInt("levelId", lvl)
                                                        .navigation()
                                                },
                                                onNoticeClick = { showNoticeListScreen = true },
                                                onLoginClick = { showLoginDialog = true },
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
                                                onLoginClick = { showLoginDialog = true },
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
                                                onLoginClick = { showLoginDialog = true },
                                                onLogoutClick = {
                                                    viewModel.sendIntent(
                                                        MenuViewIntent.Logout
                                                    )
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
                                                onChangeLanguage = { lang ->
                                                    viewModel.sendIntent(
                                                        MenuViewIntent.ChangeLanguage(lang)
                                                    )
                                                },
                                                onThemeChange = { recreate() },
                                                onApplySkin = { skin ->
                                                    viewModel.sendIntent(
                                                        MenuViewIntent.ChangeSkin(skin)
                                                    )
                                                },
                                                onGoToPlay = { currentTab = "game" },
                                                onChangeAvatar = { pickAvatarLauncher.launch("image/*") },
                                                onUpdateNickname = { nickname ->
                                                    viewModel.sendIntent(
                                                        MenuViewIntent.UpdateNickname(nickname)
                                                    )
                                                }
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

                                    // 登录/注册弹窗
                                    if (showLoginDialog) {
                                        LoginDialog(
                                            onDismiss = { showLoginDialog = false },
                                            onSendCode = { phone ->
                                                viewModel.sendIntent(
                                                    MenuViewIntent.SendSmsCode(phone)
                                                )
                                            },
                                            onLogin = { phone, code ->
                                                viewModel.sendIntent(
                                                    MenuViewIntent.LoginWithCode(
                                                        phone,
                                                        code
                                                    )
                                                )
                                                showLoginDialog = false
                                            },
                                            onPasswordLogin = { phone, password ->
                                                viewModel.sendIntent(
                                                    MenuViewIntent.LoginWithPassword(
                                                        phone,
                                                        password
                                                    )
                                                )
                                                showLoginDialog = false
                                            },
                                            onRegister = { phone, password, code ->
                                                viewModel.sendIntent(
                                                    MenuViewIntent.Register(
                                                        phone,
                                                        password,
                                                        code
                                                    )
                                                )
                                            },
                                            onResetPassword = { phone, code, newPassword ->
                                                viewModel.sendIntent(
                                                    MenuViewIntent.ResetPassword(
                                                        phone,
                                                        code,
                                                        newPassword
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
    }

    override fun initData() {
        // ViewModel 在创建时自动加载初始数据
    }

    override fun onResume() {
        super.onResume()
        // 每次回到前台刷新一次数据（如积分、体力等）
        viewModel.sendIntent(MenuViewIntent.LoadData)
    }
}