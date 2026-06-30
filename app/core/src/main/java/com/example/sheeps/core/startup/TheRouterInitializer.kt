package com.example.sheeps.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.therouter.TheRouter

class TheRouterInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        TheRouter.init(context as android.app.Application)
    }

    // 必须在 MMKV 初始化完成之后执行
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(MmkvInitializer::class.java)
}
