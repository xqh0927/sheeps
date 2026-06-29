package com.example.sheeps.core.startup

import android.content.Context
import androidx.startup.Initializer
import logcat.AndroidLogcatLogger
import logcat.LogPriority

class LogcatInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        AndroidLogcatLogger.installOnDebuggableApp(context as android.app.Application, minPriority = LogPriority.VERBOSE)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
