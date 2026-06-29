package com.example.sheeps.theme

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// =============================================================================
// 秘境消消乐 · 主题系统
// Theme Management System - 基于 MMKV 持久化，支持运行时切换
// =============================================================================

/**
 * 应用支持的主题枚举
 */
enum class AppTheme(val key: String, val displayName: String) {
    MO_YE_GOLD("mo_ye_gold", "墨夜金（默认）"),    // 暗色，墨夜底色+帝王金
    QING_RI_CHUN("qing_ri_chun", "清日春"),           // 浅色，宣纸白+朱砂
    DARK_MODE("dark_mode", "暗黑模式");               // 纯暗色

    companion object {
        fun fromKey(key: String): AppTheme =
            values().find { it.key == key } ?: MO_YE_GOLD
    }
}

/**
 * 全局主题管理器（单例）
 * 使用 MMKV 持久化当前主题，切换后立即生效，无需重启 App
 */
object ThemeManager {
    private const val THEME_KEY = "app_current_theme"

    private val _currentTheme = MutableStateFlow(AppTheme.MO_YE_GOLD)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    /** 在 App 启动时调用，从 MMKV 读取上次保存的主题 */
    fun init() {
        val kv = MMKV.defaultMMKV()
        val savedKey = kv.decodeString(THEME_KEY, AppTheme.MO_YE_GOLD.key) ?: AppTheme.MO_YE_GOLD.key
        _currentTheme.value = AppTheme.fromKey(savedKey)
    }

    /** 切换到指定主题，并持久化到 MMKV */
    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
        val kv = MMKV.defaultMMKV()
        kv.encode(THEME_KEY, theme.key)
    }

    /** 获取当前主题（同步，用于非 Compose 场景）*/
    fun getCurrentTheme(): AppTheme = _currentTheme.value

    /** 是否为暗色主题 */
    fun isDarkTheme(theme: AppTheme = _currentTheme.value): Boolean =
        theme == AppTheme.MO_YE_GOLD || theme == AppTheme.DARK_MODE
}
