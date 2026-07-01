package com.example.sheeps.core.utils;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u0010\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\bv\u0018\u00002\u00020\u0001:\u0001\u0002\u0082\u0001\u0001\u0003\u00a8\u0006\u0004\u00c0\u0006\u0003"}, d2 = {"Lcom/example/sheeps/core/utils/AuthEvent;", "", "Logout", "Lcom/example/sheeps/core/utils/AuthEvent$Logout;", "core_release"})
public abstract interface AuthEvent {
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/core/utils/AuthEvent$Logout;", "Lcom/example/sheeps/core/utils/AuthEvent;", "<init>", "()V", "core_release"})
    public static final class Logout implements com.example.sheeps.core.utils.AuthEvent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.core.utils.AuthEvent.Logout INSTANCE = null;
        
        private Logout() {
            super();
        }
    }
}