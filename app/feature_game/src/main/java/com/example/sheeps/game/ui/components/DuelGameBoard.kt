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
import androidx.compose.ui.platform.LocalConfiguration
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
    onTileClick: (Tile) -> Unit,
    modifier: Modifier = Modifier
) {
    // 边界优先用 ViewModel 计算好的 boardBounds，再用当前 boardTiles 兜底，防止旧缓存导致偏移
    val actualMinX = state.boardTiles.minOfOrNull { it.x } ?: 0f
    val actualMaxX = state.boardTiles.maxOfOrNull { it.x } ?: 0f
    val actualMinY = state.boardTiles.minOfOrNull { it.y } ?: 0f
    val actualMaxY = state.boardTiles.maxOfOrNull { it.y } ?: 0f

    val minX = minOf(state.boardBounds.minX, actualMinX)
    val maxX = maxOf(state.boardBounds.maxX, actualMaxX)
    val minY = minOf(state.boardBounds.minY, actualMinY)
    val maxY = maxOf(state.boardBounds.maxY, actualMaxY)

    val tileSize = 48
    val spacing = 48
    // 计算卡片内容边界尺寸
    val contentWidth = (maxX - minX) * spacing + tileSize
    val contentHeight = (maxY - minY) * spacing + tileSize

    // 棋盘自适应宽度：保证左右比屏幕边缘少 16dp
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val boardWidth = screenWidth - 32.dp
    val boardHeight = 420.dp

    // 预留边距需覆盖卡牌 4dp shadow + 2dp 边框外沿，确保视觉边距仍有 16dp
    val horizontalPadding = 42.dp
    val verticalPadding = 42.dp

    // 计算缩放比例：优先按宽度缩放，同时受高度限制
    val scale = remember(contentWidth, contentHeight, boardWidth, boardHeight) {
        val availableWidth = (boardWidth - horizontalPadding).value
        val availableHeight = (boardHeight - verticalPadding).value

        val scaleW = if (contentWidth > availableWidth) availableWidth / contentWidth else 1f
        val scaleH = if (contentHeight > availableHeight) availableHeight / contentHeight else 1f
        minOf(scaleW, scaleH)
    }

    // 缩放后真实占位尺寸 —— Box 用这个尺寸，让 Alignment.Center 按真实大小居中
    val displayedWidth = (contentWidth * scale).dp
    val displayedHeight = (contentHeight * scale).dp
    Box(
        modifier = modifier
            .size(width = boardWidth, height = boardHeight)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        val visibleTiles = remember(state.boardTiles) {
            state.boardTiles.filter { it.state == TileState.NORMAL || it.state == TileState.BLOCKED }
        }

        if (visibleTiles.isNotEmpty()) {
            // 关键修复：用"缩放后实际尺寸"作为 Box size
            Box(
                modifier = Modifier.size(width = displayedWidth, height = displayedHeight)
            ) {
                visibleTiles.forEach { tile ->
                    key(tile.id) {
                        val isFlying = flyingTileIds.contains(tile.id)
                        TileView(
                            tile = tile,
                            onClick = { if (!isFlying) onTileClick(tile) },
                            currentSkin = "classic",
                            tileSize = 48.dp,
                            isShaking = state.shakingTileIds.contains(tile.id),
                            modifier = Modifier
                                .offset(
                                    x = ((tile.x - minX) * spacing * scale).dp,
                                    y = ((tile.y - minY) * spacing * scale).dp
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
