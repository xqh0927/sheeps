package com.example.sheeps.core.startup

import android.content.Context
import androidx.startup.Initializer
import logcat.AndroidLogcatLogger
import logcat.LogPriority

/**
 * [androidx.startup.Initializer] 实现：安装 logcat 日志框架。
 *
 * 在应用启动早期通过 [AndroidLogcatLogger.installOnDebuggableApp] 安装日志实现，
 * 仅对 debuggable 应用启用，最低级别 VERBOSE。
 *
 * ⚠️ 线程约束：@MainThread（Initializer.create 在应用主线程执行）。
 * 依赖：无。
 */
class LogcatInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        AndroidLogcatLogger.installOnDebuggableApp(context as android.app.Application, minPriority = LogPriority.VERBOSE)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
