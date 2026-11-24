package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sheeps.data.model.Notice
import com.example.sheeps.menu.R as MenuR
import com.example.sheeps.menu.ui.components.CatalogDivider
import com.example.sheeps.menu.ui.dialogs.NoticeDetailDialog
import com.example.sheeps.ui.R
import com.example.sheeps.ui.components.SheepsTopAppBar
import java.text.DateFormat
import java.util.Date

@Composable
fun NoticeListScreen(notices: List<Notice>, onBack: () -> Unit) {
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateFormat.getDateInstance(DateFormat.MEDIUM, locale) }
    Scaffold(topBar = { SheepsTopAppBar(stringResource(R.string.notice_list_title), onBack) }) { inset ->
        if (notices.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(inset), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
                Icon(Icons.Default.NotificationsNone, null, modifier = Modifier.size(44.dp), tint = colors.primary)
                Text(stringResource(R.string.notice_empty), color = colors.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(inset), contentPadding = PaddingValues(18.dp)) {
                itemsIndexed(notices, key = { _, notice -> notice.id }) { index, notice ->
                    val top = if (index == 0) 20.dp else 0.dp
                    val bottom = if (index == notices.lastIndex) 20.dp else 0.dp
                    Surface(onClick = { selectedId = notice.id }, color = colors.surface,
                        shape = RoundedCornerShape(top, top, bottom, bottom)) {
                        Column {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val icon = when (notice.type) {
                                    "ACTIVITY" -> Icons.Default.Event
                                    "UPDATE" -> Icons.Default.SystemUpdate
                                    "MAINTENANCE" -> Icons.Default.Build
                                    "SYSTEM" -> Icons.Default.Info
                                    else -> Icons.Default.NotificationsNone
                                }
                                Box(Modifier.size(34.dp).background(colors.primary.copy(alpha = .09f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(noticeTypeLabel(notice.type)), Modifier.weight(1f), color = colors.primary,
                                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        Text(formatter.format(Date(notice.created_at)), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(notice.title, color = colors.onSurface, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleMedium)
                                    if (notice.content.isNotBlank()) {
                                        Text(notice.content, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp).align(Alignment.CenterVertically))
                            }
                            if (index < notices.lastIndex) CatalogDivider(62.dp)
                        }
                    }
                }
            }
        }
    }
    notices.firstOrNull { it.id == selectedId }?.let { NoticeDetailDialog(it) { selectedId = null } }
}

internal fun noticeTypeLabel(type: String): Int = when (type) {
    "SYSTEM" -> R.string.notice_type_system
    "ACTIVITY" -> R.string.notice_type_activity
    "UPDATE" -> MenuR.string.catalog_update
    "MAINTENANCE" -> MenuR.string.catalog_maintenance
    else -> R.string.notice_type_general
}
