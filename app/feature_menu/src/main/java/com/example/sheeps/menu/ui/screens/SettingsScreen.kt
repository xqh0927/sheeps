package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.components.LanguageSettingsCard
import com.example.sheeps.menu.ui.components.ThemeSettingsCard
import com.example.sheeps.ui.components.SheepsTopAppBar
import com.example.sheeps.ui.R
import androidx.compose.ui.res.stringResource

/**
 * 全屏设置页面
 *
 * @param state 界面状态数据
 * @param onBack 返回回调
 * @param onChangeLanguage 语言切换回调
 * @param onThemeChange 主题切换回调
 * @param onShowGameGuide 显示游戏指南回调
 * @param onOpenAgreement 打开用户协议回调
 * @param onOpenPrivacyPolicy 打开隐私协议回调
 * @param onOpenPersonalInfoCollection 打开个人信息收集清单回调
 * @param onOpenThirdPartySharing 打开第三方信息共享清单回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: MenuViewState,
    onBack: () -> Unit,
    onChangeLanguage: (String) -> Unit,
    onThemeChange: () -> Unit,
    onShowGameGuide: () -> Unit,
    onOpenAgreement: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenPersonalInfoCollection: () -> Unit = {},
    onOpenThirdPartySharing: () -> Unit = {}
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

            // ===== 关于与帮助 =====
            item {
                Text(
                    text = "关于与帮助",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.MenuBook,
                    title = stringResource(id = R.string.description_game_guide),
                    onClick = onShowGameGuide
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.Description,
                    title = stringResource(id = R.string.settings_user_agreement),
                    onClick = onOpenAgreement
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.Security,
                    title = stringResource(id = R.string.settings_privacy_policy),
                    onClick = onOpenPrivacyPolicy
                )
            }

            // ===== 法律信息 =====
            item {
                Text(
                    text = "法律信息",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.Assignment,
                    title = stringResource(id = R.string.settings_personal_info_collection),
                    onClick = onOpenPersonalInfoCollection
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Default.Share,
                    title = stringResource(id = R.string.settings_third_party_sharing),
                    onClick = onOpenThirdPartySharing
                )
            }
        }
    }
}

/**
 * 设置页列表项（Material 3 风格：左侧圆形图标容器 + 标题 + 右侧箭头）
 */
@Composable
private fun SettingsListItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
