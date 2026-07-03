package com.example.sheeps.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.theme.Text_Secondary_Dark
import com.example.sheeps.ui.components.AnimatedCounter
import androidx.compose.ui.res.stringResource
import com.example.sheeps.core.R

/**
 * 游戏状态栏组件
 * 展示：当前得分、双倍积分标识、剩余卡牌数量
 * 
 * @param state 游戏界面状态
 */
@Composable
fun GameStatusBar(state: GameViewState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // 得分展示区域
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = R.string.game_score_label),
                style = MaterialTheme.typography.labelMedium,
                color = Text_Secondary_Dark
            )
            Spacer(Modifier.width(6.dp))
            AnimatedCounter(
                count = state.score,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color      = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            )
            
            // 双倍积分标识
            if (state.isDoublePointsActive) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text  = "×2",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 剩余卡牌数量展示
        val remaining = state.boardTiles.count {
            it.state == TileState.NORMAL || it.state == TileState.BLOCKED
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text  = stringResource(id = R.string.game_remaining_tiles, remaining),
                style = MaterialTheme.typography.bodyMedium,
                color = Text_Secondary_Dark
            )
        }
    }
}
