package com.example.sheeps.lib_base.base

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat

/**
 * 应用内所有 Activity 的基类。
 * 提供基础的生命周期日志记录、沉浸式状态栏配置以及主题管理。
 */
abstract class BaseActivity : ComponentActivity() {

    /**
     * 生命周期（Context 注入阶段）：在 [super.attachBaseContext] 之前完成 MMKV 初始化与动态语言切换。
     * 必须最先初始化 MMKV，否则后续语言偏好读取会失败。
     * ⚠️ 线程约束：@MainThread。
     */
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

    /**
     * 生命周期：Activity 创建。
     * 职责：按需设置主题、配置沉浸式状态栏、记录生命周期日志、开启 edge-to-edge，
     * 最后调用 [initView] 与 [initData] 完成界面与数据初始化。
     * ⚠️ 子类若在此前注册了 BroadcastReceiver / EventBus / Flow 收集 / 协程作用域，
     *    必须在 [onDestroy] 中反注册或取消，否则泄漏。
     * ⚠️ 线程约束：@MainThread。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // 优先使用子类特定的主题覆盖，若未提供（为 0）则回退到全局委托提供的主题资源 ID
        var themeResId = getOverrideThemeResId()
        if (themeResId == 0) {
            themeResId = BaseActivityThemeDelegate.themeProvider?.invoke() ?: 0
        }

        // 应用主题（SplashActivity 已在 Manifest 声明独立主题，在此排除）
        if (themeResId != 0 && javaClass.simpleName != "SplashActivity") {
            setTheme(themeResId)
        }
        super.onCreate(savedInstanceState)

        // 设置状态栏图标颜色风格
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        Log.d("BaseActivity", "${javaClass.simpleName} onCreate")

        // 开启全屏/沉浸式边缘到边缘支持
        enableEdgeToEdge()

        // 初始化视图与数据
        initView(savedInstanceState)
        initData()
    }

    /**
     * 子类可以通过重写此方法提供具体的 XML Theme 资源 ID，以解耦底层与业务主题管理器。
     */
    protected open fun getOverrideThemeResId(): Int = 0

    /**
     * 在此方法中进行视图相关的初始化（如设置 Compose 内容、ViewBinding 等）。
     * @param savedInstanceState 界面销毁重建时保存的状态包
     */
    abstract fun initView(savedInstanceState: Bundle?)

    /**
     * 在此方法中进行数据加载、网络请求或 ViewModel 的订阅。
     */
    abstract fun initData()

    /**
     * 生命周期：Activity 销毁。
     * 职责：仅记录销毁日志。基类不持有需手动释放的全局资源；
     * ⚠️ 子类必须在此反注册所有监听器 / 取消协程 / 停止 Flow 收集以释放引用。
     * ⚠️ 线程约束：@MainThread。
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d("BaseActivity", "${javaClass.simpleName} onDestroy")
    }
}

/**
 * 全局主题回调提供者，用于在底层 BaseActivity 与上层业务主题管理系统之间实现松耦合桥接。
 */
object BaseActivityThemeDelegate {
    var themeProvider: (() -> Int)? = null
}
