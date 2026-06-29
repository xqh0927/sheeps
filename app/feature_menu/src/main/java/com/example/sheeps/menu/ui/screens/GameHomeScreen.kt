package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.sheeps.menu.ui.components.LevelItemRow
import com.example.sheeps.menu.ui.dialogs.DuelMatchDialog
import com.example.sheeps.theme.*
import com.hjq.toast.Toaster
import kotlinx.coroutines.delay

@Composable
fun GameHomeScreen(
    state: MenuViewState,
    onLevelClick: (Int) -> Unit,
    onLoginClick: () -> Unit,
    onJoinMatch: () -> Unit,
    onLeaveMatch: () -> Unit,
    onResetMatch: () -> Unit,
    onNavigateToDuel: (String, String, Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        // === 天命对决 · 多人实时对战入口 ===
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

        // Auto-navigate when matched
        LaunchedEffect(state.matchStatus) {
            if (state.matchStatus == "matched") {
                delay(1500)
                onNavigateToDuel(state.matchedGameId ?: "", state.phone, state.duelLevel, state.gameSeed)
                showDuelMatch = false
                onResetMatch()
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (!state.isLoggedIn) {
                        onLoginClick()
                        Toaster.show("天命对决需先登录，方可一战！")
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
                Canvas(modifier = Modifier.size(36.dp)) {
                    val r = size.width / 2f
                    drawCircle(color = CrimsonRed, radius = r, style = Stroke(width = 2.dp.toPx()))
                    drawArc(color = CrimsonRed, startAngle = -90f, sweepAngle = 180f, useCenter = true, size = size)
                    drawCircle(color = Color.White, radius = r * 0.2f, center = Offset(size.width / 2f, size.height * 0.25f))
                    drawCircle(color = CrimsonRed, radius = r * 0.2f, center = Offset(size.width / 2f, size.height * 0.75f))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "天命对决 · 实时匹配",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = CrimsonRed,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        "与天下修士一较高下，赢取修为积分！",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = CrimsonRed.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 关卡列表
        val levels = remember(state.unlockedLevel) { (1..maxOf(20, state.unlockedLevel + 5)).toList() }
        val listState = rememberLazyListState()

        LaunchedEffect(state.unlockedLevel) {
            val targetIndex = maxOf(0, state.unlockedLevel - 1)
            if (targetIndex < levels.size) {
                listState.scrollToItem(targetIndex)
            }
        }

        LazyColumn(
            state = listState,
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
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Gold_Subtle.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, Gold_Primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable {
                            Toaster.show("正在窥探玄妙天机... 闯关积攒更高修为，可自动显现无尽难关！")
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✨ 窥探天机 · 解锁更多未知关卡 ✨",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Gold_Primary,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            }
        }
    }
}
