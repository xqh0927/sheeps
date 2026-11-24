package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.menu.ui.components.CoinCanvas
import com.example.sheeps.menu.ui.components.CompassCanvas
import com.example.sheeps.menu.ui.components.GourdCanvas
import com.example.sheeps.menu.ui.components.StarCanvas
import com.example.sheeps.ui.R

@Composable
fun GameGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.description_game_guide), fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { GuideSectionCard(stringResource(R.string.guide_sec1_title), stringResource(R.string.guide_sec1_desc)) { CompassCanvas() } }
                item { GuideSectionCard(stringResource(R.string.guide_sec2_title), stringResource(R.string.guide_sec2_desc)) { GourdCanvas() } }
                item { GuideSectionCard(stringResource(R.string.guide_sec3_title), stringResource(R.string.guide_sec3_desc)) { CoinCanvas() } }
                item { GuideSectionCard(stringResource(R.string.guide_sec4_title), stringResource(R.string.guide_sec4_desc)) { GourdCanvas() } }
                item { GuideSectionCard(stringResource(R.string.guide_sec5_title), stringResource(R.string.guide_sec5_desc)) { StarCanvas() } }
            }
        },
        confirmButton = { Button(onDismiss) { Text(stringResource(R.string.dialog_guide_ok)) } }
    )
}

@Composable
fun GuideSectionCard(title: String, description: String, icon: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icon()
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
