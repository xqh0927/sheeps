package com.example.sheeps.game.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.sheeps.core.multiplayer.WebSocketManager
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.*
import com.example.sheeps.game.ui.components.TileView
import com.example.sheeps.theme.*
import com.example.sheeps.ui.components.*

@Composable
fun DuelScreen(
    state: DuelViewState,
    onTileClick: (Tile) -> Unit,
    onLeave: () -> Unit,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部：双方进度对比
            DuelHeader(state = state, onLeave = onLeave)
            
            Spacer(Modifier.height(16.dp))

            // 游戏棋盘
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    SheepsLoading(size = 56.dp)
                } else {
                    val visibleTiles = state.boardTiles.filter {
                        it.state == TileState.NORMAL || it.state == TileState.BLOCKED
                    }
                    if (visibleTiles.isNotEmpty()) {
                        val minX = visibleTiles.minOf { it.x }
                        val maxX = visibleTiles.maxOf { it.x }
                        val minY = visibleTiles.minOf { it.y }
                        val maxY = visibleTiles.maxOf { it.y }

                        val tileSize = 52
                        val boardWidth = (maxX - minX) * 46 + tileSize
                        val boardHeight = (maxY - minY) * 46 + tileSize

                        Box(modifier = Modifier.size(width = boardWidth.dp, height = boardHeight.dp)) {
                            visibleTiles.forEach { tile ->
                                TileView(
                                    tile = tile,
                                    onClick = { onTileClick(tile) },
                                    currentSkin = "classic",
                                    tileSize = 52.dp,
                                    modifier = Modifier
                                        .offset(
                                            x = ((tile.x - minX) * 46).dp,
                                            y = ((tile.y - minY) * 46).dp
                                        )
                                        .zIndex(tile.z.toFloat())
                                )
                            }
                        }
                    }
                }
                
                // 攻击提示覆盖层
                state.incomingAttackMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 20.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Crimson_Primary.copy(alpha = 0.9f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(text = msg, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            
            // 移出置物架
            if (state.movedOutTiles.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(8.dp)
                ) {
                    state.movedOutTiles.forEach { tile ->
                        TileView(
                            tile = tile,
                            onClick = { onTileClick(tile) },
                            currentSkin = "classic",
                            tileSize = 46.dp
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))

            // 消除槽
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 7) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (i < state.slotTiles.size) {
                            TileView(
                                tile = state.slotTiles[i],
                                onClick = {},
                                currentSkin = "classic",
                                tileSize = 40.dp
                            )
                        }
                    }
                }
            }
        }

        // 结算弹窗
        if (state.gameStatus == GameStatus.WON || state.gameStatus == GameStatus.LOST) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state.gameStatus == GameStatus.WON) "旗开得胜！" else "棋差一招",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (state.gameStatus == GameStatus.WON) Gold_Primary else Crimson_Primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(text = "你的得分: ${state.score}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "对手得分: ${state.opponentScore}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton(text = "离开", onClick = onLeave, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun DuelHeader(state: DuelViewState, onLeave: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
            
            // 连接状态指示器
            Row(verticalAlignment = Alignment.CenterVertically) {
                val color = when (state.connectionState) {
                    WebSocketManager.ConnectionState.Connected -> Color.Green
                    WebSocketManager.ConnectionState.Connecting -> Color.Yellow
                    else -> Color.Red
                }
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
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
        
        // 进度条对比
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // 自己
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("我", modifier = Modifier.width(30.dp), style = MaterialTheme.typography.labelSmall)
                val remaining = state.boardTiles.count { it.state == TileState.NORMAL || it.state == TileState.BLOCKED } + state.movedOutTiles.size
                LinearProgressIndicator(
                    progress = 1f - (remaining.toFloat() / 100f).coerceIn(0f, 1f), // 假设 100 张牌
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Gold_Primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            // 对手
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("敌", modifier = Modifier.width(30.dp), style = MaterialTheme.typography.labelSmall)
                LinearProgressIndicator(
                    progress = state.opponentProgress.coerceIn(0f, 1f),
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Crimson_Primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
