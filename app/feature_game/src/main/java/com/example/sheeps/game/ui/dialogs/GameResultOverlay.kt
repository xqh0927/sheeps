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
import androidx.compose.ui.res.stringResource
import com.example.sheeps.core.R

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
                            colors = listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
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
    val themeSecondary = MaterialTheme.colorScheme.secondary
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
                colors = listOf(themeSecondary.copy(alpha = glowAlpha * 0.5f), Color.Transparent)
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
        text       = stringResource(id = R.string.game_won_title),
        style      = MaterialTheme.typography.displaySmall,
        fontFamily = FontFamily.Serif,
        color      = MaterialTheme.colorScheme.secondary,
        modifier   = Modifier.padding(bottom = 6.dp)
    )
    Text(
        text  = stringResource(id = R.string.game_won_desc),
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
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), ShapeMedium)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = stringResource(id = R.string.game_won_score),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnimatedCounter(
                count = state.finalScore,
                style = MaterialTheme.typography.displayMedium.copy(color = MaterialTheme.colorScheme.secondary)
            )
            Spacer(Modifier.height(8.dp))
            val minutes = (state.elapsedMs / 60000).toInt()
            val seconds = ((state.elapsedMs % 60000) / 1000).toInt()
            Text(
                text = stringResource(
                    id = R.string.game_clear_time,
                    String.format("%02d:%02d", minutes, seconds)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SecondaryButton(
            text     = stringResource(id = R.string.btn_leaderboard),
            onClick  = onShowLeaderboard,
            modifier = Modifier.weight(1f)
        )
        SecondaryButton(
            text     = stringResource(id = R.string.btn_home),
            onClick  = onBack,
            modifier = Modifier.weight(1f)
        )
        PrimaryButton(
            text     = stringResource(id = R.string.btn_next_level),
            onClick  = onNextLevel,
            modifier = Modifier.weight(1.5f)
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
        text       = stringResource(id = R.string.game_lost_title),
        style      = MaterialTheme.typography.headlineLarge,
        fontFamily = FontFamily.Serif,
        color      = MaterialTheme.colorScheme.primary,
        modifier   = Modifier.padding(bottom = 6.dp)
    )
    Text(
        text  = stringResource(id = R.string.game_lost_desc),
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
            text     = if (canRevive) stringResource(id = R.string.btn_revive_count, state.reviveCount) else stringResource(id = R.string.btn_revive_empty),
            onClick  = onRevive,
            enabled  = canRevive,
            modifier = Modifier.fillMaxWidth()
        )
        SecondaryButton(
            text     = stringResource(id = R.string.btn_restart),
            onClick  = onRestart,
            modifier = Modifier.fillMaxWidth()
        )
        GhostButton(
            text    = stringResource(id = R.string.btn_back_home),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
