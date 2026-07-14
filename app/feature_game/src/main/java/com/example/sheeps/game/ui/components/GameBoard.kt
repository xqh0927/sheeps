package com.example.sheeps.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    // 使用 remember 缓存棋盘的边界和宽高计算，防止每次微小的重绘都要去遍历 state.boardTiles
    val dimensions = remember(state.boardTiles, state.boardBounds) {
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
        val scale = 1f

        BoardDimensions(
            minX = minX,
            minY = minY,
            spacing = spacing,
            scale = scale,
            displayedWidth = (contentWidth * scale).dp,
            displayedHeight = (contentHeight * scale).dp
        )
    }

    // 棋盘自适应宽度：保证左右比屏幕边缘少 16dp
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val boardWidth = screenWidth - 32.dp
    val boardHeight = 420.dp

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
            val boardRootPos = remember { mutableStateOf(Offset.Zero) }
            val density = LocalDensity.current.density

            Box(
                modifier = Modifier
                    .size(width = dimensions.displayedWidth, height = dimensions.displayedHeight)
                    .onGloballyPositioned { coords ->
                        boardRootPos.value = coords.positionInRoot()
                    }
            ) {
                visibleTiles.forEach { tile ->
                    key(tile.id) {
                        val isFlying = flyingTileIds.contains(tile.id)

                        TileView(
                            tile = tile,
                            onClick = {
                                if (!isFlying) {
                                    // 精准即时计算当前点击卡牌的全局屏幕位置，省去 O(N) 批量写入导致的大面积重组
                                    val posX =
                                        boardRootPos.value.x + (tile.x - dimensions.minX) * dimensions.spacing * dimensions.scale * density
                                    val posY =
                                        boardRootPos.value.y + (tile.y - dimensions.minY) * dimensions.spacing * dimensions.scale * density
                                    tileGlobalPositions[tile.id] = Offset(posX, posY)
                                    onTileClick(tile)
                                }
                            },
                            currentSkin = state.currentSkin,
                            tileSize = (46 * dimensions.scale).dp,
                            isShakingProvider = { state.shakingTileIds.contains(tile.id) },
                            isHighlightedProvider = { state.highlightedTileIds.contains(tile.id) },
                            gateLockedProvider = { tile.sealedCount > 0 && tile.id !in state.sealedUnlockedIds },
                            modifier = Modifier
                                .offset(
                                    x = ((tile.x - dimensions.minX) * dimensions.spacing * dimensions.scale).dp,
                                    y = ((tile.y - dimensions.minY) * dimensions.spacing * dimensions.scale).dp
                                )
                                .zIndex(tile.z.toFloat())
                        )
                    }
                }
            }
        }
    }
}

/** 封装棋盘宽高边界与缩放比例的局部实体类 */
internal data class BoardDimensions(
    val minX: Float,
    val minY: Float,
    val spacing: Int,
    val scale: Float,
    val displayedWidth: androidx.compose.ui.unit.Dp,
    val displayedHeight: androidx.compose.ui.unit.Dp
)
