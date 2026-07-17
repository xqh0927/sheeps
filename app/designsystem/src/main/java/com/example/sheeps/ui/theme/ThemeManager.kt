package com.example.sheeps.ui.theme

import com.example.sheeps.ui.R
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
    SHUANG_FUN("shuang_fun", "萌趣竞技（默认）"),            // 爽爽蓝+阳光金，轻竞技卡通
    FOREST("forest", "🌿 森林绿"),                           // 淡绿基调，治愈自然
    QING_RI_CHUN("qing_ri_chun", "清日春"),                 // 浅色，宣纸白+朱砂
    MO_YE_GOLD("mo_ye_gold", "墨夜金"),                     // 暗色，墨夜底色+帝王金
    SAKURA("sakura", "🌸 樱花粉"),                          // 粉白基调，甜美女性向
    COSMIC("cosmic", "🌌 星空蓝"),                           // 淡蓝紫基调，优雅神秘
    SUNSET("sunset", "🌅 暖阳橙");                          // 暖黄基调，温暖活力

    companion object {
        fun fromKey(key: String): AppTheme =
            values().find { it.key == key } ?: SHUANG_FUN
    }
}

/**
 * 全局主题管理器（单例）
 * 使用 MMKV 持久化当前主题，切换后立即生效，无需重启 App
 */
object ThemeManager {
    private const val THEME_KEY = "app_current_theme"

    private val _currentTheme = MutableStateFlow(AppTheme.QING_RI_CHUN)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private var kv: MMKV? = null

    /** 在 App 启动时调用，使用 Hilt 提供的 MMKV 实例读取上次保存的主题 */
    fun init(kvInstance: MMKV) {
        kv = kvInstance
        val savedKey =
            kvInstance.decodeString(THEME_KEY, AppTheme.QING_RI_CHUN.key) ?: AppTheme.QING_RI_CHUN.key
        _currentTheme.value = AppTheme.fromKey(savedKey)
    }

    /** 切换到指定主题，并持久化到 MMKV */
    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
        val mmkv = kv ?: MMKV.defaultMMKV()
        mmkv.encode(THEME_KEY, theme.key)
    }

    /** 获取当前主题（同步，用于非 Compose 场景）*/
    fun getCurrentTheme(): AppTheme = _currentTheme.value

    /** 获取 XML 主题的 Style 资源 ID */
    fun getThemeResId(theme: AppTheme = _currentTheme.value): Int {
        return when (theme) {
            AppTheme.MO_YE_GOLD -> R.style.Theme_Sheeps_MoYeGold
            AppTheme.QING_RI_CHUN -> R.style.Theme_Sheeps_QingRiChun
            AppTheme.SAKURA -> R.style.Theme_Sheeps_Sakura
            AppTheme.COSMIC -> R.style.Theme_Sheeps_Cosmic
            AppTheme.SUNSET -> R.style.Theme_Sheeps_Sunset
            AppTheme.FOREST -> R.style.Theme_Sheeps_Forest
            AppTheme.SHUANG_FUN -> R.style.Theme_Sheeps_ShuangFun
        }
    }

    /** 是否为暗色主题 */
    fun isDarkTheme(theme: AppTheme = _currentTheme.value): Boolean =
        theme == AppTheme.MO_YE_GOLD
}
