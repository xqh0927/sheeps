package com.example.sheeps.game.state;

import com.example.sheeps.data.model.RankingEntry;
import com.example.sheeps.data.model.Tile;

/**
 * 游戏界面意图（MVI 中的 Intent）。
 * 描述了用户可以在游戏界面上发起的各种操作。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0010\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\bv\u0018\u00002\u00020\u0001:\u0010\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n\u000b\f\r\u000e\u000f\u0010\u0011\u0082\u0001\u0010\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f !\u00a8\u0006\"\u00c0\u0006\u0003"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent;", "", "AgreePrivacy", "InitUser", "ChangeUsername", "LoadLevel", "ClickTile", "UseUndo", "UseMoveOut", "UseShuffle", "Revive", "UseHint", "UseBomb", "UseJoker", "UseDoublePoints", "LoadLeaderboard", "RestartLevel", "GoBackToMenu", "Lcom/example/sheeps/game/state/GameViewIntent$AgreePrivacy;", "Lcom/example/sheeps/game/state/GameViewIntent$ChangeUsername;", "Lcom/example/sheeps/game/state/GameViewIntent$ClickTile;", "Lcom/example/sheeps/game/state/GameViewIntent$GoBackToMenu;", "Lcom/example/sheeps/game/state/GameViewIntent$InitUser;", "Lcom/example/sheeps/game/state/GameViewIntent$LoadLeaderboard;", "Lcom/example/sheeps/game/state/GameViewIntent$LoadLevel;", "Lcom/example/sheeps/game/state/GameViewIntent$RestartLevel;", "Lcom/example/sheeps/game/state/GameViewIntent$Revive;", "Lcom/example/sheeps/game/state/GameViewIntent$UseBomb;", "Lcom/example/sheeps/game/state/GameViewIntent$UseDoublePoints;", "Lcom/example/sheeps/game/state/GameViewIntent$UseHint;", "Lcom/example/sheeps/game/state/GameViewIntent$UseJoker;", "Lcom/example/sheeps/game/state/GameViewIntent$UseMoveOut;", "Lcom/example/sheeps/game/state/GameViewIntent$UseShuffle;", "Lcom/example/sheeps/game/state/GameViewIntent$UseUndo;", "feature_game_debug"})
public abstract interface GameViewIntent {
    
    /**
     * 同意隐私协议
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$AgreePrivacy;", "Lcom/example/sheeps/game/state/GameViewIntent;", "<init>", "()V", "feature_game_debug"})
    public static final class AgreePrivacy implements com.example.sheeps.game.state.GameViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.GameViewIntent.AgreePrivacy INSTANCE = null;
        
        private AgreePrivacy() {
            super();
        }
    }
    
    /**
     * 修改昵称
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u00d6\u0003J\t\u0010\u000e\u001a\u00020\u000fH\u00d6\u0001J\t\u0010\u0010\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0011"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$ChangeUsername;", "Lcom/example/sheeps/game/state/GameViewIntent;", "newName", "", "<init>", "(Ljava/lang/String;)V", "getNewName", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "feature_game_debug"})
    public static final class ChangeUsername implements com.example.sheeps.game.state.GameViewIntent {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String newName = null;
        
        public ChangeUsername(@org.jetbrains.annotations.NotNull()
        java.lang.String newName) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getNewName() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.state.GameViewIntent.ChangeUsername copy(@org.jetbrains.annotations.NotNull()
        java.lang.String newName) {
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
     * 点击了棋盘上的某张卡牌
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u00d6\u0003J\t\u0010\u000e\u001a\u00020\u000fH\u00d6\u0001J\t\u0010\u0010\u001a\u00020\u0011H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0012"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$ClickTile;", "Lcom/example/sheeps/game/state/GameViewIntent;", "tile", "Lcom/example/sheeps/data/model/Tile;", "<init>", "(Lcom/example/sheeps/data/model/Tile;)V", "getTile", "()Lcom/example/sheeps/data/model/Tile;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "feature_game_debug"})
    public static final class ClickTile implements com.example.sheeps.game.state.GameViewIntent {
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
        public final com.example.sheeps.game.state.GameViewIntent.ClickTile copy(@org.jetbrains.annotations.NotNull()
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
     * 返回主菜单界面
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$GoBackToMenu;", "Lcom/example/sheeps/game/state/GameViewIntent;", "<init>", "()V", "feature_game_debug"})
    public static final class GoBackToMenu implements com.example.sheeps.game.state.GameViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.GameViewIntent.GoBackToMenu INSTANCE = null;
        
        private GoBackToMenu() {
            super();
        }
    }
    
    /**
     * 初始化用户信息
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$InitUser;", "Lcom/example/sheeps/game/state/GameViewIntent;", "<init>", "()V", "feature_game_debug"})
    public static final class InitUser implements com.example.sheeps.game.state.GameViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.GameViewIntent.InitUser INSTANCE = null;
        
        private InitUser() {
            super();
        }
    }
    
    /**
     * 加载关卡排行榜数据
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u00d6\u0003J\t\u0010\u000e\u001a\u00020\u0003H\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0011"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$LoadLeaderboard;", "Lcom/example/sheeps/game/state/GameViewIntent;", "levelId", "", "<init>", "(I)V", "getLevelId", "()I", "component1", "copy", "equals", "", "other", "", "hashCode", "toString", "", "feature_game_debug"})
    public static final class LoadLeaderboard implements com.example.sheeps.game.state.GameViewIntent {
        private final int levelId = 0;
        
        public LoadLeaderboard(int levelId) {
            super();
        }
        
        public final int getLevelId() {
            return 0;
        }
        
        public final int component1() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.state.GameViewIntent.LoadLeaderboard copy(int levelId) {
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
     * 加载指定关卡数据
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\n\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0003\b\u0086\b\u0018\u00002\u00020\u0001B\u001b\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0004\b\u0006\u0010\u0007J\t\u0010\f\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010\r\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u001f\u0010\u000e\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005H\u00c6\u0001J\u0013\u0010\u000f\u001a\u00020\u00102\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u00d6\u0003J\t\u0010\u0013\u001a\u00020\u0003H\u00d6\u0001J\t\u0010\u0014\u001a\u00020\u0005H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u0015"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$LoadLevel;", "Lcom/example/sheeps/game/state/GameViewIntent;", "levelId", "", "carryItemsJson", "", "<init>", "(ILjava/lang/String;)V", "getLevelId", "()I", "getCarryItemsJson", "()Ljava/lang/String;", "component1", "component2", "copy", "equals", "", "other", "", "hashCode", "toString", "feature_game_debug"})
    public static final class LoadLevel implements com.example.sheeps.game.state.GameViewIntent {
        private final int levelId = 0;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String carryItemsJson = null;
        
        public LoadLevel(int levelId, @org.jetbrains.annotations.Nullable()
        java.lang.String carryItemsJson) {
            super();
        }
        
        public final int getLevelId() {
            return 0;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getCarryItemsJson() {
            return null;
        }
        
        public final int component1() {
            return 0;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.state.GameViewIntent.LoadLevel copy(int levelId, @org.jetbrains.annotations.Nullable()
        java.lang.String carryItemsJson) {
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
     * 重新开始当前关卡
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$RestartLevel;", "Lcom/example/sheeps/game/state/GameViewIntent;", "<init>", "()V", "feature_game_debug"})
    public static final class RestartLevel implements com.example.sheeps.game.state.GameViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.GameViewIntent.RestartLevel INSTANCE = null;
        
        private RestartLevel() {
            super();
        }
    }
    
    /**
     * 触发“复活”逻辑
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$Revive;", "Lcom/example/sheeps/game/state/GameViewIntent;", "<init>", "()V", "feature_game_debug"})
    public static final class Revive implements com.example.sheeps.game.state.GameViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.GameViewIntent.Revive INSTANCE = null;
        
        private Revive() {
            super();
        }
    }
    
    /**
     * 使用“炸弹”道具
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$UseBomb;", "Lcom/example/sheeps/game/state/GameViewIntent;", "<init>", "()V", "feature_game_debug"})
    public static final class UseBomb implements com.example.sheeps.game.state.GameViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.GameViewIntent.UseBomb INSTANCE = null;
        
        private UseBomb() {
            super();
        }
    }
    
    /**
     * 使用“双倍积分”道具
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$UseDoublePoints;", "Lcom/example/sheeps/game/state/GameViewIntent;", "<init>", "()V", "feature_game_debug"})
    public static final class UseDoublePoints implements com.example.sheeps.game.state.GameViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.GameViewIntent.UseDoublePoints INSTANCE = null;
        
        private UseDoublePoints() {
            super();
        }
    }
    
    /**
     * 使用“提示”道具
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$UseHint;", "Lcom/example/sheeps/game/state/GameViewIntent;", "<init>", "()V", "feature_game_debug"})
    public static final class UseHint implements com.example.sheeps.game.state.GameViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.GameViewIntent.UseHint INSTANCE = null;
        
        private UseHint() {
            super();
        }
    }
    
    /**
     * 使用“万能牌”道具
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$UseJoker;", "Lcom/example/sheeps/game/state/GameViewIntent;", "<init>", "()V", "feature_game_debug"})
    public static final class UseJoker implements com.example.sheeps.game.state.GameViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.GameViewIntent.UseJoker INSTANCE = null;
        
        private UseJoker() {
            super();
        }
    }
    
    /**
     * 使用“移出”道具
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$UseMoveOut;", "Lcom/example/sheeps/game/state/GameViewIntent;", "<init>", "()V", "feature_game_debug"})
    public static final class UseMoveOut implements com.example.sheeps.game.state.GameViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.GameViewIntent.UseMoveOut INSTANCE = null;
        
        private UseMoveOut() {
            super();
        }
    }
    
    /**
     * 使用“洗牌”道具
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$UseShuffle;", "Lcom/example/sheeps/game/state/GameViewIntent;", "<init>", "()V", "feature_game_debug"})
    public static final class UseShuffle implements com.example.sheeps.game.state.GameViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.GameViewIntent.UseShuffle INSTANCE = null;
        
        private UseShuffle() {
            super();
        }
    }
    
    /**
     * 使用“撤销”道具
     */
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003\u00a8\u0006\u0004"}, d2 = {"Lcom/example/sheeps/game/state/GameViewIntent$UseUndo;", "Lcom/example/sheeps/game/state/GameViewIntent;", "<init>", "()V", "feature_game_debug"})
    public static final class UseUndo implements com.example.sheeps.game.state.GameViewIntent {
        @org.jetbrains.annotations.NotNull()
        public static final com.example.sheeps.game.state.GameViewIntent.UseUndo INSTANCE = null;
        
        private UseUndo() {
            super();
        }
    }
}