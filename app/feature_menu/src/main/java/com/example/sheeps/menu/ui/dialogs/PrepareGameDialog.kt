package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.core.utils.getLocalizedItemName
import com.example.sheeps.menu.R as MenuR
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.ui.R
import com.example.sheeps.ui.components.ItemIcon
import com.hjq.toast.Toaster

private val carryTypes = listOf("UNDO", "SHUFFLE", "MOVEOUT", "REVIVE", "HINT", "BOMB", "JOKER", "DOUBLE_POINTS")

@Composable
fun PrepareGameDialog(
    levelId: Int,
    state: MenuViewState,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onUpdateItem: (String, Int) -> Unit,
    onUnlock: (Int) -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val locked = levelId > state.unlockedLevel
    val cost = ((state.unlockedLevel + 1)..levelId).fold(0) { total, stage ->
        total + when (stage) { 2 -> 50; 3 -> 100; else -> 200 }
    }
    val selectedTypes = state.selectedCarryItems.count { it.value > 0 }
    val warning = when {
        levelId % 5 == 0 -> null
        levelId % 3 == 0 -> R.string.prepare_blind_warning
        levelId % 2 == 0 -> R.string.prepare_sealed_warning
        else -> null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth(), color = colors.surface) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, top = 15.dp, end = 10.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(if (locked) R.string.prepare_title_locked else MenuR.string.lobby_loadout),
                            color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(R.string.level_number, levelId), color = colors.onSurface,
                            fontSize = 29.sp, lineHeight = 36.sp, letterSpacing = (-1).sp, fontWeight = FontWeight.Bold)
                    }
                    LobbyDialogClose(onDismiss, stringResource(R.string.dialog_prepare_close))
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)) {
                    if (locked) {
                        Text(stringResource(R.string.prepare_locked_desc, cost, state.points),
                            color = colors.onSurface, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(5.dp))
                    } else {
                        if (warning != null) {
                            Text(stringResource(warning), color = colors.onSecondaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth().background(colors.secondaryContainer, RoundedCornerShape(13.dp)).padding(10.dp))
                            Spacer(Modifier.height(10.dp))
                        }
                        Row(Modifier.padding(horizontal = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.prepare_setup_desc), modifier = Modifier.weight(1f),
                                color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(9.dp))
                            Text(stringResource(MenuR.string.lobby_picked, selectedTypes), color = colors.primary,
                                fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(10.dp))
                        Column(Modifier.clip(RoundedCornerShape(17.dp)).background(colors.surfaceContainer)) {
                            carryTypes.forEachIndexed { index, type ->
                                val stock = state.backpackItems.find { it.item_type == type }?.count ?: 0
                                val selected = state.selectedCarryItems[type] ?: 0
                                CarryItem(type, stock, selected, canAdd = selected < stock && (selected > 0 || selectedTypes < 5),
                                    onChange = { delta -> onUpdateItem(type, delta) })
                                if (index < carryTypes.lastIndex) {
                                    HorizontalDivider(Modifier.padding(start = 48.dp), thickness = .5.dp,
                                        color = colors.outlineVariant)
                                }
                            }
                        }
                    }
                }
                Button(onClick = {
                    if (!locked) onConfirm(levelId)
                    else if (state.points >= cost) onUnlock(levelId)
                    else Toaster.show(context.getString(R.string.prepare_locked_insufficient))
                }, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
                    .heightIn(min = 48.dp), shape = CircleShape) {
                    Icon(if (locked) Icons.Outlined.Lock else Icons.Default.ArrowForward, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (locked) R.string.dialog_prepare_unlock else R.string.prepare_btn_start))
                }
            }
        }
    }
}

@Composable
private fun CarryItem(type: String, stock: Int, selected: Int, canAdd: Boolean, onChange: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val name = getLocalizedItemName(type)
    Row(
        Modifier.fillMaxWidth().padding(start = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemIcon(itemType = type, imageUrl = TileIconProvider.getItemIconUrl(type),
            size = 30.dp, isGray = stock == 0)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(name, color = colors.onSurface, fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.stock_prefix, stock), color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall)
        }
        IconButton(enabled = selected > 0, onClick = { onChange(-1) }) {
            Icon(Icons.Default.Remove, "${stringResource(R.string.prepare_desc_decrease)} $name",
                Modifier.size(26.dp).background(colors.surface, CircleShape).padding(5.dp))
        }
        Text(selected.toString(), color = if (selected > 0) colors.primary else colors.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        IconButton(enabled = canAdd, onClick = { onChange(1) }) {
            Icon(Icons.Default.Add, "${stringResource(R.string.prepare_desc_increase)} $name",
                Modifier.size(26.dp).background(colors.surface, CircleShape).padding(5.dp))
        }
    }
}

@Composable
internal fun LobbyDialogClose(onClick: () -> Unit, description: String) {
    val colors = MaterialTheme.colorScheme
    IconButton(onClick = onClick) {
        Icon(Icons.Default.Close, description, tint = colors.onSurfaceVariant,
            modifier = Modifier.size(32.dp).background(colors.surfaceContainer, CircleShape).padding(8.dp))
    }
}
