package com.example.sheeps.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
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
    onRestart: () -> Unit,
    onCastSpell: (String) -> Unit // 新增：施放法术事件
) {
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        showExitConfirmDialog = true
    }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("确定要认输退出吗？", fontWeight = FontWeight.Bold) },
            text = { Text("中途退出对局将直接判定失败。确认要认输退出吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmDialog = false
                        onLeave()
                    }
                ) {
                    Text("确定认输", color = Crimson_Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

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
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部：双方进度与能量条对比
            DuelHeader(state = state, onLeave = { showExitConfirmDialog = true })
            
            // 攻击与诅咒大招提示条（在棋盘上方，避免遮盖）
            val overlayMsg = state.activeSpellMessage ?: state.incomingAttackMessage
            AnimatedVisibility(
                visible = overlayMsg != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                overlayMsg?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Crimson_Primary.copy(alpha = 0.9f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = msg, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }

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

                    // 迷雾障眼特效覆盖层
                    if (state.isFogActive) {
                        var touchOffset by remember { mutableStateOf<Offset?>(null) }
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown()
                                        touchOffset = down.position
                                        do {
                                            val event = awaitPointerEvent()
                                            val anyPressed = event.changes.any { it.pressed }
                                            if (anyPressed) {
                                                val position = event.changes.firstOrNull { it.pressed }?.position
                                                if (position != null) {
                                                    touchOffset = position
                                                }
                                            } else {
                                                touchOffset = null
                                            }
                                        } while (event.changes.any { it.pressed })
                                        touchOffset = null
                                    }
                                }
                        ) {
                            val offset = touchOffset
                            if (offset == null) {
                                drawRect(color = Color(0xFA202020))
                            } else {
                                val paint = Paint().apply {
                                    color = Color.Black
                                    style = PaintingStyle.Fill
                                }
                                drawIntoCanvas { canvas ->
                                    canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
                                    drawRect(color = Color(0xFA202020))
                                    val radius = 180f
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color.Transparent, Color(0xFA202020)),
                                            center = offset,
                                            radius = radius
                                        ),
                                        radius = radius,
                                        center = offset,
                                        blendMode = BlendMode.DstOut
                                    )
                                    canvas.restore()
                                }
                            }
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
                    val isLocked = i == 6 && state.maxSlotSize == 6
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isLocked) Crimson_Primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isLocked) Crimson_Primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLocked) {
                            Text("🔒", fontSize = 16.sp)
                        } else if (i < state.slotTiles.size) {
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

            Spacer(Modifier.height(12.dp))

            // 恶搞大招/法宝面板
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpellButton(
                    name = "迷雾",
                    cost = 3,
                    currentEnergy = state.currentEnergy,
                    isUsed = state.usedSpells.contains("FOG"),
                    isSilenced = state.isSilenced,
                    onClick = { onCastSpell("FOG") },
                    color = Color(0xFF8C7B70),
                    modifier = Modifier.weight(1f)
                )
                SpellButton(
                    name = "禁魔",
                    cost = 4,
                    currentEnergy = state.currentEnergy,
                    isUsed = state.usedSpells.contains("SILENCE"),
                    isSilenced = state.isSilenced,
                    onClick = { onCastSpell("SILENCE") },
                    color = Color(0xFF7E57C2),
                    modifier = Modifier.weight(1f)
                )
                SpellButton(
                    name = "乱序",
                    cost = 5,
                    currentEnergy = state.currentEnergy,
                    isUsed = state.usedSpells.contains("SHUFFLE"),
                    isSilenced = state.isSilenced,
                    onClick = { onCastSpell("SHUFFLE") },
                    color = Color(0xFF26A69A),
                    modifier = Modifier.weight(1f)
                )
                SpellButton(
                    name = "锁槽",
                    cost = 6,
                    currentEnergy = state.currentEnergy,
                    isUsed = state.usedSpells.contains("SHRINK"),
                    isSilenced = state.isSilenced,
                    onClick = { onCastSpell("SHRINK") },
                    color = Crimson_Primary,
                    modifier = Modifier.weight(1f)
                )
                SpellButton(
                    name = "封魔",
                    cost = 10,
                    currentEnergy = state.currentEnergy,
                    isUsed = state.usedSpells.contains("SEAL_ALL"),
                    isSilenced = state.isSilenced,
                    onClick = { onCastSpell("SEAL_ALL") },
                    color = Gold_Primary,
                    modifier = Modifier.weight(1f)
                )
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

        // 阻止加载时手势穿透的全屏遮罩
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
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
                SheepsLoading(size = 56.dp)
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
                val totalTiles = if (state.totalTileCount > 0) state.totalTileCount else 100
                LinearProgressIndicator(
                    progress = 1f - (remaining.toFloat() / totalTiles.toFloat()).coerceIn(0f, 1f), // 假设 100 张牌
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

            Spacer(Modifier.height(4.dp))

            // 能量流光槽
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("能", modifier = Modifier.width(30.dp), style = MaterialTheme.typography.labelSmall)
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
                            .fillMaxWidth(state.currentEnergy.toFloat() / 10f)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF00E5FF), Color(0xFF0288D1))
                                )
                            )
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${state.currentEnergy}/10",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
            }
        }
    }
}

@Composable
private fun SpellButton(
    name: String,
    cost: Int,
    currentEnergy: Int,
    isUsed: Boolean,
    isSilenced: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isEnabled = currentEnergy >= cost && !isUsed && !isSilenced
    val alpha = if (isEnabled) 1.0f else 0.4f
    
    val buttonText = if (isUsed) "已用" else if (isSilenced) "禁魔" else name
    
    Button(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp),
        modifier = modifier.alpha(alpha)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = buttonText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) Color.White else Color.Gray,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${cost}能",
                fontSize = 9.sp,
                color = if (isEnabled) Color.White.copy(alpha = 0.8f) else Color.Gray,
                maxLines = 1
            )
        }
    }
}
