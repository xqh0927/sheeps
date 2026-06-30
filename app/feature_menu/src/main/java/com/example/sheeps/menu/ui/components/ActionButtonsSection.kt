package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.R
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.theme.CrimsonRed

/**
 * 个人中心底部操作按钮区域
 * 包含：积分日志、兑换日志、退出登录
 *
 * @param state 界面状态数据
 * @param onShowPointHistory 显示积分历史回调
 * @param onShowExchangeHistory 显示兑换历史回调
 * @param onLogoutClick 退出登录回调
 */
@Composable
fun ActionButtonsSection(
    state: MenuViewState,
    onShowPointHistory: () -> Unit,
    onShowExchangeHistory: () -> Unit,
    onLogoutClick: () -> Unit
) {
    if (!state.isLoggedIn) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onShowPointHistory,
            border = BorderStroke(0.5.dp, CrimsonRed),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRed)
        ) {
            Text(stringResource(id = R.string.btn_view_points_log))
        }

        OutlinedButton(
            onClick = onShowExchangeHistory,
            border = BorderStroke(0.5.dp, CrimsonRed),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRed)
        ) {
            Text(stringResource(id = R.string.btn_view_exchange_log))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLogoutClick,
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(stringResource(id = R.string.btn_logout), color = Color.White)
        }
    }
}
