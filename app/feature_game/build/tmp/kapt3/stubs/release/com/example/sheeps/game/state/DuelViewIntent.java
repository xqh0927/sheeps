package com.example.sheeps.game.state;

import com.example.sheeps.core.multiplayer.WebSocketManager;
import com.example.sheeps.data.model.Tile;

/**
 * 对决模式界面意图。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\bv\u0018\u00002\u00020\u0001:\u0005\u0002\u0003\u0004\u0005\u0006\u0082\u0001\u0005\u0007\b\t\n\u000b\u00a8\u0006\f\u00c0\u0006\u0003"}, d2 = {"Lcom/example/sheeps/game/state/DuelViewIntent;", "", "Init", "ClickTile", "Restart", "Leave", "CastSpell", "Lcom/example/sheeps/game/state/DuelViewIntent$CastSpell;", "Lcom/example/sheeps/game/state/DuelViewIntent$ClickTile;", "Lcom/example/sheeps/game/state/DuelViewIntent$Init;", "Lcom/example/sheeps/game/state/DuelViewIntent$Leave;", "Lcom/example/sheeps/game/state/DuelViewIntent$Restart;", "feature_game_release"})
public abstract interface DuelViewIntent {
    
    /**
     * 主动释放恶搞大招
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u00d6\u0003J\t\u0010\u000e\u001a\u00020\u000fH\u00d6\u0001J\t\u0010\u0010\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0011"}, d2 = {"Lcom/example/sheeps/game/state/DuelViewIntent$CastSpell;", "Lcom/example/sheeps/game/state/DuelViewIntent;", "spellType", "", "<init>", "(Ljava/lang/String;)V", "getSpellType", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "feature_game_release"})
    public static final class CastSpell implements com.example.sheeps.game.state.DuelViewIntent {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String spellType = null;
        
        public CastSpell(@org.jetbrains.annotations.NotNull()
        java.lang.String spellType) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getSpellType() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.state.DuelViewIntent.CastSpell copy(@org.jetbrains.annotations.NotNull()
        java.lang.String spellType) {
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
    }
    
    /**
     * 点击棋盘卡牌
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u00d6\u0003J\t\u0010\u000e\u001a\u00020\u000fH\u00d6\u0001J\t\u0010\u0010\u001a\u00020\u0011H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0012"}, d2 = {"Lcom/example/sheeps/game/state/DuelViewIntent$ClickTile;", "Lcom/example/sheeps/game/state/DuelViewIntent;", "tile", "Lcom/example/sheeps/data/model/Tile;", "<init>", "(Lcom/example/sheeps/data/model/Tile;)V", "getTile", "()Lcom/example/sheeps/data/model/Tile;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "feature_game_release"})
    public static final class ClickTile implements com.example.sheeps.game.state.DuelViewIntent {
        @org.jetbrains.annotations.NotNull()
        private final com.example.sheeps.data.model.Tile tile = null;
        
        public ClickTile(@org.jetbrains.annotations.NotNull()
        com.example.sheeps.data.model.Tile tile) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.data.model.Tile getTile() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.data.model.Tile component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.state.DuelViewIntent.ClickTile copy(@org.jetbrains.annotations.NotNull()
        com.example.sheeps.data.model.Tile tile) {
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
    }
    
    /**
     * 初始化对局，开始连接 WebSocket 并加载相同种子的关卡
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u000f\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0003\b\u0086\b\u0018\u00002\u00020\u0001B\'\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\u0004\b\b\u0010\tJ\t\u0010\u0010\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0011\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0012\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u0013\u001a\u00020\u0006H\u00c6\u0003J1\u0010\u0014\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\u0006H\u00c6\u0001J\u0013\u0010\u0015\u001a\u00020\u00162\b\u0010\u0017\u001a\u0004\u0018\u00010\u0018H\u00d6\u0003J\t\u0010\u0019\u001a\u00020\u0006H\u00d6\u0001J\t\u0010\u001a\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\u000bR\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u000e\u00a8\u0006\u001b"}, d2 = {"Lcom/example/sheeps/game/state/DuelViewIntent$Init;", "Lcom/example/sheeps/game/state/DuelViewIntent;", "gameId", "", "playerId", "levelId", "", "seed", "<init>", "(Ljava/lang/String;Ljava/lang/String;II)V", "getGameId", "()Ljava/lang/String;", "getPlayerId", "getLevelId", "()I", "getSeed", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "", "hashCode", "toString", "feature_game_release"})
    public static final class Init implements com.example.sheeps.game.state.DuelViewIntent {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String gameId = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String playerId = null;
        private final int levelId = 0;
        private final int seed = 0;
        
        public Init(@org.jetbrains.annotations.NotNull()
        java.lang.String gameId, @org.jetbrains.annotations.NotNull()
        java.lang.String playerId, int levelId, int seed) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getGameId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getPlayerId() {
            return null;
        }
        
        public final int getLevelId() {
            return 0;
        }
        
        public final int getSeed() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        public final int component3() {
            return 0;
        }
        
        public final int component4() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.state.DuelViewIntent.Init copy(@org.jetbrains.annotations.NotNull()
        java.lang.String gameId, @org.jetbrains.annotations.NotNull()
        java.lang.String playerId, int levelId, int seed) {
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
    }
    
    /**
     * 离开房间并退出对局
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/DuelViewIntent$Leave;", "Lcom/example/sheeps/game/state/DuelViewIntent;", "<init>", "()V", "feature_game_release"})
    public static final class Leave implements com.example.sheeps.game.state.DuelViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.DuelViewIntent.Leave INSTANCE = null;
        
        private Leave() {
            super();
        }
    }
    
    /**
     * 重新开始对局
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/DuelViewIntent$Restart;", "Lcom/example/sheeps/game/state/DuelViewIntent;", "<init>", "()V", "feature_game_release"})
    public static final class Restart implements com.example.sheeps.game.state.DuelViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.DuelViewIntent.Restart INSTANCE = null;
        
        private Restart() {
            super();
        }
    }
}