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
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.ui.R

@Composable
fun ActionButtonsSection(
    state: MenuViewState,
    onShowPointHistory: () -> Unit,
    onShowExchangeHistory: () -> Unit,
    onLogoutClick: () -> Unit
) {
    if (!state.isLoggedIn) return
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column {
                CatalogLink(Icons.Default.History, stringResource(R.string.points_history_title), onShowPointHistory)
                CatalogDivider()
                CatalogLink(Icons.Default.Receipt, stringResource(R.string.exchange_history_title), onShowExchangeHistory)
            }
        }
        TextButton(onLogoutClick, modifier = Modifier.padding(top = 16.dp).fillMaxWidth().heightIn(min = 50.dp),
            shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text(stringResource(R.string.btn_logout), color = MaterialTheme.colorScheme.error)
        }
    }
}
