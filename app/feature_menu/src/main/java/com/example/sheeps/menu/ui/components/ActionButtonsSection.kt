package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sheeps.ui.R

@Composable
fun ActionButtonsSection(onShowPointHistory: () -> Unit, onShowExchangeHistory: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple(Icons.Default.History, R.string.points_history_title, onShowPointHistory),
            Triple(Icons.Default.Receipt, R.string.exchange_history_title, onShowExchangeHistory)
        ).forEach { (icon, label, onClick) ->
            Surface(onClick = onClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.heightIn(min = 52.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
