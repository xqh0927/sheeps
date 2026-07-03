package com.example.sheeps.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sheeps.theme.Text_Secondary_Dark

import androidx.compose.ui.res.stringResource
import com.example.sheeps.core.R

/**
 * 游戏顶栏组件
 * 包含：返回按钮、关卡标题、重玩按钮
 * 
 * @param currentLevelId 当前关卡ID
 * @param onBack 返回点击回调
 * @param onRestart 重玩点击回调
 */
@Composable
fun GameHeader(
    currentLevelId: Int,
    onBack: () -> Unit,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .statusBarsPadding()
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.btn_back),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            // 关卡标题
            Text(
                text = stringResource(id = R.string.prepare_title_unlocked, currentLevelId),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // 重玩按钮
            IconButton(onClick = onRestart) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "重玩",
                    tint = Text_Secondary_Dark
                )
            }
        }
    }
}
