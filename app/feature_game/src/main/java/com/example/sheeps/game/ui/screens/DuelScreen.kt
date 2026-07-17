package com.example.sheeps.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.example.sheeps.core.R
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.core.game.GameEngine
import com.example.sheeps.core.game.rememberTileCardBorderColors
import com.example.sheeps.game.state.*
import com.example.sheeps.game.ui.components.*
import com.example.sheeps.game.ui.dialogs.DuelExitDialog
import com.example.sheeps.game.ui.dialogs.DuelResultDialog
import com.example.sheeps.theme.*
import com.example.sheeps.ui.components.*
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sheeps.game.ui.components.DuelGameBoardSurfaceView
import kotlinx.coroutines.launch

/**
 * 飞行卡片动画状态记录 (对决模式)
 */
private data class DuelFlyingTile(
    val tileId: String,
    val type: Int,
    val start: Offset,
    val end: Offset,
    val progress: Animatable<Float, AnimationVector1D>
)

/**
 * 对决模式（Duel）主界面。
 *
 * 负责组合对决头部、施法提示条、含迷雾特效的棋盘、移出置物架、消除槽、
 * 技能释放面板，以及飞行卡牌动画层与结算弹窗。通过回调将用户操作上抛给
 * Activity / ViewModel 处理。
 *
 * 重组注意点：
 * - [state] 每次变化都会触发本函数重组；位置映射 [tileGlobalPositions] /
 *   [slotGlobalPositions] 使用 `remember { mutableStateMapOf(...) }` 跨重组保留，
 *   避免重复测量。
 * - 飞行卡牌列表 [flyingTiles] 由 `remember { mutableStateOf(...) }` 持有，
 *   仅作纯视觉过渡，不参与游戏数据状态。
 *
 * 线程约束：Composable 运行于主线程。飞行动画通过 `rememberCoroutineScope()`
 * 启动，该作用域在组合销毁时自动取消。
 * ⚠️ 内存隐患：必须使用组合作用域协程（而非全局 `CoroutineScope` / `GlobalScope`），
 * 否则卡牌动画协程会脱离界面生命周期而泄漏。当前实现为安全用法。
 *
 * @param state       当前对决视图状态（含棋盘、卡槽、技能、对手分数等）
 * @param onTileClick 点击可用卡牌的回调（被遮挡/封印牌已在此拦截）
 * @param onLeave     主动离开对决的回调
 * @param onRestart   重新开始对决的回调
 * @param onCastSpell 释放技能 Spell 的回调，参数为技能类型字符串
 */
@Composable
fun DuelScreen(
    state: DuelViewState,
    onTileClick: (Tile) -> Unit,
    onLeave: () -> Unit,
    onRestart: () -> Unit,
    onCastSpell: (String) -> Unit
) {
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // 位置记录映射
    val tileGlobalPositions = remember { mutableStateMapOf<String, Offset>() }
    val slotGlobalPositions = remember { mutableStateMapOf<Int, Offset>() }
    
    // 飞行卡片状态
    var flyingTiles by remember { mutableStateOf(emptyList<DuelFlyingTile>()) }
    var flyingTileIds by remember { mutableStateOf(emptySet<String>()) }
    
    var screenRootOffset by remember { mutableStateOf(Offset.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    // 物理返回键拦截
    BackHandler(enabled = true) { showExitConfirmDialog = true }

    // 退出确认弹窗
    if (showExitConfirmDialog) {
        DuelExitDialog(
            onConfirm = { showExitConfirmDialog = false; onLeave() },
            onDismiss = { showExitConfirmDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)))
            .onGloballyPositioned { screenRootOffset = it.positionInRoot() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 顶部：对决信息与进度
            DuelHeader(state = state, onLeave = { showExitConfirmDialog = true })
            
            // 2. 攻击/施法提示条
            SpellMessageBar(state = state)

            Spacer(Modifier.height(16.dp))

            // 3. 游戏核心棋盘（包含迷雾特效自绘）
            val (borderColor, decorColor) = rememberTileCardBorderColors(state.currentSkin)
            AndroidView(
                factory = { ctx ->
                    DuelGameBoardSurfaceView(ctx)
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
                                val fly = DuelFlyingTile(tile.id, tile.type, startPos, endPos, anim)
                                
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
            )

            Spacer(Modifier.height(16.dp))
            
            // 4. 置物架区域
            DuelMovedOutTray(state = state, onTileClick = onTileClick)
            
            Spacer(Modifier.height(8.dp))

            // 5. 消除槽区域
            DuelMatchingSlot(state = state, flyingTileIds = flyingTileIds, slotGlobalPositions = slotGlobalPositions)

            Spacer(Modifier.height(12.dp))

            // 6. 技能释放面板
            DuelSpellPanel(state = state, onCastSpell = onCastSpell)
        }

        // 7. 结算弹窗
        DuelResultDialog(state = state, onLeave = onLeave)

        // 8. 全屏加载遮罩
        if (state.isLoading) {
            FullScreenLoadingOverlay()
        }

        // 9. 飞行卡片动画层
        DuelFlyingTilesLayer(
            flyingTiles = flyingTiles,
            screenRootOffset = screenRootOffset
        )
    }
}

/**
 * 施法/受击消息提示条。
 *
 * 监听 [state.activeSpellMessage]（己方施法）与 [state.incomingAttackMessage]（对手攻击），
 * 有提示文案时借助 `AnimatedVisibility` 以淡入 + 展开动画呈现于棋盘上方。
 *
 * 线程约束：纯 Composable，运行于主线程；`AnimatedVisibility` 的进出场动画由
 * Compose 动画框架在内部协程中驱动，随组件销毁自动取消，无泄漏风险。
 *
 * @param state 当前对决视图状态，提供主动施法提示与来自对手的受击提示
 */
@Composable
private fun SpellMessageBar(state: DuelViewState) {
    val overlayMsg = state.activeSpellMessage ?: state.incomingAttackMessage
    AnimatedVisibility(
        visible = overlayMsg != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        overlayMsg?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = msg, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * 对决模式移出置物架。
 *
 * 当 [state.movedOutTiles] 非空时，横向排列展示被移出棋盘的卡牌，点击可触发
 * [onTileClick] 将其放回卡槽。空列表时直接 `return` 不渲染，避免无意义重组。
 *
 * 线程约束：纯 Composable，运行于主线程；`key(tile.id)` 保证列表项稳定复用。
 *
 * @param state       当前对决视图状态，提供移出卡牌集合
 * @param onTileClick 点击移出卡牌的回调
 */
@Composable
private fun DuelMovedOutTray(state: DuelViewState, onTileClick: (Tile) -> Unit) {
    if (state.movedOutTiles.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp)
    ) {
        state.movedOutTiles.forEach { tile ->
            key(tile.id) {
                TileView(tile = tile, onClick = { onTileClick(tile) }, currentSkin = "shuang", tileSize = 48.dp)
            }
        }
    }
}

/**
 * 对决模式消除槽位（支持缩减槽位诅咒）。
 *
 * 渲染 7 个插槽背景（当 [DuelViewState.maxSlotSize] == 6 时第 7 个插槽锁定并显示 🔒），
 * 并以 `animateDpAsState` + spring 动画驱动已放置卡牌的横向滑动定位。
 * 通过 [slotGlobalPositions] 将每个插槽的全局坐标回填，供飞行卡牌计算落点。
 *
 * ⚠️ 内存/生命周期提示：[slotGlobalPositions] 为外部传入的可变 Map，由
 * `onGloballyPositioned` 在布局阶段写入；该回调绑定于布局测量，随组件销毁
 * 释放，不会长期持有引用。注意不要在回调中捕获 Activity/Context，否则会造成泄漏。
 *
 * 线程约束：纯 Composable，运行于主线程；坐标换算 `toDp()` 借助 `LocalDensity`。
 *
 * @param state              当前对决视图状态，提供卡槽卡牌与最大槽位数
 * @param flyingTileIds      正在飞行中的卡牌 id 集合（飞行时隐藏原槽位卡牌）
 * @param slotGlobalPositions 插槽全局坐标映射（会被本函数回填写入）
 */
@Composable
private fun DuelMatchingSlot(
    state: DuelViewState,
    flyingTileIds: Set<String>,
    slotGlobalPositions: MutableMap<Int, Offset>
) {
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableStateOf(0) }
    val containerRoot = remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp)
            .onGloballyPositioned { coords ->
                // 仅在此根容器测量一次，捕获容器全局坐标与宽度（px），
                // 再按固定布局（padding 8dp + 7 等分槽位 + 4dp 间隔）推导 7 个槽位全局坐标，
                // 取代此前给每个槽位挂 onGloballyPositioned（7 次回调）。
                containerRoot.value = coords.positionInRoot()
                containerWidthPx = coords.size.width
                computeSlotGlobalPositions(
                    containerRoot = containerRoot.value,
                    density = density.density,
                    containerWidthPx = containerWidthPx,
                    slotCount = 7,
                    slotGlobalPositions = slotGlobalPositions
                )
            }
    ) {
        // 1. 绘制 7 个空插槽背景（不再挂 onGloballyPositioned）
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 7) {
                val isLocked = i == 6 && state.maxSlotSize == 6
                Box(
                    modifier = Modifier
                        .weight(1f).aspectRatio(1f)
                        .background(if (isLocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(width = 1.dp, color = if (isLocked) MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLocked) {
                        Text("🔒", fontSize = 16.sp)
                    }
                }
            }
        }

        // 2. 绘制卡槽中的实体卡牌，并添加位置滑动动画
        if (containerWidthPx > 0 && state.slotTiles.isNotEmpty()) {
            val containerWidthDp = with(density) { containerWidthPx.toDp() }
            val itemWidth = (containerWidthDp - 24.dp) / 7

            state.slotTiles.forEachIndexed { index, tile ->
                val targetX = index * (itemWidth + 4.dp)
                val animatedX by animateDpAsState(
                    targetValue = targetX,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "duel_slot_tile_move_${tile.id}"
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
                        onClick = {},
                        currentSkin = "shuang",
                        tileSize = itemWidth
                    )
                }
            }
        }
    }
}

/**
 * 加载全屏遮罩。
 *
 * 以半透明黑色覆盖全屏并居中显示 `SheepsLoading`，并通过 `pointerInput` 吞掉
 * 所有指针事件、配合 `clickable(enabled = false)` 阻断底层交互，防止加载期间误触。
 *
 * 线程约束：纯 Composable，运行于主线程；`awaitEachGesture` 在内部挂起协程中
 * 消费指针事件，随组件销毁自动取消。
 */
@Composable
private fun FullScreenLoadingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))
            .pointerInput(Unit) { awaitEachGesture { while (true) { awaitPointerEvent().changes.forEach { it.consume() } } } }
            .clickable(enabled = false, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        SheepsLoading(size = 56.dp)
    }
}

/**
 * 飞行卡片渲染层（对决模式）。
 *
 * 遍历 [flyingTiles]，为每张飞行卡牌在 `graphicsLayer` 中根据 [DuelFlyingTile.progress]
 * （0→1）对 [DuelFlyingTile.start]→[DuelFlyingTile.end] 做线性插值位移，并以
 * [screenRootOffset] 将坐标折算回屏幕根坐标系。
 *
 * 重组注意点：动画通过 `Animatable` 驱动，进度变化会以高帧率触发本层重组；
 * 仅绘制飞行卡牌（占位 `Tile(id="fly_view")`），不参与游戏逻辑。
 *
 * 线程约束：纯 Composable，运行于主线程；位移计算在每一动画帧的主线程完成。
 *
 * @param flyingTiles      当前正在飞行的卡牌状态集合
 * @param screenRootOffset 屏幕根布局的全局偏移，用于坐标折算
 */
@Composable
private fun DuelFlyingTilesLayer(
    flyingTiles: List<DuelFlyingTile>,
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
                TileView(tile = Tile(id = "fly_view", type = fly.type, x = 0f, y = 0f, z = 0), onClick = {}, currentSkin = "shuang", tileSize = 48.dp)
            }
        }
    }
}

/**
 * 依据消除槽容器根坐标与宽度（px）推导各槽位全局坐标（px）。
 *
 * 布局：容器 padding 8dp，内部 Row 为 7 等分（weight(1f)）加 4dp 间隔（共 6 个间隔）。
 * 与原始逐槽 [androidx.compose.ui.layout.onGloballyPositioned] 写入的槽位左上角坐标一致，飞行动画落点不变。
 * `density` 用于把 dp 间距换算为 px，与 [androidx.compose.ui.layout.positionInRoot] 的 px 坐标系对齐。
 */
private fun computeSlotGlobalPositions(
    containerRoot: Offset,
    density: Float,
    containerWidthPx: Int,
    slotCount: Int,
    slotGlobalPositions: MutableMap<Int, Offset>
) {
    if (containerWidthPx <= 0) return
    val padPx = 8f * density
    val gapPx = 4f * density
    val rowWidthPx = containerWidthPx - 2f * padPx
    val slotW = (rowWidthPx - (slotCount - 1) * gapPx) / slotCount
    for (i in 0 until slotCount) {
        val x = containerRoot.x + padPx + i * (slotW + gapPx)
        val y = containerRoot.y + padPx
        slotGlobalPositions[i] = Offset(x, y)
    }
}
