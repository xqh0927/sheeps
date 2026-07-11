package com.example.sheeps.core.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 全局登录鉴权事件总线（进程级单例）。
 *
 * 基于 [kotlinx.coroutines.flow.MutableSharedFlow] 实现，采用 extraBufferCapacity = 1 的容错缓冲，
 * 用于在 Token 静默刷新失败等场景中跨模块广播 [AuthEvent]（如强制登出）。
 *
 * ⚠️ 内存隐患：events 由进程级单例持有，订阅方若在 lifecycleScope/repeatOnLifecycle 之外持续 collect 且未取消，
 *    该共享流会强引用订阅者（常包含 Activity/Fragment）导致内存泄漏；
 *    务必使用生命周期感知的作用域收集，并在界面销毁时停止收集。
 */
object AuthEventBus {
    
    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
    
    // 通过 tryEmit 非阻塞发送；若缓冲已满（extraBufferCapacity 溢出）则丢弃，不保证送达。
    fun post(event: AuthEvent) {
        _events.tryEmit(event)
    }
}

sealed interface AuthEvent {
    object Logout : AuthEvent
}
