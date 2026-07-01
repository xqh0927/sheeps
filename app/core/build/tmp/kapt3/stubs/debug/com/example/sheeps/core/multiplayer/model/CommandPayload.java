package com.example.sheeps.core.multiplayer.model;

import kotlinx.serialization.Serializable;

/**
 * 详细载荷数据模型
 */
@kotlinx.serialization.Serializable()
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0006\n\u0002\u0010\t\n\u0002\b\u001b\n\u0002\u0010\u000b\n\u0002\b\u0006\b\u0087\b\u0018\u0000 02\u00020\u0001:\u0002/0Bs\u0012\u0010\b\u0002\u0010\u0002\u001a\n\u0012\u0004\u0012\u00020\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u0004\u0012\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u0004\u0012\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\u0004\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u0004\u0012\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u0004\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u000f\u00a2\u0006\u0004\b\u0010\u0010\u0011J\u0011\u0010 \u001a\n\u0012\u0004\u0012\u00020\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010!\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\"\u001a\u00020\bH\u00c6\u0003J\u000b\u0010#\u001a\u0004\u0018\u00010\u0004H\u00c6\u0003J\u000b\u0010$\u001a\u0004\u0018\u00010\u0004H\u00c6\u0003J\u000b\u0010%\u001a\u0004\u0018\u00010\u0004H\u00c6\u0003J\u000b\u0010&\u001a\u0004\u0018\u00010\u0004H\u00c6\u0003J\u000b\u0010\'\u001a\u0004\u0018\u00010\u0004H\u00c6\u0003J\t\u0010(\u001a\u00020\u000fH\u00c6\u0003Ju\u0010)\u001a\u00020\u00002\u0010\b\u0002\u0010\u0002\u001a\n\u0012\u0004\u0012\u00020\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u00042\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u00042\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\u00042\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u00042\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u00042\b\b\u0002\u0010\u000e\u001a\u00020\u000fH\u00c6\u0001J\u0013\u0010*\u001a\u00020+2\b\u0010,\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010-\u001a\u00020\u0006H\u00d6\u0001J\t\u0010.\u001a\u00020\u0004H\u00d6\u0001R\u0019\u0010\u0002\u001a\n\u0012\u0004\u0012\u00020\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0013\u0010\t\u001a\u0004\u0018\u00010\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0013\u0010\n\u001a\u0004\u0018\u00010\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0019R\u0013\u0010\u000b\u001a\u0004\u0018\u00010\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0019R\u0013\u0010\f\u001a\u0004\u0018\u00010\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0019R\u0013\u0010\r\u001a\u0004\u0018\u00010\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0019R\u0011\u0010\u000e\u001a\u00020\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001f\u00a8\u00061"}, d2 = {"Lcom/example/sheeps/core/multiplayer/model/CommandPayload;", "", "tilesEliminated", "", "", "comboCount", "", "attackPower", "", "obstacleType", "targetPlayerId", "activeBuffId", "systemMessage", "spellType", "spellDuration", "", "<init>", "(Ljava/util/List;IDLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;J)V", "getTilesEliminated", "()Ljava/util/List;", "getComboCount", "()I", "getAttackPower", "()D", "getObstacleType", "()Ljava/lang/String;", "getTargetPlayerId", "getActiveBuffId", "getSystemMessage", "getSpellType", "getSpellDuration", "()J", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "", "other", "hashCode", "toString", "$serializer", "Companion", "core_debug"})
public final class CommandPayload {
    @org.jetbrains.annotations.Nullable()
    private final java.util.List<java.lang.String> tilesEliminated = null;
    private final int comboCount = 0;
    private final double attackPower = 0.0;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String obstacleType = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String targetPlayerId = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String activeBuffId = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String systemMessage = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String spellType = null;
    private final long spellDuration = 0L;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.sheeps.core.multiplayer.model.CommandPayload.Companion Companion = null;
    
    public CommandPayload(@org.jetbrains.annotations.Nullable()
    java.util.List<java.lang.String> tilesEliminated, int comboCount, double attackPower, @org.jetbrains.annotations.Nullable()
    java.lang.String obstacleType, @org.jetbrains.annotations.Nullable()
    java.lang.String targetPlayerId, @org.jetbrains.annotations.Nullable()
    java.lang.String activeBuffId, @org.jetbrains.annotations.Nullable()
    java.lang.String systemMessage, @org.jetbrains.annotations.Nullable()
    java.lang.String spellType, long spellDuration) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.List<java.lang.String> getTilesEliminated() {
        return null;
    }
    
    public final int getComboCount() {
        return 0;
    }
    
    public final double getAttackPower() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getObstacleType() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getTargetPlayerId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getActiveBuffId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getSystemMessage() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getSpellType() {
        return null;
    }
    
    public final long getSpellDuration() {
        return 0L;
    }
    
    public CommandPayload() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.List<java.lang.String> component1() {
        return null;
    }
    
    public final int component2() {
        return 0;
    }
    
    public final double component3() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component5() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component6() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component8() {
        return null;
    }
    
    public final long component9() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.core.multiplayer.model.CommandPayload copy(@org.jetbrains.annotations.Nullable()
    java.util.List<java.lang.String> tilesEliminated, int comboCount, double attackPower, @org.jetbrains.annotations.Nullable()
    java.lang.String obstacleType, @org.jetbrains.annotations.Nullable()
    java.lang.String targetPlayerId, @org.jetbrains.annotations.Nullable()
    java.lang.String activeBuffId, @org.jetbrains.annotations.Nullable()
    java.lang.String systemMessage, @org.jetbrains.annotations.Nullable()
    java.lang.String spellType, long spellDuration) {
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
     * 详细载荷数据模型
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c7\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0003\u0010\u0004J\u0015\u0010\u0005\u001a\f\u0012\b\u0012\u0006\u0012\u0002\b\u00030\u00070\u0006\u00a2\u0006\u0002\u0010\bJ\u000e\u0010\t\u001a\u00020\u00022\u0006\u0010\n\u001a\u00020\u000bJ\u0016\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0002R\u0011\u0010\u0011\u001a\u00020\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014\u00a8\u0006\u0015"}, d2 = {"com/example/sheeps/core/multiplayer/model/CommandPayload.$serializer", "Lkotlinx/serialization/internal/GeneratedSerializer;", "Lcom/example/sheeps/core/multiplayer/model/CommandPayload;", "<init>", "()V", "childSerializers", "", "Lkotlinx/serialization/KSerializer;", "()[Lkotlinx/serialization/KSerializer;", "deserialize", "decoder", "Lkotlinx/serialization/encoding/Decoder;", "serialize", "", "encoder", "Lkotlinx/serialization/encoding/Encoder;", "value", "descriptor", "Lkotlinx/serialization/descriptors/SerialDescriptor;", "getDescriptor", "()Lkotlinx/serialization/descriptors/SerialDescriptor;", "core_debug"})
    @java.lang.Deprecated()
    public static final class $serializer implements kotlinx.serialization.internal.GeneratedSerializer<com.example.sheeps.core.multiplayer.model.CommandPayload> {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.core.multiplayer.model.CommandPayload.$serializer INSTANCE = null;
        @org.jetbrains.annotations.NotNull()
        private static final kotlinx.serialization.descriptors.SerialDescriptor descriptor = null;
        
        /**
         * 详细载荷数据模型
         */
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public final kotlinx.serialization.KSerializer<?>[] childSerializers() {
            return null;
        }
        
        /**
         * 详细载荷数据模型
         */
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.core.multiplayer.model.CommandPayload deserialize(@org.jetbrains.annotations.NotNull()
        kotlinx.serialization.encoding.Decoder decoder) {
            return null;
        }
        
        /**
         * 详细载荷数据模型
         */
        @java.lang.Override()
        public final void serialize(@org.jetbrains.annotations.NotNull()
        kotlinx.serialization.encoding.Encoder encoder, @org.jetbrains.annotations.NotNull()
        com.example.sheeps.core.multiplayer.model.CommandPayload value) {
        }
        
        private $serializer() {
            super();
        }
        
        /**
         * 详细载荷数据模型
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
     * 详细载荷数据模型
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a8\u0006\u0007"}, d2 = {"Lcom/example/sheeps/core/multiplayer/model/CommandPayload$Companion;", "", "<init>", "()V", "serializer", "Lkotlinx/serialization/KSerializer;", "Lcom/example/sheeps/core/multiplayer/model/CommandPayload;", "core_debug"})
    public static final class Companion {
        
        /**
         * 详细载荷数据模型
         */
        @org.jetbrains.annotations.NotNull()
        public final kotlinx.serialization.KSerializer<com.example.sheeps.core.multiplayer.model.CommandPayload> serializer() {
            return null;
        }
        
        private Companion() {
            super();
        }
    }
}