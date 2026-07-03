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
// Design System Theme - 支持森林绿 / 樱花粉 / 星空蓝 / 暖阳橙 / 墨夜金 / 清日春 六套主题
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

// --- 主题四：🌸 樱花粉（女性向甜美）---
private val SakuraColorScheme = lightColorScheme(
    primary             = Sakura_Primary,
    onPrimary           = Text_OnPrimary,
    primaryContainer    = Sakura_SurfaceVariant,
    onPrimaryContainer  = Sakura_PrimaryDark,
    secondary           = Sakura_PrimaryLight,
    onSecondary         = Text_OnPrimary,
    secondaryContainer  = Sakura_SurfaceContainer,
    onSecondaryContainer = Sakura_PrimaryDark,
    tertiary            = Jade_Success,
    onTertiary          = Text_OnPrimary,
    tertiaryContainer   = Color(0xFFD0F5E0),
    onTertiaryContainer = Color(0xFF003918),
    background          = Sakura_Background,
    onBackground        = Text_Primary_Light,
    surface             = Sakura_Surface,
    onSurface           = Text_Primary_Light,
    surfaceVariant      = Sakura_SurfaceVariant,
    onSurfaceVariant    = Sakura_Primary,
    surfaceContainer    = Sakura_SurfaceContainer,
    outline             = Sakura_Outline,
    outlineVariant      = Sakura_OutlineVariant,
    error               = Vermilion_Error,
    onError             = Text_OnPrimary,
    scrim               = Overlay_Dark_Heavy
)

// --- 主题五：🌌 星空蓝（优雅神秘）---
private val CosmicColorScheme = lightColorScheme(
    primary             = Cosmic_Primary,
    onPrimary           = Text_OnPrimary,
    primaryContainer    = Cosmic_SurfaceVariant,
    onPrimaryContainer  = Cosmic_PrimaryDark,
    secondary           = Cosmic_PrimaryLight,
    onSecondary         = Text_OnPrimary,
    secondaryContainer  = Cosmic_SurfaceContainer,
    onSecondaryContainer = Cosmic_PrimaryDark,
    tertiary            = Jade_Success,
    onTertiary          = Text_OnPrimary,
    tertiaryContainer   = Color(0xFFD0F5E0),
    onTertiaryContainer = Color(0xFF003918),
    background          = Cosmic_Background,
    onBackground        = Text_Primary_Light,
    surface             = Cosmic_Surface,
    onSurface           = Text_Primary_Light,
    surfaceVariant      = Cosmic_SurfaceVariant,
    onSurfaceVariant    = Cosmic_Primary,
    surfaceContainer    = Cosmic_SurfaceContainer,
    outline             = Cosmic_Outline,
    outlineVariant      = Cosmic_OutlineVariant,
    error               = Vermilion_Error,
    onError             = Text_OnPrimary,
    scrim               = Overlay_Dark_Heavy
)

// --- 主题六：🌅 暖阳橙（温暖活力）---
private val SunsetColorScheme = lightColorScheme(
    primary             = Sunset_Primary,
    onPrimary           = Text_OnPrimary,
    primaryContainer    = Sunset_SurfaceVariant,
    onPrimaryContainer  = Sunset_PrimaryDark,
    secondary           = Sunset_PrimaryLight,
    onSecondary         = Text_OnPrimary,
    secondaryContainer  = Sunset_SurfaceContainer,
    onSecondaryContainer = Sunset_PrimaryDark,
    tertiary            = Jade_Success,
    onTertiary          = Text_OnPrimary,
    tertiaryContainer   = Color(0xFFFFF3E0),
    onTertiaryContainer = Color(0xFF003918),
    background          = Sunset_Background,
    onBackground        = Text_Primary_Light,
    surface             = Sunset_Surface,
    onSurface           = Text_Primary_Light,
    surfaceVariant      = Sunset_SurfaceVariant,
    onSurfaceVariant    = Sunset_Primary,
    surfaceContainer    = Sunset_SurfaceContainer,
    outline             = Sunset_Outline,
    outlineVariant      = Sunset_OutlineVariant,
    error               = Vermilion_Error,
    onError             = Text_OnPrimary,
    scrim               = Overlay_Dark_Heavy
)

// --- 主题七：🌿 森林绿（治愈自然）---
private val ForestColorScheme = lightColorScheme(
    primary             = Forest_Primary,
    onPrimary           = Text_OnPrimary,
    primaryContainer    = Forest_SurfaceVariant,
    onPrimaryContainer  = Forest_PrimaryDark,
    secondary           = Forest_PrimaryLight,
    onSecondary         = Text_OnPrimary,
    secondaryContainer  = Forest_SurfaceContainer,
    onSecondaryContainer = Forest_PrimaryDark,
    tertiary            = Jade_Success,
    onTertiary          = Text_OnPrimary,
    tertiaryContainer   = Color(0xFFD0F5E0),
    onTertiaryContainer = Color(0xFF003918),
    background          = Forest_Background,
    onBackground        = Text_Primary_Light,
    surface             = Forest_Surface,
    onSurface           = Text_Primary_Light,
    surfaceVariant      = Forest_SurfaceVariant,
    onSurfaceVariant    = Forest_Primary,
    surfaceContainer    = Forest_SurfaceContainer,
    outline             = Forest_Outline,
    outlineVariant      = Forest_OutlineVariant,
    error               = Vermilion_Error,
    onError             = Text_OnPrimary,
    scrim               = Overlay_Dark_Heavy
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
        AppTheme.SAKURA        -> SakuraColorScheme
        AppTheme.COSMIC        -> CosmicColorScheme
        AppTheme.SUNSET        -> SunsetColorScheme
        AppTheme.FOREST        -> ForestColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = SheepsShapes,
        content     = content
    )
}


