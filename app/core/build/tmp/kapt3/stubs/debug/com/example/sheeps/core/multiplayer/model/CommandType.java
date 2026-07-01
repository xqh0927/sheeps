package com.example.sheeps.core.multiplayer.model;

import kotlinx.serialization.Serializable;

/**
 * 指令类别
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\t\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007j\u0002\b\bj\u0002\b\t\u00a8\u0006\n"}, d2 = {"Lcom/example/sheeps/core/multiplayer/model/CommandType;", "", "<init>", "(Ljava/lang/String;I)V", "ELIMINATE", "ATTACK", "BUFF", "RECONNECT_RESUME", "SYSTEM_EVENT", "CAST_SPELL", "core_debug"})
public enum CommandType {
    /*public static final*/ ELIMINATE /* = new ELIMINATE() */,
    /*public static final*/ ATTACK /* = new ATTACK() */,
    /*public static final*/ BUFF /* = new BUFF() */,
    /*public static final*/ RECONNECT_RESUME /* = new RECONNECT_RESUME() */,
    /*public static final*/ SYSTEM_EVENT /* = new SYSTEM_EVENT() */,
    /*public static final*/ CAST_SPELL /* = new CAST_SPELL() */;
    
    CommandType() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.example.sheeps.core.multiplayer.model.CommandType> getEntries() {
        return null;
    }
}