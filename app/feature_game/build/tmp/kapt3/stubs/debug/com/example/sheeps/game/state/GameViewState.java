package com.example.sheeps.game.state;

import com.example.sheeps.data.model.RankingEntry;
import com.example.sheeps.data.model.Tile;

/**
 * 游戏界面状态模型（MVI 中的 State）。
 * 包含了维持游戏运行所需的所有 UI 数据。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\"\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b;\b\u0086\b\u0018\u00002\u00020\u0001B\u0091\u0002\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\b\b\u0002\u0010\t\u001a\u00020\b\u0012\u000e\b\u0002\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\u000e\b\u0002\u0010\r\u001a\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\u000e\b\u0002\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\b\b\u0002\u0010\u000f\u001a\u00020\b\u0012\b\b\u0002\u0010\u0010\u001a\u00020\b\u0012\b\b\u0002\u0010\u0011\u001a\u00020\b\u0012\b\b\u0002\u0010\u0012\u001a\u00020\b\u0012\b\b\u0002\u0010\u0013\u001a\u00020\b\u0012\b\b\u0002\u0010\u0014\u001a\u00020\b\u0012\b\b\u0002\u0010\u0015\u001a\u00020\b\u0012\b\b\u0002\u0010\u0016\u001a\u00020\b\u0012\b\b\u0002\u0010\u0017\u001a\u00020\u0003\u0012\u000e\b\u0002\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00060\u0019\u0012\b\b\u0002\u0010\u001a\u001a\u00020\b\u0012\b\b\u0002\u0010\u001b\u001a\u00020\u001c\u0012\u000e\b\u0002\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001e0\u000b\u0012\b\b\u0002\u0010\u001f\u001a\u00020\u0006\u0012\u000e\b\u0002\u0010 \u001a\b\u0012\u0004\u0012\u00020\u00060\u0019\u00a2\u0006\u0004\b!\u0010\"J\t\u0010=\u001a\u00020\u0003H\u00c6\u0003J\t\u0010>\u001a\u00020\u0003H\u00c6\u0003J\t\u0010?\u001a\u00020\u0006H\u00c6\u0003J\t\u0010@\u001a\u00020\bH\u00c6\u0003J\t\u0010A\u001a\u00020\bH\u00c6\u0003J\u000f\u0010B\u001a\b\u0012\u0004\u0012\u00020\f0\u000bH\u00c6\u0003J\u000f\u0010C\u001a\b\u0012\u0004\u0012\u00020\f0\u000bH\u00c6\u0003J\u000f\u0010D\u001a\b\u0012\u0004\u0012\u00020\f0\u000bH\u00c6\u0003J\t\u0010E\u001a\u00020\bH\u00c6\u0003J\t\u0010F\u001a\u00020\bH\u00c6\u0003J\t\u0010G\u001a\u00020\bH\u00c6\u0003J\t\u0010H\u001a\u00020\bH\u00c6\u0003J\t\u0010I\u001a\u00020\bH\u00c6\u0003J\t\u0010J\u001a\u00020\bH\u00c6\u0003J\t\u0010K\u001a\u00020\bH\u00c6\u0003J\t\u0010L\u001a\u00020\bH\u00c6\u0003J\t\u0010M\u001a\u00020\u0003H\u00c6\u0003J\u000f\u0010N\u001a\b\u0012\u0004\u0012\u00020\u00060\u0019H\u00c6\u0003J\t\u0010O\u001a\u00020\bH\u00c6\u0003J\t\u0010P\u001a\u00020\u001cH\u00c6\u0003J\u000f\u0010Q\u001a\b\u0012\u0004\u0012\u00020\u001e0\u000bH\u00c6\u0003J\t\u0010R\u001a\u00020\u0006H\u00c6\u0003J\u000f\u0010S\u001a\b\u0012\u0004\u0012\u00020\u00060\u0019H\u00c6\u0003J\u0093\u0002\u0010T\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\b2\u000e\b\u0002\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000b2\u000e\b\u0002\u0010\r\u001a\b\u0012\u0004\u0012\u00020\f0\u000b2\u000e\b\u0002\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\f0\u000b2\b\b\u0002\u0010\u000f\u001a\u00020\b2\b\b\u0002\u0010\u0010\u001a\u00020\b2\b\b\u0002\u0010\u0011\u001a\u00020\b2\b\b\u0002\u0010\u0012\u001a\u00020\b2\b\b\u0002\u0010\u0013\u001a\u00020\b2\b\b\u0002\u0010\u0014\u001a\u00020\b2\b\b\u0002\u0010\u0015\u001a\u00020\b2\b\b\u0002\u0010\u0016\u001a\u00020\b2\b\b\u0002\u0010\u0017\u001a\u00020\u00032\u000e\b\u0002\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00060\u00192\b\b\u0002\u0010\u001a\u001a\u00020\b2\b\b\u0002\u0010\u001b\u001a\u00020\u001c2\u000e\b\u0002\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001e0\u000b2\b\b\u0002\u0010\u001f\u001a\u00020\u00062\u000e\b\u0002\u0010 \u001a\b\u0012\u0004\u0012\u00020\u00060\u0019H\u00c6\u0001J\u0013\u0010U\u001a\u00020\u00032\b\u0010V\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010W\u001a\u00020\bH\u00d6\u0001J\t\u0010X\u001a\u00020\u0006H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0002\u0010#R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0004\u0010#R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010%R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\'R\u0011\u0010\t\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010\'R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010*R\u0017\u0010\r\u001a\b\u0012\u0004\u0012\u00020\f0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010*R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\f0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010*R\u0011\u0010\u000f\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010\'R\u0011\u0010\u0010\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b.\u0010\'R\u0011\u0010\u0011\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u0010\'R\u0011\u0010\u0012\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u0010\'R\u0011\u0010\u0013\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b1\u0010\'R\u0011\u0010\u0014\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b2\u0010\'R\u0011\u0010\u0015\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b3\u0010\'R\u0011\u0010\u0016\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b4\u0010\'R\u0011\u0010\u0017\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010#R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00060\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b5\u00106R\u0011\u0010\u001a\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b7\u0010\'R\u0011\u0010\u001b\u001a\u00020\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b8\u00109R\u0017\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001e0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b:\u0010*R\u0011\u0010\u001f\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b;\u0010%R\u0017\u0010 \u001a\b\u0012\u0004\u0012\u00020\u00060\u0019\u00a2\u0006\b\n\u0000\u001a\u0004\b<\u00106\u00a8\u0006Y"}, d2 = {"Lcom/example/sheeps/game/state/GameViewState;", "", "isLoading", "", "isPrivacyAccepted", "username", "", "unlockedLevel", "", "currentLevelId", "boardTiles", "", "Lcom/example/sheeps/data/model/Tile;", "slotTiles", "movedOutTiles", "undoCount", "moveOutCount", "shuffleCount", "reviveCount", "hintCount", "bombCount", "jokerCount", "doublePointsCount", "isDoublePointsActive", "highlightedTileIds", "", "score", "gameStatus", "Lcom/example/sheeps/game/state/GameStatus;", "rankings", "Lcom/example/sheeps/data/model/RankingEntry;", "currentSkin", "shakingTileIds", "<init>", "(ZZLjava/lang/String;IILjava/util/List;Ljava/util/List;Ljava/util/List;IIIIIIIIZLjava/util/Set;ILcom/example/sheeps/game/state/GameStatus;Ljava/util/List;Ljava/lang/String;Ljava/util/Set;)V", "()Z", "getUsername", "()Ljava/lang/String;", "getUnlockedLevel", "()I", "getCurrentLevelId", "getBoardTiles", "()Ljava/util/List;", "getSlotTiles", "getMovedOutTiles", "getUndoCount", "getMoveOutCount", "getShuffleCount", "getReviveCount", "getHintCount", "getBombCount", "getJokerCount", "getDoublePointsCount", "getHighlightedTileIds", "()Ljava/util/Set;", "getScore", "getGameStatus", "()Lcom/example/sheeps/game/state/GameStatus;", "getRankings", "getCurrentSkin", "getShakingTileIds", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "component10", "component11", "component12", "component13", "component14", "component15", "component16", "component17", "component18", "component19", "component20", "component21", "component22", "component23", "copy", "equals", "other", "hashCode", "toString", "feature_game_debug"})
public final class GameViewState {
    
    /**
     * 是否正在加载关卡数据
     */
    private final boolean isLoading = false;
    
    /**
     * 隐私协议是否已接受
     */
    private final boolean isPrivacyAccepted = false;
    
    /**
     * 当前用户的昵称
     */
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String username = null;
    
    /**
     * 用户已解锁的最高关卡 ID
     */
    private final int unlockedLevel = 0;
    
    /**
     * 当前正在进行的关卡 ID
     */
    private final int currentLevelId = 0;
    
    /**
     * 棋盘上所有的卡牌列表
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.sheeps.data.model.Tile> boardTiles = null;
    
    /**
     * 消除槽（七格槽位）中的卡牌列表
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.sheeps.data.model.Tile> slotTiles = null;
    
    /**
     * 已使用“移出”道具暂时存放的卡牌列表
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.sheeps.data.model.Tile> movedOutTiles = null;
    private final int undoCount = 0;
    private final int moveOutCount = 0;
    private final int shuffleCount = 0;
    private final int reviveCount = 0;
    private final int hintCount = 0;
    private final int bombCount = 0;
    private final int jokerCount = 0;
    private final int doublePointsCount = 0;
    
    /**
     * 当前是否激活了双倍积分倍率
     */
    private final boolean isDoublePointsActive = false;
    
    /**
     * 当前处于提示状态的高亮卡牌 ID 集合
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> highlightedTileIds = null;
    
    /**
     * 本次关卡的当前得分
     */
    private final int score = 0;
    
    /**
     * 当前游戏运行状态
     */
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.game.state.GameStatus gameStatus = null;
    
    /**
     * 排行榜数据
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.sheeps.data.model.RankingEntry> rankings = null;
    
    /**
     * 当前使用的卡牌皮肤主题名称
     */
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String currentSkin = null;
    
    /**
     * 正在抖动的卡牌 ID 集合（用于提示遮挡）
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> shakingTileIds = null;
    
    public GameViewState(boolean isLoading, boolean isPrivacyAccepted, @org.jetbrains.annotations.NotNull()
    java.lang.String username, int unlockedLevel, int currentLevelId, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> boardTiles, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> slotTiles, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> movedOutTiles, int undoCount, int moveOutCount, int shuffleCount, int reviveCount, int hintCount, int bombCount, int jokerCount, int doublePointsCount, boolean isDoublePointsActive, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> highlightedTileIds, int score, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameStatus gameStatus, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.RankingEntry> rankings, @org.jetbrains.annotations.NotNull()
    java.lang.String currentSkin, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> shakingTileIds) {
        super();
    }
    
    /**
     * 是否正在加载关卡数据
     */
    public final boolean isLoading() {
        return false;
    }
    
    /**
     * 隐私协议是否已接受
     */
    public final boolean isPrivacyAccepted() {
        return false;
    }
    
    /**
     * 当前用户的昵称
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getUsername() {
        return null;
    }
    
    /**
     * 用户已解锁的最高关卡 ID
     */
    public final int getUnlockedLevel() {
        return 0;
    }
    
    /**
     * 当前正在进行的关卡 ID
     */
    public final int getCurrentLevelId() {
        return 0;
    }
    
    /**
     * 棋盘上所有的卡牌列表
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.Tile> getBoardTiles() {
        return null;
    }
    
    /**
     * 消除槽（七格槽位）中的卡牌列表
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.Tile> getSlotTiles() {
        return null;
    }
    
    /**
     * 已使用“移出”道具暂时存放的卡牌列表
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.Tile> getMovedOutTiles() {
        return null;
    }
    
    public final int getUndoCount() {
        return 0;
    }
    
    public final int getMoveOutCount() {
        return 0;
    }
    
    public final int getShuffleCount() {
        return 0;
    }
    
    public final int getReviveCount() {
        return 0;
    }
    
    public final int getHintCount() {
        return 0;
    }
    
    public final int getBombCount() {
        return 0;
    }
    
    public final int getJokerCount() {
        return 0;
    }
    
    public final int getDoublePointsCount() {
        return 0;
    }
    
    /**
     * 当前是否激活了双倍积分倍率
     */
    public final boolean isDoublePointsActive() {
        return false;
    }
    
    /**
     * 当前处于提示状态的高亮卡牌 ID 集合
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.String> getHighlightedTileIds() {
        return null;
    }
    
    /**
     * 本次关卡的当前得分
     */
    public final int getScore() {
        return 0;
    }
    
    /**
     * 当前游戏运行状态
     */
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.game.state.GameStatus getGameStatus() {
        return null;
    }
    
    /**
     * 排行榜数据
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.RankingEntry> getRankings() {
        return null;
    }
    
    /**
     * 当前使用的卡牌皮肤主题名称
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getCurrentSkin() {
        return null;
    }
    
    /**
     * 正在抖动的卡牌 ID 集合（用于提示遮挡）
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.String> getShakingTileIds() {
        return null;
    }
    
    public GameViewState() {
        super();
    }
    
    public final boolean component1() {
        return false;
    }
    
    public final int component10() {
        return 0;
    }
    
    public final int component11() {
        return 0;
    }
    
    public final int component12() {
        return 0;
    }
    
    public final int component13() {
        return 0;
    }
    
    public final int component14() {
        return 0;
    }
    
    public final int component15() {
        return 0;
    }
    
    public final int component16() {
        return 0;
    }
    
    public final boolean component17() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.String> component18() {
        return null;
    }
    
    public final int component19() {
        return 0;
    }
    
    public final boolean component2() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.game.state.GameStatus component20() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.RankingEntry> component21() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component22() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.String> component23() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    public final int component4() {
        return 0;
    }
    
    public final int component5() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.Tile> component6() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.Tile> component7() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.Tile> component8() {
        return null;
    }
    
    public final int component9() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.game.state.GameViewState copy(boolean isLoading, boolean isPrivacyAccepted, @org.jetbrains.annotations.NotNull()
    java.lang.String username, int unlockedLevel, int currentLevelId, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> boardTiles, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> slotTiles, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> movedOutTiles, int undoCount, int moveOutCount, int shuffleCount, int reviveCount, int hintCount, int bombCount, int jokerCount, int doublePointsCount, boolean isDoublePointsActive, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> highlightedTileIds, int score, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameStatus gameStatus, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.RankingEntry> rankings, @org.jetbrains.annotations.NotNull()
    java.lang.String currentSkin, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> shakingTileIds) {
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