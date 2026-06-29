package com.example.sheeps.game.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.*
import com.example.sheeps.game.ui.components.TileView
import com.example.sheeps.theme.*
import com.example.sheeps.ui.components.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameViewState,
    onTileClick: (Tile) -> Unit,
    onUseUndo: () -> Unit,
    onUseMoveOut: () -> Unit,
    onUseShuffle: () -> Unit,
    onRevive: () -> Unit,
    onUseHint: () -> Unit,
    onUseBomb: () -> Unit,
    onUseJoker: () -> Unit,
    onUseDouble: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    onNextLevel: () -> Unit,
    onShowLeaderboard: () -> Unit
) {
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
    ) {
        // 顶部微光装饰
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Crimson_Primary.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(this.size.width * 0.5f, 0f),
                    radius = this.size.width * 0.7f
                ),
                radius = this.size.width * 0.7f,
                center = Offset(this.size.width * 0.5f, 0f)
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // 自定义 TopBar（半透明毛玻璃效果）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(0.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = Gold_Primary
                            )
                        }

                        Text(
                            text = "第 ${state.currentLevelId} 关",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Serif,
                            color = Gold_Primary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        IconButton(onClick = onRestart) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "重玩",
                                tint = Text_Secondary_Dark
                            )
                        }
                    }
                }
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
                // 顶部状态栏（积分+剩余牌数）
                GameStatusBar(state = state)

                // 游戏棋盘
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val visibleTiles = state.boardTiles.filter {
                            it.state == TileState.NORMAL || it.state == TileState.BLOCKED
                        }
                        if (visibleTiles.isNotEmpty()) {
                            val minX = visibleTiles.minOf { it.x }
                            val maxX = visibleTiles.maxOf { it.x }
                            val minY = visibleTiles.minOf { it.y }
                            val maxY = visibleTiles.maxOf { it.y }

                            val tileSize = 52
                            val boardWidth  = (maxX - minX) * 46 + tileSize
                            val boardHeight = (maxY - minY) * 46 + tileSize

                            android.util.Log.d("GameScreen", "visibleTiles: ${visibleTiles.size}, boardSize: ${boardWidth}x${boardHeight}")

                            Box(modifier = Modifier.size(width = boardWidth.dp, height = boardHeight.dp)) {
                                visibleTiles.forEach { tile ->
                                    val isHighlighted = state.highlightedTileIds.contains(tile.id)
                                    TileView(
                                        tile     = tile,
                                        onClick  = { onTileClick(tile) },
                                        currentSkin = state.currentSkin,
                                        tileSize = 52.dp,
                                        modifier = Modifier
                                            .offset(
                                                x = ((tile.x - minX) * 46).dp,
                                                y = ((tile.y - minY) * 46).dp
                                            )
                                            .zIndex(tile.z.toFloat())
                                            .then(
                                                if (isHighlighted) {
                                                    Modifier.border(
                                                        width = 2.dp,
                                                        color = Gold_Primary,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                } else Modifier
                                            )
                                    )
                                }
                            }
                        }
                    }

                // 移出置物架
                if (state.movedOutTiles.isNotEmpty()) {
                    MovedOutTray(tiles = state.movedOutTiles, currentSkin = state.currentSkin, onTileClick = onTileClick)
                }

                // 消除槽
                MatchingSlot(state = state)

                // 道具栏
                ToolButtonRow(
                    state        = state,
                    onUseMoveOut = onUseMoveOut,
                    onUseUndo    = onUseUndo,
                    onUseShuffle = onUseShuffle,
                    onUseHint    = onUseHint,
                    onUseBomb    = onUseBomb,
                    onUseJoker   = onUseJoker,
                    onUseDouble  = onUseDouble
                )

                // 携带道具展示栏（在最下方显示携带的道具图片与名称）
                val carriedItems = remember(state) {
                    listOf(
                        "UNDO" to ("撤销符" to state.undoCount),
                        "SHUFFLE" to ("洗牌咒" to state.shuffleCount),
                        "MOVEOUT" to ("移出印" to state.moveOutCount),
                        "REVIVE" to ("复活丹" to state.reviveCount),
                        "HINT" to ("提示符" to state.hintCount),
                        "BOMB" to ("爆裂弹" to state.bombCount),
                        "JOKER" to ("万能牌" to state.jokerCount),
                        "DOUBLE_POINTS" to ("双倍卡" to state.doublePointsCount)
                    ).filter { it.second.second > 0 }
                }

                if (carriedItems.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "— 已携带法宝 —",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold_Primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            carriedItems.forEach { (type, pair) ->
                                val (name, count) = pair
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    com.example.sheeps.ui.components.ItemAnimationIcon(
                                        itemType = type,
                                        size = 36.dp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "$name x$count",
                                        fontSize = 10.sp,
                                        color = Text_Secondary_Dark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }

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

        // 阻止加载时手势穿透的全屏遮罩
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                    .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                SheepsLoading(size = 56.dp)
            }
        }
    }
}

// --- 顶部状态栏 ---
@Composable
private fun GameStatusBar(state: GameViewState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // 得分
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "得分",
                style = MaterialTheme.typography.labelMedium,
                color = Text_Secondary_Dark
            )
            Spacer(Modifier.width(6.dp))
            AnimatedCounter(
                count = state.score,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color      = Gold_Primary,
                    fontWeight = FontWeight.Bold
                )
            )
            if (state.isDoublePointsActive) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Crimson_PrimaryContainer.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text  = "×2",
                        color = Gold_Primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 剩余卡牌
        val remaining = state.boardTiles.count {
            it.state == TileState.NORMAL || it.state == TileState.BLOCKED
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text  = "剩余 $remaining 张",
                style = MaterialTheme.typography.bodyMedium,
                color = Text_Secondary_Dark
            )
        }
    }
}

// --- 移出置物架 ---
@Composable
private fun MovedOutTray(
    tiles: List<Tile>,
    currentSkin: String,
    onTileClick: (Tile) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text  = "— 置物架 —",
            style = MaterialTheme.typography.labelSmall,
            color = Gold_Primary.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, Gold_Subtle.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            tiles.forEach { tile ->
                TileView(
                    tile    = tile,
                    onClick = { onTileClick(tile) },
                    currentSkin = currentSkin,
                    tileSize = 46.dp
                )
            }
        }
    }
}

// --- 消除槽 ---
@Composable
private fun MatchingSlot(state: GameViewState) {
    val slotBorderAlpha by rememberInfiniteTransition(label = "slotBorder").animateFloat(
        initialValue = 0.6f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "slotBorderAlpha"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = 1.5.dp,
                color = Crimson_Primary.copy(alpha = slotBorderAlpha),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 7) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (i < state.slotTiles.size) {
                    TileView(
                        tile    = state.slotTiles[i],
                        onClick = {},
                        currentSkin = state.currentSkin,
                        tileSize = 40.dp
                    )
                }
            }
        }
    }
}

// --- 道具栏 ---
@Composable
private fun ToolButtonRow(
    state: GameViewState,
    onUseMoveOut: () -> Unit,
    onUseUndo: () -> Unit,
    onUseShuffle: () -> Unit,
    onUseHint: () -> Unit,
    onUseBomb: () -> Unit,
    onUseJoker: () -> Unit,
    onUseDouble: () -> Unit
) {
    val tools = listOf(
        Triple("移出", state.moveOutCount, onUseMoveOut to (state.moveOutCount > 0 && state.slotTiles.isNotEmpty())),
        Triple("撤销", state.undoCount,    onUseUndo   to (state.undoCount > 0)),
        Triple("洗牌", state.shuffleCount, onUseShuffle to (state.shuffleCount > 0)),
        Triple("提示", state.hintCount,    onUseHint   to (state.hintCount > 0)),
        Triple("炸弹", state.bombCount,    onUseBomb   to (state.bombCount > 0 && state.slotTiles.size >= 2)),
        Triple("万能", state.jokerCount,   onUseJoker  to (state.jokerCount > 0 && state.slotTiles.isNotEmpty())),
        Triple("双倍", state.doublePointsCount, onUseDouble to (state.doublePointsCount > 0 && !state.isDoublePointsActive))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tools.forEach { (label, count, pair) ->
            val (onClick, enabled) = pair
            ToolItemButton(
                label   = label,
                count   = count,
                onClick = onClick,
                enabled = enabled
            )
        }
    }
}

// --- 胜利/失败覆盖层 ---
@Composable
private fun GameResultOverlay(
    won: Boolean,
    state: GameViewState,
    onBack: () -> Unit,
    onRestart: () -> Unit = {},
    onRevive: () -> Unit = {},
    onNextLevel: () -> Unit = {},
    onShowLeaderboard: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Overlay_Dark_Heavy)
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(ShapeLarge)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // 金边渐变
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Gold_Subtle, Gold_Primary.copy(alpha = 0.5f), Gold_Subtle)
                        ),
                        shape = ShapeLarge
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (won) {
                    // 胜利视觉
                    WonContent(
                        state = state,
                        onBack = onBack,
                        onNextLevel = onNextLevel,
                        onShowLeaderboard = onShowLeaderboard
                    )
                } else {
                    // 失败视觉
                    LostContent(
                        state     = state,
                        onBack    = onBack,
                        onRestart = onRestart,
                        onRevive  = onRevive
                    )
                }
            }
        }
    }
}

@Composable
private fun WonContent(
    state: GameViewState,
    onBack: () -> Unit,
    onNextLevel: () -> Unit,
    onShowLeaderboard: () -> Unit
) {
    // 动态光晕
    val infiniteTransition = rememberInfiniteTransition(label = "won")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 0.9f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wonGlow"
    )

    // 金色光晕
    Canvas(modifier = Modifier.size(80.dp)) {
        drawCircle(
            brush  = Brush.radialGradient(
                colors = listOf(Gold_Primary.copy(alpha = glowAlpha * 0.5f), Color.Transparent)
            ),
            radius = this.size.width * 0.5f
        )
    }

    Text(
        text     = "🏮",
        fontSize = 48.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Text(
        text       = "恭喜通关！",
        style      = MaterialTheme.typography.displaySmall,
        fontFamily = FontFamily.Serif,
        color      = Gold_Primary,
        modifier   = Modifier.padding(bottom = 6.dp)
    )
    Text(
        text  = "名扬四海，金榜题名！",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 20.dp)
    )

    // 得分展示
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeMedium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, Gold_Subtle.copy(alpha = 0.3f), ShapeMedium)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "本次积分",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnimatedCounter(
                count = if (state.isDoublePointsActive) state.score * 2 else state.score,
                style = MaterialTheme.typography.displayMedium.copy(color = Gold_Primary)
            )
        }
    }

    Spacer(Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SecondaryButton(
            text     = "英雄榜",
            onClick  = onShowLeaderboard,
            modifier = Modifier.weight(1f)
        )
        SecondaryButton(
            text     = "主页",
            onClick  = onBack,
            modifier = Modifier.weight(1f)
        )
        PrimaryButton(
            text     = "下一关",
            onClick  = onNextLevel,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
private fun LostContent(
    state: GameViewState,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onRevive: () -> Unit
) {
    Text(
        text     = "☁️",
        fontSize = 48.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Text(
        text       = "挑战失败",
        style      = MaterialTheme.typography.headlineLarge,
        fontFamily = FontFamily.Serif,
        color      = Crimson_PrimaryLight,
        modifier   = Modifier.padding(bottom = 6.dp)
    )
    Text(
        text  = "消除槽已满，棋局陷入死局！",
        style = MaterialTheme.typography.bodyMedium,
        color = Text_Secondary_Dark,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 24.dp)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val canRevive = state.reviveCount > 0
        PrimaryButton(
            text     = if (canRevive) "使用还魂丹复活（剩 ${state.reviveCount} 次）" else "复活法宝已耗尽",
            onClick  = onRevive,
            enabled  = canRevive,
            modifier = Modifier.fillMaxWidth()
        )
        SecondaryButton(
            text     = "重新开始",
            onClick  = onRestart,
            modifier = Modifier.fillMaxWidth()
        )
        GhostButton(
            text    = "返回主页",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 旧版 ToolButton 兼容保留（实际已被 ToolItemButton 替代）
@Composable
fun ToolButton(
    text: String,
    count: Int,
    onClick: () -> Unit,
    enabled: Boolean
) {
    ToolItemButton(label = text, count = count, onClick = onClick, enabled = enabled)
}
