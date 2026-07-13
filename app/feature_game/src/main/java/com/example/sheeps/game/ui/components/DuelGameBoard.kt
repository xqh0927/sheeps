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
import androidx.compose.ui.platform.LocalDensity
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

    val tileSize = 46
    val spacing = 46
    // 计算卡片内容边界尺寸
    val contentWidth = (maxX - minX) * spacing + tileSize
    val contentHeight = (maxY - minY) * spacing + tileSize

    // 棋盘自适应宽度：保证左右比屏幕边缘少 16dp
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val boardWidth = screenWidth - 32.dp
    val boardHeight = 420.dp

    val displayedWidth = contentWidth.dp
    val displayedHeight = contentHeight.dp
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
            // 棋盘内容区根容器：仅在此测量一次，捕获全局坐标（px）后按公式推导每张牌全局坐标，
            // 取代逐牌 onGloballyPositioned。飞行动画点击瞬间读取 tileGlobalPositions[tile.id] 作为起点。
            val boardRootPos = remember { mutableStateOf(Offset.Zero) }
            val density = LocalDensity.current.density

            Box(
                modifier = Modifier
                    .size(width = displayedWidth, height = displayedHeight)
                    .onGloballyPositioned { coords ->
                        boardRootPos.value = coords.positionInRoot()
                        computeTileGlobalPositions(
                            boardRootPos = boardRootPos.value,
                            density = density,
                            minX = minX,
                            minY = minY,
                            spacing = spacing,
                            scale = 1f,
                            visibleTiles = visibleTiles,
                            tileGlobalPositions = tileGlobalPositions
                        )
                    }
            ) {
                visibleTiles.forEach { tile ->
                    key(tile.id) {
                        val isFlying = flyingTileIds.contains(tile.id)
                        TileView(
                            tile = tile,
                            onClick = { if (!isFlying) onTileClick(tile) },
                            currentSkin = "shuang",
                            tileSize = 46.dp,
                            isShaking = state.shakingTileIds.contains(tile.id),
                            modifier = Modifier
                                .offset(
                                    x = ((tile.x - minX) * spacing).dp,
                                    y = ((tile.y - minY) * spacing).dp
                                )
                                .zIndex(tile.z.toFloat())
                                .alpha(if (isFlying) 0f else 1f)
                        )
                    }
                }
            }

            // 当 visibleTiles 变化（揭示新牌）而根布局未触发布局回调时，主动补写一次坐标（兜底）。
            LaunchedEffect(visibleTiles) {
                if (boardRootPos.value != Offset.Zero) {
                    computeTileGlobalPositions(
                        boardRootPos = boardRootPos.value,
                        density = density,
                        minX = minX,
                        minY = minY,
                        spacing = spacing,
                        scale = 1f,
                        visibleTiles = visibleTiles,
                        tileGlobalPositions = tileGlobalPositions
                    )
                }
            }
        }

        // 迷雾障眼特效（诅咒效果）
        // 线程边界：FogEffectOverlay 的 pointerInput 手势处理运行在主线程 UI 协程上下文中，
        // 仅更新 Compose 状态 touchOffset，不阻塞渲染；下方 onGloballyPositioned 同样为主线程布局回调，
        // 写入外部 MutableMap tileGlobalPositions 供飞行动画使用。
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

/**
 * 依据棋盘内容区根容器全局坐标（px）与布局参数，推导每张可见牌的屏幕全局坐标（px）。
 * 原实现为每张 TileView 挂 [androidx.compose.ui.layout.onGloballyPositioned]；
 * 此处改为根容器测一次后按公式推算（dp→px 需乘 density），与逐牌回调写入的左上角坐标一致。
 */
private fun computeTileGlobalPositions(
    boardRootPos: Offset,
    density: Float,
    minX: Float,
    minY: Float,
    spacing: Int,
    scale: Float,
    visibleTiles: List<Tile>,
    tileGlobalPositions: MutableMap<String, Offset>
) {
    visibleTiles.forEach { tile ->
        val x = boardRootPos.x + (tile.x - minX) * spacing * scale * density
        val y = boardRootPos.y + (tile.y - minY) * spacing * scale * density
        tileGlobalPositions[tile.id] = Offset(x, y)
    }
}
