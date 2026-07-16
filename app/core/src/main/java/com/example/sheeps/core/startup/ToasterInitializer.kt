package com.example.sheeps.core.startup

import android.content.Context
import android.view.Gravity
import androidx.startup.Initializer
import com.hjq.toast.Toaster

/**
 * [androidx.startup.Initializer] 实现：初始化 Toaster 全局 Toast 组件。
 *
 * 安装 Toaster 并将 Toast 显示在屏幕顶部（默认居中会影响游戏体验）。
 *
 * ⚠️ 线程约束：@MainThread。
 */
class ToasterInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Toaster.init(context as android.app.Application)
        // Toast 显示在屏幕上方（默认居中影响游戏体验）
        Toaster.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 150)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
