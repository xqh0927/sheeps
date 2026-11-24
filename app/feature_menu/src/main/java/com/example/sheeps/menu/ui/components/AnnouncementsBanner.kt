package com.example.sheeps.menu.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sheeps.data.model.Notice
import com.example.sheeps.ui.R
import kotlinx.coroutines.delay

@Composable
fun AnnouncementsBanner(notices: List<Notice>, onClick: () -> Unit) {
    val titles = remember(notices) { notices.take(2).map { it.title } }
    var index by rememberSaveable(titles) { mutableIntStateOf(0) }
    LaunchedEffect(titles) {
        if (titles.size > 1) while (true) {
            delay(5500)
            index = (index + 1) % titles.size
        }
    }
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick)
        .heightIn(min = 48.dp).padding(horizontal = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).background(colors.surface, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Campaign, stringResource(R.string.notice_banner_title),
                tint = colors.primary, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(9.dp))
        Crossfade(targetState = titles.getOrNull(index) ?: titles.firstOrNull() ?: stringResource(R.string.notice_empty),
            modifier = Modifier.weight(1f), label = "notice") { title ->
            Text(title, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}
