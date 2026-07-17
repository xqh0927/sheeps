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
 * 游戏结果覆盖层组件（胜利或失败弹窗）。
 *
 * 以全屏半透明遮罩 + 居中卡片（金边渐变描边）呈现；根据 [won] 分别组合
 * [WonContent]（胜利：得分/用时、排行榜、下一关）或 [LostContent]
 * （失败：复活/重开/返回）。卡片内点击通过各自回调上抛。
 *
 * 线程约束：纯 Composable，运行于主线程；得分数字滚动由 `AnimatedCounter` 驱动。
 * ⚠️ 内存隐患：遮罩 `Box` 的 `clickable(enabled = false)` 仅用于消费点击、
 * 阻断底层交互；本组件不持有任何 Activity/Context 或静态引用，随父级
 * `AnimatedVisibility` 显隐而组合/销毁，无泄漏。
 *
 * @param won             是否为胜利状态
 * @param state          当前游戏视图状态
 * @param onBack         返回首页
 * @param onRestart      重新开始本关（失败态使用）
 * @param onRevive       复活（失败态使用）
 * @param onNextLevel    进入下一关（胜利态使用）
 * @param onShowLeaderboard 打开排行榜（胜利态使用）
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
 * 胜利内容展示。
 *
 * 渲染金色光晕（循环动画）、胜利标题/描述、本次得分（`AnimatedCounter` 滚动）
 * 与通关用时，并提供“排行榜”与“返回首页 / 下一关”按钮。
 *
 * ⚠️ 内存/生命周期：下方 `rememberInfiniteTransition` 用于光晕呼吸动画，
 * 它绑定于本组合项生命周期，组件从组合树移除时动画协程自动取消，无泄漏风险。
 *
 * 线程约束：纯 Composable，运行于主线程；时间换算（分:秒）在 UI 主线程完成。
 *
 * @param state          当前游戏视图状态，提供得分与用时
 * @param onBack         返回首页
 * @param onNextLevel    进入下一关
 * @param onShowLeaderboard 打开排行榜
 */
@Composable
private fun WonContent(
    state: GameViewState,
    onBack: () -> Unit,
    onNextLevel: () -> Unit,
    onShowLeaderboard: () -> Unit
) {
    val themeSecondary = MaterialTheme.colorScheme.secondary
    // 无限循环光晕动画；rememberInfiniteTransition 绑定组合生命周期，组件销毁时自动停止，无泄漏
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

    // 排行榜 - 单独一行（全宽）
    SecondaryButton(
        text     = stringResource(id = R.string.btn_leaderboard),
        onClick  = onShowLeaderboard,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))
    // 返回首页 + 下一关 - 一行
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SecondaryButton(
            text     = stringResource(id = R.string.btn_home),
            onClick  = onBack,
            modifier = Modifier.weight(1f)
        )
        PrimaryButton(
            text     = stringResource(id = R.string.btn_next_level),
            onClick  = onNextLevel,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 失败内容展示。
 *
 * 渲染失败标题/描述，以及“复活”（[GameViewState.reviveCount] > 0 时可用）、
 * “重新开始”、“返回首页”三个按钮；[onRevive] 仅在可复活时启用。
 *
 * 线程约束：纯 Composable，运行于主线程；按钮可用态直接由 [state.reviveCount] 驱动。
 * ⚠️ 内存隐患：仅渲染即时回调，不捕获 Activity/Context，随父级覆盖层销毁而释放。
 *
 * @param state     当前游戏视图状态，提供剩余复活次数
 * @param onBack    返回首页
 * @param onRestart 重新开始本关
 * @param onRevive  复活（可复活时）
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
