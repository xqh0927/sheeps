package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.menu.R as MenuR
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.components.CatalogDivider
import com.example.sheeps.menu.ui.components.CatalogLink
import com.example.sheeps.menu.ui.components.LanguageSettingsCard
import com.example.sheeps.menu.ui.components.ThemeSettingsCard
import com.example.sheeps.ui.R
import com.example.sheeps.ui.components.SheepsTopAppBar

@Composable
fun SettingsScreen(
    state: MenuViewState,
    onBack: () -> Unit,
    gameMusicEnabled: Boolean,
    onGameMusicChange: (Boolean) -> Unit,
    matchSoundEnabled: Boolean,
    onMatchSoundChange: (Boolean) -> Unit,
    onChangeLanguage: (String) -> Unit,
    onThemeChange: () -> Unit,
    onShowGameGuide: () -> Unit,
    onAccountSecurity: () -> Unit,
    onOpenAgreement: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenPersonalInfoCollection: () -> Unit = {},
    onOpenThirdPartySharing: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(topBar = { SheepsTopAppBar(stringResource(MenuR.string.catalog_settings_title), onBack = onBack) }) { inset ->
        LazyColumn(Modifier.fillMaxSize().padding(inset),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = colors.surface) {
                    Column {
                        ThemeSettingsCard(onThemeChange)
                        CatalogDivider()
                        LanguageSettingsCard(state, onChangeLanguage)
                        CatalogDivider()
                        AudioSettingSwitch(
                            Icons.Default.MusicNote, stringResource(MenuR.string.settings_game_music),
                            gameMusicEnabled, onGameMusicChange
                        )
                        CatalogDivider()
                        AudioSettingSwitch(
                            Icons.AutoMirrored.Filled.VolumeUp, stringResource(MenuR.string.settings_match_sound),
                            matchSoundEnabled, onMatchSoundChange
                        )
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = colors.surface) {
                    CatalogLink(Icons.Default.MenuBook, stringResource(R.string.description_game_guide), onShowGameGuide, colors.tertiary)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(MenuR.string.catalog_privacy), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Surface(shape = RoundedCornerShape(20.dp), color = colors.surface) {
                        Column {
                            CatalogLink(Icons.Default.AccountCircle, stringResource(MenuR.string.account_security), onAccountSecurity)
                            CatalogDivider()
                            CatalogLink(Icons.Default.Description, stringResource(MenuR.string.catalog_terms), onOpenAgreement)
                            CatalogDivider()
                            CatalogLink(Icons.Default.Security, stringResource(MenuR.string.catalog_policy), onOpenPrivacyPolicy)
                            CatalogDivider()
                            CatalogLink(Icons.Default.Assignment, stringResource(MenuR.string.catalog_collection), onOpenPersonalInfoCollection)
                            CatalogDivider()
                            CatalogLink(Icons.Default.Share, stringResource(MenuR.string.catalog_sharing), onOpenThirdPartySharing)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioSettingSwitch(icon: ImageVector, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(32.dp).background(colors.primary.copy(alpha = .13f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = colors.primary)
        }
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}
