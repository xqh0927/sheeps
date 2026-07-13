package com.example.sheeps.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.GameViewState

/**
 * 游戏主棋盘组件
 * 负责渲染棋盘背景、层级排列的所有卡牌（Tile）
 *
 * @param state 游戏界面状态
 * @param flyingTileIds 当前正在飞向槽位的卡牌ID集合（用于隐藏原始位置卡牌）
 * @param tileGlobalPositions 用于记录卡牌在屏幕上的全局位置，供动画使用
 * @param onTileClick 卡牌点击事件回调
 */
@Composable
fun GameBoard(
    state: GameViewState,
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
    // 计算卡片内容边界尺寸（未缩放）
    val contentWidth = (maxX - minX) * spacing + tileSize
    val contentHeight = (maxY - minY) * spacing + tileSize

    // 棋盘自适应宽度：保证左右比屏幕边缘少 16dp
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val boardWidth = screenWidth - 32.dp
    val boardHeight = 420.dp

    // 采用方案二：服务端限制生成网格在 6x7 内，客户端固定卡牌尺寸不缩放
    val scale = 1f

    val displayedWidth = (contentWidth * scale).dp
    val displayedHeight = (contentHeight * scale).dp

    Box(
        modifier = modifier
            .width(boardWidth)
            .heightIn(min = 200.dp, max = boardHeight)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clipToBounds()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        // 当 boardTiles 引用或 flyingTileIds 变化时重算 visibleTiles
        // flyingTileIds 加入 key 确保动画结束后 flyingTileIds 清空时正确重算
        // 线程边界：remember 在主线程组合期求值；下方 onGloballyPositioned 为布局期回调（主线程），
        // 直接写入外部传入的可变 Map tileGlobalPositions，供飞行动画读取屏幕绝对坐标。
        val visibleTiles = remember(state.boardTiles, flyingTileIds) {
            state.boardTiles.filter { tile ->
                (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED) &&
                tile.id !in flyingTileIds
            }
        }

        if (visibleTiles.isNotEmpty()) {
            // 关键修复：用"缩放后实际尺寸"作为 Box size（不是原始 contentWidth）
            // 然后把每个 tile 的 offset 同步乘以 scale
            // 这样 Alignment.Center 会按真实可见大小居中，不再有偏移/裁切
            // 棋盘内容区根容器：仅在根容器测量一次，捕获其全局坐标（px），
            // 再按布局公式推导每张可见牌的全局坐标写入 tileGlobalPositions，
            // 取代此前给每张 TileView 挂 onGloballyPositioned（300 次回调/帧）。
            // 飞行动画点击瞬间读取 tileGlobalPositions[tile.id] 作为起点，保证首帧布局后即写入。
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
                            scale = scale,
                            visibleTiles = visibleTiles,
                            tileGlobalPositions = tileGlobalPositions
                        )
                    }
            ) {
                visibleTiles.forEach { tile ->
                    key(tile.id) {
                        val isHighlighted = state.highlightedTileIds.contains(tile.id)
                        val isFlying = flyingTileIds.contains(tile.id)

                        TileView(
                            tile = tile,
                            onClick = { if (!isFlying) onTileClick(tile) },
                            currentSkin = state.currentSkin,
                            tileSize = (46 * scale).dp,
                            isShaking = state.shakingTileIds.contains(tile.id),
                            isHighlighted = isHighlighted,
                            gateLocked = tile.sealedCount > 0 && tile.id !in state.sealedUnlockedIds,
                            modifier = Modifier
                                .offset(
                                    x = ((tile.x - minX) * spacing * scale).dp,
                                    y = ((tile.y - minY) * spacing * scale).dp
                                )
                                .zIndex(tile.z.toFloat())
                        )
                    }
                }
            }

            // 当 visibleTiles 变化（揭示新牌）而根布局未触发布局回调时，主动补写一次坐标，
            // 确保飞行动画在点击瞬间能读到正确起点（首帧布局已写入，此处为兜底）。
            LaunchedEffect(visibleTiles) {
                if (boardRootPos.value != Offset.Zero) {
                    computeTileGlobalPositions(
                        boardRootPos = boardRootPos.value,
                        density = density,
                        minX = minX,
                        minY = minY,
                        spacing = spacing,
                        scale = scale,
                        visibleTiles = visibleTiles,
                        tileGlobalPositions = tileGlobalPositions
                    )
                }
            }
        }
    }
}

/**
 * 依据棋盘内容区根容器全局坐标（px）与布局参数，推导每张可见牌的屏幕全局坐标（px）。
 *
 * 原实现为每张 TileView 挂 [androidx.compose.ui.layout.onGloballyPositioned]；
 * 此处改为根容器测一次后按公式推算：
 * `globalX = boardRootPos.x + (tile.x - minX) * spacing * scale * density`
 * （[androidx.compose.ui.layout.positionInRoot] 返回 px，TileView 的 offset 为 dp，故需乘 density 还原到 px 坐标系）。
 * 与逐牌回调写入的卡牌左上角坐标保持一致，飞行动画起点不变。
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
