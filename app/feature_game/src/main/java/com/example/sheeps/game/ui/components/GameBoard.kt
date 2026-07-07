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
            Box(
                modifier = Modifier.size(width = displayedWidth, height = displayedHeight)
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
                            modifier = Modifier
                                .offset(
                                    x = ((tile.x - minX) * spacing * scale).dp,
                                    y = ((tile.y - minY) * spacing * scale).dp
                                )
                                .zIndex(tile.z.toFloat())
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInRoot()
                                    val prev = tileGlobalPositions[tile.id]
                                    if (prev == null || prev != pos) {
                                        tileGlobalPositions[tile.id] = pos
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}
