package com.example.sheeps.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.example.sheeps.ui.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


@Composable
fun SheepsTopAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showAction: Boolean = false,
    actionIcon: ImageVector = Icons.Default.Refresh,
    onActionClick: () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background
) {
    Surface(color = containerColor, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.statusBarsPadding().fillMaxWidth().height(52.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Box(Modifier.size(38.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.btn_back),
                        modifier = Modifier.size(29.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Text(title, Modifier.weight(1f).padding(horizontal = 6.dp), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium, maxLines = 1,
                overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
            if (showAction) {
                IconButton(onClick = onActionClick) {
                    Box(Modifier.size(38.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(actionIcon, stringResource(R.string.topbar_action), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            } else Spacer(Modifier.size(48.dp))
        }
    }
}
