package com.example.sheeps.game.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.sheeps.ui.R

@Composable
fun EndlessHud(score: Int, combo: Int, wave: Int, bestScore: Int) {
    val colors = MaterialTheme.colorScheme
    Scoreboard {
        MatchScore(stringResource(R.string.game_score_label), score, colors.onSurface, Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            AnimatedContent(wave, label = "endlessWave") { value ->
                Text(stringResource(R.string.endless_wave, value),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            }
            Text(stringResource(R.string.endless_best, bestScore),
                style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            AnimatedContent(combo, label = "endlessCombo") { value ->
                Text(stringResource(R.string.endless_combo, value),
                    style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}
