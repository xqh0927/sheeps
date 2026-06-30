package com.example.sheeps.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.R
import com.example.sheeps.core.multiplayer.WebSocketManager
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.DuelViewState
import com.example.sheeps.theme.Crimson_Primary
import com.example.sheeps.theme.Gold_Primary

/**
 * 对决模式顶部状态栏
 * 展示：对决标题、连接状态、双方进度条、能量条
 * 
 * @param state 对决界面状态
 * @param onLeave 离开按钮点击回调
 */
@Composable
fun DuelHeader(state: DuelViewState, onLeave: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 第一行：标题与连接状态
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "天命对决",
                style = MaterialTheme.typography.titleMedium,
                color = Gold_Primary,
                fontFamily = FontFamily.Serif
            )
            
            // 连接状态指示
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusColor = when (state.connectionState) {
                    WebSocketManager.ConnectionState.Connected -> Color.Green
                    WebSocketManager.ConnectionState.Connecting -> Color.Yellow
                    else -> Color.Red
                }
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(statusColor))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = when (state.connectionState) {
                        WebSocketManager.ConnectionState.Connected -> "已连接"
                        WebSocketManager.ConnectionState.Connecting -> "连接中"
                        else -> "断开"
                    },
                    style = MaterialTheme.typography.labelSmall
                )
            }

            IconButton(onClick = onLeave) {
                Icon(Icons.Default.Close, contentDescription = "离开")
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 第二部分：双方进度条对比
        DuelProgressBars(state = state)

        Spacer(Modifier.height(4.dp))

        // 第三部分：能量流光槽
        DuelEnergyBar(currentEnergy = state.currentEnergy)
    }
}

@Composable
private fun DuelProgressBars(state: DuelViewState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // 己方进度
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(id = R.string.label_duel_me), modifier = Modifier.width(30.dp), style = MaterialTheme.typography.labelSmall)
            val remaining = state.boardTiles.count { it.state == TileState.NORMAL || it.state == TileState.BLOCKED } + state.movedOutTiles.size
            val totalTiles = if (state.totalTileCount > 0) state.totalTileCount else 100
            LinearProgressIndicator(
                progress = { 1f - (remaining.toFloat() / totalTiles.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Gold_Primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
        // 对手进度
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(id = R.string.label_duel_enemy), modifier = Modifier.width(30.dp), style = MaterialTheme.typography.labelSmall)
            LinearProgressIndicator(
                progress = { state.opponentProgress.coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Crimson_Primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun DuelEnergyBar(currentEnergy: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(id = R.string.label_duel_energy), modifier = Modifier.width(30.dp), style = MaterialTheme.typography.labelSmall)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(currentEnergy.toFloat() / 10f)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00E5FF), Color(0xFF0288D1))
                        )
                    )
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$currentEnergy/10",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF)
        )
    }
}
