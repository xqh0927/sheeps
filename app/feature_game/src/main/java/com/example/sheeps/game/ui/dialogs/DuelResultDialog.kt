package com.example.sheeps.game.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.sheeps.game.state.DuelViewState
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.ui.components.PrimaryButton
import androidx.compose.ui.res.stringResource
import com.example.sheeps.core.R

/**
 * 对决模式结算对话框
 */
@Composable
fun DuelResultDialog(
    state: DuelViewState,
    onLeave: () -> Unit
) {
    if (state.gameStatus != GameStatus.WON && state.gameStatus != GameStatus.LOST) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (state.gameStatus == GameStatus.WON) stringResource(id = R.string.duel_won_title) else stringResource(id = R.string.duel_lost_title),
                style = MaterialTheme.typography.headlineMedium,
                color = if (state.gameStatus == GameStatus.WON) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(text = stringResource(id = R.string.duel_your_score, state.score), style = MaterialTheme.typography.bodyLarge)
            Text(text = stringResource(id = R.string.duel_opponent_score, state.opponentScore), style = MaterialTheme.typography.bodyLarge)
            
            Spacer(Modifier.height(24.dp))
            
            PrimaryButton(
                text = stringResource(id = R.string.duel_btn_leave),
                onClick = onLeave,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
