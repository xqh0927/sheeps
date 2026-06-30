package com.example.sheeps.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
    onTileClick: (Tile) -> Unit
) {
    // 棋盘固定尺寸
    val boardFixedWidth = 340.dp
    val boardFixedHeight = 400.dp

    Box(
        modifier = Modifier
            .size(width = boardFixedWidth, height = boardFixedHeight)
            .padding(vertical = 8.dp)
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
            val minX = visibleTiles.minOf { it.x }
            val maxX = visibleTiles.maxOf { it.x }
            val minY = visibleTiles.minOf { it.y }
            val maxY = visibleTiles.maxOf { it.y }

            val tileSize = 52
            val spacing = 46
            // 计算棋盘内部所有卡片占据的实际区域大小
            val contentWidth = (maxX - minX) * spacing + tileSize
            val contentHeight = (maxY - minY) * spacing + tileSize

            Box(modifier = Modifier.size(width = contentWidth.dp, height = contentHeight.dp)) {
                visibleTiles.forEach { tile ->
                    val isHighlighted = state.highlightedTileIds.contains(tile.id)
                    val isFlying = flyingTileIds.contains(tile.id)

                    TileView(
                        tile = tile,
                        onClick = { if (!isFlying) onTileClick(tile) },
                        currentSkin = state.currentSkin,
                        tileSize = tileSize.dp,
                        isShaking = state.shakingTileIds.contains(tile.id),
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
                            .then(
                                if (isHighlighted) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = Gold_Primary,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                } else Modifier
                            )
                    )
                }
            }
        }
    }
}
