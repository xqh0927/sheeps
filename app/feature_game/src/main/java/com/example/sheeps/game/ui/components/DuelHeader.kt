package com.example.sheeps.game.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.multiplayer.WebSocketManager
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.DuelViewState
import com.example.sheeps.ui.R

@Composable
fun DuelHeader(state: DuelViewState, onLeave: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.duel_title), Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            val (label, color) = when (state.connectionState) {
                WebSocketManager.ConnectionState.Connected -> R.string.duel_status_connected to colors.primary
                WebSocketManager.ConnectionState.Connecting -> R.string.duel_status_connecting to colors.tertiary
                else -> R.string.duel_status_disconnected to colors.error
            }
            Box(Modifier.size(7.dp).background(color, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onLeave) {
                Box(Modifier.size(36.dp).gameControlSurface(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, stringResource(R.string.duel_btn_leave),
                        Modifier.size(19.dp), tint = colors.onSurface)
                }
            }
        }
        val remaining = state.boardTiles.count { it.state == TileState.NORMAL || it.state == TileState.BLOCKED } +
            state.movedOutTiles.size
        val progress = if (state.totalTileCount > 0) 1f - remaining.toFloat() / state.totalTileCount else 0f
        Scoreboard {
            DuelSide(
                stringResource(R.string.label_duel_me), state.score, progress,
                colors.primary, Modifier.weight(1f)
            )
            Spacer(Modifier.width(24.dp))
            DuelSide(
                stringResource(R.string.label_duel_enemy), state.opponentScore, state.opponentProgress,
                colors.secondary, Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DuelSide(label: String, score: Int, progress: Float, ink: Color, modifier: Modifier) {
    val animatedProgress by animateFloatAsState(progress.coerceIn(0f, 1f), label = "duelProgress")
    Column(modifier) {
        MatchScore(label, score, MaterialTheme.colorScheme.onSurface)
        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(4.dp),
            color = ink, trackColor = ink.copy(alpha = .15f))
    }
}

@Composable
internal fun DuelEnergyBar(currentEnergy: Int) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.label_duel_energy), style = MaterialTheme.typography.labelMedium)
        repeat(10) { segment ->
            val fill by animateColorAsState(
                if (segment < currentEnergy) colors.primary else colors.surfaceContainerHighest, label = "energyCell"
            )
            Box(Modifier.weight(1f).height(6.dp).background(fill, CircleShape))
        }
        Text("$currentEnergy/10", style = MaterialTheme.typography.labelMedium, color = colors.primary)
    }
}
