package com.example.sheeps.menu

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.sheeps.core.base.BaseActivity
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.dialogs.GameGuideDialog
import com.example.sheeps.menu.ui.screens.SettingsScreen
import com.example.sheeps.theme.SheepsTheme
import com.example.sheeps.theme.ThemeManager
import com.tencent.mmkv.MMKV
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 全屏设置 Activity。
 * 包含语言切换、主题切换、玩法说明等功能。
 */
@Route(path = "/menu/settings")
@AndroidEntryPoint
class SettingsActivity : BaseActivity() {

    @Inject lateinit var prefs: UserPreferences

    override fun initView(savedInstanceState: Bundle?) {
        setContent {
            SheepsTheme {
                BackHandler { finish() }
                var showGameGuide by remember { mutableStateOf(false) }

                // 构建简化版 MenuViewState（仅提供 SettingsScreen 所需字段）
                val settingsState = remember {
                    MenuViewState(language = prefs.getLanguage())
                }

                // 游戏指南弹窗
                if (showGameGuide) {
                    GameGuideDialog(onDismiss = { showGameGuide = false })
                }

                SettingsScreen(
                    state = settingsState,
                    onBack = { finish() },
                    onChangeLanguage = { lang ->
                        prefs.setLanguage(lang)
                        // 标记语言已变更，MenuActivity 返回时通过 onResume 感知
                        MMKV.defaultMMKV().encode("language_changed_in_settings", true)
                        finish()
                    },
                    onThemeChange = {
                        // 标记主题已变更，MenuActivity onResume 检测即可
                        // ThemeManager 是全局 StateFlow，SheepsTheme 自动响应，无需 recreate
                        MMKV.defaultMMKV().encode("theme_changed_in_settings", true)
                    },
                    onShowGameGuide = { showGameGuide = true }
                )
            }
        }
    }

    override fun initData() {}
}
