package com.example.sheeps.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.therouter.TheRouter

class TheRouterInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        TheRouter.init(context as android.app.Application)
    }

    // Must run after MMKV is initialized
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(MmkvInitializer::class.java)
}
