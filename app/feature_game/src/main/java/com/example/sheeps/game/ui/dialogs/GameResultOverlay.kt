package com.example.sheeps.game.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.game.ui.components.MatchScore
import com.example.sheeps.ui.R
import com.example.sheeps.ui.components.GhostButton
import com.example.sheeps.ui.components.PrimaryButton
import com.example.sheeps.ui.components.SecondaryButton

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
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxSize().background(colors.scrim.copy(alpha = .42f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Spacer(Modifier.matchParentSize().clickable(remember { MutableInteractionSource() }, indication = null) {})
        Surface(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().statusBarsPadding(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = colors.surface
        ) {
            Column(
                Modifier.navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.level_number, state.currentLevelId), color = colors.primary,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(if (won) R.string.game_won_title else R.string.game_lost_title),
                    style = MaterialTheme.typography.headlineLarge, color = colors.onSurface, fontWeight = FontWeight.Bold)
                Text(stringResource(if (won) R.string.game_won_desc else R.string.game_lost_desc),
                    color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Row(
                    Modifier.fillMaxWidth().background(colors.surfaceContainerHigh, RoundedCornerShape(18.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MatchScore(stringResource(if (won) R.string.game_won_score else R.string.game_score_label),
                        if (won) state.finalScore else state.score, colors.onSurface, Modifier.weight(1f))
                    if (won) {
                        val time = String.format("%02d:%02d", state.elapsedMs / 60000, (state.elapsedMs / 1000) % 60)
                        Text(stringResource(R.string.game_clear_time, time), style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurfaceVariant)
                    }
                }
                if (won) {
                    PrimaryButton(stringResource(R.string.btn_next_level), onClick = onNextLevel, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(stringResource(R.string.btn_leaderboard), onClick = onShowLeaderboard, modifier = Modifier.weight(1f))
                        GhostButton(stringResource(R.string.btn_home), onClick = onBack, modifier = Modifier.weight(1f))
                    }
                } else {
                    val canRevive = state.reviveCount > 0
                    val revive = if (canRevive) stringResource(R.string.btn_revive_count, state.reviveCount)
                        else stringResource(R.string.btn_revive_empty)
                    PrimaryButton(revive, onClick = onRevive, enabled = canRevive, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(stringResource(R.string.btn_restart), onClick = onRestart, modifier = Modifier.weight(1f))
                        GhostButton(stringResource(R.string.btn_back_home), onClick = onBack, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
