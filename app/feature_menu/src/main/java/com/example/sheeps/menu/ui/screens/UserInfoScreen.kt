package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.components.CatalogDivider
import com.example.sheeps.menu.ui.components.CatalogLink
import com.example.sheeps.menu.ui.components.PlayerPortrait
import com.example.sheeps.menu.ui.dialogs.ChangePasswordDialog
import com.example.sheeps.menu.ui.dialogs.NicknameEditDialog
import com.example.sheeps.menu.R as MenuR
import com.example.sheeps.ui.R
import com.example.sheeps.ui.components.SheepsLoading
import com.example.sheeps.ui.components.SheepsTopAppBar

@Composable
fun UserInfoScreen(
    state: MenuViewState,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onChangeAvatar: () -> Unit,
    onUpdateNickname: (String) -> Unit,
    onChangePassword: (String, String) -> Unit
) {
    var nickname by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    if (nickname) NicknameEditDialog(state.username, { nickname = false }) { nickname = false; onUpdateNickname(it) }
    if (password) ChangePasswordDialog(state.phone, { password = false }) { code, value ->
        password = false
        onChangePassword(code, value)
    }
    Scaffold(topBar = { SheepsTopAppBar(stringResource(MenuR.string.catalog_profile_title), onBack) }) { inset ->
        Box(Modifier.fillMaxSize().padding(inset)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(onClick = onChangeAvatar, enabled = !isLoading, shape = RoundedCornerShape(24.dp),
                            color = colors.background) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box {
                                    PlayerPortrait(state.avatarUrl, Modifier.size(90.dp))
                                    Box(Modifier.align(Alignment.BottomEnd).background(colors.surface, CircleShape).padding(3.dp)
                                        .background(colors.primary, CircleShape).padding(6.dp)) {
                                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(14.dp), tint = colors.onPrimary)
                                    }
                                }
                                Text(stringResource(R.string.click_to_change_avatar), style = MaterialTheme.typography.labelLarge,
                                    color = colors.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.surface) {
                        Column {
                            Row(Modifier.fillMaxWidth().clickable(enabled = !isLoading) { nickname = true }.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.label_nickname), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                Text(state.username.ifBlank { stringResource(MenuR.string.catalog_label_not_set) },
                                    modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant)
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                            CatalogDivider(16.dp)
                            Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.label_phone), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                Text(state.phone, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = colors.surface) {
                        CatalogLink(Icons.Default.Security, stringResource(R.string.dialog_title_change_password), { password = true }, enabled = !isLoading)
                    }
                }
            }
            if (isLoading) {
                Box(Modifier.fillMaxSize().background(colors.scrim.copy(alpha = .35f)).pointerInput(Unit) {
                    awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
                }, contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(16.dp), color = colors.surface) {
                        Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) { SheepsLoading(size = 40.dp) }
                    }
                }
            }
        }
    }
}
