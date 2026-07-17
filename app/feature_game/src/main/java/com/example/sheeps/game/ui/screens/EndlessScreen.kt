package com.example.sheeps.game.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.sheeps.core.R
import com.example.sheeps.data.model.Tile
import com.example.sheeps.game.state.EndlessStatus
import com.example.sheeps.game.state.EndlessViewIntent
import com.example.sheeps.game.state.EndlessViewState
import com.example.sheeps.game.ui.components.EndlessBoardSurfaceView
import com.example.sheeps.game.ui.components.EndlessDock
import com.example.sheeps.game.ui.components.EndlessHud
import com.example.sheeps.game.ui.components.EndlessResultDialog
import com.example.sheeps.game.ui.components.TileView
import com.example.sheeps.core.game.rememberTileCardBorderColors
import com.example.sheeps.ui.components.SheepsTopAppBar
import kotlinx.coroutines.launch

private data class WellBoardSizes(
    val rowHeight: androidx.compose.ui.unit.Dp
)

private data class EndlessFlyingTile(
    val tileId: String,
    val type: Int,
    val start: Offset,
    val end: Offset,
    val progress: Animatable<Float, AnimationVector1D>
)

/**
 * 无尽生存模式主界面。
 * 组装 HUD（[com.example.sheeps.game.ui.components.EndlessHud]）+ 6 列棋盘
 * （[com.example.sheeps.game.ui.components.EndlessDock]）；当 [EndlessViewState.showResult]
 * 为 true 时弹出结算对话框（[com.example.sheeps.game.ui.components.EndlessResultDialog]）。
 */
@Composable
fun EndlessScreen(
    state: EndlessViewState,
    onIntent: (EndlessViewIntent) -> Unit
) {
    val actionIcon =
        if (state.status == EndlessStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause
    val coroutineScope = rememberCoroutineScope()

    // 位置记录映射与飞行动画状态
    val tileGlobalPositions = remember { mutableStateMapOf<String, Offset>() }
    val slotGlobalPositions = remember { mutableStateMapOf<Int, Offset>() }

    var flyingTiles by remember { mutableStateOf(emptyList<EndlessFlyingTile>()) }
    var flyingTileIds by remember { mutableStateOf(emptySet<String>()) }

    var screenRootOffset by remember { mutableStateOf(Offset.Zero) }

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val gapVal = 4
    val tileSizeVal = remember(screenWidth) {
        val calculated = (screenWidth * 0.9f - 28) / 6f
        calculated.coerceIn(40f, 56f) // 限制在 40dp ~ 56dp 之间以保证显示合适
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { screenRootOffset = it.positionInRoot() }
    ) {
        Scaffold(
            topBar = {
                SheepsTopAppBar(
                    title = stringResource(id = R.string.home_endless_title),
                    onBack = { onIntent(EndlessViewIntent.Leave) },
                    showAction = true,
                    actionIcon = actionIcon,
                    onActionClick = { onIntent(EndlessViewIntent.Pause) }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EndlessHud(
                    score = state.score,
                    combo = state.combo,
                    wave = state.wave,
                    bestScore = state.bestScore,
                    isFrozen = state.isFrozen,
                    freezeCount = state.freezeCount,
                    onFreeze = { onIntent(EndlessViewIntent.UseFreeze) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                val sizes = remember(state.deathRow, tileSizeVal) {
                    val stackHeightVal =
                        state.deathRow * tileSizeVal + (state.deathRow - 1) * gapVal
                    val columnHeightVal = gapVal + stackHeightVal + gapVal
                    val rowHeight = (columnHeightVal + gapVal * 2).dp
                    WellBoardSizes(rowHeight = rowHeight)
                }

                Box(
                    modifier = Modifier
                        .height(sizes.rowHeight)
                        .fillMaxWidth(0.95f)
                        .padding(gapVal.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val (borderColor, decorColor) = rememberTileCardBorderColors(state.currentSkin)
                    AndroidView(
                        factory = { context ->
                            EndlessBoardSurfaceView(context)
                        },
                        update = { view ->
                            view.setSkinColors(borderColor, decorColor)
                            val densityVal = view.context.resources.displayMetrics.density
                            val tileSizePx = tileSizeVal * densityVal
                            view.updateData(
                                columns = state.columns,
                                currentSkin = state.currentSkin,
                                deathRow = state.deathRow,
                                visibleLayers = state.visibleLayers,
                                tileSizePx = tileSizePx,
                                flyingTileIds = flyingTileIds,
                                tileGlobalPositions = tileGlobalPositions,
                                onColumnClick = { col, tileId ->
                                    val column = state.columns.getOrNull(col)
                                    val tile = column?.find { it.id == tileId }
                                    if (tile != null) {
                                        val startPos = tileGlobalPositions[tile.id] ?: Offset.Zero
                                        val lastIdx =
                                            state.slotTiles.indexOfLast { it.type == tile.type }
                                        val targetSlotIdx =
                                            if (lastIdx == -1) state.slotTiles.size else (lastIdx + 1)
                                        val endPos = slotGlobalPositions[targetSlotIdx]
                                            ?: slotGlobalPositions[0] ?: Offset.Zero

                                        val anim = Animatable(0f)
                                        val fly = EndlessFlyingTile(
                                            tile.id,
                                            tile.type,
                                            startPos,
                                            endPos,
                                            anim
                                        )

                                        flyingTiles = flyingTiles + fly
                                        flyingTileIds = flyingTileIds + tile.id

                                        onIntent(EndlessViewIntent.ClickColumn(col, tileId))

                                        coroutineScope.launch {
                                            com.example.sheeps.game.ui.animations.GameAnimations.runTileFlyAnimation(
                                                anim
                                            )
                                            flyingTiles =
                                                flyingTiles.filter { it.tileId != tile.id }
                                            flyingTileIds = flyingTileIds - tile.id
                                        }
                                    } else {
                                        onIntent(EndlessViewIntent.ClickColumn(col, tileId))
                                    }
                                }
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                EndlessDock(
                    slotTiles = state.slotTiles,
                    currentSkin = state.currentSkin,
                    flyingTileIds = flyingTileIds,
                    slotGlobalPositions = slotGlobalPositions
                )
            }
        }

        // 飞行卡片动画层
        EndlessFlyingTilesLayer(
            flyingTiles = flyingTiles,
            currentSkin = state.currentSkin,
            screenRootOffset = screenRootOffset,
            tileSize = tileSizeVal.dp
        )

        if (state.showResult) {
            EndlessResultDialog(
                score = state.score,
                bestScore = state.bestScore,
                deathReason = state.deathReason,
                isNewBest = state.score > 0 && state.score >= state.bestScore,
                onRestart = { onIntent(EndlessViewIntent.Restart) },
                onLeave = { onIntent(EndlessViewIntent.Leave) }
            )
        }
    }
}

/**
 * 飞行卡片渲染层（生存模式）。
 */
@Composable
private fun EndlessFlyingTilesLayer(
    flyingTiles: List<EndlessFlyingTile>,
    currentSkin: String,
    screenRootOffset: Offset,
    tileSize: Dp = 48.dp
) {
    flyingTiles.forEach { fly ->
        key(fly.tileId) {
            Box(
                modifier = Modifier
                    .zIndex(999f)
                    .size(tileSize)
                    .graphicsLayer {
                        val progress = fly.progress.value
                        translationX =
                            fly.start.x + (fly.end.x - fly.start.x) * progress - screenRootOffset.x
                        translationY =
                            fly.start.y + (fly.end.y - fly.start.y) * progress - screenRootOffset.y
                    }
            ) {
                TileView(
                    tile = Tile(id = "fly_view", type = fly.type, x = 0f, y = 0f, z = 0),
                    onClick = {},
                    currentSkin = currentSkin,
                    tileSize = tileSize
                )
            }
        }
    }
}
