package com.example.sheeps.core.utils;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u000e\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u0006R\u0014\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00060\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u000e"}, d2 = {"Lcom/example/sheeps/core/utils/AuthEventBus;", "", "<init>", "()V", "_events", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "Lcom/example/sheeps/core/utils/AuthEvent;", "events", "Lkotlinx/coroutines/flow/SharedFlow;", "getEvents", "()Lkotlinx/coroutines/flow/SharedFlow;", "post", "", "event", "core_release"})
public final class AuthEventBus {
    @org.jetbrains.annotations.NotNull()
    private static final kotlinx.coroutines.flow.MutableSharedFlow<com.example.sheeps.core.utils.AuthEvent> _events = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlinx.coroutines.flow.SharedFlow<com.example.sheeps.core.utils.AuthEvent> events = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.sheeps.core.utils.AuthEventBus INSTANCE = null;
    
    private AuthEventBus() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.SharedFlow<com.example.sheeps.core.utils.AuthEvent> getEvents() {
        return null;
    }
    
    public final void post(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.utils.AuthEvent event) {
    }
}