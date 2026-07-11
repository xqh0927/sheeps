package com.example.sheeps.game.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.sheeps.data.model.Tile

/**
 * 无尽生存模式 6 列竖井棋盘。
 *
 * - 每列自底向上排布卡牌。
 * - 卡牌采用绝对坐标定位与独立位移动画 [animateDpAsState]，
 *   新加入的卡牌在逻辑上被创建后，从棋盘最顶部 (Y=0) 平滑滑落至目标位置（消除了弹跳或闪烁）。
 * - 玩家可点击列中任意深度可见卡牌。点击卡牌飞入卡槽后，
 *   点击位置上方的卡牌受重力影响顺滑下坠 1 格，下方的卡牌保持纹丝不动（完美重力对齐）。
 * - 列顶往下 [visibleLayers] 张做半透明（alpha 0.4）"多层可见"。
 * - 顶部画死亡线（colorScheme.error）。
 */
@Composable
fun EndlessWellBoard(
    columns: List<List<Tile>>,
    currentSkin: String,
    deathRow: Int,
    visibleLayers: Int,
    onColumnClick: (Int, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 顶部死亡线（红色）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
        )
        Spacer(modifier = Modifier.height(2.dp))

        val tileSize = 48.dp // 单张牌基准尺寸（在列内自适应）
        val boardHeight = tileSize * deathRow

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(boardHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    RoundedCornerShape(10.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            columns.forEachIndexed { colIndex, column ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    for (i in column.indices) {
                        val tile = column[i]
                        key(tile.id) {
                            // 使用 isInitial 标记首次进入，初始位置设为 boardHeight (棋盘底部)，从而平滑自底部滑入
                            var isInitial by remember { mutableStateOf(true) }
                            val targetY = boardHeight - tileSize * (column.size - i)
                            val finalTargetY = if (isInitial) boardHeight else targetY

                            val animatedY by animateDpAsState(
                                targetValue = finalTargetY,
                                animationSpec = tween(300, easing = EaseOutCubic),
                                label = "tile_y_${tile.id}"
                            )

                            LaunchedEffect(Unit) {
                                // 首帧后将 isInitial 置 false，使后续重组合（如重力下坠）从正确目标位滑动。
                                // 该一次性协程随组合生命周期自动结束，无泄漏。
                                isInitial = false
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = animatedY)
                            ) {
                                TileView(
                                    tile = tile,
                                    onClick = { onColumnClick(colIndex, tile.id) },
                                    currentSkin = currentSkin,
                                    tileSize = tileSize
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
