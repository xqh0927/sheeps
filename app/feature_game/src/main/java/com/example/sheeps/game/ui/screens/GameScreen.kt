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
import com.example.sheeps.core.game.rememberTileCardBorderColors
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sheeps.game.ui.components.GameBoardSurfaceView
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
 * 单机游戏主界面（关卡模式）。
 *
 * 负责组合顶部状态栏、封印门控进度条、核心棋盘、底部槽位区、道具交互区，
 * 并管理卡牌飞行动画层、结算覆盖层、携带道具选择弹窗与全屏加载遮罩。
 * 所有用户操作通过回调上抛给 [GameActivity] / [com.example.sheeps.game.viewmodel.GameViewModel]。
 *
 * 重组注意点：
 * - [state] 变化即触发重组；[tileGlobalPositions]/[slotGlobalPositions] 用
 *   `remember { mutableStateMapOf(...) }` 跨重组保留，[flyingTiles]/[flyingTileIds]
 *   用 `remember { mutableStateOf(...) }` 持有，仅用于视觉过渡。
 * - 震屏效果由 [androidx.compose.animation.core.Animatable]（[boardShakeOffset]）
 *   驱动，[prevBombCount] 用于对比炸弹数变化以触发一次动画。
 *
 * 线程约束：Composable 运行于主线程。飞行动画通过 `rememberCoroutineScope()`
 * 启动，组合销毁时自动取消。
 * ⚠️ 内存隐患：务必使用组合作用域协程（非全局 `CoroutineScope`/`GlobalScope`），
 * 否则卡牌/震屏动画协程会脱离界面生命周期而泄漏。当前实现为安全用法。
 *
 * @param state                     当前游戏视图状态
 * @param onTileClick              点击可用卡牌的回调（被遮挡/封印牌已拦截）
 * @param onUseUndo               使用撤销道具
 * @param onUseMoveOut            使用移出道具
 * @param onUseShuffle            使用洗牌道具
 * @param onUseHint               使用提示道具
 * @param onUseBomb              使用炸弹道具（触发震屏）
 * @param onUseJoker             使用百搭道具
 * @param onUseDouble            使用双倍得分道具
 * @param onRevive               复活
 * @param onRestart              重新开始本关
 * @param onBack                 返回首页
 * @param onNextLevel            进入下一关
 * @param onShowLeaderboard      打开排行榜
 * @param onUpdateTempCarryItem  更新临时携带道具数量（默认空实现）
 * @param onConfirmRestartWithCarry 确认携带道具重开（默认空实现）
 * @param onDismissCarrySelection  关闭携带选择弹窗（默认空实现）
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

    // 炸弹使用时震屏效果状态
    var prevBombCount by remember { mutableIntStateOf(state.bombCount) }
    val boardShakeOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    // ⚠️ 内存/生命周期：下方 LaunchedEffect 仅在 state.bombCount 变化时触发一次，
    // 内部运行短动画 runBoardShakeAnimation，Compose 会在作用域结束时自动取消，无泄漏风险。
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
                val (borderColor, decorColor) = rememberTileCardBorderColors(state.currentSkin)
                AndroidView(
                    factory = { ctx ->
                        GameBoardSurfaceView(ctx)
                    },
                    update = { view ->
                        view.setSkinColors(borderColor, decorColor)
                        view.updateData(
                            state = state,
                            flyingTileIds = flyingTileIds,
                            onTileClick = { tile ->
                                val isBlocked = tile.state == TileState.BLOCKED || 
                                        GameEngine.isTileBlocked(tile, state.boardTiles)
                                val isSealed = tile.sealedCount > 0

                                if (isBlocked || isSealed) {
                                     onTileClick(tile)
                                 } else {
                                     val startPos = tileGlobalPositions[tile.id] ?: Offset.Zero
                                     val lastIdx = state.slotTiles.indexOfLast { it.type == tile.type }
                                     val targetSlotIdx = if (lastIdx == -1) state.slotTiles.size else (lastIdx + 1)
                                     val endPos = slotGlobalPositions[targetSlotIdx] ?: slotGlobalPositions[0] ?: Offset.Zero
                                     
                                     val anim = Animatable(0f)
                                     val fly = GameFlyingTile(tile.id, tile.type, startPos, endPos, anim)
                                     flyingTiles = flyingTiles + fly
                                     flyingTileIds = flyingTileIds + tile.id
                                     
                                     onTileClick(tile)

                                     coroutineScope.launch {
                                         com.example.sheeps.game.ui.animations.GameAnimations.runTileFlyAnimation(anim)
                                         flyingTiles = flyingTiles.filter { it.tileId != tile.id }
                                         flyingTileIds = flyingTileIds - tile.id
                                     }
                                 }
                            },
                            tileGlobalPositions = tileGlobalPositions
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
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
 * 封印门控进度条：显示已消除正常牌数 / 阈值，以及已解锁封印牌数。
 *
 * 仅当 [GameViewState.sealedOrder] 非空（封印关卡）时由调用方渲染。以
 * `LinearProgressIndicator` 展示已解锁封印牌占比
 * （[GameViewState.sealedUnlockedIds].size / [GameViewState.sealedOrder].size）。
 *
 * 线程约束：纯 Composable，运行于主线程；读取 state 直接驱动 UI，无需额外线程切换。
 *
 * @param state 当前游戏视图状态，提供封印门控相关数据
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
 * 飞行卡片动画层组件。
 *
 * 遍历 [flyingTiles]，在 `graphicsLayer` 中根据 [GameFlyingTile.progress]（0→1）
 * 对 [GameFlyingTile.start]→[GameFlyingTile.end] 做线性插值位移，并以
 * [screenRootOffset] 折算回屏幕根坐标系；卡牌皮肤由 [currentSkin] 决定。
 *
 * 重组注意点：动画由 `Animatable` 驱动，进度变化以高帧率触发本层重组；
 * 渲染的 `Tile(id="fly_view")` 为占位卡片，不参与游戏逻辑。
 *
 * 线程约束：纯 Composable，运行于主线程；位移计算在每一动画帧的主线程完成。
 *
 * @param flyingTiles      当前正在飞行的卡牌状态集合
 * @param currentSkin     当前卡牌皮肤标识
 * @param screenRootOffset 屏幕根布局的全局偏移，用于坐标折算
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
 * 游戏背景装饰（微光效果）。
 *
 * 在顶部绘制一个以主题主色径向渐变形成的柔光圆，纯静态装饰，不依赖动画或状态。
 *
 * 线程约束：纯 Composable，运行于主线程；`Canvas` 绘制在布局/绘制阶段的主线程完成。
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
 * 游戏结果处理区。
 *
 * 依据 [state.gameStatus] 以 `AnimatedVisibility` 分别呈现胜利覆盖层
 * （[GameStatus.WON]，提供下一关/排行榜）与失败覆盖层（[GameStatus.LOST]，
 * 提供复活/重开/返回）。两个覆盖层互斥，仅其一可见。
 *
 * ⚠️ 内存/生命周期：`AnimatedVisibility` 的进出场动画由 Compose 内部协程驱动，
 * 随组件销毁自动取消，无泄漏风险。注意回调（[onBack]/[onRestart]/[onRevive] 等）
 * 由上层 Activity 提供，切勿在此捕获 Activity/Context 引用。
 *
 * 线程约束：纯 Composable，运行于主线程。
 *
 * @param state             当前游戏视图状态，决定胜/负覆盖层显隐
 * @param onBack           返回首页
 * @param onRestart        重新开始本关
 * @param onRevive         复活
 * @param onNextLevel      进入下一关
 * @param onShowLeaderboard 打开排行榜
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
