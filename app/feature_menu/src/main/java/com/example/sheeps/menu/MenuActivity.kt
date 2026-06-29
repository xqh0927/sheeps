package com.example.sheeps.menu

import android.os.Bundle
import androidx.compose.ui.res.stringResource
import com.example.sheeps.core.R
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.sheeps.core.base.BaseActivity
import com.example.sheeps.data.model.Notice
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.data.model.UserItem
import com.example.sheeps.menu.state.*
import com.example.sheeps.menu.viewmodel.MenuViewModel
import com.example.sheeps.theme.*
import com.example.sheeps.ui.components.SheepsLoading
import com.example.sheeps.ui.components.PrimaryButton
import com.example.sheeps.ui.components.GhostButton
import com.example.sheeps.ui.components.AnimatedCounter
import com.hjq.toast.Toaster
import com.therouter.TheRouter
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
            val context = androidx.compose.ui.platform.LocalContext.current
            val localizedContext = remember(currentLang) {
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
                    val config = android.content.res.Configuration(context.resources.configuration)
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
                                        MoYe_Background,
                                        MoYe_Surface,
                                        MoYe_SurfaceVariant
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
                                "game" -> GameHomeTabContent(
                                    state = state,
                                    onLevelClick = { lvl ->
                                        if (lvl > 3 && !state.isLoggedIn) {
                                            showLoginDialog = true
                                            Toaster.show("第四关及后续关卡需要登录解锁，请先登录！")
                                        } else {
                                            showPrepareDialog = lvl
                                        }
                                    },
                                    onLoginClick = { showLoginDialog = true }
                                )
                                "shop" -> ShopTabContent(
                                    state = state,
                                    onLoginClick = { showLoginDialog = true },
                                    onExchangeClick = { itemId, count ->
                                        viewModel.sendIntent(MenuViewIntent.ExchangeShopItem(itemId, count))
                                    }
                                )
                                "me" -> PersonalTabContent(
                                     state = state,
                                     onLoginClick = { showLoginDialog = true },
                                     onLogoutClick = { viewModel.sendIntent(MenuViewIntent.Logout) },
                                     onSignInClick = { viewModel.sendIntent(MenuViewIntent.SignIn) },
                                     onClaimTask = { taskId -> viewModel.sendIntent(MenuViewIntent.ClaimTask(taskId)) },
                                     onChangeLanguage = { lang -> viewModel.sendIntent(MenuViewIntent.ChangeLanguage(lang)) }
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
                                        .background(MoYe_Surface.copy(alpha = 0.92f)),
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

// 底部导航栏 Composable
@Composable
fun MenuBottomNavigation(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    val navItems = listOf(
        Triple("game", "消除", Icons.Default.GridOn),
        Triple("shop", "商城", Icons.Default.Storefront),
        Triple("me",   "我的", Icons.Default.AccountCircle)
    )

    NavigationBar(
        containerColor = MoYe_Surface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .border(width = 0.5.dp, color = MoYe_Outline)
            .shadow(elevation = 8.dp)
    ) {
        navItems.forEach { (tab, label, icon) ->
            val selected = currentTab == tab
            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "navIconScale_$tab"
            )
            NavigationBarItem(
                selected = selected,
                onClick  = { onTabSelected(tab) },
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.scale(iconScale)
                        )
                        // 金色选中指示点
                        if (selected) {
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Gold_Primary)
                            )
                        }
                    }
                },
                label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Gold_Primary,
                    selectedTextColor   = Gold_Primary,
                    indicatorColor      = Crimson_PrimaryContainer.copy(alpha = 0.4f),
                    unselectedIconColor = Text_Secondary_Dark,
                    unselectedTextColor = Text_Secondary_Dark
                )
            )
        }
    }
}

// --- TAB 1: GAME SCREEN ---
@Composable
fun GameHomeTabContent(
    state: MenuViewState,
    onLevelClick: (Int) -> Unit,
    onLoginClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 豪华用户信息卡
        UserMiniProfileHeader(state = state, onLoginClick = onLoginClick)

        Spacer(modifier = Modifier.height(14.dp))

        // 公告轮播
        AnnouncementsBanner(notices = state.notices)

        Spacer(modifier = Modifier.height(14.dp))

        // Section Title
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Gold_Subtle.copy(alpha = 0.4f))
                        )
                    )
            )
            Text(
                text = "  破阵陀邪 · 奇门遁甲  ",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gold_Primary.copy(alpha = 0.85f),
                fontFamily = FontFamily.Serif
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Gold_Subtle.copy(alpha = 0.4f), Color.Transparent)
                        )
                    )
            )
        }

        // 关卡列表
        val levels = remember { (1..20).toList() }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(levels) { lvl ->
                val isUnlocked = lvl <= state.unlockedLevel
                LevelItemRow(
                    levelId    = lvl,
                    isUnlocked = isUnlocked,
                    onStart    = { onLevelClick(lvl) }
                )
            }
        }
    }
}


@Composable

fun UserMiniProfileHeader(
    state: MenuViewState,
    onLoginClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatarGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 0.85f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarGlowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeLarge)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Gold_Subtle.copy(alpha = 0.12f),
                        MoYe_SurfaceVariant,
                        MoYe_SurfaceContainer
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(Gold_Subtle.copy(alpha = 0.6f), Gold_Primary.copy(alpha = 0.3f), Gold_Subtle.copy(alpha = 0.6f))
                    )
                ),
                shape = ShapeLarge
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 头像 + 光晕圆框
            Box(contentAlignment = Alignment.Center) {
                // 底层光晕动画
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(60.dp)
                ) {
                    drawCircle(
                        color  = Gold_Primary.copy(alpha = glowAlpha * 0.3f),
                        radius = this.size.width * 0.45f
                    )
                }
                // 头像圆
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Crimson_Primary, Crimson_PrimaryDark)
                            ),
                            shape = CircleShape
                        )
                        .border(
                            BorderStroke(1.5.dp, Gold_Subtle.copy(alpha = glowAlpha)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = if (state.username.isNotEmpty()) state.username.first().toString() else "侠",
                        color      = Gold_Primary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = if (state.isLoggedIn) state.username else "逑客云游",
                    style      = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    color      = Text_Primary_Dark
                )
                Spacer(Modifier.height(4.dp))
                if (state.isLoggedIn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Gold_Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        AnimatedCounter(
                            count = state.points,
                            style = MaterialTheme.typography.titleSmall.copy(color = Gold_Primary)
                        )
                        Text(
                            text  = " 积分",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold_Primary.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text  = "登录同步存档 · 解锁演化关卡",
                        style = MaterialTheme.typography.bodySmall,
                        color = Text_Secondary_Dark
                    )
                }
            }

            if (!state.isLoggedIn) {
                PrimaryButton(
                    text     = "登录",
                    onClick  = onLoginClick,
                    height   = 36.dp,
                    modifier = Modifier.widthIn(min = 64.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnnouncementsBanner(notices: List<Notice>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeMedium)
            .background(MoYe_SurfaceVariant)
            .border(
                BorderStroke(0.5.dp, MoYe_Outline),
                shape = ShapeMedium
            )
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "公告",
                    tint = Crimson_Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "福禄公告栏",
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    color = Gold_Primary
                )
            }

            if (notices.isEmpty()) {
                Text(
                    text  = "凡尘清静，暂无公告发布。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Text_Secondary_Dark
                )
            } else {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                ) {
                    notices.forEach { n ->
                        Box(
                            modifier = Modifier
                                .width(280.dp)
                                .padding(end = 10.dp)
                                .clip(ShapeSmall)
                                .background(MoYe_SurfaceContainer)
                                .border(0.5.dp, MoYe_Outline, ShapeSmall)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = n.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Text_Primary_Dark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = n.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Text_Secondary_Dark,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LevelItemRow(
    levelId: Int,
    isUnlocked: Boolean,
    onStart: () -> Unit
) {

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isUnlocked) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "levelCardScale"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "levelGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.7f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levelGlowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(ShapeLarge)
            .background(
                if (isUnlocked) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E2535),
                            Color(0xFF252D3D)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(MoYe_SurfaceContainer, MoYe_SurfaceVariant)
                    )
                }
            )
            .border(
                width = if (isUnlocked) 1.dp else 0.5.dp,
                brush = if (isUnlocked) {
                    Brush.linearGradient(
                        colors = listOf(
                            Gold_Subtle.copy(alpha = glowAlpha),
                            Gold_Primary.copy(alpha = glowAlpha * 0.5f),
                            Gold_Subtle.copy(alpha = glowAlpha)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(MoYe_Outline, MoYe_Outline)
                    )
                },
                shape = ShapeLarge
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onStart
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧：关卡编号 + 难度
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 关卡编号圆标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isUnlocked) Crimson_PrimaryContainer.copy(alpha = 0.5f)
                            else MoYe_SurfaceContainer,
                            shape = ShapeMedium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUnlocked) {
                        Text(
                            text       = "$levelId",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            color      = Gold_Primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Text_Disabled_Dark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text       = "第 $levelId 关",
                        style      = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Serif,
                        color      = if (isUnlocked) Text_Primary_Dark else Text_Disabled_Dark
                    )
                    Text(
                        text  = when (levelId) {
                            1    -> "初稺门径 · 极其简单"
                            2    -> "略有小成 · 普通难度"
                            3    -> "降妖伏魔 · 略有挑战"
                            else -> "奇门生死局 · 包含封印/盲盒"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUnlocked) Text_Secondary_Dark else Text_Disabled_Dark.copy(alpha = 0.6f)
                    )
                }
            }

            // 右侧：状态指示
            if (isUnlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(ShapeSmall)
                        .background(Crimson_Primary.copy(alpha = 0.15f))
                        .border(0.5.dp, Crimson_Primary.copy(alpha = 0.3f), ShapeSmall)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text       = "破阵",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp,
                        color      = Gold_Primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Gold_Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Text(
                    text  = "封印中",
                    style = MaterialTheme.typography.labelSmall,
                    color = Text_Disabled_Dark
                )
            }
        }
    }
}

// --- TAB 2: SHOP CONTENT ---

@Composable
fun ShopTabContent(
    state: MenuViewState,
    onLoginClick: () -> Unit,
    onExchangeClick: (Int, Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "福禄聚宝阁",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonRed,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "积分: ${state.points}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonRed
                )
            }

            if (state.shopItems.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CrimsonRed)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.shopItems) { item ->
                        ShopItemCard(item = item, onExchange = { count ->
                            onExchangeClick(item.id, count)
                        })
                    }
                }
            }
        }

        // Locked mask if guest mode
        if (!state.isLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                    border = BorderStroke(1.dp, GoldenBronze),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "锁定",
                            tint = CrimsonRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "金库封锁",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonRed,
                            fontFamily = FontFamily.Serif
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请先登录游戏账号，登录后可用积分在此阁内兑换破阵之法宝神器。",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onLoginClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("立即登录", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopItemCard(
    item: ShopItem,
    onExchange: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, Color(0xFFE5DDD3)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mock graphic representation using Canvas
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF9F5EF)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(36.dp)) {
                    val p = Path().apply {
                        moveTo(size.width / 2, 0f)
                        lineTo(size.width, size.height / 2)
                        lineTo(size.width / 2, size.height)
                        lineTo(0f, size.height / 2)
                        close()
                    }
                    drawPath(p, color = CrimsonRed, style = Stroke(width = 3f))
                }
                Text(
                    text = item.item_type.take(3),
                    fontSize = 10.sp,
                    color = CrimsonRed,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            Text(
                text = item.description ?: "奇门法宝",
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.points_price} 积分",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CrimsonRed
                )
                Text(
                    text = "存: ${item.stock}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onExchange(1) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            ) {
                Text("兑换", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

// --- TAB 3: PERSONAL CENTER & TASKS ---
@Composable
fun PersonalTabContent(
    state: MenuViewState,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSignInClick: () -> Unit,
    onClaimTask: (String) -> Unit,
    onChangeLanguage: (String) -> Unit
) {
    var showPointHistory by remember { mutableStateOf(false) }
    var showExchangeHistory by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User profile Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                border = BorderStroke(1.dp, GoldenBronze),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(CrimsonRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Me", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (state.isLoggedIn) state.username else "游客小友",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonRed,
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = if (state.isLoggedIn) "UID: ${state.phone}" else "尚未签到登录",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("积分余额", fontSize = 12.sp, color = Color.Gray)
                            Text("${state.points}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CrimsonRed)
                        }
                        Column {
                            Text("连续签到", fontSize = 12.sp, color = Color.Gray)
                            Text("${state.signStreak} 天", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CrimsonRed)
                        }
                        Column {
                            Text("最高通关", fontSize = 12.sp, color = Color.Gray)
                            Text("${state.highestLevelCleared} 关", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CrimsonRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.isLoggedIn) {
                        Button(
                            onClick = onSignInClick,
                            enabled = !state.todaySigned,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonRed,
                                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (state.todaySigned) "今日已签署签到" else "签到签署福禄",
                                color = if (state.todaySigned) Color.DarkGray else Color.White
                            )
                        }
                    } else {
                        Button(
                            onClick = onLoginClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("登录同步签到", color = Color.White)
                        }
                    }
                }
            }
        }

        // Daily Tasks Box
        if (state.isLoggedIn) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(0.5.dp, Color(0xFFE5DDD3)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "每日修仙任务",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CrimsonRed,
                            modifier = Modifier.padding(bottom = 12.dp),
                            fontFamily = FontFamily.Serif
                        )

                        if (state.dailyTasks.isEmpty()) {
                            Text("无日常任务", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            state.dailyTasks.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(task.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${task.description} (${task.progress}/${task.target_count})",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    if (task.is_rewarded) {
                                        Text("已领奖", fontSize = 12.sp, color = Color.Gray)
                                    } else {
                                        Button(
                                            onClick = { onClaimTask(task.task_id) },
                                            enabled = task.is_completed,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CrimsonRed,
                                                disabledContainerColor = Color.LightGray
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(
                                                text = if (task.is_completed) "领奖 (+${task.points_reward})" else "未完成",
                                                fontSize = 10.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Backpack items detail grid
        if (state.isLoggedIn && state.backpackItems.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(0.5.dp, Color(0xFFE5DDD3)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "乾坤法宝袋 (我的背包)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CrimsonRed,
                            modifier = Modifier.padding(bottom = 12.dp),
                            fontFamily = FontFamily.Serif
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            state.backpackItems.forEach { item ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .background(Color(0xFFFBF9F6), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                        .width(72.dp)
                                ) {
                                    Text(
                                        text = item.item_type,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonRed
                                    )
                                    Text(
                                        text = "存: ${item.count}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Language settings item
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(0.5.dp, Color(0xFFE5DDD3)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.language_settings),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = CrimsonRed,
                        modifier = Modifier.padding(bottom = 12.dp),
                        fontFamily = FontFamily.Serif
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val languages = listOf(
                            "" to stringResource(id = R.string.lang_zh),
                            "en" to stringResource(id = R.string.lang_en),
                            "tw" to stringResource(id = R.string.lang_tw),
                            "ja" to stringResource(id = R.string.lang_ja),
                            "ko" to stringResource(id = R.string.lang_ko)
                        )
                        
                        languages.forEach { (code, name) ->
                            val isSelected = state.language == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) CrimsonRed else Color(0xFFFBF9F6),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        onChangeLanguage(code)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.White else Color.DarkGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Logs & Buttons
        if (state.isLoggedIn) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showPointHistory = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        border = BorderStroke(0.5.dp, CrimsonRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("查看积分流水", color = CrimsonRed)
                    }

                    Button(
                        onClick = { showExchangeHistory = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        border = BorderStroke(0.5.dp, CrimsonRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("查看兑换历史记录", color = CrimsonRed)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onLogoutClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("退出登录", color = Color.White)
                    }
                }
            }
        }
    }


    // Modal point history dialog
    if (showPointHistory) {
        AlertDialog(
            onDismissRequest = { showPointHistory = false },
            title = { Text("积分流水日志", fontWeight = FontWeight.Bold, color = CrimsonRed) },
            text = {
                if (state.pointsHistory.isEmpty()) {
                    Text("暂无流水变动记载")
                } else {
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(state.pointsHistory) { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(record.source, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.created_at)),
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = if (record.type == "IN") "+${record.amount}" else "-${record.amount}",
                                    color = if (record.type == "IN") Color(0xFF4CAF50) else CrimsonRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPointHistory = false }) { Text("关闭") }
            }
        )
    }

    // Modal exchange history dialog
    if (showExchangeHistory) {
        AlertDialog(
            onDismissRequest = { showExchangeHistory = false },
            title = { Text("商品兑换历史记录", fontWeight = FontWeight.Bold, color = CrimsonRed) },
            text = {
                if (state.exchangeHistory.isEmpty()) {
                    Text("暂无兑换记录记载")
                } else {
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(state.exchangeHistory) { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("兑换了 ${record.count} 个 ${record.item_type}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.created_at)),
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = "-${record.points_cost} 积分",
                                    color = CrimsonRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExchangeHistory = false }) { Text("关闭") }
            }
        )
    }
}

// --- MODALS & DIALOGS ---

// SMS Phone OTP Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onSendCode: (String) -> Unit,
    onLogin: (String, String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(0) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "玄门登录验证",
                fontWeight = FontWeight.Bold,
                color = CrimsonRed,
                fontFamily = FontFamily.Serif
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("手机号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("验证码") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (phone.length == 11) {
                                onSendCode(phone)
                                countdown = 60
                            } else {
                                Toaster.show("请输入11位手机号")
                            }
                        },
                        enabled = countdown == 0,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = if (countdown > 0) "${countdown}s" else "获取",
                            color = Color.White
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLogin(phone, code) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
            ) {
                Text("验证登录", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// Prepare Game / Carry Items Dialog
@Composable
fun PrepareGameDialog(
    levelId: Int,
    state: MenuViewState,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onUpdateItem: (String, Int) -> Unit,
    onUnlock: (Int) -> Unit
) {
    val isLocked = levelId > state.unlockedLevel
    // Calculate unlock points required
    val cost = if (levelId == 2) 50 else if (levelId == 3) 100 else 200

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isLocked) "关卡结界未解" else "整装破局（第 $levelId 关）",
                fontWeight = FontWeight.Bold,
                color = CrimsonRed,
                fontFamily = FontFamily.Serif
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isLocked) {
                    Text(
                        text = "本关卡处于封印锁定状态。少侠当前尚未通关前面的关卡，或者您可选择使用积分强行破除关卡结界提前解锁。\n\n提前破阵所需积分：$cost 积分\n您当前积分余额：${state.points}",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color.DarkGray
                    )
                } else {
                    Text(
                        text = "少侠即将踏入消除棋阵，可在此选择挑选背包中法宝携带入局辅佐消除（仅本局生效且会最终消耗）：",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val itemTypes = listOf(
                        "UNDO" to "撤销符",
                        "SHUFFLE" to "洗牌咒",
                        "MOVEOUT" to "移出印",
                        "REVIVE" to "复活丹",
                        "HINT" to "提示符",
                        "BOMB" to "爆裂弹",
                        "JOKER" to "万能牌",
                        "DOUBLE_POINTS" to "双倍卡"
                    )

                    Column(
                        modifier = Modifier
                            .height(220.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        itemTypes.forEach { (type, name) ->
                            val stock = state.backpackItems.find { it.item_type == type }?.count ?: 0
                            val selected = state.selectedCarryItems[type] ?: 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("背包库存: $stock", fontSize = 11.sp, color = Color.Gray)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onUpdateItem(type, -1) },
                                        enabled = selected > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "减", tint = if (selected > 0) CrimsonRed else Color.Gray)
                                    }
                                    Text(
                                        text = selected.toString(),
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { onUpdateItem(type, 1) },
                                        enabled = selected < stock,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "加", tint = if (selected < stock) CrimsonRed else Color.Gray)
                                    }
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isLocked) {
                Button(
                    onClick = {
                        if (state.points >= cost) {
                            onUnlock(levelId)
                        } else {
                            Toaster.show("积分余额不足以强行破阵！")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("积分子系统强行解封", color = Color.White)
                }
            } else {
                Button(
                    onClick = { onConfirm(levelId) },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("启程破阵", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

data class ConflictInfo(
    val localPoints: Int,
    val localLevel: Int,
    val cloudPoints: Int,
    val cloudLevel: Int
)

@Composable
fun ConflictDialog(
    info: ConflictInfo,
    onChooseLocal: () -> Unit,
    onChooseCloud: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {}, // Force selection
        title = {
            Text(
                text = stringResource(id = R.string.dialog_conflict_title),
                fontWeight = FontWeight.Bold,
                color = CrimsonRed
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.dialog_conflict_desc),
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Local Save Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .clickable { onChooseLocal() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F7F5)),
                        border = BorderStroke(1.dp, GoldenBronze),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(id = R.string.save_local),
                                fontWeight = FontWeight.Bold,
                                color = GoldenBronze,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.save_level, info.localLevel),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = stringResource(id = R.string.save_points, info.localPoints),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Cloud Save Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .clickable { onChooseCloud() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F9F7)),
                        border = BorderStroke(1.dp, CrimsonRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(id = R.string.save_cloud),
                                fontWeight = FontWeight.Bold,
                                color = CrimsonRed,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.save_level, info.cloudLevel),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = stringResource(id = R.string.save_points, info.cloudPoints),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        shape = RoundedCornerShape(16.dp)
    )
}


@Composable
fun OfflineWarnBanner() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFBEBEB))
            .clickable {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // ignore
                }
            }
            .padding(vertical = 10.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = CrimsonRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.no_net_warn),
                color = CrimsonRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
