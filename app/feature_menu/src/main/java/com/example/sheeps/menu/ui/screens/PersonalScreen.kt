package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.R
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.components.*
import com.example.sheeps.menu.ui.dialogs.ExchangeHistoryDialog
import com.example.sheeps.menu.ui.dialogs.GameGuideDialog
import com.example.sheeps.menu.ui.dialogs.PointHistoryDialog
import com.therouter.TheRouter

/**
 * 个人中心主界面
 * 采用组件化拆分，保持主文件简洁易读
 *
 * @param state 界面状态数据
 * @param onLoginClick 登录点击回调
 * @param onLogoutClick 退出登录点击回调
 * @param onSignInClick 每日签到点击回调
 * @param onClaimTask 领取任务奖励回调
 * @param onApplySkin 应用/切换皮肤回调
 * @param onGoToPlay 前往游戏关卡回调
 */
@Composable
fun PersonalScreen(
    state: MenuViewState,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSignInClick: () -> Unit,
    onClaimTask: (String) -> Unit,
    onApplySkin: (String) -> Unit,
    onGoToPlay: () -> Unit
) {
    val context = LocalContext.current

    // 弹窗控制状态
    var showPointHistory by remember { mutableStateOf(false) }
    var showExchangeHistory by remember { mutableStateOf(false) }
    var showGameGuide by remember { mutableStateOf(false) }

    // --- 弹窗逻辑 ---
    
    // 游戏指南弹窗
    if (showGameGuide) {
        GameGuideDialog(onDismiss = { showGameGuide = false })
    }

    // 积分历史弹窗
    if (showPointHistory) {
        PointHistoryDialog(
            history = state.pointsHistory,
            onDismiss = { showPointHistory = false }
        )
    }

    // 兑换历史弹窗
    if (showExchangeHistory) {
        ExchangeHistoryDialog(
            history = state.exchangeHistory,
            onDismiss = { showExchangeHistory = false }
        )
    }

    // --- 主界面布局 ---

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. 用户信息卡片与签到（点击头像/昵称跳转用户信息页）
        item {
            UserProfileCard(
                state = state,
                onLoginClick = onLoginClick,
                onSignInClick = onSignInClick,
                onNavigateToUserInfo = {
                    TheRouter.build("/menu/userinfo").navigation(context)
                }
            )
        }

        // 2. 每日任务列表（仅登录可见）
        item {
            DailyTasksCard(
                state = state,
                onClaimTask = onClaimTask
            )
        }

        // 3. 背包物品展示（仅登录且有物品时可见）
        item {
            BackpackCard(
                state = state,
                onApplySkin = onApplySkin,
                onGoToPlay = onGoToPlay
            )
        }

        // 4. 设置入口卡片（使用 TheRouter 导航到独立 Activity）
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // 设置入口
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                TheRouter.build("/menu/settings").navigation(context)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.settings_title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 5. 底部操作按钮（积分历史、兑换历史、退出登录）
        item {
            ActionButtonsSection(
                state = state,
                onShowPointHistory = { showPointHistory = true },
                onShowExchangeHistory = { showExchangeHistory = true },
                onLogoutClick = onLogoutClick
            )
        }
    }
}
