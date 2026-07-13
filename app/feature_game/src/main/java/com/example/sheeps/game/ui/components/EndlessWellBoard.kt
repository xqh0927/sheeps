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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.example.sheeps.data.model.Tile

/**
 * 无尽生存模式 6 列竖井棋盘。
 *
 * - 每列自底向上排布卡牌。
 * - 卡牌采用绝对坐标定位与独立位移动画 [animateDpAsState]，
 *   新加入的卡牌在逻辑上被创建后，从棋盘底部平滑上升（自底向上堆叠）至目标位置（消除弹跳或闪烁）。
 * - 玩家可点击列中任意深度可见卡牌。点击卡牌飞入卡槽后，
 *   点击位置上方的卡牌受重力影响顺滑下坠 1 格，下方的卡牌保持纹丝不动（完美重力对齐）。
 * - 列顶往下 [visibleLayers] 张做半透明（alpha 0.4）"多层可见"。
 * - **统一间隙**：卡牌四周（上/下/左/右）间隙恒为 [GAP]，棋盘内边距亦为 [GAP]，
 *   保证视觉上所有方向留白完全一致。列宽固定为牌宽（消除横向多余空隙），棋盘宽度随内容自适应并居中。
 */
@Composable
fun EndlessWellBoard(
    columns: List<List<Tile>>,
    currentSkin: String,
    deathRow: Int,
    visibleLayers: Int,
    onColumnClick: (Int, String) -> Unit
) {
    // 统一间隙：卡牌四周与棋盘内边距保持一致，避免「左右有缝、上下贴死」的不一致
    val gap = 4.dp
    val pad = gap // 棋盘内边距（四边一致）
    val topReserve = gap // 顶部留白：最顶排卡牌与棋盘上沿保持 gap
    val bottomReserve = gap // 底部留白：最底排卡牌与棋盘下沿保持 gap

    val tileSize = 48.dp // 单张牌尺寸（固定）
    val verticalStep = tileSize + gap // 纵向步进 = 牌高 + 间隙（上下间隙 = gap）

    // 缓存无尽模式的列高和总高计算，防止重绘时频繁重新计算 Dp
    val sizes = remember(deathRow) {
        val stackHeight = deathRow * tileSize + (deathRow - 1) * gap
        val columnHeight = topReserve + stackHeight + bottomReserve
        val rowHeight = columnHeight + pad * 2
        WellBoardSizes(
            columnHeight = columnHeight,
            rowHeight = rowHeight
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .height(sizes.rowHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    RoundedCornerShape(10.dp)
                )
                .padding(pad),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.Bottom
        ) {
            columns.forEachIndexed { colIndex, column ->
                Box(
                    modifier = Modifier
                        .width(tileSize) // 列宽 = 牌宽，消除横向额外空隙
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    for (i in column.indices) {
                        val tile = column[i]
                        key(tile.id) {
                            // isInitial：首帧从列底滑入，之后以目标位做重力动画
                            var isInitial by remember { mutableStateOf(true) }
                            // p = 距底第几张（0 为最底）。目标顶部 y = sizes.columnHeight - bottomReserve - tileSize - p*verticalStep
                            val p = column.size - 1 - i
                            val targetY = sizes.columnHeight - bottomReserve - tileSize - p * verticalStep
                            val finalTargetY = if (isInitial) sizes.columnHeight else targetY

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

/** 封装生存模式棋盘列高和总高的局部实体类 */
private data class WellBoardSizes(
    val columnHeight: androidx.compose.ui.unit.Dp,
    val rowHeight: androidx.compose.ui.unit.Dp
)
