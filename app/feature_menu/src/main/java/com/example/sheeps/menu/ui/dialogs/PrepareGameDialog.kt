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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.sheeps.data.repository.StageBoards
import com.example.sheeps.data.repository.StageBoard
import java.util.UUID
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
    stageBoards: StageBoards,
    replacedLevel: Int?,
    userId: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit,
    onUpdateItem: (String, Int) -> Unit,
    loadUnlockQuote: suspend (Int) -> com.example.sheeps.data.model.UnlockQuote?,
    onUnlock: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val locked = levelId > state.unlockedLevel
    var retry by remember(levelId) { mutableStateOf(0) }
    var quote by remember(levelId, state.unlockQuoteRevision, retry) { mutableStateOf<com.example.sheeps.data.model.UnlockQuote?>(null) }
    var quoteFailed by remember(levelId, state.unlockQuoteRevision, retry) { mutableStateOf(false) }
    LaunchedEffect(levelId, locked, state.unlockQuoteRevision, retry) {
        if (locked) {
            quote = loadUnlockQuote(levelId)
            quoteFailed = quote == null
        }
    }
    val selectedTypes = state.selectedCarryItems.count { it.value > 0 }
    val runId = rememberSaveable(levelId) { UUID.randomUUID().toString() }
    var board by remember(runId) { mutableStateOf<StageBoard?>(null) }
    LaunchedEffect(runId, locked) {
        if (!locked) board = stageBoards.resolve(levelId, runId, userId)
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
                        val offer = quote
                        when {
                            offer != null -> Text(stringResource(R.string.prepare_locked_desc, offer.cost, offer.current_points),
                                color = colors.onSurface, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(5.dp))
                            quoteFailed -> Column {
                                Text(stringResource(MenuR.string.personal_load_failed))
                                TextButton(onClick = { retry++ }) { Text(stringResource(MenuR.string.personal_retry)) }
                            }
                            else -> LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    } else {
                        if (replacedLevel != null) {
                            Text(stringResource(MenuR.string.stage_replaces_save, replacedLevel),
                                color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(10.dp))
                        }
                        val warnings = buildList {
                            if (board?.hasBlind == true) add(R.string.prepare_blind_warning)
                            if (board?.hasSeals == true) add(R.string.prepare_sealed_warning)
                        }
                        for (warning in warnings) {
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
                    if (!locked) onConfirm(levelId, runId)
                    else quote?.let { offer ->
                        if (offer.current_points >= offer.cost) onUnlock(levelId, offer.cost)
                        else Toaster.show(context.getString(R.string.prepare_locked_insufficient))
                    }
                }, enabled = if (locked) quote != null && !state.unlockInFlight else board != null, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
                    .heightIn(min = 48.dp), shape = CircleShape) {
                    if (!locked && board == null) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(if (locked) Icons.Outlined.Lock else Icons.Default.ArrowForward, null, Modifier.size(18.dp))
                    }
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
