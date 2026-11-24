package com.example.sheeps.leaderboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sheeps.data.model.RankingEntry
import com.example.sheeps.ui.R

@Composable
internal fun RankingBoard(
    rankings: List<RankingEntry>,
    currentUser: String?,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState()
) {
    val palette = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier.navigationBarsPadding(),
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        itemsIndexed(rankings) { index, entry ->
            val isCurrentUser = entry.username == currentUser
            if (index == 0) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        .background(palette.surface, RoundedCornerShape(24.dp)).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        Modifier.size(54.dp).background(palette.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("1", style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold, color = palette.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f)) {
                        RankName(entry.username, isCurrentUser)
                        Text(
                            stringResource(R.string.leaderboard_score_format, entry.score),
                            style = MaterialTheme.typography.headlineLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold, color = palette.onSurface
                        )
                        RankTime(entry)
                    }
                }
            } else {
                RankingRow(index, entry, isCurrentUser, first = index == 1, last = index == rankings.lastIndex)
            }
        }
    }
}

@Composable
private fun RankName(name: String, isCurrentUser: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            name, Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (isCurrentUser) {
            Text(stringResource(R.string.label_duel_me), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RankTime(entry: RankingEntry) {
    Text(stringResource(R.string.leaderboard_clear_time_format, entry.clear_time_ms / 1000.0),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun RankingRow(
    index: Int,
    entry: RankingEntry,
    isCurrentUser: Boolean,
    first: Boolean = false,
    last: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val top = if (first) 20.dp else 0.dp
    val bottom = if (last) 20.dp else 0.dp
    val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    Column(Modifier.fillMaxWidth().background(colors.surface, shape)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.width(36.dp).heightIn(min = 36.dp)
                    .background(if (index <= 2) colors.surfaceContainerHigh else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text((index + 1).toString(), style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                RankName(entry.username, isCurrentUser)
                RankTime(entry)
            }
            Text(stringResource(R.string.leaderboard_score_format, entry.score),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = colors.onSurface, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        }
        if (!last) HorizontalDivider(
            Modifier.padding(start = 62.dp, end = 14.dp),
            thickness = .5.dp, color = colors.outlineVariant
        )
    }
}

@Composable
internal fun RankingNotice(message: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                listOf(20.dp, 32.dp, 14.dp).forEach { height ->
                    Box(Modifier.width(10.dp).height(height).background(colors.primary.copy(alpha = .62f), CircleShape))
                }
            }
            Text(message, color = colors.onSurfaceVariant, style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center)
        }
    }
}
