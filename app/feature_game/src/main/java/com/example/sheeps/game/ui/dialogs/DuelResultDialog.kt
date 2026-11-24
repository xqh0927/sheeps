package com.example.sheeps.game.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.game.state.DuelViewState
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.game.ui.components.MatchScore
import com.example.sheeps.ui.R
import com.example.sheeps.ui.components.PrimaryButton

@Composable
fun DuelResultDialog(state: DuelViewState, onLeave: () -> Unit) {
    if (state.gameStatus != GameStatus.WON && state.gameStatus != GameStatus.LOST) return
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = {},
        containerColor = colors.surface,
        title = {
            Text(stringResource(if (state.gameStatus == GameStatus.WON) R.string.duel_won_title else R.string.duel_lost_title),
                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Row(
                Modifier.fillMaxWidth().background(colors.surfaceContainerHigh, RoundedCornerShape(18.dp)).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MatchScore(stringResource(R.string.label_duel_me), state.score, colors.onSurface, Modifier.weight(1f))
                MatchScore(stringResource(R.string.label_duel_enemy), state.opponentScore, colors.onSurface, Modifier.weight(1f))
            }
        },
        confirmButton = {
            PrimaryButton(stringResource(R.string.duel_btn_leave), onClick = onLeave, modifier = Modifier.fillMaxWidth())
        }
    )
}
