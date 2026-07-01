package com.example.sheeps.game.state;

import com.example.sheeps.core.multiplayer.WebSocketManager;
import com.example.sheeps.data.model.Tile;

/**
 * 对决模式界面副作用。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\bv\u0018\u00002\u00020\u0001:\u0004\u0002\u0003\u0004\u0005\u0082\u0001\u0004\u0006\u0007\b\t\u00a8\u0006\n\u00c0\u0006\u0003"}, d2 = {"Lcom/example/sheeps/game/state/DuelViewEffect;", "", "ShowToast", "PlaySound", "Vibrate", "ExitGame", "Lcom/example/sheeps/game/state/DuelViewEffect$ExitGame;", "Lcom/example/sheeps/game/state/DuelViewEffect$PlaySound;", "Lcom/example/sheeps/game/state/DuelViewEffect$ShowToast;", "Lcom/example/sheeps/game/state/DuelViewEffect$Vibrate;", "feature_game_release"})
public abstract interface DuelViewEffect {
    
    /**
     * 退出游戏界面返回主页
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/DuelViewEffect$ExitGame;", "Lcom/example/sheeps/game/state/DuelViewEffect;", "<init>", "()V", "feature_game_release"})
    public static final class ExitGame implements com.example.sheeps.game.state.DuelViewEffect {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.DuelViewEffect.ExitGame INSTANCE = null;
        
        private ExitGame() {
            super();
        }
    }
    
    /**
     * 播放音效
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u00d6\u0003J\t\u0010\u000e\u001a\u00020\u000fH\u00d6\u0001J\t\u0010\u0010\u001a\u00020\u0011H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0012"}, d2 = {"Lcom/example/sheeps/game/state/DuelViewEffect$PlaySound;", "Lcom/example/sheeps/game/state/DuelViewEffect;", "soundType", "Lcom/example/sheeps/game/state/SoundType;", "<init>", "(Lcom/example/sheeps/game/state/SoundType;)V", "getSoundType", "()Lcom/example/sheeps/game/state/SoundType;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "feature_game_release"})
    public static final class PlaySound implements com.example.sheeps.game.state.DuelViewEffect {
        @org.jetbrains.annotations.NotNull()
        private final com.example.sheeps.game.state.SoundType soundType = null;
        
        public PlaySound(@org.jetbrains.annotations.NotNull()
        com.example.sheeps.game.state.SoundType soundType) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.state.SoundType getSoundType() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.state.SoundType component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.state.DuelViewEffect.PlaySound copy(@org.jetbrains.annotations.NotNull()
        com.example.sheeps.game.state.SoundType soundType) {
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
     * 显示提示消息
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u00d6\u0003J\t\u0010\u000e\u001a\u00020\u000fH\u00d6\u0001J\t\u0010\u0010\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0011"}, d2 = {"Lcom/example/sheeps/game/state/DuelViewEffect$ShowToast;", "Lcom/example/sheeps/game/state/DuelViewEffect;", "message", "", "<init>", "(Ljava/lang/String;)V", "getMessage", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "feature_game_release"})
    public static final class ShowToast implements com.example.sheeps.game.state.DuelViewEffect {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String message = null;
        
        public ShowToast(@org.jetbrains.annotations.NotNull()
        java.lang.String message) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getMessage() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.state.DuelViewEffect.ShowToast copy(@org.jetbrains.annotations.NotNull()
        java.lang.String message) {
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
     * 设备振动
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/DuelViewEffect$Vibrate;", "Lcom/example/sheeps/game/state/DuelViewEffect;", "<init>", "()V", "feature_game_release"})
    public static final class Vibrate implements com.example.sheeps.game.state.DuelViewEffect {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.DuelViewEffect.Vibrate INSTANCE = null;
        
        private Vibrate() {
            super();
        }
    }
}