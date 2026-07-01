package com.example.sheeps.game.viewmodel.delegates;

import com.example.sheeps.data.model.Tile;
import com.example.sheeps.data.model.TileState;
import com.example.sheeps.game.state.*;
import javax.inject.Inject;

/**
 * 游戏核心逻辑委派类
 * 处理卡片点击、匹配检测、输赢判定
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\t\b\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u00ac\u0001\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00050\r2#\u0010\u000e\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\u000f\u00a2\u0006\u0002\b\u0010\u0012\u0004\u0012\u00020\u00050\u000f2\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u00050\u000f2@\u0010\u0013\u001a<\b\u0001\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\u0015\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\u0015\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\u0015\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u0016\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u0014\u00a2\u0006\u0002\u0010\u0017J\u008f\u0001\u0010\u0018\u001a\u00020\u00192\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\t0\u00152\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\t0\u00152\f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\t0\u00152\u0006\u0010\u001d\u001a\u00020\u00192\u0006\u0010\u001e\u001a\u00020\u001f2#\u0010\u000e\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\u000f\u00a2\u0006\u0002\b\u0010\u0012\u0004\u0012\u00020\u00050\u000f2\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u00050\u000f2\f\u0010 \u001a\b\u0012\u0004\u0012\u00020\u00050\rH\u0086@\u00a2\u0006\u0002\u0010!J$\u0010\"\u001a\b\u0012\u0004\u0012\u00020\t0\u00152\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\t0\u00152\u0006\u0010#\u001a\u00020\tH\u0002\u00a8\u0006$"}, d2 = {"Lcom/example/sheeps/game/viewmodel/delegates/GameLogicDelegate;", "", "<init>", "()V", "handleClickTile", "", "scope", "Lkotlinx/coroutines/CoroutineScope;", "tile", "Lcom/example/sheeps/data/model/Tile;", "state", "Lcom/example/sheeps/game/state/GameViewState;", "onAddHistory", "Lkotlin/Function0;", "updateState", "Lkotlin/Function1;", "Lkotlin/ExtensionFunctionType;", "setEffect", "Lcom/example/sheeps/game/state/GameViewEffect;", "processSlotMatch", "Lkotlin/Function4;", "", "Lkotlin/coroutines/Continuation;", "(Lkotlinx/coroutines/CoroutineScope;Lcom/example/sheeps/data/model/Tile;Lcom/example/sheeps/game/state/GameViewState;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function4;)V", "processSlotMatchAndCheckEndGame", "", "board", "slot", "movedOut", "currentScore", "isDoublePoints", "", "onVictory", "(Ljava/util/List;Ljava/util/List;Ljava/util/List;IZLkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insertIntoSlot", "newTile", "feature_game_debug"})
public final class GameLogicDelegate {
    
    @javax.inject.Inject()
    public GameLogicDelegate() {
        super();
    }
    
    /**
     * 处理卡片点击
     */
    public final void handleClickTile(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.Tile tile, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onAddHistory, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewState, com.example.sheeps.game.state.GameViewState>, kotlin.Unit> updateState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewEffect, kotlin.Unit> setEffect, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function4<? super java.util.List<com.example.sheeps.data.model.Tile>, ? super java.util.List<com.example.sheeps.data.model.Tile>, ? super java.util.List<com.example.sheeps.data.model.Tile>, ? super kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object> processSlotMatch) {
    }
    
    /**
     * 执行槽位匹配检测与输赢判定
     *
     * @return 最终的得分增加值
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object processSlotMatchAndCheckEndGame(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> board, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> slot, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> movedOut, int currentScore, boolean isDoublePoints, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewState, com.example.sheeps.game.state.GameViewState>, kotlin.Unit> updateState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewEffect, kotlin.Unit> setEffect, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onVictory, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
    
    /**
     * 辅助函数：向卡槽中追加卡牌。
     * 如果卡槽中已存在相同图案的牌，则插入到最后一个相同牌的后面；
     * 否则，直接追加到卡槽尾部。
     */
    private final java.util.List<com.example.sheeps.data.model.Tile> insertIntoSlot(java.util.List<com.example.sheeps.data.model.Tile> slot, com.example.sheeps.data.model.Tile newTile) {
        return null;
    }
}