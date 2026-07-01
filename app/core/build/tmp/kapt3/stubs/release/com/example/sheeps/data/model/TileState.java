package com.example.sheeps.data.model;

import kotlinx.serialization.Serializable;

@kotlinx.serialization.Serializable()
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\b\b\u0087\u0081\u0002\u0018\u0000 \b2\b\u0012\u0004\u0012\u00020\u00000\u0001:\u0001\bB\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007\u00a8\u0006\t"}, d2 = {"Lcom/example/sheeps/data/model/TileState;", "", "<init>", "(Ljava/lang/String;I)V", "NORMAL", "BLOCKED", "IN_SLOT", "MOVED_OUT", "Companion", "core_release"})
public enum TileState {
    /*public static final*/ NORMAL /* = new NORMAL() */,
    /*public static final*/ BLOCKED /* = new BLOCKED() */,
    /*public static final*/ IN_SLOT /* = new IN_SLOT() */,
    /*public static final*/ MOVED_OUT /* = new MOVED_OUT() */;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.sheeps.data.model.TileState.Companion Companion = null;
    
    TileState() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.example.sheeps.data.model.TileState> getEntries() {
        return null;
    }
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a8\u0006\u0007"}, d2 = {"Lcom/example/sheeps/data/model/TileState$Companion;", "", "<init>", "()V", "serializer", "Lkotlinx/serialization/KSerializer;", "Lcom/example/sheeps/data/model/TileState;", "core_release"})
    public static final class Companion {
        
        @org.jetbrains.annotations.NotNull()
        public final kotlinx.serialization.KSerializer<com.example.sheeps.data.model.TileState> serializer() {
            return null;
        }
        
        private Companion() {
            super();
        }
    }
}