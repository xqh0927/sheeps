package com.example.sheeps.game.viewmodel.delegates;

import com.example.sheeps.data.model.Tile;
import com.example.sheeps.data.model.TileState;
import com.example.sheeps.game.state.*;
import javax.inject.Inject;

/**
 * 对决模式操作委派类
 * 处理玩家点击卡牌、匹配检测及施放法术逻辑
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000^\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\u0018\u00002\u00020\u0001B\t\b\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u009e\u0001\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2#\u0010\f\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\r\u00a2\u0006\u0002\b\u000e\u0012\u0004\u0012\u00020\u00050\r2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0010\u0012\u0004\u0012\u00020\u00050\r2@\u0010\u0011\u001a<\b\u0001\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\u0013\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\u0013\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\u0013\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u0014\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u0012\u00a2\u0006\u0002\u0010\u0015J\u00a1\u0001\u0010\u0011\u001a\u00020\u00052\u0006\u0010\n\u001a\u00020\u000b2\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\t0\u00132\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\t0\u00132\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\t0\u00132#\u0010\f\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\r\u00a2\u0006\u0002\b\u000e\u0012\u0004\u0012\u00020\u00050\r2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0010\u0012\u0004\u0012\u00020\u00050\r2\u0018\u0010\u0019\u001a\u0014\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001a0\u0013\u0012\u0004\u0012\u00020\u00050\r2\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00050\u001cH\u0086@\u00a2\u0006\u0002\u0010\u001dJ\u008f\u0001\u0010\u001e\u001a\u00020\u00052\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u001f\u001a\u00020\u001a2*\u0010 \u001a&\u0012\u0004\u0012\u00020\u001a\u0012\u0004\u0012\u00020\u001a\u0012\u0004\u0012\u00020\u001a\u0012\u0004\u0012\u00020\u001a\u0012\u0004\u0012\u00020\u001a\u0012\u0004\u0012\u00020\u001a0!2#\u0010\f\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\r\u00a2\u0006\u0002\b\u000e\u0012\u0004\u0012\u00020\u00050\r2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0010\u0012\u0004\u0012\u00020\u00050\r2\u0012\u0010\"\u001a\u000e\u0012\u0004\u0012\u00020#\u0012\u0004\u0012\u00020\u00050\r\u00a8\u0006$"}, d2 = {"Lcom/example/sheeps/game/viewmodel/delegates/DuelActionDelegate;", "", "<init>", "()V", "handleClickTile", "", "scope", "Lkotlinx/coroutines/CoroutineScope;", "tile", "Lcom/example/sheeps/data/model/Tile;", "state", "Lcom/example/sheeps/game/state/DuelViewState;", "updateState", "Lkotlin/Function1;", "Lkotlin/ExtensionFunctionType;", "setEffect", "Lcom/example/sheeps/game/state/DuelViewEffect;", "processSlotMatch", "Lkotlin/Function4;", "", "Lkotlin/coroutines/Continuation;", "(Lkotlinx/coroutines/CoroutineScope;Lcom/example/sheeps/data/model/Tile;Lcom/example/sheeps/game/state/DuelViewState;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function4;)V", "board", "slot", "movedOut", "onMatchSuccess", "", "onVictory", "Lkotlin/Function0;", "(Lcom/example/sheeps/game/state/DuelViewState;Ljava/util/List;Ljava/util/List;Ljava/util/List;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleCastSpell", "spellType", "getLocalizedString", "Lkotlin/Function5;", "onCastSuccess", "", "feature_game_debug"})
public final class DuelActionDelegate {
    
    @javax.inject.Inject()
    public DuelActionDelegate() {
        super();
    }
    
    /**
     * 处理卡牌点击逻辑
     */
    public final void handleClickTile(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.Tile tile, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.DuelViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewState, com.example.sheeps.game.state.DuelViewState>, kotlin.Unit> updateState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewEffect, kotlin.Unit> setEffect, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function4<? super java.util.List<com.example.sheeps.data.model.Tile>, ? super java.util.List<com.example.sheeps.data.model.Tile>, ? super java.util.List<com.example.sheeps.data.model.Tile>, ? super kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object> processSlotMatch) {
    }
    
    /**
     * 执行槽位匹配
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object processSlotMatch(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.DuelViewState state, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> board, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> slot, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Tile> movedOut, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewState, com.example.sheeps.game.state.DuelViewState>, kotlin.Unit> updateState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewEffect, kotlin.Unit> setEffect, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.util.List<java.lang.String>, kotlin.Unit> onMatchSuccess, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onVictory, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * 处理施放法术逻辑
     */
    public final void handleCastSpell(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.DuelViewState state, @org.jetbrains.annotations.NotNull()
    java.lang.String spellType, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function5<? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, java.lang.String> getLocalizedString, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewState, com.example.sheeps.game.state.DuelViewState>, kotlin.Unit> updateState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewEffect, kotlin.Unit> setEffect, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> onCastSuccess) {
    }
}