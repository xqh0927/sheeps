package com.example.sheeps.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import com.example.sheeps.theme.Gold_Primary

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
    val minX = remember(state.currentLevelId, state.boardTiles.isEmpty()) {
        state.boardTiles.minOfOrNull { it.x } ?: 0f
    }
    val maxX = remember(state.currentLevelId, state.boardTiles.isEmpty()) {
        state.boardTiles.maxOfOrNull { it.x } ?: 0f
    }
    val minY = remember(state.currentLevelId, state.boardTiles.isEmpty()) {
        state.boardTiles.minOfOrNull { it.y } ?: 0f
    }
    val maxY = remember(state.currentLevelId, state.boardTiles.isEmpty()) {
        state.boardTiles.maxOfOrNull { it.y } ?: 0f
    }

    val tileSize = 48
    val spacing = 46
    // 计算卡片内容边界尺寸
    val contentWidth = (maxX - minX) * spacing + tileSize
    val contentHeight = (maxY - minY) * spacing + tileSize

    // 棋盘自适应宽度：保证左右比屏幕边缘少 16dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val boardWidth = screenWidth - 32.dp
    val boardHeight = 450.dp

    Box(
        modifier = modifier
            .size(width = boardWidth, height = boardHeight)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
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
        val visibleTiles = state.boardTiles.filter {
            it.state == TileState.NORMAL || it.state == TileState.BLOCKED
        }

        if (visibleTiles.isNotEmpty()) {

            Box(modifier = Modifier.size(width = contentWidth.dp, height = contentHeight.dp)) {
                visibleTiles.forEach { tile ->
                    key(tile.id) {
                        val isHighlighted = state.highlightedTileIds.contains(tile.id)
                        val isFlying = flyingTileIds.contains(tile.id)

                        TileView(
                            tile = tile,
                            onClick = { if (!isFlying) onTileClick(tile) },
                            currentSkin = state.currentSkin,
                            tileSize = tileSize.dp,
                            isShaking = state.shakingTileIds.contains(tile.id),
                            isHighlighted = isHighlighted,
                            modifier = Modifier
                                .offset(
                                    x = ((tile.x - minX) * spacing).dp,
                                    y = ((tile.y - minY) * spacing).dp
                                )
                                .zIndex(tile.z.toFloat())
                                .alpha(if (isFlying) 0f else 1f)
                                .onGloballyPositioned { coords ->
                                    // 记录卡牌位置
                                    tileGlobalPositions[tile.id] = coords.positionInRoot()
                                }
                        )
                    }
                }
            }
        }
    }
}
