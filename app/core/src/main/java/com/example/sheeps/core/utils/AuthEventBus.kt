package com.example.sheeps.core.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AuthEventBus {
    
    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
    
    fun post(event: AuthEvent) {
        _events.tryEmit(event)
    }
}

sealed interface AuthEvent {
    object Logout : AuthEvent
}
