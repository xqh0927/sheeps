package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sheeps.ui.R

@Composable
internal fun CatalogTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier.semantics { heading() }, color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
}

@Composable
internal fun CatalogSwitch(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().background(colors.surfaceVariant, CircleShape)
        .selectableGroup().padding(3.dp)) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            val pill = if (active) Modifier.shadow(2.dp, CircleShape).background(colors.surface, CircleShape) else Modifier
            Box(
                Modifier.weight(1f).then(pill).clip(CircleShape)
                    .selectable(active, role = Role.Tab) { onSelect(index) }
                    .heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (active) colors.onSurface else colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
internal fun CatalogDivider(start: Dp = 60.dp) {
    HorizontalDivider(Modifier.padding(start = start), thickness = Dp.Hairline,
        color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun CatalogLink(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    detail: String? = null,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(32.dp).background(tint.copy(alpha = .13f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Text(title, modifier = Modifier.weight(1f), color = colors.onSurface, style = MaterialTheme.typography.bodyLarge)
        if (detail != null) Text(detail, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
            tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CatalogSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), containerColor = colors.surface,
        contentColor = colors.onSurface, dragHandle = null) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
            Spacer(Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                .size(36.dp, 5.dp).background(colors.outlineVariant, CircleShape))
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Box(Modifier.size(32.dp).background(colors.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, stringResource(R.string.dialog_prepare_close),
                            modifier = Modifier.size(17.dp), tint = colors.onSurfaceVariant)
                    }
                }
            }
            content()
        }
    }
}
