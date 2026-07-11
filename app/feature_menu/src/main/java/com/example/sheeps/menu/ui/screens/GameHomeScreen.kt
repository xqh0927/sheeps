package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.components.AnnouncementsBanner
import com.example.sheeps.menu.ui.components.EndlessIntroDialog
import com.example.sheeps.menu.ui.components.EndlessModeEntry
import com.example.sheeps.menu.ui.components.LevelItemRow
import com.example.sheeps.menu.ui.dialogs.DuelMatchDialog
import com.example.sheeps.theme.*
import com.example.sheeps.core.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.hjq.toast.Toaster
import com.therouter.TheRouter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ⚠️ 状态隐患：文件级顶层可变变量（相当于静态全局状态），跨 Composable 实例与配置变更（如旋转屏幕）
// 持续保留，用于「冷启动仅自动滚动一次」的全局标记。它不是 Context/View 引用，故不构成内存泄漏，
// 但属于隐式共享可变状态，存在多实例/测试环境相互干扰风险；若需更严谨应改为 ViewModel 或 rememberSaveable 状态。
private var isColdStartAutoScrolled = false

@Composable
fun GameHomeScreen(
    state: MenuViewState,
    onLevelClick: (Int) -> Unit,
    onShowLeaderboard: (Int) -> Unit,
    onNoticeClick: () -> Unit,
    onLoginClick: () -> Unit,
    onJoinMatch: () -> Unit,
    onLeaveMatch: () -> Unit,
    onResetMatch: () -> Unit,
    onNavigateToDuel: (String, String, Int, Int) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 公告轮播
        AnnouncementsBanner(
            notices = state.notices,
            onClick = onNoticeClick
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 区域分割标题（如“天命修行”）
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
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                        )
                    )
            )
            Text(
                text = stringResource(id = R.string.home_section_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                fontFamily = FontFamily.Serif
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), Color.Transparent)
                        )
                    )
            )
        }

        // === 天命对决 · 多人实时对战入口 & 无尽生存入口（并排等大卡片） ===
        var showDuelMatch by remember { mutableStateOf(false) }

        if (showDuelMatch) {
            DuelMatchDialog(
                state = state,
                onJoin = onJoinMatch,
                onLeave = onLeaveMatch,
                onDismiss = { 
                    showDuelMatch = false
                    onResetMatch()
                }
            )
        }

        // 无尽生存模式介绍弹窗状态
        var showEndlessIntro by remember { mutableStateOf(false) }

        if (showEndlessIntro) {
            EndlessIntroDialog(
                currentSkin = state.currentSkin,
                isLoggedIn = state.isLoggedIn,
                onDismiss = { showEndlessIntro = false },
                onStart = {
                    showEndlessIntro = false
                    TheRouter.build("/endless/play").navigation(context)
                },
                onLoginClick = {
                    showEndlessIntro = false
                    onLoginClick()
                }
            )
        }

        // 匹配成功后自动倒计时并跳转到对决战场
        // 协程绑定组合生命周期，退出组合即取消；delay 为主线程挂起不阻塞 UI。
        LaunchedEffect(state.matchStatus) {
            if (state.matchStatus == "matched") {
                delay(1500)
                onNavigateToDuel(state.matchedGameId ?: "", state.phone, state.duelLevel, state.gameSeed)
                showDuelMatch = false
                onResetMatch()
            }
        }

        val matchHint = stringResource(id = R.string.home_match_hint)
        val endlessLoginHint = stringResource(id = R.string.home_endless_login_hint)

        // PvP 对战入口与无尽生存入口上下两行展示
        Column(modifier = Modifier.fillMaxWidth()) {
            // 上行：在线匹配对战（PvP）入口 — 服务端可通过 game_modes.battle=false 关闭
            val battleEnabled = state.gameModes?.battle == true
            if (battleEnabled) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!state.isLoggedIn) {
                                onLoginClick()
                                Toaster.show(matchHint)
                            } else {
                                showDuelMatch = true
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 太极小图标
                        val tp = MaterialTheme.colorScheme.primary
                        Canvas(modifier = Modifier.size(36.dp)) {
                            val r = size.width / 2f
                            val centerOffset = center
                            drawCircle(color = Color.White, radius = r, center = centerOffset)
                            drawArc(
                                color = tp,
                                startAngle = -90f,
                                sweepAngle = 180f,
                                useCenter = true,
                                size = size
                            )
                            drawCircle(
                                color = tp,
                                radius = r / 2f,
                                center = Offset(centerOffset.x, centerOffset.y - r / 2f)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = r / 2f,
                                center = Offset(centerOffset.x, centerOffset.y + r / 2f)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = r * 0.15f,
                                center = Offset(centerOffset.x, centerOffset.y - r / 2f)
                            )
                            drawCircle(
                                color = tp,
                                radius = r * 0.15f,
                                center = Offset(centerOffset.x, centerOffset.y + r / 2f)
                            )
                            drawCircle(
                                color = tp,
                                radius = r,
                                center = centerOffset,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.home_match_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = stringResource(id = R.string.home_match_desc),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }

                // 行间分隔
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 下行：无尽生存入口（皮肤感知；未登录时拦截跳登录，登录后打开介绍弹窗）
            // 服务端可通过 /api/app/check-update 返回 game_modes.endless=false 来关闭此模式
            val endlessEnabled = state.gameModes?.endless == true
            if (endlessEnabled) {
                EndlessModeEntry(
                    currentSkin = state.currentSkin,
                    isLoggedIn = state.isLoggedIn,
                    onOpenIntro = { showEndlessIntro = true },
                    onLoginClick = {
                        onLoginClick()
                        Toaster.show(endlessLoginHint)
                    },
                    onShowLeaderboard = {
                        TheRouter.build("/endless/leaderboard").navigation(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 关卡列表
        val levels = remember(state.unlockedLevel) { (1..maxOf(20, state.unlockedLevel + 5)).toList() }
        val listState = rememberLazyListState()
        var hasAutoScrolled by remember { mutableStateOf(isColdStartAutoScrolled) }

        // 解锁关卡变化时的自动定位滚动：协程随组合生命周期自动取消；内部 launch{} 为子协程同样受作用域约束。
        LaunchedEffect(state.unlockedLevel) {
            val targetIndex = maxOf(0, state.unlockedLevel - 1)
            if (targetIndex < levels.size) {
                if (state.unlockedLevel > 1) {
                    if (!hasAutoScrolled) {
                        delay(300) // 延迟 300ms 等界面渲染稳定后开始滚动
                        listState.animateScrollToItemSmoothly(targetIndex)
                        hasAutoScrolled = true
                        isColdStartAutoScrolled = true
                    }
                } else {
                    // 如果是第 1 关，说明可能是刚开机默认的初始占位值，先瞬间重置定位但不要锁定 hasAutoScrolled，直到真实关卡值加载出来
                    if (!hasAutoScrolled) {
                        listState.scrollToItem(0)
                        // 启动一个保护协程：如果 1500ms 内 unlockedLevel 一直保持为 1，说明该用户确实只解锁了第 1 关，此时锁定自动滚动状态
                        launch {
                            delay(1500)
                            if (state.unlockedLevel == 1) {
                                hasAutoScrolled = true
                                
                                isColdStartAutoScrolled = true
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = levels, key = { it }) { lvl ->
                    val isUnlocked = lvl <= state.unlockedLevel
                    LevelItemRow(
                        levelId    = lvl,
                        isUnlocked = isUnlocked,
                        onStart    = { onLevelClick(lvl) },
                        onShowLeaderboard = { onShowLeaderboard(lvl) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    val levelsUnlockedDesc = stringResource(id = R.string.home_levels_unlocked_desc)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clickable {
                                Toaster.show(levelsUnlockedDesc)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_levels_unlocked_btn),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }
                }
            }

            // 定位按钮 (仅在当前有已解锁关卡且列表未在动画滚动时响应)
            // 通过 rememberCoroutineScope 获取与组合生命周期绑定的作用域，FAB 点击触发的一次性滚动在组合销毁时自动取消。
            val coroutineScope = rememberCoroutineScope()
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        val targetIndex = maxOf(0, state.unlockedLevel - 1)
                        if (targetIndex < levels.size) {
                            listState.animateScrollToItemSmoothly(targetIndex)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 8.dp)
                    .size(44.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "定位最新关卡",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 平滑且低卡顿滚动到目标 Item 的扩展方法
 * 针对长距离滚动进行优化：如果跨度大，则先进行无感知 Snap 跳转到附近，再启动平滑动画滚动
 */
private suspend fun androidx.compose.foundation.lazy.LazyListState.animateScrollToItemSmoothly(
    index: Int,
    scrollOffset: Int = 0
) {
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) {
        scrollToItem(index, scrollOffset)
        return
    }
    val firstVisible = visibleItems.first().index
    val lastVisible = visibleItems.last().index
    
    // 如果目标项已经在屏幕中可见，直接进行微调平滑滚动即可
    if (index in firstVisible..lastVisible) {
        animateScrollToItem(index, scrollOffset)
        return
    }
    
    val distance = index - firstVisible
    if (kotlin.math.abs(distance) > 4) {
        // 先跳到距离目标还有 3 个 Item 的位置，避免长距离渲染大量不可见组件导致的帧率卡顿
        val snapIndex = if (distance > 0) index - 3 else index + 3
        scrollToItem(snapIndex.coerceIn(0, layoutInfo.totalItemsCount - 1))
    }
    animateScrollToItem(index, scrollOffset)
}
