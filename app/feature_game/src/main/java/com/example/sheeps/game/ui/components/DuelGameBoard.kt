package com.example.sheeps.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.DuelViewState

/**
 * 对决模式游戏棋盘
 * 负责渲染卡牌排列以及特殊的“迷雾障眼”干扰特效
 */
@Composable
fun DuelGameBoard(
    state: DuelViewState,
    flyingTileIds: Set<String>,
    tileGlobalPositions: MutableMap<String, Offset>,
    onTileClick: (Tile) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
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
                    val isFlying = flyingTileIds.contains(tile.id)
                    TileView(
                        tile = tile,
                        onClick = { if (!isFlying) onTileClick(tile) },
                        currentSkin = "classic",
                        tileSize = 52.dp,
                        isShaking = state.shakingTileIds.contains(tile.id),
                        modifier = Modifier
                            .offset(
                                x = ((tile.x - minX) * 46).dp,
                                y = ((tile.y - minY) * 46).dp
                            )
                            .zIndex(tile.z.toFloat())
                            .alpha(if (isFlying) 0f else 1f)
                            .onGloballyPositioned { coords ->
                                tileGlobalPositions[tile.id] = coords.positionInRoot()
                            }
                    )
                }
            }
        }

        // 迷雾障眼特效（诅咒效果）
        if (state.isFogActive) {
            FogEffectOverlay()
        }
    }
}

/**
 * 迷雾效果遮罩
 * 用户可以通过手指触摸来临时“擦除”一小块迷雾以观察下方卡牌
 */
@Composable
private fun FogEffectOverlay() {
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
                            if (position != null) touchOffset = position
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
