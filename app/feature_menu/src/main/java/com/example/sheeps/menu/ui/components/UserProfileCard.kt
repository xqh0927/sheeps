package com.example.sheeps.menu.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.ui.R

@Composable
fun UserProfileCard(
    state: MenuViewState,
    onLoginClick: () -> Unit,
    onSignInClick: () -> Unit,
    onNavigateToUserInfo: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = colors.surface) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(
                onClick = if (state.isLoggedIn) onNavigateToUserInfo else onLoginClick
            ).padding(horizontal = 18.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                PlayerPortrait(state.avatarUrl, Modifier.size(60.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (state.isLoggedIn) state.username else stringResource(R.string.user_guest),
                        style = typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    if (state.isLoggedIn && state.phone.isNotBlank()) {
                        Text(state.phone, style = typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.onSurfaceVariant)
            }
            CatalogDivider(18.dp)
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.points_balance_label), style = typography.bodySmall, color = colors.onSurfaceVariant)
                    AnimatedContent(state.points, label = "wallet") { amount ->
                        Text(amount.toString(), style = typography.headlineLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    }
                }
                Button(onClick = if (state.isLoggedIn) onSignInClick else onLoginClick,
                    enabled = !state.isLoggedIn || !state.todaySigned, shape = CircleShape) {
                    Text(stringResource(when {
                        !state.isLoggedIn -> R.string.btn_login_sync
                        state.todaySigned -> R.string.btn_signed_today
                        else -> R.string.btn_sign_today
                    }))
                }
            }
            Row(Modifier.fillMaxWidth().background(colors.primary.copy(alpha = .06f)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatTile(stringResource(R.string.highest_level_label), stringResource(R.string.level_number, state.highestLevelCleared), Modifier.weight(1f))
                VerticalDivider(Modifier.height(32.dp), color = colors.outlineVariant)
                StatTile(stringResource(R.string.streak_label), stringResource(R.string.streak_days, state.signStreak), Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun PlayerPortrait(url: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) {
            AsyncImage(url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.Person, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.fillMaxSize(.56f))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}
