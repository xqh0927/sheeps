package com.example.sheeps.game.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.sheeps.data.model.Tile

/**
 * 无尽生存模式消除槽（7 格）。
 *
 * 复制 [GameDock] 中 `MatchingSlot` 的 7 槽布局与滑动动画，但参数化
 * （不依赖 GameViewState），使用 [TileView] 渲染。所有颜色走 MaterialTheme 令牌。
 *
 * @param slotTiles 当前卡槽中的卡牌（最多 7 张）
 * @param currentSkin 当前皮肤
 */
@Composable
fun EndlessDock(
    slotTiles: List<Tile>,
    currentSkin: String,
    flyingTileIds: Set<String> = emptySet(),
    slotGlobalPositions: MutableMap<Int, Offset> = remember { mutableMapOf() }
) {
    val infiniteTransition = rememberInfiniteTransition(label = "endlessSlotBorder")
    val slotBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "endlessSlotBorderAlpha"
    )

    val density = LocalDensity.current
    var containerWidthPx by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = slotBorderAlpha),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
            .onGloballyPositioned { coords -> 
                containerWidthPx = coords.size.width
                val containerRoot = coords.positionInRoot()
                val densityVal = density.density
                if (containerWidthPx > 0) {
                    val padPx = 8f * densityVal
                    val gapPx = 4f * densityVal
                    val rowWidthPx = containerWidthPx - 2f * padPx
                    val slotW = (rowWidthPx - 6 * gapPx) / 7
                    for (i in 0 until 7) {
                        val x = containerRoot.x + padPx + i * (slotW + gapPx)
                        val y = containerRoot.y + padPx
                        slotGlobalPositions[i] = Offset(x, y)
                    }
                }
            }
    ) {
        // 1. 7 个空插槽背景
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 7) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {}
            }
        }

        // 2. 卡槽中的实体卡牌 + 位置滑动动画
        if (containerWidthPx > 0 && slotTiles.isNotEmpty()) {
            val containerWidthDp = with(density) { containerWidthPx.toDp() }
            val itemWidth = (containerWidthDp - 24.dp) / 7

            slotTiles.forEachIndexed { index, tile ->
                val targetX = (itemWidth + 4.dp) * index
                val animatedX by animateDpAsState(
                    targetValue = targetX,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "endless_slot_${tile.id}"
                )

                val isFlying = flyingTileIds.contains(tile.id)

                Box(
                    modifier = Modifier
                        .offset(x = animatedX)
                        .size(itemWidth)
                        .alpha(if (isFlying) 0f else 1f),
                    contentAlignment = Alignment.Center
                ) {
                    TileView(
                        tile = tile,
                        onClick = { },
                        currentSkin = currentSkin,
                        tileSize = itemWidth
                    )
                }
            }
        }
    }
}
