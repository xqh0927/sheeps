package com.example.sheeps.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.tencent.mmkv.MMKV

/**
 * [androidx.startup.Initializer] 实现：初始化 MMKV 键值存储。
 *
 * 必须在任何读取/写入 MMKV 的组件之前完成初始化；被 TheRouterInitializer 等依赖。
 *
 * ⚠️ 线程约束：@MainThread。
 */
class MmkvInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        MMKV.initialize(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
