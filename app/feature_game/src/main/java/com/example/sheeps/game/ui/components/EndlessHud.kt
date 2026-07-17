package com.example.sheeps.game.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.R

/**
 * 无尽生存模式顶栏 HUD：分数 / 连击 / 波次 / 最高分 + 冻结与暂停按钮。
 * 所有颜色走 MaterialTheme.colorScheme 令牌。
 */
@Composable
fun EndlessHud(
    score: Int,
    combo: Int,
    wave: Int,
    bestScore: Int,
    isFrozen: Boolean,
    freezeCount: Int,
    onFreeze: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.endless_score, score),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.endless_combo, combo),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = stringResource(R.string.endless_wave, wave),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = stringResource(R.string.endless_best, bestScore),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 冻结按钮
        FilledTonalButton(
            onClick = onFreeze,
            enabled = freezeCount > 0 && !isFrozen,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isFrozen) stringResource(R.string.endless_freeze) + "…"
                else stringResource(R.string.endless_freeze) + " ×$freezeCount",
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
