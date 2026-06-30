package com.example.sheeps.game.ui.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.theme.*
import com.example.sheeps.ui.components.*

/**
 * 游戏结果覆盖层组件（胜利或失败弹窗）
 */
@Composable
fun GameResultOverlay(
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
            // 金边渐变装饰
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
                    WonContent(
                        state = state,
                        onBack = onBack,
                        onNextLevel = onNextLevel,
                        onShowLeaderboard = onShowLeaderboard
                    )
                } else {
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

/**
 * 胜利内容展示
 */
@Composable
private fun WonContent(
    state: GameViewState,
    onBack: () -> Unit,
    onNextLevel: () -> Unit,
    onShowLeaderboard: () -> Unit
) {
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

    // 金色光晕效果
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

    // 本次得分
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

/**
 * 失败内容展示
 */
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
