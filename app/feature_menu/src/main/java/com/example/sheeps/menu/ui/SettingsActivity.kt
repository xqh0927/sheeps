package com.example.sheeps.menu.ui


import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.lib_base.base.BaseActivity
import com.example.sheeps.lib_base.router.RouterPath
import com.example.sheeps.lib_network.AppConfig
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.dialogs.GameGuideDialog
import com.example.sheeps.menu.ui.screens.SettingsScreen
import com.example.sheeps.ui.theme.SheepsTheme
import com.tencent.mmkv.MMKV
import com.therouter.TheRouter
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 设置中心全屏 Activity。
 * 包含语言切换、主题切换、玩法说明等功能。
 *
 * 生命周期与内存说明：
 * - 注入的 prefs 为 Hilt 单例，Activity 销毁即释放。
 * - 语言/主题变更通过 MMKV 静态标记（"language_changed_in_settings"/"theme_changed_in_settings"）通知 MenuActivity
 */
@Route(path = RouterPath.Menu.SETTINGS)
@AndroidEntryPoint
class SettingsActivity : BaseActivity() {

    @Inject
    lateinit var prefs: UserPreferences

    @Inject
    lateinit var kv: MMKV

    override fun initView(savedInstanceState: Bundle?) {
        setContent {
            SheepsTheme {
                BackHandler { finish() }
                var showGameGuide by remember { mutableStateOf(false) }

                // 构建简化版 MenuViewState（仅提供 SettingsScreen 所需字段）
                // 线程边界：remember 在组合内创建，随组合重建/销毁；不跨 Configuration 长期持有。
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
                        kv.encode("language_changed_in_settings", true)
                        finish()
                    },
                    onThemeChange = {
                        // 标记主题已变更，MenuActivity onResume 检测即可
                        // ThemeManager 是全局 StateFlow，SheepsTheme 自动响应，无需 recreate
                        kv.encode("theme_changed_in_settings", true)
                    },
                    onShowGameGuide = { showGameGuide = true },
                    onOpenAgreement = { openH5(AppConfig.BASE_URL + "agreement.html", "用户协议") },
                    onOpenPrivacyPolicy = {
                        openH5(
                            AppConfig.BASE_URL + "privacy.html",
                            "隐私政策"
                        )
                    },
                    onOpenPersonalInfoCollection = {
                        openH5(
                            AppConfig.BASE_URL + "personal-info-collection.html",
                            "个人信息收集清单"
                        )
                    },
                    onOpenThirdPartySharing = {
                        openH5(
                            AppConfig.BASE_URL + "third-party-sharing.html",
                            "第三方信息共享清单"
                        )
                    }
                )
            }
        }
    }

    /**
     * 统一通过独立 H5 Activity 内部加载目标页面，
     * 避免外部浏览器跳转与 Compose Dialog 弹窗。
     *
     * @param url 目标地址（远程 H5，由 H5Activity 内部 WebView 加载）
     * @param title 顶部标题
     */
    private fun openH5(url: String, title: String) {
        TheRouter.build(RouterPath.Web.H5)
            .withString("url", url)
            .withString("title", title)
            .navigation(this)
    }

    override fun initData() {}
}
