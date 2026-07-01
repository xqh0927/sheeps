package com.example.sheeps.core.multiplayer.model;

import kotlinx.serialization.Serializable;

/**
 * 核心对局同步指令包
 */
@kotlinx.serialization.Serializable()
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0014\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0004\b\u0087\b\u0018\u0000 &2\u00020\u0001:\u0002%&B7\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u0012\u0006\u0010\u0007\u001a\u00020\u0003\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0004\b\f\u0010\rJ\t\u0010\u0018\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0019\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u001a\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001c\u001a\u00020\tH\u00c6\u0003J\t\u0010\u001d\u001a\u00020\u000bH\u00c6\u0003JE\u0010\u001e\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\b\b\u0002\u0010\u0007\u001a\u00020\u00032\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\u000bH\u00c6\u0001J\u0013\u0010\u001f\u001a\u00020 2\b\u0010!\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\"\u001a\u00020#H\u00d6\u0001J\t\u0010$\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0011R\u0011\u0010\u0007\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u000fR\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017\u00a8\u0006\'"}, d2 = {"Lcom/example/sheeps/core/multiplayer/model/GameCommand;", "", "gameId", "", "seqId", "", "timestamp", "senderId", "type", "Lcom/example/sheeps/core/multiplayer/model/CommandType;", "payload", "Lcom/example/sheeps/core/multiplayer/model/CommandPayload;", "<init>", "(Ljava/lang/String;JJLjava/lang/String;Lcom/example/sheeps/core/multiplayer/model/CommandType;Lcom/example/sheeps/core/multiplayer/model/CommandPayload;)V", "getGameId", "()Ljava/lang/String;", "getSeqId", "()J", "getTimestamp", "getSenderId", "getType", "()Lcom/example/sheeps/core/multiplayer/model/CommandType;", "getPayload", "()Lcom/example/sheeps/core/multiplayer/model/CommandPayload;", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "", "other", "hashCode", "", "toString", "$serializer", "Companion", "core_debug"})
public final class GameCommand {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String gameId = null;
    private final long seqId = 0L;
    private final long timestamp = 0L;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String senderId = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.core.multiplayer.model.CommandType type = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.core.multiplayer.model.CommandPayload payload = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.sheeps.core.multiplayer.model.GameCommand.Companion Companion = null;
    
    public GameCommand(@org.jetbrains.annotations.NotNull()
    java.lang.String gameId, long seqId, long timestamp, @org.jetbrains.annotations.NotNull()
    java.lang.String senderId, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.multiplayer.model.CommandType type, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.multiplayer.model.CommandPayload payload) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getGameId() {
        return null;
    }
    
    public final long getSeqId() {
        return 0L;
    }
    
    public final long getTimestamp() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSenderId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.core.multiplayer.model.CommandType getType() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.core.multiplayer.model.CommandPayload getPayload() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    public final long component2() {
        return 0L;
    }
    
    public final long component3() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.core.multiplayer.model.CommandType component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.core.multiplayer.model.CommandPayload component6() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.core.multiplayer.model.GameCommand copy(@org.jetbrains.annotations.NotNull()
    java.lang.String gameId, long seqId, long timestamp, @org.jetbrains.annotations.NotNull()
    java.lang.String senderId, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.multiplayer.model.CommandType type, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.multiplayer.model.CommandPayload payload) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
    
    /**
     * 核心对局同步指令包
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c7\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0003\u0010\u0004J\u0015\u0010\u0005\u001a\f\u0012\b\u0012\u0006\u0012\u0002\b\u00030\u00070\u0006\u00a2\u0006\u0002\u0010\bJ\u000e\u0010\t\u001a\u00020\u00022\u0006\u0010\n\u001a\u00020\u000bJ\u0016\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0002R\u0011\u0010\u0011\u001a\u00020\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014\u00a8\u0006\u0015"}, d2 = {"com/example/sheeps/core/multiplayer/model/GameCommand.$serializer", "Lkotlinx/serialization/internal/GeneratedSerializer;", "Lcom/example/sheeps/core/multiplayer/model/GameCommand;", "<init>", "()V", "childSerializers", "", "Lkotlinx/serialization/KSerializer;", "()[Lkotlinx/serialization/KSerializer;", "deserialize", "decoder", "Lkotlinx/serialization/encoding/Decoder;", "serialize", "", "encoder", "Lkotlinx/serialization/encoding/Encoder;", "value", "descriptor", "Lkotlinx/serialization/descriptors/SerialDescriptor;", "getDescriptor", "()Lkotlinx/serialization/descriptors/SerialDescriptor;", "core_debug"})
    @java.lang.Deprecated()
    public static final class $serializer implements kotlinx.serialization.internal.GeneratedSerializer<com.example.sheeps.core.multiplayer.model.GameCommand> {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.core.multiplayer.model.GameCommand.$serializer INSTANCE = null;
        @org.jetbrains.annotations.NotNull()
        private static final kotlinx.serialization.descriptors.SerialDescriptor descriptor = null;
        
        /**
         * 核心对局同步指令包
         */
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public final kotlinx.serialization.KSerializer<?>[] childSerializers() {
            return null;
        }
        
        /**
         * 核心对局同步指令包
         */
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.core.multiplayer.model.GameCommand deserialize(@org.jetbrains.annotations.NotNull()
        kotlinx.serialization.encoding.Decoder decoder) {
            return null;
        }
        
        /**
         * 核心对局同步指令包
         */
        @java.lang.Override()
        public final void serialize(@org.jetbrains.annotations.NotNull()
        kotlinx.serialization.encoding.Encoder encoder, @org.jetbrains.annotations.NotNull()
        com.example.sheeps.core.multiplayer.model.GameCommand value) {
        }
        
        private $serializer() {
            super();
        }
        
        /**
         * 核心对局同步指令包
         */
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public final kotlinx.serialization.descriptors.SerialDescriptor getDescriptor() {
            return null;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public kotlinx.serialization.KSerializer<?>[] typeParametersSerializers() {
            return null;
        }
    }
    
    /**
     * 核心对局同步指令包
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a8\u0006\u0007"}, d2 = {"Lcom/example/sheeps/core/multiplayer/model/GameCommand$Companion;", "", "<init>", "()V", "serializer", "Lkotlinx/serialization/KSerializer;", "Lcom/example/sheeps/core/multiplayer/model/GameCommand;", "core_debug"})
    public static final class Companion {
        
        /**
         * 核心对局同步指令包
         */
        @org.jetbrains.annotations.NotNull()
        public final kotlinx.serialization.KSerializer<com.example.sheeps.core.multiplayer.model.GameCommand> serializer() {
            return null;
        }
        
        private Companion() {
            super();
        }
    }
}