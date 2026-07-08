package com.example.sheeps.game.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.core.game.GameEngine
import com.example.sheeps.game.state.*
import com.example.sheeps.game.ui.components.*
import com.example.sheeps.game.ui.dialogs.GameResultOverlay
import com.example.sheeps.game.ui.dialogs.CarrySelectionDialog
import com.example.sheeps.ui.components.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.sheeps.core.R
import kotlinx.coroutines.launch

/**
 * 飞行卡片动画状态记录
 */
private data class GameFlyingTile(
    val tileId: String,
    val type: Int,
    val start: Offset,
    val end: Offset,
    val progress: Animatable<Float, AnimationVector1D>
)

/**
 * 游戏主界面
 * 负责游戏逻辑控制、动画管理以及各功能组件的组合
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameViewState,
    onTileClick: (Tile) -> Unit,
    onUseUndo: () -> Unit,
    onUseMoveOut: () -> Unit,
    onUseShuffle: () -> Unit,
    onUseHint: () -> Unit,
    onUseBomb: () -> Unit,
    onUseJoker: () -> Unit,
    onUseDouble: () -> Unit,
    onRevive: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    onNextLevel: () -> Unit,
    onShowLeaderboard: () -> Unit,
    onUpdateTempCarryItem: (String, Int) -> Unit = { _, _ -> },
    onConfirmRestartWithCarry: () -> Unit = {},
    onDismissCarrySelection: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    
    // 全局位置映射，用于卡片飞行路径计算
    val tileGlobalPositions = remember { mutableStateMapOf<String, Offset>() }
    val slotGlobalPositions = remember { mutableStateMapOf<Int, Offset>() }
    
    // 飞行中的卡片列表
    var flyingTiles by remember { mutableStateOf(emptyList<GameFlyingTile>()) }
    var flyingTileIds by remember { mutableStateOf(emptySet<String>()) }
    
    var screenRootOffset by remember { mutableStateOf(Offset.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    // 炸弹使用时震屏效果状态
    var prevBombCount by remember { mutableStateOf(state.bombCount) }
    val boardShakeOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    LaunchedEffect(state.bombCount) {
        if (state.bombCount < prevBombCount) {
            com.example.sheeps.game.ui.animations.GameAnimations.runBoardShakeAnimation(boardShakeOffset)
        }
        prevBombCount = state.bombCount
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .onGloballyPositioned { coords ->
                screenRootOffset = coords.positionInRoot()
            }
    ) {
        // 背景装饰
        GameBackgroundDecoration()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                SheepsTopAppBar(
                    title = stringResource(id = R.string.prepare_title_unlocked, state.currentLevelId),
                    onBack = onBack,
                    showAction = true,
                    actionIcon = Icons.Default.Refresh,
                    onActionClick = onRestart
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. 顶部状态栏（得分、卡牌数）
                GameStatusBar(state = state)

                // 封印门控进度条（仅封印关卡显示）
                if (state.sealedOrder.isNotEmpty()) {
                    SealedGateProgress(state = state)
                }

                // 2. 游戏核心棋盘：可自适应高度，置物架出现时自动压缩
                GameBoard(
                    state = state,
                    flyingTileIds = flyingTileIds,
                    tileGlobalPositions = tileGlobalPositions,
                    onTileClick = { tile ->
                        // 双层防护：先看 state 标记，再用引擎实时重算（spacing=46，重叠>0.25px即为遮挡）
                        val isBlocked = tile.state == TileState.BLOCKED || 
                                GameEngine.isTileBlocked(tile, state.boardTiles)
                        val isSealed = tile.sealedCount > 0

                        if (isBlocked || isSealed) {
                             onTileClick(tile)
                         } else {
                             // 处理卡片点击逻辑，触发飞行动画
                             val startPos = tileGlobalPositions[tile.id] ?: Offset.Zero
                             // 计算卡牌在卡槽中的实际插入索引位置，使其飞向真正安放的位置，而非一律追加到卡槽最末尾
                             val lastIdx = state.slotTiles.indexOfLast { it.type == tile.type }
                             val targetSlotIdx = if (lastIdx == -1) state.slotTiles.size else (lastIdx + 1)
                             val endPos = slotGlobalPositions[targetSlotIdx] ?: slotGlobalPositions[0] ?: Offset.Zero
                             
                             val anim = Animatable(0f)
                             val fly = GameFlyingTile(tile.id, tile.type, startPos, endPos, anim)
                             flyingTiles = flyingTiles + fly
                             flyingTileIds = flyingTileIds + tile.id
                             
                             onTileClick(tile) // 瞬间更新数据状态，以便下一次点击能正确获取下个卡槽位置且刷新棋盘遮挡状态
                             
                             coroutineScope.launch {
                                 com.example.sheeps.game.ui.animations.GameAnimations.runTileFlyAnimation(anim)
                                 flyingTiles = flyingTiles.filter { it.tileId != tile.id }
                                 flyingTileIds = flyingTileIds - tile.id
                             }
                         }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .offset(
                            x = boardShakeOffset.value.x.dp,
                            y = boardShakeOffset.value.y.dp
                        )
                )

                // 3. 底部槽位区（置物架 + 消除槽）
                GameDock(
                    state = state,
                    flyingTileIds = flyingTileIds,
                    slotGlobalPositions = slotGlobalPositions,
                    onTileClick = onTileClick
                )

                // 4. 道具交互与展示区
                GameTools(
                    state = state,
                    onUseMoveOut = onUseMoveOut,
                    onUseUndo = onUseUndo,
                    onUseShuffle = onUseShuffle,
                    onUseHint = onUseHint,
                    onUseBomb = onUseBomb,
                    onUseJoker = onUseJoker,
                    onUseDouble = onUseDouble
                )

                Spacer(Modifier.height(8.dp))
            }
        }

        // 5. 飞行卡片动画层
        FlyingTilesLayer(
            flyingTiles = flyingTiles,
            currentSkin = state.currentSkin,
            screenRootOffset = screenRootOffset
        )

        // 6. 结果覆盖层（胜利/失败）
        GameResultSection(
            state = state,
            onBack = onBack,
            onRestart = onRestart,
            onRevive = onRevive,
            onNextLevel = onNextLevel,
            onShowLeaderboard = onShowLeaderboard
        )

        if (state.showCarrySelection) {
            CarrySelectionDialog(
                state = state,
                onDismiss = onDismissCarrySelection,
                onConfirm = onConfirmRestartWithCarry,
                onUpdateItem = onUpdateTempCarryItem
            )
        }

        if (state.isLoading) {
            FullScreenLoading()
        }
    }
}

/**
 * 封印门控进度条：显示已消除正常牌数 / 阈值，以及已解锁封印牌数
 */
@Composable
private fun SealedGateProgress(state: GameViewState) {
    val progress = if (state.sealedOrder.isEmpty()) {
        0f
    } else {
        state.sealedUnlockedIds.size.toFloat() / state.sealedOrder.size
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = stringResource(
                    id = R.string.sealed_gate_progress,
                    state.sealedClearCount,
                    state.sealedUnlockThreshold,
                    state.sealedUnlockedIds.size,
                    state.sealedOrder.size
                ),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f)
            )
        }
    }
}

/**
 * 飞行卡片动画层组件
 */
@Composable
private fun FlyingTilesLayer(
    flyingTiles: List<GameFlyingTile>,
    currentSkin: String,
    screenRootOffset: Offset
) {
    flyingTiles.forEach { fly ->
        key(fly.tileId) {
            Box(
                modifier = Modifier
                    .zIndex(999f)
                    .size(48.dp)
                    .graphicsLayer {
                        val progress = fly.progress.value
                        translationX = fly.start.x + (fly.end.x - fly.start.x) * progress - screenRootOffset.x
                        translationY = fly.start.y + (fly.end.y - fly.start.y) * progress - screenRootOffset.y
                        
                        scaleX = 1f
                        scaleY = 1f
                    }
            ) {
                TileView(
                    tile = Tile(id = "fly_view", type = fly.type, x = 0f, y = 0f, z = 0),
                    onClick = {},
                    currentSkin = currentSkin,
                    tileSize = 48.dp
                )
            }
        }
    }
}

/**
 * 游戏背景装饰（微光效果）
 */
@Composable
private fun GameBackgroundDecoration() {
    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor, Color.Transparent),
                center = Offset(this.size.width * 0.5f, 0f),
                radius = this.size.width * 0.7f
            ),
            radius = this.size.width * 0.7f,
            center = Offset(this.size.width * 0.5f, 0f)
        )
    }
}

/**
 * 游戏结果处理区
 */
@Composable
private fun GameResultSection(
    state: GameViewState,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onRevive: () -> Unit,
    onNextLevel: () -> Unit,
    onShowLeaderboard: () -> Unit
) {
    // 胜利覆盖层
    AnimatedVisibility(
        visible = state.gameStatus == GameStatus.WON,
        enter   = fadeIn(tween(400)) + scaleIn(initialScale = 0.85f),
        exit    = fadeOut(tween(300))
    ) {
        GameResultOverlay(
            won = true,
            state = state,
            onBack = onBack,
            onNextLevel = onNextLevel,
            onShowLeaderboard = onShowLeaderboard
        )
    }

    // 失败覆盖层
    AnimatedVisibility(
        visible = state.gameStatus == GameStatus.LOST,
        enter   = fadeIn(tween(400)) + scaleIn(initialScale = 0.85f),
        exit    = fadeOut(tween(300))
    ) {
        GameResultOverlay(
            won       = false,
            state     = state,
            onBack    = onBack,
            onRestart = onRestart,
            onRevive  = onRevive
        )
    }
}
