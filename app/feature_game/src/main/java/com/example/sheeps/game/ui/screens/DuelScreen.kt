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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.sheeps.core.R
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
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
                    // 飞行动画触发逻辑
                    val startPos = tileGlobalPositions[tile.id] ?: Offset.Zero
                    val nextSlotIdx = state.slotTiles.size
                    val endPos = slotGlobalPositions[nextSlotIdx] ?: slotGlobalPositions[0] ?: Offset.Zero
                    val anim = Animatable(0f)
                    val fly = DuelFlyingTile(tile.id, tile.type, startPos, endPos, anim)
                    
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

            Spacer(Modifier.height(16.dp))
            
            // 4. 置物架区域
            DuelMovedOutTray(state = state, onTileClick = onTileClick)
            
            Spacer(Modifier.height(8.dp))

            // 5. 消除槽区域
            DuelMatchingSlot(state = state, slotGlobalPositions = slotGlobalPositions)

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
            screenRootOffset = screenRootOffset,
            density = density
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
            TileView(tile = tile, onClick = { onTileClick(tile) }, currentSkin = "classic", tileSize = 46.dp)
        }
    }
}

/**
 * 对决模式消除槽位（支持缩减槽位诅咒）
 */
@Composable
private fun DuelMatchingSlot(state: DuelViewState, slotGlobalPositions: MutableMap<Int, Offset>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 7) {
            val isLocked = i == 6 && state.maxSlotSize == 6
            Box(
                modifier = Modifier
                    .weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (isLocked) Crimson_Primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(width = 1.dp, color = if (isLocked) Crimson_Primary else Color.Transparent, shape = RoundedCornerShape(8.dp))
                    .onGloballyPositioned { slotGlobalPositions[i] = it.positionInRoot() },
                contentAlignment = Alignment.Center
            ) {
                if (isLocked) {
                    Text("🔒", fontSize = 16.sp)
                } else if (i < state.slotTiles.size) {
                    TileView(tile = state.slotTiles[i], onClick = {}, currentSkin = "classic", tileSize = 40.dp)
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

        Box(modifier = Modifier.offset(x = xDp, y = yDp).size(sizeDp).zIndex(999f)) {
            TileView(tile = Tile(id = "fly_view", type = fly.type, x = 0f, y = 0f, z = 0), onClick = {}, currentSkin = "classic", tileSize = sizeDp)
        }
    }
}
