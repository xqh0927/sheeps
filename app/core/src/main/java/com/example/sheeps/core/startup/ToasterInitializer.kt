package com.example.sheeps.core.startup

import android.content.Context
import android.view.Gravity
import androidx.startup.Initializer
import com.hjq.toast.Toaster

class ToasterInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Toaster.init(context as android.app.Application)
        // Toast 显示在屏幕上方（默认居中影响游戏体验）
        Toaster.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 180)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
