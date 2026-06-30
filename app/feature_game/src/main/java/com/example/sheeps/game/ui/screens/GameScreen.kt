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
import com.example.sheeps.data.model.Tile
import com.example.sheeps.game.state.*
import com.example.sheeps.game.ui.components.*
import com.example.sheeps.game.ui.dialogs.GameResultOverlay
import com.example.sheeps.ui.components.*
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
    onShowLeaderboard: () -> Unit
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
                GameHeader(
                    currentLevelId = state.currentLevelId,
                    onBack = onBack,
                    onRestart = onRestart
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. 顶部状态栏（得分、卡牌数）
                GameStatusBar(state = state)

                // 2. 游戏核心棋盘
                GameBoard(
                    state = state,
                    flyingTileIds = flyingTileIds,
                    tileGlobalPositions = tileGlobalPositions,
                    onTileClick = { tile ->
                        // 处理卡片点击逻辑，触发飞行动画
                        val startPos = tileGlobalPositions[tile.id] ?: Offset.Zero
                        val nextSlotIdx = state.slotTiles.size
                        val endPos = slotGlobalPositions[nextSlotIdx] ?: slotGlobalPositions[0] ?: Offset.Zero
                        
                        val anim = Animatable(0f)
                        val fly = GameFlyingTile(tile.id, tile.type, startPos, endPos, anim)
                        flyingTiles = flyingTiles + fly
                        flyingTileIds = flyingTileIds + tile.id
                        
                        coroutineScope.launch {
                            anim.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
                            flyingTiles = flyingTiles.filter { it.tileId != tile.id }
                            flyingTileIds = flyingTileIds - tile.id
                            onTileClick(tile)
                        }
                    }
                )

                // 3. 底部槽位区（置物架 + 消除槽）
                GameDock(
                    state = state,
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
            screenRootOffset = screenRootOffset,
            density = density
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

        if (state.isLoading) {
            FullScreenLoading()
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
    screenRootOffset: Offset,
    density: androidx.compose.ui.unit.Density
) {
    flyingTiles.forEach { fly ->
        val progress = fly.progress.value
        val x = fly.start.x + (fly.end.x - fly.start.x) * progress - screenRootOffset.x
        val y = fly.start.y + (fly.end.y - fly.start.y) * progress - screenRootOffset.y
        val sizeDp = 52.dp - (12.dp * progress)
        val xDp = with(density) { x.toDp() }
        val yDp = with(density) { y.toDp() }

        Box(
            modifier = Modifier
                .offset(x = xDp, y = yDp)
                .size(sizeDp)
                .zIndex(999f)
        ) {
            TileView(
                tile = Tile(id = "fly_view", type = fly.type, x = 0f, y = 0f, z = 0),
                onClick = {},
                currentSkin = currentSkin,
                tileSize = sizeDp
            )
        }
    }
}

/**
 * 游戏背景装饰（微光效果）
 */
@Composable
private fun GameBackgroundDecoration() {
    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(com.example.sheeps.theme.Crimson_Primary.copy(alpha = 0.12f), Color.Transparent),
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
