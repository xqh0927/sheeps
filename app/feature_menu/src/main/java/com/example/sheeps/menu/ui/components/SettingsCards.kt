package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.core.R
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.theme.AppTheme
import com.example.sheeps.theme.CrimsonRed
import com.example.sheeps.theme.ThemeManager

/**
 * 语言设置卡片组件
 *
 * @param state 界面状态数据
 * @param onChangeLanguage 语言切换回调
 */
@Composable
fun LanguageSettingsCard(
    state: MenuViewState,
    onChangeLanguage: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.language_settings),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = CrimsonRed,
                modifier = Modifier.padding(bottom = 12.dp),
                fontFamily = FontFamily.Serif
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val languages = listOf(
                    "" to stringResource(id = R.string.lang_zh),
                    "en" to stringResource(id = R.string.lang_en),
                    "tw" to stringResource(id = R.string.lang_tw),
                    "ja" to stringResource(id = R.string.lang_ja),
                    "ko" to stringResource(id = R.string.lang_ko)
                )
                
                languages.forEach { (code, name) ->
                    LanguageOption(
                        name = name,
                        isSelected = state.language == code,
                        onClick = { onChangeLanguage(code) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 主题设置卡片组件
 *
 * @param onThemeChange 主题切换后的回调
 */
@Composable
fun ThemeSettingsCard(
    onThemeChange: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.theme_settings_title),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = CrimsonRed,
                modifier = Modifier.padding(bottom = 12.dp),
                fontFamily = FontFamily.Serif
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val currentTheme = ThemeManager.currentTheme.collectAsState().value
                val themes = listOf(
                    AppTheme.QING_RI_CHUN to stringResource(id = R.string.theme_light),
                    AppTheme.MO_YE_GOLD to stringResource(id = R.string.theme_gold),
                    AppTheme.DARK_MODE to stringResource(id = R.string.theme_dark)
                )
                
                themes.forEach { (theme, name) ->
                    ThemeOption(
                        name = name,
                        isSelected = currentTheme == theme,
                        onClick = {
                            if (currentTheme != theme) {
                                ThemeManager.setTheme(theme)
                                onThemeChange()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) CrimsonRed else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ThemeOption(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) CrimsonRed else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
