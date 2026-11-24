package com.example.sheeps.game.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.core.utils.getLocalizedItemName
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.ui.R
import com.example.sheeps.ui.components.ItemIcon

private val carryItemTypes = listOf("UNDO", "SHUFFLE", "MOVEOUT", "REVIVE", "HINT", "BOMB", "JOKER", "DOUBLE_POINTS")

@Composable
fun CarrySelectionDialog(
    state: GameViewState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onUpdateItem: (String, Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = colors.surface,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text(
                    stringResource(R.string.carry_dialog_title), style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface, fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.carry_dialog_desc), Modifier.padding(top = 4.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant
                )
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    itemsIndexed(carryItemTypes, key = { _, type -> type }) { index, type ->
                        val selected = state.tempCarryItems[type] ?: 0
                        val stock = state.backpackItemStocks[type] ?: 0
                        val name = getLocalizedItemName(type)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ItemIcon(itemType = type, imageUrl = TileIconProvider.getItemIconUrl(type),
                                size = 32.dp, isGray = selected == 0)
                            Column(Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                                Text(stringResource(R.string.carry_stock_label, stock),
                                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                            Row(
                                Modifier.background(colors.surfaceContainerHigh, CircleShape),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(enabled = selected > 0, onClick = { onUpdateItem(type, -1) }) {
                                    Icon(Icons.Default.Remove,
                                        "${stringResource(R.string.prepare_desc_decrease)} $name", Modifier.size(18.dp))
                                }
                                Text(selected.toString(), style = MaterialTheme.typography.titleSmall,
                                    color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                                IconButton(enabled = selected < stock, onClick = { onUpdateItem(type, 1) }) {
                                    Icon(Icons.Default.Add,
                                        "${stringResource(R.string.prepare_desc_increase)} $name", Modifier.size(18.dp))
                                }
                            }
                        }
                        if (index != carryItemTypes.lastIndex) {
                            HorizontalDivider(Modifier.padding(start = 40.dp), thickness = .5.dp, color = colors.outlineVariant)
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
                    Button(onClick = onConfirm) { Text(stringResource(R.string.carry_confirm_start)) }
                }
            }
        }
    }
}
