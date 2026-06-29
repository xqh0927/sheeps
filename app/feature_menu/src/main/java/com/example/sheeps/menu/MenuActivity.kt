package com.example.sheeps.menu

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.base.BaseActivity
import com.example.sheeps.menu.state.*
import com.example.sheeps.menu.ui.components.*
import com.example.sheeps.menu.ui.dialogs.*
import com.example.sheeps.menu.ui.screens.*
import com.example.sheeps.menu.viewmodel.MenuViewModel
import com.example.sheeps.theme.*
import com.example.sheeps.ui.components.SheepsLoading
import com.hjq.toast.Toaster
import com.therouter.TheRouter
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Route(path = "/menu/main")
@AndroidEntryPoint
class MenuActivity : BaseActivity() {

    private val viewModel: MenuViewModel by viewModels()

    @Inject
    lateinit var json: Json

    override fun initView(savedInstanceState: Bundle?) {
        setContent {
            val state by viewModel.viewState.collectAsState()
            
            // Real-time language updates in Compose context without Activity reload
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
                    context.createConfigurationContext(config)
                }
            }

            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalContext provides localizedContext) {
                SheepsTheme {
                    var currentTab by remember { mutableStateOf("game") } // game, shop, me
                    var showLoginDialog by remember { mutableStateOf(false) }
                    var showPrepareDialog by remember { mutableStateOf<Int?>(null) } // levelId
                    var showConflictInfo by remember { mutableStateOf<ConflictInfo?>(null) }

                    // Observe Side Effects
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
                            }
                        }
                    }

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
                                .padding(paddingValues)
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
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
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
                                        onLoginClick = { showLoginDialog = true },
                                        onJoinMatch = { viewModel.sendIntent(MenuViewIntent.JoinMatch(state.phone)) },
                                        onLeaveMatch = { viewModel.sendIntent(MenuViewIntent.LeaveMatch(state.phone)) },
                                        onResetMatch = { viewModel.sendIntent(MenuViewIntent.ResetMatchStatus) },
                                        onNavigateToDuel = { gId, pId ->
                                            TheRouter.build("/game/duel")
                                                .withString("gameId", gId)
                                                .withString("playerId", pId)
                                                .navigation()
                                        }
                                    )
                                    "shop" -> ShopScreen(
                                        state = state,
                                        onLoginClick = { showLoginDialog = true },
                                        onExchangeClick = { itemId, count ->
                                            viewModel.sendIntent(MenuViewIntent.ExchangeShopItem(itemId, count))
                                        },
                                        onChangeSkin = { skin ->
                                            viewModel.sendIntent(MenuViewIntent.ChangeSkin(skin))
                                        }
                                    )
                                    "me" -> PersonalScreen(
                                        state = state,
                                        onLoginClick = { showLoginDialog = true },
                                        onLogoutClick = { viewModel.sendIntent(MenuViewIntent.Logout) },
                                        onSignInClick = { viewModel.sendIntent(MenuViewIntent.SignIn) },
                                        onClaimTask = { taskId -> viewModel.sendIntent(MenuViewIntent.ClaimTask(taskId)) },
                                        onChangeLanguage = { lang -> viewModel.sendIntent(MenuViewIntent.ChangeLanguage(lang)) },
                                        onThemeChange = { recreate() }
                                    )
                                }
                            }

                            // Prepare game dialog
                            if (showPrepareDialog != null) {
                                PrepareGameDialog(
                                    levelId = showPrepareDialog!!,
                                    state = state,
                                    onDismiss = {
                                        showPrepareDialog = null
                                        viewModel.sendIntent(MenuViewIntent.ClearCarryItems)
                                    },
                                    onConfirm = { lvl ->
                                        val carryJson = json.encodeToString(state.selectedCarryItems)
                                        viewModel.sendIntent(MenuViewIntent.ClearCarryItems)
                                        viewModel.sendIntent(MenuViewIntent.GoToGame(lvl, carryJson))
                                    },
                                    onUpdateItem = { itemType, change ->
                                        viewModel.sendIntent(MenuViewIntent.UpdateCarryItem(itemType, change))
                                    },
                                    onUnlock = { lvl ->
                                        viewModel.sendIntent(MenuViewIntent.UnlockLevelWithPoints(lvl))
                                    }
                                )
                            }

                            // Login simulation dialog
                            if (showLoginDialog) {
                                LoginDialog(
                                    onDismiss = { showLoginDialog = false },
                                    onSendCode = { phone -> viewModel.sendIntent(MenuViewIntent.SendSmsCode(phone)) },
                                    onLogin = { phone, code ->
                                        viewModel.sendIntent(MenuViewIntent.LoginWithCode(phone, code))
                                        showLoginDialog = false
                                    }
                                )
                            }

                            if (showConflictInfo != null) {
                                ConflictDialog(
                                    info = showConflictInfo!!,
                                    onChooseLocal = {
                                        viewModel.sendIntent(MenuViewIntent.ResolveConflict(useLocal = true))
                                        showConflictInfo = null
                                    },
                                    onChooseCloud = {
                                        viewModel.sendIntent(MenuViewIntent.ResolveConflict(useLocal = false))
                                        showConflictInfo = null
                                    }
                                )
                            }

                            // 检查 App 版本更新并显示弹窗
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

                            if (state.isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Overlay_Dark_Medium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
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

    override fun initData() {
        // ViewModel handles loading on creation
    }

    override fun onResume() {
        super.onResume()
        viewModel.sendIntent(MenuViewIntent.LoadData)
    }
}
