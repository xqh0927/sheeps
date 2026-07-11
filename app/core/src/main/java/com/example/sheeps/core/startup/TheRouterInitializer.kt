package com.example.sheeps.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.therouter.TheRouter

/**
 * [androidx.startup.Initializer] 实现：初始化 TheRouter 路由框架。
 *
 * 必须在 MMKV 初始化完成后执行（见 [dependencies]），以便在路由表中读取本地化等配置。
 *
 * ⚠️ 线程约束：@MainThread。
 */
class TheRouterInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        TheRouter.init(context as android.app.Application)
    }

    // 必须在 MMKV 初始化完成之后执行
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(MmkvInitializer::class.java)
}
