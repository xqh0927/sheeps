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
import com.example.sheeps.game.state.*
import com.example.sheeps.game.ui.components.*
import com.example.sheeps.game.ui.dialogs.DuelResultDialog
import com.example.sheeps.theme.*
import com.example.sheeps.ui.components.*
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
 * 对决模式主界面
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
        ExitConfirmDialog(
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

            // 3. 游戏核心棋盘（包含迷雾特效）
            DuelGameBoard(
                state = state,
                flyingTileIds = flyingTileIds,
                tileGlobalPositions = tileGlobalPositions,
                onTileClick = { tile ->
                    val isBlocked = tile.state == TileState.BLOCKED || 
                            GameEngine.isTileBlocked(tile, state.boardTiles)
                    val isSealed = tile.sealedCount > 0

                    if (isBlocked || isSealed) {
                        onTileClick(tile)
                    } else {
                        // 飞行动画触发逻辑
                        val startPos = tileGlobalPositions[tile.id] ?: Offset.Zero
                        // 计算卡牌在卡槽中的实际插入索引位置，使其飞向真正安放的位置，而非一律追加到卡槽最末尾
                        val lastIdx = state.slotTiles.indexOfLast { it.type == tile.type }
                        val targetSlotIdx = if (lastIdx == -1) state.slotTiles.size else (lastIdx + 1)
                        val endPos = slotGlobalPositions[targetSlotIdx] ?: slotGlobalPositions[0] ?: Offset.Zero
                        val anim = Animatable(0f)
                        val fly = DuelFlyingTile(tile.id, tile.type, startPos, endPos, anim)
                        
                        flyingTiles = flyingTiles + fly
                        flyingTileIds = flyingTileIds + tile.id
                        
                        onTileClick(tile) // 瞬间更新数据状态，以便下一次点击能正确获取下个卡槽位置且刷新棋盘遮挡状态
                        
                        coroutineScope.launch {
                            com.example.sheeps.game.ui.animations.GameAnimations.runTileFlyAnimation(anim)
                            flyingTiles = flyingTiles.filter { it.tileId != tile.id }
                            flyingTileIds = flyingTileIds - tile.id
                        }
                    }
                }
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
 * 退出确认对话框
 */
@Composable
private fun ExitConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.dialog_duel_exit_title), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(id = R.string.dialog_duel_exit_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(id = R.string.dialog_duel_exit_confirm), color = Crimson_Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_duel_exit_cancel))
            }
        }
    )
}

/**
 * 施法/受击消息提示条
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
                    .background(Crimson_Primary.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = msg, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * 对决模式移出置物架
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
                TileView(tile = tile, onClick = { onTileClick(tile) }, currentSkin = "classic", tileSize = 48.dp)
            }
        }
    }
}

/**
 * 对决模式消除槽位（支持缩减槽位诅咒）
 */
@Composable
private fun DuelMatchingSlot(
    state: DuelViewState,
    flyingTileIds: Set<String>,
    slotGlobalPositions: MutableMap<Int, Offset>
) {
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp)
            .onGloballyPositioned { coords ->
                containerWidthPx = coords.size.width
            }
    ) {
        // 1. 绘制 7 个空插槽背景
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
                        .background(if (isLocked) Crimson_Primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(width = 1.dp, color = if (isLocked) Crimson_Primary else Color.Transparent, shape = RoundedCornerShape(8.dp))
                        .onGloballyPositioned { slotGlobalPositions[i] = it.positionInRoot() },
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
                        currentSkin = "classic",
                        tileSize = 48.dp
                    )
                }
            }
        }
    }
}

/**
 * 加载全屏遮罩
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
 * 飞行卡片渲染层 (对决模式)
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
                TileView(tile = Tile(id = "fly_view", type = fly.type, x = 0f, y = 0f, z = 0), onClick = {}, currentSkin = "classic", tileSize = 48.dp)
            }
        }
    }
}
