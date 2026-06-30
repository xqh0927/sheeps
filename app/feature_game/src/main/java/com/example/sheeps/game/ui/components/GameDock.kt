package com.example.sheeps.game.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.example.sheeps.data.model.Tile
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.theme.Crimson_Primary
import com.example.sheeps.theme.Gold_Primary
import com.example.sheeps.theme.Gold_Subtle

/**
 * 游戏底部托盘/消除槽组件
 * 包含：置物架（移出功能）、消除槽（Slot）
 * 
 * @param state 游戏界面状态
 * @param slotGlobalPositions 用于记录槽位全局位置，供动画使用
 * @param onTileClick 置物架中卡牌点击回调
 */
@Composable
fun GameDock(
    state: GameViewState,
    slotGlobalPositions: MutableMap<Int, Offset>,
    onTileClick: (Tile) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- 这里的逻辑负责展示置物架 ---
        if (state.movedOutTiles.isNotEmpty()) {
            MovedOutTray(
                tiles = state.movedOutTiles,
                currentSkin = state.currentSkin,
                onTileClick = onTileClick
            )
        }
        // ------------------------------

        // 消除槽
        MatchingSlot(
            state = state,
            slotGlobalPositions = slotGlobalPositions
        )
    }
}

/**
 * 移出置物架组件
 */
@Composable
private fun MovedOutTray(
    tiles: List<Tile>,
    currentSkin: String,
    onTileClick: (Tile) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text  = "— 置物架 —",
            style = MaterialTheme.typography.labelSmall,
            color = Gold_Primary.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, Gold_Subtle.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            tiles.forEach { tile ->
                TileView(
                    tile    = tile,
                    onClick = { onTileClick(tile) },
                    currentSkin = currentSkin,
                    tileSize = 46.dp
                )
            }
        }
    }
}

/**
 * 消除槽（七格槽位）组件
 */
@Composable
private fun MatchingSlot(
    state: GameViewState,
    slotGlobalPositions: MutableMap<Int, Offset>
) {
    // 呼吸边框效果
    val infiniteTransition = rememberInfiniteTransition(label = "slotBorder")
    val slotBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "slotBorderAlpha"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = 1.dp,
                color = Crimson_Primary.copy(alpha = slotBorderAlpha),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 7) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .onGloballyPositioned { coords ->
                        slotGlobalPositions[i] = coords.positionInRoot()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (i < state.slotTiles.size) {
                    TileView(
                        tile    = state.slotTiles[i],
                        onClick = {},
                        currentSkin = state.currentSkin,
                        tileSize = 40.dp
                    )
                }
            }
        }
    }
}
