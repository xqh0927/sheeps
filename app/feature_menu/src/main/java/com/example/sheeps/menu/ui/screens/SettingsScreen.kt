package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.components.LanguageSettingsCard
import com.example.sheeps.menu.ui.components.ThemeSettingsCard
import com.example.sheeps.ui.components.SheepsTopAppBar
import com.example.sheeps.core.R
import androidx.compose.ui.res.stringResource

/**
 * 全屏设置页面
 *
 * @param state 界面状态数据
 * @param onBack 返回回调
 * @param onChangeLanguage 语言切换回调
 * @param onThemeChange 主题切换回调
 * @param onShowGameGuide 显示游戏指南回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: MenuViewState,
    onBack: () -> Unit,
    onChangeLanguage: (String) -> Unit,
    onThemeChange: () -> Unit,
    onShowGameGuide: () -> Unit
) {
    Scaffold(
        topBar = {
            SheepsTopAppBar(title = stringResource(R.string.settings_title), onBack = onBack)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 语言设置
            item {
                LanguageSettingsCard(
                    state = state,
                    onChangeLanguage = onChangeLanguage
                )
            }

            // 主题设置
            item {
                ThemeSettingsCard(
                    onThemeChange = onThemeChange
                )
            }

            // 玩法说明按钮
            item {
                Button(
                    onClick = onShowGameGuide,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(id = R.string.description_game_guide),
                        color = Color.White
                    )
                }
            }
        }
    }
}
