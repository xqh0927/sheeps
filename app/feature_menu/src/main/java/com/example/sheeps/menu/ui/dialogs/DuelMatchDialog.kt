package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.sheeps.menu.R as MenuR
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.ui.R

@Composable
fun DuelMatchDialog(
    state: MenuViewState,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val leave by rememberUpdatedState(onLeave)
    LaunchedEffect(Unit) { onJoin() }
    DisposableEffect(Unit) { onDispose { leave() } }
    val matched = state.matchStatus == "matched"
    val error = state.matchStatus == "error"
    val opponent = state.matchedOpponentId?.takeLast(4)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.surface) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_match_title), modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    LobbyDialogClose(onDismiss, stringResource(R.string.dialog_match_exit))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    DuelSeat(Modifier.weight(1f), stringResource(MenuR.string.lobby_you),
                        state.phone.takeLast(4), Icons.Outlined.Person, colors.primaryContainer, colors.onPrimaryContainer)
                    Text("VS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
                    DuelSeat(Modifier.weight(1f), stringResource(MenuR.string.lobby_opponent), opponent ?: "···",
                        if (matched) Icons.Outlined.Person else Icons.Outlined.PersonSearch,
                        colors.secondaryContainer, colors.onSecondaryContainer)
                }
                if (!matched && !error) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = colors.primary,
                        trackColor = colors.surfaceContainer)
                }
                Text(when (state.matchStatus) {
                    "searching" -> stringResource(R.string.duel_match_searching, "")
                    "matched" -> stringResource(R.string.duel_match_success, opponent ?: "—")
                    "error" -> stringResource(R.string.duel_match_error)
                    else -> stringResource(R.string.duel_match_pending)
                }, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.titleMedium, color = if (error) colors.error else colors.onSurface)
                if (matched) state.matchedGameId?.let {
                    Text(stringResource(R.string.duel_match_id_label, it.takeLast(10)), fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_match_exit)) }
                    if (error) Button(onClick = onJoin) { Text(stringResource(R.string.dialog_match_re)) }
                }
            }
        }
    }
}

@Composable
private fun DuelSeat(modifier: Modifier, label: String, number: String, icon: ImageVector, background: Color, ink: Color) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.background(colors.surfaceContainer, RoundedCornerShape(19.dp))
        .padding(vertical = 15.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(40.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(23.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        Text(number, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = colors.onSurface)
    }
}
