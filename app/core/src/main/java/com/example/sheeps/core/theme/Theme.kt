package com.example.sheeps.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

data class AppColors(
    val iconColor: Color
)

val LightColors = AppColors(
    iconColor = Color(0xFF333333) // 浅色模式下的图标颜色（深灰）
)

val DarkColors = AppColors(
    iconColor = Color(0xFFCCCCCC) // 深色模式下的图标颜色（浅灰）
)

val LocalAppColors = compositionLocalOf { LightColors }

@Composable
fun SheepsTheme(
    content: @Composable () -> Unit
) {
    val themeMode by ThemeManager.themeState.collectAsState()
    
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }
    
    val appColors = if (useDarkTheme) DarkColors else LightColors
    val colorScheme = if (useDarkTheme) {
        darkColorScheme(
            primary = Color(0xFF8D6E63),
            secondary = Color(0xFFD7CCC8),
            background = Color(0xFF121212)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF5D4037),
            secondary = Color(0xFFD7CCC8),
            background = Color(0xFFF5F5F5)
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
