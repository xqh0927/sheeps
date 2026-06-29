package com.example.sheeps.core.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.blankj.utilcode.util.LogUtils

abstract class BaseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (javaClass.simpleName != "SplashActivity") {
            setTheme(com.example.sheeps.theme.ThemeManager.getThemeResId())
        }
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true // 或根据主题判断
        }
        LogUtils.d("${javaClass.simpleName} onCreate")
        enableEdgeToEdge()
        initView(savedInstanceState)
        initData()
    }

    abstract fun initView(savedInstanceState: Bundle?)
    abstract fun initData()

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d("${javaClass.simpleName} onDestroy")
    }
}
