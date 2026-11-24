package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.menu.R as MenuR
import com.example.sheeps.ui.R

@Composable
internal fun LevelAtlasTile(
    levelId: Int,
    unlockedLevel: Int,
    onStart: () -> Unit,
    onShowLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val current = levelId == unlockedLevel
    val locked = levelId > unlockedLevel
    val shape = RoundedCornerShape(17.dp)
    val title = stringResource(R.string.level_number, levelId)
    val kind = stringResource(when {
        levelId == 1 -> R.string.level_desc_1
        levelId == 2 -> R.string.level_desc_2
        levelId == 3 -> R.string.level_desc_3
        levelId % 5 == 0 -> MenuR.string.lobby_rest
        levelId % 3 == 0 -> R.string.level_desc_blind
        levelId % 2 == 0 -> R.string.level_desc_sealed
        else -> R.string.level_desc_hell
    })
    val action = stringResource(if (locked) R.string.dialog_prepare_unlock else R.string.prepare_btn_start)
    val rank = stringResource(R.string.home_btn_leaderboard)
    Row(
        modifier.fillMaxHeight().clip(shape)
            .background(if (current) colors.primaryContainer else colors.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier.weight(1f).fillMaxHeight()
                .clickable(role = Role.Button, onClickLabel = action, onClick = onStart)
                .semantics { contentDescription = title }
                .padding(start = 13.dp, end = if (locked) 12.dp else 0.dp, top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    levelId.toString().padStart(2, '0'),
                    color = if (current) colors.primary else colors.onSurface,
                    fontSize = 24.sp, lineHeight = 28.sp,
                    letterSpacing = (-1).sp, fontWeight = FontWeight.SemiBold
                )
                if (locked) {
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Outlined.Lock, stringResource(R.string.prepare_title_locked),
                        tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(kind, color = if (current) colors.onPrimaryContainer else colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium)
        }
        if (!locked) {
            IconButton(onClick = onShowLeaderboard, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Leaderboard, "$title · $rank", modifier = Modifier.size(20.dp),
                    tint = if (current) colors.primary else colors.onSurfaceVariant)
            }
        }
    }
}
