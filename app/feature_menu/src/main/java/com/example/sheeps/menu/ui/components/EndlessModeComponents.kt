package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.sheeps.core.game.TileCardBase
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.ui.R
import com.example.sheeps.ui.components.RemoteImage
import com.example.sheeps.menu.R as MenuR

@Composable
internal fun DuelModeEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    ModeDestination(stringResource(MenuR.string.lobby_duel), colors.secondaryContainer, colors.onSecondaryContainer,
        onClick, modifier, emblem = {
            Icon(Icons.Outlined.CompareArrows, null, modifier = Modifier.size(24.dp))
        })
}

@Composable
fun EndlessModeEntry(
    currentSkin: String,
    isLoggedIn: Boolean,
    onOpenIntro: () -> Unit,
    onLoginClick: (() -> Unit)? = null,
    onShowLeaderboard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    ModeDestination(stringResource(R.string.home_endless_title), colors.primaryContainer, colors.onPrimaryContainer,
        { if (isLoggedIn) onOpenIntro() else onLoginClick?.invoke() }, modifier,
        emblem = { SkinTile(currentSkin, 1, 24.dp) }, onShowLeaderboard = onShowLeaderboard)
}

@Composable
private fun ModeDestination(title: String, background: Color, ink: Color, onClick: () -> Unit,
                            modifier: Modifier, emblem: @Composable () -> Unit, onShowLeaderboard: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier, color = colors.surface, contentColor = colors.onSurface,
        shape = RoundedCornerShape(18.dp)) {
        Box {
            Column(
                Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = background, contentColor = ink, shape = CircleShape) {
                        Box(Modifier.size(33.dp), contentAlignment = Alignment.Center) { emblem() }
                    }
                    Spacer(Modifier.weight(1f))
                    if (onShowLeaderboard == null) {
                        Icon(Icons.Default.ChevronRight, null, tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            if (onShowLeaderboard != null) {
                IconButton(onClick = onShowLeaderboard, modifier = Modifier.size(48.dp).align(Alignment.TopEnd)) {
                    Icon(Icons.Default.Leaderboard, stringResource(R.string.endless_leaderboard_entry),
                        tint = colors.onSurfaceVariant, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

@Composable
fun EndlessIntroDialog(
    currentSkin: String,
    isLoggedIn: Boolean,
    onDismiss: () -> Unit,
    onStart: () -> Unit = {},
    onLoginClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.surface) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth().background(colors.surfaceContainer, RoundedCornerShape(18.dp)).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                    for (type in 1..3) SkinTile(currentSkin, type, 42.dp)
                }
                Text(stringResource(R.string.endless_intro_title), style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.endless_intro_desc), style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_prepare_back)) }
                    Button(onClick = {
                        if (isLoggedIn) { onDismiss(); onStart() } else onLoginClick?.invoke()
                    }) { Text(stringResource(R.string.endless_start)) }
                }
            }
        }
    }
}

@Composable
private fun SkinTile(skin: String, type: Int, size: Dp) {
    TileCardBase(skin = skin, modifier = Modifier.size(size)) {
        RemoteImage(url = TileIconProvider.getTileUrl(skin, type),
            fallbackResId = TileIconProvider.getFallbackResId(LocalContext.current, type),
            contentDescription = null, modifier = Modifier.fillMaxSize().padding(2.dp))
    }
}
