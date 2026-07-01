package com.example.sheeps.game.state;

import com.example.sheeps.core.multiplayer.WebSocketManager;
import com.example.sheeps.data.model.Tile;

/**
 * 对决模式（多人联机）界面状态模型。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010\"\n\u0002\b@\b\u0086\b\u0018\u00002\u00020\u0001B\u00a5\u0002\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0007\u001a\u00020\u0005\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u0012\u000e\b\u0002\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\u000e\b\u0002\u0010\r\u001a\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\u000e\b\u0002\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\b\b\u0002\u0010\u000f\u001a\u00020\u0010\u0012\b\b\u0002\u0010\u0011\u001a\u00020\u0010\u0012\b\b\u0002\u0010\u0012\u001a\u00020\u0013\u0012\b\b\u0002\u0010\u0014\u001a\u00020\u0010\u0012\b\b\u0002\u0010\u0015\u001a\u00020\u0016\u0012\n\b\u0002\u0010\u0017\u001a\u0004\u0018\u00010\u0005\u0012\n\b\u0002\u0010\u0018\u001a\u0004\u0018\u00010\u0005\u0012\b\b\u0002\u0010\u0019\u001a\u00020\u0010\u0012\b\b\u0002\u0010\u001a\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u001b\u001a\u00020\u0010\u0012\n\b\u0002\u0010\u001c\u001a\u0004\u0018\u00010\u0005\u0012\b\b\u0002\u0010\u001d\u001a\u00020\u0010\u0012\b\b\u0002\u0010\u001e\u001a\u00020\u0010\u0012\b\b\u0002\u0010\u001f\u001a\u00020\u0010\u0012\b\b\u0002\u0010 \u001a\u00020\u0003\u0012\u000e\b\u0002\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00050\"\u0012\u000e\b\u0002\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00050\"\u00a2\u0006\u0004\b$\u0010%J\t\u0010D\u001a\u00020\u0003H\u00c6\u0003J\t\u0010E\u001a\u00020\u0005H\u00c6\u0003J\t\u0010F\u001a\u00020\u0005H\u00c6\u0003J\t\u0010G\u001a\u00020\u0005H\u00c6\u0003J\t\u0010H\u001a\u00020\tH\u00c6\u0003J\u000f\u0010I\u001a\b\u0012\u0004\u0012\u00020\f0\u000bH\u00c6\u0003J\u000f\u0010J\u001a\b\u0012\u0004\u0012\u00020\f0\u000bH\u00c6\u0003J\u000f\u0010K\u001a\b\u0012\u0004\u0012\u00020\f0\u000bH\u00c6\u0003J\t\u0010L\u001a\u00020\u0010H\u00c6\u0003J\t\u0010M\u001a\u00020\u0010H\u00c6\u0003J\t\u0010N\u001a\u00020\u0013H\u00c6\u0003J\t\u0010O\u001a\u00020\u0010H\u00c6\u0003J\t\u0010P\u001a\u00020\u0016H\u00c6\u0003J\u000b\u0010Q\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u000b\u0010R\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\t\u0010S\u001a\u00020\u0010H\u00c6\u0003J\t\u0010T\u001a\u00020\u0003H\u00c6\u0003J\t\u0010U\u001a\u00020\u0010H\u00c6\u0003J\u000b\u0010V\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\t\u0010W\u001a\u00020\u0010H\u00c6\u0003J\t\u0010X\u001a\u00020\u0010H\u00c6\u0003J\t\u0010Y\u001a\u00020\u0010H\u00c6\u0003J\t\u0010Z\u001a\u00020\u0003H\u00c6\u0003J\u000f\u0010[\u001a\b\u0012\u0004\u0012\u00020\u00050\"H\u00c6\u0003J\u000f\u0010\\\u001a\b\u0012\u0004\u0012\u00020\u00050\"H\u00c6\u0003J\u00a7\u0002\u0010]\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\b\b\u0002\u0010\u0007\u001a\u00020\u00052\b\b\u0002\u0010\b\u001a\u00020\t2\u000e\b\u0002\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000b2\u000e\b\u0002\u0010\r\u001a\b\u0012\u0004\u0012\u00020\f0\u000b2\u000e\b\u0002\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\f0\u000b2\b\b\u0002\u0010\u000f\u001a\u00020\u00102\b\b\u0002\u0010\u0011\u001a\u00020\u00102\b\b\u0002\u0010\u0012\u001a\u00020\u00132\b\b\u0002\u0010\u0014\u001a\u00020\u00102\b\b\u0002\u0010\u0015\u001a\u00020\u00162\n\b\u0002\u0010\u0017\u001a\u0004\u0018\u00010\u00052\n\b\u0002\u0010\u0018\u001a\u0004\u0018\u00010\u00052\b\b\u0002\u0010\u0019\u001a\u00020\u00102\b\b\u0002\u0010\u001a\u001a\u00020\u00032\b\b\u0002\u0010\u001b\u001a\u00020\u00102\n\b\u0002\u0010\u001c\u001a\u0004\u0018\u00010\u00052\b\b\u0002\u0010\u001d\u001a\u00020\u00102\b\b\u0002\u0010\u001e\u001a\u00020\u00102\b\b\u0002\u0010\u001f\u001a\u00020\u00102\b\b\u0002\u0010 \u001a\u00020\u00032\u000e\b\u0002\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00050\"2\u000e\b\u0002\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00050\"H\u00c6\u0001J\u0013\u0010^\u001a\u00020\u00032\b\u0010_\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010`\u001a\u00020\u0010H\u00d6\u0001J\t\u0010a\u001a\u00020\u0005H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0002\u0010&R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010(R\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010(R\u0011\u0010\u0007\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010(R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010,R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010.R\u0017\u0010\r\u001a\b\u0012\u0004\u0012\u00020\f0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u0010.R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\f0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u0010.R\u0011\u0010\u000f\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b1\u00102R\u0011\u0010\u0011\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b3\u00102R\u0011\u0010\u0012\u001a\u00020\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b4\u00105R\u0011\u0010\u0014\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b6\u00102R\u0011\u0010\u0015\u001a\u00020\u0016\u00a2\u0006\b\n\u0000\u001a\u0004\b7\u00108R\u0013\u0010\u0017\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b9\u0010(R\u0013\u0010\u0018\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b:\u0010(R\u0011\u0010\u0019\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b;\u00102R\u0011\u0010\u001a\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010&R\u0011\u0010\u001b\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b<\u00102R\u0013\u0010\u001c\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b=\u0010(R\u0011\u0010\u001d\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b>\u00102R\u0011\u0010\u001e\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b?\u00102R\u0011\u0010\u001f\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b@\u00102R\u0011\u0010 \u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010&R\u0017\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00050\"\u00a2\u0006\b\n\u0000\u001a\u0004\bA\u0010BR\u0017\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00050\"\u00a2\u0006\b\n\u0000\u001a\u0004\bC\u0010B\u00a8\u0006b"}, d2 = {"Lcom/example/sheeps/game/state/DuelViewState;", "", "isLoading", "", "gameId", "", "playerId", "opponentId", "connectionState", "Lcom/example/sheeps/core/multiplayer/WebSocketManager$ConnectionState;", "boardTiles", "", "Lcom/example/sheeps/data/model/Tile;", "slotTiles", "movedOutTiles", "score", "", "combo", "opponentProgress", "", "opponentScore", "gameStatus", "Lcom/example/sheeps/game/state/GameStatus;", "incomingAttackMessage", "winnerId", "currentEnergy", "isFogActive", "maxSlotSize", "activeSpellMessage", "totalTileCount", "opponentEliminatedCount", "spellCountdownSeconds", "isSilenced", "usedSpells", "", "shakingTileIds", "<init>", "(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/example/sheeps/core/multiplayer/WebSocketManager$ConnectionState;Ljava/util/List;Ljava/util/List;Ljava/util/List;IIFILcom/example/sheeps/game/state/GameStatus;Ljava/lang/String;Ljava/lang/String;IZILjava/lang/String;IIIZLjava/util/Set;Ljava/util/Set;)V", "()Z", "getGameId", "()Ljava/lang/String;", "getPlayerId", "getOpponentId", "getConnectionState", "()Lcom/example/sheeps/core/multiplayer/WebSocketManager$ConnectionState;", "getBoardTiles", "()Ljava/util/List;", "getSlotTiles", "getMovedOutTiles", "getScore", "()I", "getCombo", "getOpponentProgress", "()F", "getOpponentScore", "getGameStatus", "()Lcom/example/sheeps/game/state/GameStatus;", "getIncomingAttackMessage", "getWinnerId", "getCurrentEnergy", "getMaxSlotSize", "getActiveSpellMessage", "getTotalTileCount", "getOpponentEliminatedCount", "getSpellCountdownSeconds", "getUsedSpells", "()Ljava/util/Set;", "getShakingTileIds", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "component10", "component11", "component12", "component13", "component14", "component15", "component16", "component17", "component18", "component19", "component20", "component21", "component22", "component23", "component24", "component25", "copy", "equals", "other", "hashCode", "toString", "feature_game_debug"})
public final class DuelViewState {
    
    /**
     * 是否正在加载对局资源
     */
    private final boolean isLoading = false;
    
    /**
     * 当前对局房间 ID
     */
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String gameId = null;
    
    /**
     * 本地玩家 ID
     */
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String playerId = null;
    
    /**
     * 对手玩家 ID
     */
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String opponentId = null;
    
    /**
     * 实时连接状态（WebSocket 状态）
     */
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.core.multiplayer.WebSocketManager.ConnectionState connectionState = null;
    
    /**
     * 本地棋盘卡牌列表
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.sheeps.data.model.Tile> boardTiles = null;
    
    /**
     * 本地消除槽卡牌列表
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.sheeps.data.model.Tile> slotTiles = null;
    
    /**
     * 本地已移出暂存区的卡牌列表
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.sheeps.data.model.Tile> movedOutTiles = null;
    
    /**
     * 本地玩家得分
     */
    private final int score = 0;
    
    /**
     * 当前连续消除组合数（连击）
     */
    private final int combo = 0;
    
    /**
     * 对手完成进度的比例（0.0 到 1.0）
     */
    private final float opponentProgress = 0.0F;
    
    /**
     * 对手玩家得分
     */
    private final int opponentScore = 0;
    
    /**
     * 对局运行状态
     */
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.game.state.GameStatus gameStatus = null;
    
    /**
     * 正在受到的攻击提示文本
     */
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String incomingAttackMessage = null;
    
    /**
     * 胜者玩家 ID（对局结束后赋值）
     */
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String winnerId = null;
    
    /**
     * 玩家当前能量值（上限 10，用于释放大招）
     */
    private final int currentEnergy = 0;
    
    /**
     * 当前是否被迷雾致盲（对方释放了迷雾咒）
     */
    private final boolean isFogActive = false;
    
    /**
     * 消除槽最大容量（对方释放缩槽咒时会从 7 变为 6）
     */
    private final int maxSlotSize = 0;
    
    /**
     * 正在生效的法术状态描述消息
     */
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String activeSpellMessage = null;
    
    /**
     * 对局初始卡牌总数（用于精准进度计算）
     */
    private final int totalTileCount = 0;
    
    /**
     * 对手已累计消除的卡牌总数
     */
    private final int opponentEliminatedCount = 0;
    
    /**
     * 诅咒/道具效果生效剩余秒数
     */
    private final int spellCountdownSeconds = 0;
    
    /**
     * 是否处于“禁魔/沉默”状态，无法释放法术
     */
    private final boolean isSilenced = false;
    
    /**
     * 本局内已使用过的大招 Key 集合（单局限次）
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> usedSpells = null;
    
    /**
     * 正在执行阻挡反馈动画的卡牌 ID 集合
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> shakingTileIds = null;
    
    public DuelViewState(boolean isLoading, @org.jetbrains.annotations.NotNull()
    java.lang.String gameId, @org.jetbrains.annotations.NotNull()
    java.lang.String playerId, @org.jetbrains.annotations.NotNull()
    java.lang.String opponentId, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.multiplayer.WebSocketManager.ConnectionState connectionState, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> boardTiles, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> slotTiles, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> movedOutTiles, int score, int combo, float opponentProgress, int opponentScore, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameStatus gameStatus, @org.jetbrains.annotations.Nullable()
    java.lang.String incomingAttackMessage, @org.jetbrains.annotations.Nullable()
    java.lang.String winnerId, int currentEnergy, boolean isFogActive, int maxSlotSize, @org.jetbrains.annotations.Nullable()
    java.lang.String activeSpellMessage, int totalTileCount, int opponentEliminatedCount, int spellCountdownSeconds, boolean isSilenced, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> usedSpells, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> shakingTileIds) {
        super();
    }
    
    /**
     * 是否正在加载对局资源
     */
    public final boolean isLoading() {
        return false;
    }
    
    /**
     * 当前对局房间 ID
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getGameId() {
        return null;
    }
    
    /**
     * 本地玩家 ID
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getPlayerId() {
        return null;
    }
    
    /**
     * 对手玩家 ID
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getOpponentId() {
        return null;
    }
    
    /**
     * 实时连接状态（WebSocket 状态）
     */
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.core.multiplayer.WebSocketManager.ConnectionState getConnectionState() {
        return null;
    }
    
    /**
     * 本地棋盘卡牌列表
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.Tile> getBoardTiles() {
        return null;
    }
    
    /**
     * 本地消除槽卡牌列表
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.Tile> getSlotTiles() {
        return null;
    }
    
    /**
     * 本地已移出暂存区的卡牌列表
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.Tile> getMovedOutTiles() {
        return null;
    }
    
    /**
     * 本地玩家得分
     */
    public final int getScore() {
        return 0;
    }
    
    /**
     * 当前连续消除组合数（连击）
     */
    public final int getCombo() {
        return 0;
    }
    
    /**
     * 对手完成进度的比例（0.0 到 1.0）
     */
    public final float getOpponentProgress() {
        return 0.0F;
    }
    
    /**
     * 对手玩家得分
     */
    public final int getOpponentScore() {
        return 0;
    }
    
    /**
     * 对局运行状态
     */
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.game.state.GameStatus getGameStatus() {
        return null;
    }
    
    /**
     * 正在受到的攻击提示文本
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getIncomingAttackMessage() {
        return null;
    }
    
    /**
     * 胜者玩家 ID（对局结束后赋值）
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getWinnerId() {
        return null;
    }
    
    /**
     * 玩家当前能量值（上限 10，用于释放大招）
     */
    public final int getCurrentEnergy() {
        return 0;
    }
    
    /**
     * 当前是否被迷雾致盲（对方释放了迷雾咒）
     */
    public final boolean isFogActive() {
        return false;
    }
    
    /**
     * 消除槽最大容量（对方释放缩槽咒时会从 7 变为 6）
     */
    public final int getMaxSlotSize() {
        return 0;
    }
    
    /**
     * 正在生效的法术状态描述消息
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getActiveSpellMessage() {
        return null;
    }
    
    /**
     * 对局初始卡牌总数（用于精准进度计算）
     */
    public final int getTotalTileCount() {
        return 0;
    }
    
    /**
     * 对手已累计消除的卡牌总数
     */
    public final int getOpponentEliminatedCount() {
        return 0;
    }
    
    /**
     * 诅咒/道具效果生效剩余秒数
     */
    public final int getSpellCountdownSeconds() {
        return 0;
    }
    
    /**
     * 是否处于“禁魔/沉默”状态，无法释放法术
     */
    public final boolean isSilenced() {
        return false;
    }
    
    /**
     * 本局内已使用过的大招 Key 集合（单局限次）
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.String> getUsedSpells() {
        return null;
    }
    
    /**
     * 正在执行阻挡反馈动画的卡牌 ID 集合
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.String> getShakingTileIds() {
        return null;
    }
    
    public DuelViewState() {
        super();
    }
    
    public final boolean component1() {
        return false;
    }
    
    public final int component10() {
        return 0;
    }
    
    public final float component11() {
        return 0.0F;
    }
    
    public final int component12() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.game.state.GameStatus component13() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component14() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component15() {
        return null;
    }
    
    public final int component16() {
        return 0;
    }
    
    public final boolean component17() {
        return false;
    }
    
    public final int component18() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component19() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    public final int component20() {
        return 0;
    }
    
    public final int component21() {
        return 0;
    }
    
    public final int component22() {
        return 0;
    }
    
    public final boolean component23() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.String> component24() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.String> component25() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.core.multiplayer.WebSocketManager.ConnectionState component5() {
        return null;
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
    public final com.example.sheeps.game.state.DuelViewState copy(boolean isLoading, @org.jetbrains.annotations.NotNull()
    java.lang.String gameId, @org.jetbrains.annotations.NotNull()
    java.lang.String playerId, @org.jetbrains.annotations.NotNull()
    java.lang.String opponentId, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.multiplayer.WebSocketManager.ConnectionState connectionState, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> boardTiles, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> slotTiles, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> movedOutTiles, int score, int combo, float opponentProgress, int opponentScore, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameStatus gameStatus, @org.jetbrains.annotations.Nullable()
    java.lang.String incomingAttackMessage, @org.jetbrains.annotations.Nullable()
    java.lang.String winnerId, int currentEnergy, boolean isFogActive, int maxSlotSize, @org.jetbrains.annotations.Nullable()
    java.lang.String activeSpellMessage, int totalTileCount, int opponentEliminatedCount, int spellCountdownSeconds, boolean isSilenced, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> usedSpells, @org.jetbrains.annotations.NotNull()
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