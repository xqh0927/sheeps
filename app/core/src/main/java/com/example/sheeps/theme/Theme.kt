package com.example.sheeps.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color


// =============================================================================
// 秘境消消乐 · 主题配置
// Design System Theme - 支持墨夜金 / 清日春 / 暗黑模式 三套主题
// =============================================================================

// --- 主题一：墨夜金（默认暗色）---
private val MoYeGoldColorScheme = darkColorScheme(
    primary             = Crimson_Primary,
    onPrimary           = Text_OnPrimary,
    primaryContainer    = Crimson_PrimaryContainer,
    onPrimaryContainer  = Gold_Light,
    secondary           = Gold_Primary,
    onSecondary         = Text_Primary_Light,
    secondaryContainer  = Color(0xFF3A2A00),
    onSecondaryContainer = Gold_Light,
    tertiary            = Jade_Success,
    onTertiary          = Text_Primary_Dark,
    tertiaryContainer   = Color(0xFF00391A),
    onTertiaryContainer = Color(0xFF9FF0BC),
    background          = MoYe_Background,
    onBackground        = Text_Primary_Dark,
    surface             = MoYe_Surface,
    onSurface           = Text_Primary_Dark,
    surfaceVariant      = MoYe_SurfaceVariant,
    onSurfaceVariant    = Text_Secondary_Dark,
    surfaceContainer    = MoYe_SurfaceContainer,
    outline             = MoYe_Outline,
    outlineVariant      = MoYe_OutlineVariant,
    error               = Vermilion_Error,
    onError             = Text_Primary_Dark,
    scrim               = Overlay_Dark_Heavy
)

// --- 主题二：清日春（浅色国风）---
private val QingRiChunColorScheme = lightColorScheme(
    primary             = Crimson_Primary,
    onPrimary           = Text_OnPrimary,
    primaryContainer    = Crimson_PrimaryContainerLight,
    onPrimaryContainer  = Crimson_PrimaryDark,
    secondary           = Gold_Dark,
    onSecondary         = Text_OnPrimary,
    secondaryContainer  = Color(0xFFFFF8E0),
    onSecondaryContainer = Color(0xFF4A3100),
    tertiary            = Jade_Success,
    onTertiary          = Text_OnPrimary,
    tertiaryContainer   = Color(0xFFD0F5E0),
    onTertiaryContainer = Color(0xFF003918),
    background          = QingRi_Background,
    onBackground        = Text_Primary_Light,
    surface             = QingRi_Surface,
    onSurface           = Text_Primary_Light,
    surfaceVariant      = QingRi_SurfaceVariant,
    onSurfaceVariant    = Text_Secondary_Light,
    surfaceContainer    = QingRi_SurfaceContainer,
    outline             = QingRi_Outline,
    outlineVariant      = QingRi_OutlineVariant,
    error               = Vermilion_Error,
    onError             = Text_OnPrimary,
    scrim               = Overlay_Dark_Heavy
)

// --- 主题三：暗黑模式（深邃纯黑）---
private val DarkModeColorScheme = darkColorScheme(
    primary             = Crimson_PrimaryLight,
    onPrimary           = Text_OnPrimary,
    primaryContainer    = Crimson_PrimaryDark,
    onPrimaryContainer  = Crimson_PrimaryLight,
    secondary           = Gold_Primary,
    onSecondary         = Text_Primary_Light,
    secondaryContainer  = Color(0xFF2A1E00),
    onSecondaryContainer = Gold_Light,
    tertiary            = Jade_Success,
    onTertiary          = Text_Primary_Dark,
    background          = Color(0xFF080B10),
    onBackground        = Text_Primary_Dark,
    surface             = Color(0xFF0E1219),
    onSurface           = Text_Primary_Dark,
    surfaceVariant      = Color(0xFF161C28),
    onSurfaceVariant    = Text_Secondary_Dark,
    surfaceContainer    = Color(0xFF1A2030),
    outline             = Color(0xFF2E3650),
    outlineVariant      = Color(0xFF212840),
    error               = Vermilion_Error,
    onError             = Text_Primary_Dark,
    scrim               = Color(0xE6000000)
)

// =============================================================================
// SheepsTheme - 根据 ThemeManager 的当前主题动态切换
// =============================================================================
@Composable
fun SheepsTheme(
    // 支持外部强制指定主题（用于预览或测试）
    forceTheme: AppTheme? = null,
    content: @Composable () -> Unit
) {
    val themeState = ThemeManager.currentTheme.collectAsState()
    val activeTheme = forceTheme ?: themeState.value

    val colorScheme = when (activeTheme) {
        AppTheme.MO_YE_GOLD    -> MoYeGoldColorScheme
        AppTheme.QING_RI_CHUN  -> QingRiChunColorScheme
        AppTheme.DARK_MODE     -> DarkModeColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = SheepsShapes,
        content     = content
    )
}


