package com.example.sheeps.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color

// =============================================================================
// 秘境消消乐 · 骨架化动态主题系统
// Design System Theme - 支持骨架颜色配置与动态色盘推导，降低新增主题成本
// =============================================================================

/**
 * 主题核心骨架颜色配置契约类。
 * 允许只传入 5 个核心基础配置自动推导生成完整 M3 配色；
 * 也提供全槽位可选覆盖参数，以 100% 还原已有主题的定制细节设计。
 */
data class AppThemeColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val isDark: Boolean = false,
    
    // 全槽位个性化精细覆盖参数（为 null 时自动推导）
    val onPrimary: Color? = null,
    val primaryContainer: Color? = null,
    val onPrimaryContainer: Color? = null,
    val onSecondary: Color? = null,
    val secondaryContainer: Color? = null,
    val onSecondaryContainer: Color? = null,
    val tertiary: Color? = null,
    val onTertiary: Color? = null,
    val tertiaryContainer: Color? = null,
    val onTertiaryContainer: Color? = null,
    val onBackground: Color? = null,
    val onSurface: Color? = null,
    val surfaceVariant: Color? = null,
    val onSurfaceVariant: Color? = null,
    val surfaceContainer: Color? = null,
    val outline: Color? = null,
    val outlineVariant: Color? = null,
    val error: Color? = null,
    val onError: Color? = null
)

/**
 * 根据核心骨架与自定义覆盖颜色，动态计算组装出完整的 M3 ColorScheme。
 */
fun AppThemeColors.toColorScheme(): ColorScheme {
    val textPrimary = if (isDark) Text_Primary_Dark else Text_Primary_Light
    val textSecondary = if (isDark) Text_Secondary_Dark else Text_Secondary_Light
    val outlineColor = if (isDark) MoYe_Outline else QingRi_Outline

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary ?: Text_OnPrimary,
            primaryContainer = primaryContainer ?: Crimson_PrimaryContainer,
            onPrimaryContainer = onPrimaryContainer ?: Gold_Light,
            secondary = secondary,
            onSecondary = onSecondary ?: Text_Primary_Light,
            secondaryContainer = secondaryContainer ?: Color(0xFF3A2A00),
            onSecondaryContainer = onSecondaryContainer ?: Gold_Light,
            tertiary = tertiary ?: Jade_Success,
            onTertiary = onTertiary ?: Text_Primary_Dark,
            tertiaryContainer = tertiaryContainer ?: Color(0xFF00391A),
            onTertiaryContainer = onTertiaryContainer ?: Color(0xFF9FF0BC),
            background = background,
            onBackground = onBackground ?: textPrimary,
            surface = surface,
            onSurface = onSurface ?: textPrimary,
            surfaceVariant = surfaceVariant ?: MoYe_SurfaceVariant,
            onSurfaceVariant = onSurfaceVariant ?: textSecondary,
            surfaceContainer = surfaceContainer ?: MoYe_SurfaceContainer,
            outline = outline ?: outlineColor,
            outlineVariant = outlineVariant ?: MoYe_OutlineVariant,
            error = error ?: Vermilion_Error,
            onError = onError ?: Text_Primary_Dark,
            scrim = Overlay_Dark_Heavy
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary ?: Text_OnPrimary,
            primaryContainer = primaryContainer ?: Crimson_PrimaryContainerLight,
            onPrimaryContainer = onPrimaryContainer ?: Crimson_PrimaryDark,
            secondary = secondary,
            onSecondary = onSecondary ?: Text_OnPrimary,
            secondaryContainer = secondaryContainer ?: Color(0xFFFFF8E0),
            onSecondaryContainer = onSecondaryContainer ?: Color(0xFF4A3100),
            tertiary = tertiary ?: Jade_Success,
            onTertiary = onTertiary ?: Text_OnPrimary,
            tertiaryContainer = tertiaryContainer ?: Color(0xFFD0F5E0),
            onTertiaryContainer = onTertiaryContainer ?: Color(0xFF003918),
            background = background,
            onBackground = onBackground ?: textPrimary,
            surface = surface,
            onSurface = onSurface ?: textPrimary,
            surfaceVariant = surfaceVariant ?: QingRi_SurfaceVariant,
            onSurfaceVariant = onSurfaceVariant ?: textSecondary,
            surfaceContainer = surfaceContainer ?: QingRi_SurfaceContainer,
            outline = outline ?: outlineColor,
            outlineVariant = outlineVariant ?: QingRi_OutlineVariant,
            error = error ?: Vermilion_Error,
            onError = onError ?: Text_OnPrimary,
            scrim = Overlay_Dark_Heavy
        )
    }
}

// =============================================================================
// 精准主题数据声明（高保真还原老主题，保留前代完美视觉）
// =============================================================================

// --- 主题一：墨夜金（默认暗色）---
private val MoYeGoldColors = AppThemeColors(
    primary = Crimson_Primary,
    onPrimary = Text_OnPrimary,
    primaryContainer = Crimson_PrimaryContainer,
    onPrimaryContainer = Gold_Light,
    secondary = Gold_Primary,
    onSecondary = Text_Primary_Light,
    secondaryContainer = Color(0xFF3A2A00),
    onSecondaryContainer = Gold_Light,
    tertiary = Jade_Success,
    onTertiary = Text_Primary_Dark,
    tertiaryContainer = Color(0xFF00391A),
    onTertiaryContainer = Color(0xFF9FF0BC),
    background = MoYe_Background,
    onBackground = Text_Primary_Dark,
    surface = MoYe_Surface,
    onSurface = Text_Primary_Dark,
    surfaceVariant = MoYe_SurfaceVariant,
    onSurfaceVariant = Text_Secondary_Dark,
    surfaceContainer = MoYe_SurfaceContainer,
    outline = MoYe_Outline,
    outlineVariant = MoYe_OutlineVariant,
    error = Vermilion_Error,
    onError = Text_Primary_Dark
)

// --- 主题二：清日春（浅色国风）---
private val QingRiChunColors = AppThemeColors(
    primary = Crimson_Primary,
    onPrimary = Text_OnPrimary,
    primaryContainer = Crimson_PrimaryContainerLight,
    onPrimaryContainer = Crimson_PrimaryDark,
    secondary = Gold_Dark,
    onSecondary = Text_OnPrimary,
    secondaryContainer = Color(0xFFFFF8E0),
    onSecondaryContainer = Color(0xFF4A3100),
    tertiary = Jade_Success,
    onTertiary = Text_OnPrimary,
    tertiaryContainer = Color(0xFFD0F5E0),
    onTertiaryContainer = Color(0xFF003918),
    background = QingRi_Background,
    onBackground = Text_Primary_Light,
    surface = QingRi_Surface,
    onSurface = Text_Primary_Light,
    surfaceVariant = QingRi_SurfaceVariant,
    onSurfaceVariant = Text_Secondary_Light,
    surfaceContainer = QingRi_SurfaceContainer,
    outline = QingRi_Outline,
    outlineVariant = QingRi_OutlineVariant,
    error = Vermilion_Error,
    onError = Text_OnPrimary
)

// --- 主题三：🌸 樱花粉（女性向甜美）---
private val SakuraColors = AppThemeColors(
    primary = Sakura_Primary,
    onPrimary = Text_OnPrimary,
    primaryContainer = Sakura_SurfaceVariant,
    onPrimaryContainer = Sakura_PrimaryDark,
    secondary = Sakura_PrimaryLight,
    onSecondary = Text_OnPrimary,
    secondaryContainer = Sakura_SurfaceContainer,
    onSecondaryContainer = Sakura_PrimaryDark,
    tertiary = Jade_Success,
    onTertiary = Text_OnPrimary,
    tertiaryContainer = Color(0xFFD0F5E0),
    onTertiaryContainer = Color(0xFF003918),
    background = Sakura_Background,
    onBackground = Text_Primary_Light,
    surface = Sakura_Surface,
    onSurface = Text_Primary_Light,
    surfaceVariant = Sakura_SurfaceVariant,
    onSurfaceVariant = Sakura_Primary,
    surfaceContainer = Sakura_SurfaceContainer,
    outline = Sakura_Outline,
    outlineVariant = Sakura_OutlineVariant,
    error = Vermilion_Error,
    onError = Text_OnPrimary
)

// --- 主题四：🌌 星空蓝（优雅神秘）---
private val CosmicColors = AppThemeColors(
    primary = Cosmic_Primary,
    onPrimary = Text_OnPrimary,
    primaryContainer = Cosmic_SurfaceVariant,
    onPrimaryContainer = Cosmic_PrimaryDark,
    secondary = Cosmic_PrimaryLight,
    onSecondary = Text_OnPrimary,
    secondaryContainer = Cosmic_SurfaceContainer,
    onSecondaryContainer = Cosmic_PrimaryDark,
    tertiary = Jade_Success,
    onTertiary = Text_OnPrimary,
    tertiaryContainer = Color(0xFFD0F5E0),
    onTertiaryContainer = Color(0xFF003918),
    background = Cosmic_Background,
    onBackground = Text_Primary_Light,
    surface = Cosmic_Surface,
    onSurface = Text_Primary_Light,
    surfaceVariant = Cosmic_SurfaceVariant,
    onSurfaceVariant = Cosmic_Primary,
    surfaceContainer = Cosmic_SurfaceContainer,
    outline = Cosmic_Outline,
    outlineVariant = Cosmic_OutlineVariant,
    error = Vermilion_Error,
    onError = Text_OnPrimary
)

// --- 主题五：🌅 暖阳橙（温暖活力）---
private val SunsetColors = AppThemeColors(
    primary = Sunset_Primary,
    onPrimary = Text_OnPrimary,
    primaryContainer = Sunset_SurfaceVariant,
    onPrimaryContainer = Sunset_PrimaryDark,
    secondary = Sunset_PrimaryLight,
    onSecondary = Text_OnPrimary,
    secondaryContainer = Sunset_SurfaceContainer,
    onSecondaryContainer = Sunset_PrimaryDark,
    tertiary = Jade_Success,
    onTertiary = Text_OnPrimary,
    tertiaryContainer = Color(0xFFFFF3E0),
    onTertiaryContainer = Color(0xFF003918),
    background = Sunset_Background,
    onBackground = Text_Primary_Light,
    surface = Sunset_Surface,
    onSurface = Text_Primary_Light,
    surfaceVariant = Sunset_SurfaceVariant,
    onSurfaceVariant = Sunset_Primary,
    surfaceContainer = Sunset_SurfaceContainer,
    outline = Sunset_Outline,
    outlineVariant = Sunset_OutlineVariant,
    error = Vermilion_Error,
    onError = Text_OnPrimary
)

// --- 主题六：🌿 森林绿（治愈自然）---
private val ForestColors = AppThemeColors(
    primary = Forest_Primary,
    onPrimary = Text_OnPrimary,
    primaryContainer = Forest_SurfaceVariant,
    onPrimaryContainer = Forest_PrimaryDark,
    secondary = Forest_PrimaryLight,
    onSecondary = Text_OnPrimary,
    secondaryContainer = Forest_SurfaceContainer,
    onSecondaryContainer = Forest_PrimaryDark,
    tertiary = Jade_Success,
    onTertiary = Text_OnPrimary,
    tertiaryContainer = Color(0xFFD0F5E0),
    onTertiaryContainer = Color(0xFF003918),
    background = Forest_Background,
    onBackground = Text_Primary_Light,
    surface = Forest_Surface,
    onSurface = Text_Primary_Light,
    surfaceVariant = Forest_SurfaceVariant,
    onSurfaceVariant = Forest_Primary,
    surfaceContainer = Forest_SurfaceContainer,
    outline = Forest_Outline,
    outlineVariant = Forest_OutlineVariant,
    error = Vermilion_Error,
    onError = Text_OnPrimary
)

// --- 主题七：🐑 萌趣竞技（高饱和卡通）---
private val ShuangFunColors = AppThemeColors(
    primary = ShuangFun_Primary,
    onPrimary = Text_OnPrimary,
    primaryContainer = ShuangFun_SurfaceVariant,
    onPrimaryContainer = ShuangFun_PrimaryDark,
    secondary = ShuangFun_Gold_Subtle,
    onSecondary = Color(0xFF5D4037),
    secondaryContainer = Color(0xFFFFF8E1),
    onSecondaryContainer = Color(0xFF5D4037),
    tertiary = Color(0xFFFF5252),
    onTertiary = Text_OnPrimary,
    tertiaryContainer = Color(0xFFFFE0E0),
    onTertiaryContainer = Color(0xFF7F0000),
    background = ShuangFun_Background,
    onBackground = Text_Primary_Light,
    surface = ShuangFun_Surface,
    onSurface = Text_Primary_Light,
    surfaceVariant = ShuangFun_SurfaceVariant,
    onSurfaceVariant = ShuangFun_PrimaryDark,
    surfaceContainer = ShuangFun_SurfaceContainer,
    outline = ShuangFun_Outline,
    outlineVariant = ShuangFun_OutlineVariant,
    error = Vermilion_Error,
    onError = Text_OnPrimary
)

// =============================================================================
// SheepsTheme - 根据 ThemeManager 的当前主题动态切换并推导配色
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
        AppTheme.MO_YE_GOLD    -> MoYeGoldColors.toColorScheme()
        AppTheme.QING_RI_CHUN  -> QingRiChunColors.toColorScheme()
        AppTheme.SAKURA        -> SakuraColors.toColorScheme()
        AppTheme.COSMIC        -> CosmicColors.toColorScheme()
        AppTheme.SUNSET        -> SunsetColors.toColorScheme()
        AppTheme.FOREST        -> ForestColors.toColorScheme()
        AppTheme.SHUANG_FUN    -> ShuangFunColors.toColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = SheepsShapes,
        content     = content
    )
}
