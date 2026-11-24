package com.example.sheeps.game.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.game.state.GameViewState
import androidx.compose.ui.res.stringResource
import com.example.sheeps.ui.R
import kotlinx.coroutines.isActive

/**
 * 游戏状态栏组件
 * 展示：当前得分、双倍积分标识、实时计时器、剩余卡牌数量
 *
 * @param state 游戏界面状态
 */
@Composable
fun GameStatusBar(state: GameViewState) {
    // 剩余卡牌数量
    val remaining = state.boardTiles.count {
        it.state == TileState.NORMAL || it.state == TileState.BLOCKED
    }
    // 计时器：基于关卡起始时间戳本地自增，每秒仅重绘状态栏自身，不再依赖全局 elapsedMs 实时流（已解耦到 UI 本地计时）。
    // 仅在 PLAYING 时自增；胜利/失败后定格为当前已用时间（与原有“结算后冻结”表现一致），复活回到 PLAYING 后继续计时。
    var displayMs by remember { mutableStateOf(0L) }
    LaunchedEffect(state.levelStartTimestamp, state.gameStatus) {
        val startTs = state.levelStartTimestamp
        if (startTs <= 0L) {
            displayMs = 0L
            return@LaunchedEffect
        }
        if (state.gameStatus != GameStatus.PLAYING) {
            displayMs = System.currentTimeMillis() - startTs
            return@LaunchedEffect
        }
        while (isActive) {
            displayMs = System.currentTimeMillis() - startTs
            delay(1000)
        }
    }
    val minutes = (displayMs / 60000).toInt()
    val seconds = ((displayMs % 60000) / 1000).toInt()
    val timeText = String.format("%02d:%02d", minutes, seconds)

    val palette = MaterialTheme.colorScheme
    Scoreboard {
        val label = stringResource(R.string.game_score_label)
        MatchScore(
            label = if (state.isDoublePointsActive) "$label  ×2" else label,
            value = state.score,
            ink = palette.onSurface,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                timeText,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = palette.onSurface
            )
            Text(
                stringResource(R.string.game_remaining_tiles, remaining),
                style = MaterialTheme.typography.labelMedium,
                color = palette.onSurfaceVariant
            )
        }
    }
}
