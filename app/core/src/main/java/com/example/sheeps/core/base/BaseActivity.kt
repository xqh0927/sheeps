package com.example.sheeps.core.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.apkfuns.logutils.LogUtils

/**
 * 应用内所有 Activity 的基类。
 * 提供基础的生命周期日志记录、沉浸式状态栏配置以及主题管理。
 */
abstract class BaseActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val mmkvContext = newBase.applicationContext ?: newBase
        try {
            com.tencent.mmkv.MMKV.initialize(mmkvContext)
        } catch (e: Exception) {
        }
        val lang = com.tencent.mmkv.MMKV.defaultMMKV()?.decodeString("user_lang", "") ?: ""
        if (lang.isEmpty() || lang == "zh") {
            super.attachBaseContext(newBase)
        } else {
            val locale = when (lang) {
                "en" -> java.util.Locale.ENGLISH
                "tw" -> java.util.Locale.TRADITIONAL_CHINESE
                "ja" -> java.util.Locale.JAPANESE
                "ko" -> java.util.Locale.KOREAN
                else -> java.util.Locale.SIMPLIFIED_CHINESE
            }
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            val configContext = newBase.createConfigurationContext(config)
            super.attachBaseContext(configContext)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 配置主题，SplashActivity 使用其特定主题除外
        if (javaClass.simpleName != "SplashActivity") {
            setTheme(com.example.sheeps.theme.ThemeManager.getThemeResId())
        }
        super.onCreate(savedInstanceState)

        // 设置状态栏图标颜色风格
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        LogUtils.d("${javaClass.simpleName} onCreate")

        // 开启全屏/沉浸式边缘到边缘支持
        enableEdgeToEdge()

        // 初始化视图与数据
        initView(savedInstanceState)
        initData()
    }

    /**
     * 在此方法中进行视图相关的初始化（如设置 Compose 内容、ViewBinding 等）。
     * @param savedInstanceState 界面销毁重建时保存的状态包
     */
    abstract fun initView(savedInstanceState: Bundle?)

    /**
     * 在此方法中进行数据加载、网络请求或 ViewModel 的订阅。
     */
    abstract fun initData()

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d("${javaClass.simpleName} onDestroy")
    }
}
