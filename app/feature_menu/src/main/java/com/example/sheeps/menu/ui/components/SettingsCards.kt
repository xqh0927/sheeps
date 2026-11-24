package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.menu.R as MenuR
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.ui.R
import com.example.sheeps.ui.theme.*

@Composable
fun LanguageSettingsCard(state: MenuViewState, onChangeLanguage: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val languages = listOf("" to R.string.lang_zh, "en" to R.string.lang_en, "tw" to R.string.lang_tw,
        "ja" to R.string.lang_ja, "ko" to R.string.lang_ko)
    val selected = languages.firstOrNull { it.first == state.language } ?: languages.first()
    val title = stringResource(R.string.language_settings)
    CatalogLink(Icons.Default.Language, title, { open = true }, tint = MaterialTheme.colorScheme.secondary,
        detail = stringResource(selected.second))
    if (open) CatalogSheet(title, { open = false }) {
        Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
            languages.forEachIndexed { index, (code, label) ->
                ChoiceRow(stringResource(label), selected.first == code, onClick = {
                    open = false
                    if (code != state.language) onChangeLanguage(code)
                })
                if (index < languages.lastIndex) CatalogDivider(0.dp)
            }
        }
    }
}

private data class ThemeSample(val theme: AppTheme, val name: Int, val base: Color, val accent: Color)

private val samples = listOf(
    ThemeSample(AppTheme.SYSTEM_GLASS, R.string.theme_system_glass, Color(0xFFEAF3FF), Color(0xFF1475DF)),
    ThemeSample(AppTheme.POLAR, R.string.theme_polar, Color(0xFF132530), Color(0xFF78E3D2)),
    ThemeSample(AppTheme.QING_RI_CHUN, R.string.theme_light, QingRi_Background, Crimson_Primary),
    ThemeSample(AppTheme.MO_YE_GOLD, R.string.theme_gold, MoYe_Background, Gold_Primary),
    ThemeSample(AppTheme.SHUANG_FUN, MenuR.string.catalog_fun, ShuangFun_Background, ShuangFun_Primary),
    ThemeSample(AppTheme.FOREST, MenuR.string.catalog_forest, Forest_Background, Forest_Primary),
    ThemeSample(AppTheme.SAKURA, MenuR.string.catalog_sakura, Sakura_Background, Sakura_Primary),
    ThemeSample(AppTheme.COSMIC, MenuR.string.catalog_cosmic, Cosmic_Background, Cosmic_Primary),
    ThemeSample(AppTheme.SUNSET, MenuR.string.catalog_sunset, Sunset_Background, Sunset_Primary)
)

@Composable
fun ThemeSettingsCard(onThemeChange: () -> Unit) {
    val current by ThemeManager.currentTheme.collectAsState()
    var open by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(R.string.theme_settings_title)
    val summary = if (current == AppTheme.SYSTEM_GLASS) R.string.glass_system_summary else samples.first { it.theme == current }.name
    CatalogLink(Icons.Default.Palette, title, { open = true }, detail = stringResource(summary))
    if (open) CatalogSheet(title, { open = false }) {
        Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
            samples.forEachIndexed { index, sample ->
                ChoiceRow(stringResource(sample.name), sample.theme == current, onClick = {
                    open = false
                    if (sample.theme != current) {
                        ThemeManager.setTheme(sample.theme)
                        onThemeChange()
                    }
                }) {
                    Canvas(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(sample.base)) {
                        drawCircle(sample.accent.copy(alpha = .42f), size.width * .62f, Offset(size.width * .92f, size.height * .13f))
                        drawCircle(sample.accent, size.width * .58f, Offset(size.width * .20f, size.height * .96f))
                    }
                }
                if (index < samples.lastIndex) CatalogDivider(48.dp)
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, active: Boolean, onClick: () -> Unit, swatch: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().selectable(active, role = Role.RadioButton, onClick = onClick)
        .heightIn(min = 52.dp).padding(horizontal = 2.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        swatch?.invoke()
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
        if (active) Box(Modifier.size(23.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
        }
    }
}
