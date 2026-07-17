package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.sheeps.ui.R
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.ui.theme.AppTheme
import com.example.sheeps.ui.theme.ThemeManager

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
                color = MaterialTheme.colorScheme.primary,
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
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp),
                fontFamily = FontFamily.Serif
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ⚠️ Flow 收集生命周期：collectAsState 在组合内收集 ThemeManager.currentTheme（StateFlow），
                // 自动在重组/退出组合时取消收集，不会泄漏；其值变化会驱动本卡片重组以刷新选中态。
                val currentTheme = ThemeManager.currentTheme.collectAsState().value
                val themes = listOf(
                    AppTheme.QING_RI_CHUN to stringResource(id = R.string.theme_light),
                    AppTheme.MO_YE_GOLD to stringResource(id = R.string.theme_gold),
                    AppTheme.SHUANG_FUN to "萌趣竞技",
                    AppTheme.FOREST to "森林绿",
                    AppTheme.SAKURA to "樱花粉",
                    AppTheme.COSMIC to "星空蓝",
                    AppTheme.SUNSET to "暖阳橙"
                )
                themes.forEach { (theme, name) ->
                    ThemeOption(
                        name = name, isSelected = currentTheme == theme,
                        onClick = {
                            if (currentTheme != theme) {
                                ThemeManager.setTheme(theme); onThemeChange()
                            }
                        },
                        modifier = Modifier.widthIn(min = 80.dp)
                    )
                }
            }
        }
    }
}

/**
 * 语言选项单元（如 中文 / English / 繁體 / 日本語 / 한국어）。
 * 选中态高亮，点击触发 [onClick] 上抛所选语言 code。
 *
 * @param name 显示名称。
 * @param isSelected 是否为当前语言。
 * @param onClick 选中该语言的回调。
 * @param modifier 外部布局修饰。
 */
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
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
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

/**
 * 主题选项单元（如 晴日春 / 墨夜金 / 樱花粉 …）。
 * 选中态高亮，点击触发 [onClick] 切换主题。
 *
 * @param name 显示名称。
 * @param isSelected 是否为当前主题。
 * @param onClick 选中该主题的回调。
 * @param modifier 外部布局修饰。
 */
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
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
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
