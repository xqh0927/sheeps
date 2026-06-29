package com.example.sheeps.core.theme

import com.blankj.utilcode.util.SPStaticUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}

object ThemeManager {
    private const val KEY_THEME_MODE = "key_theme_mode"

    private val _themeState = MutableStateFlow(getSavedThemeMode())
    val themeState = _themeState.asStateFlow()

    var themeMode: ThemeMode
        get() = getSavedThemeMode()
        set(value) {
            SPStaticUtils.put(KEY_THEME_MODE, value.name)
            _themeState.value = value
        }

    private fun getSavedThemeMode(): ThemeMode {
        val name = SPStaticUtils.getString(KEY_THEME_MODE, ThemeMode.FOLLOW_SYSTEM.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.FOLLOW_SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.FOLLOW_SYSTEM
        }
    }
}
