package com.example.sheeps.game.viewmodel.delegates;

import com.example.sheeps.data.model.Tile;
import com.example.sheeps.data.model.TileState;
import com.example.sheeps.game.state.GameViewEffect;
import com.example.sheeps.game.state.GameViewState;
import com.example.sheeps.game.state.SoundType;
import javax.inject.Inject;

/**
 * 游戏道具逻辑委派类
 * 处理撤销、移出、洗牌、提示、炸弹、万能牌、双倍积分等逻辑
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\t\b\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0087\u0001\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u000720\u0010\b\u001a,\u0012(\u0012&\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\n0\t2#\u0010\r\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u000e\u00a2\u0006\u0002\b\u000f\u0012\u0004\u0012\u00020\u00050\u000e2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00050\u000e2\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00050\u0013JU\u0010\u0014\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072#\u0010\r\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u000e\u00a2\u0006\u0002\b\u000f\u0012\u0004\u0012\u00020\u00050\u000e2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00050\u000e2\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00050\u0013JU\u0010\u0015\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072#\u0010\r\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u000e\u00a2\u0006\u0002\b\u000f\u0012\u0004\u0012\u00020\u00050\u000e2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00050\u000e2\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00050\u0013J\u0081\u0001\u0010\u0016\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072*\u0010\u0017\u001a&\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u00190\u00182#\u0010\r\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u000e\u00a2\u0006\u0002\b\u000f\u0012\u0004\u0012\u00020\u00050\u000e2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00050\u000e2\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00050\u0013J\u00c8\u0001\u0010\u001a\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072*\u0010\u0017\u001a&\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u00190\u00182#\u0010\r\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u000e\u00a2\u0006\u0002\b\u000f\u0012\u0004\u0012\u00020\u00050\u000e2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00050\u000e2\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00050\u00132@\u0010\u001b\u001a<\b\u0001\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u001d\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u001c\u00a2\u0006\u0002\u0010\u001eJ\u00d6\u0001\u0010\u001f\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072*\u0010\u0017\u001a&\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u00190\u00182#\u0010\r\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070\u000e\u00a2\u0006\u0002\b\u000f\u0012\u0004\u0012\u00020\u00050\u000e2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00050\u000e2\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00050\u00132\f\u0010 \u001a\b\u0012\u0004\u0012\u00020\u00050\u00132@\u0010\u001b\u001a<\b\u0001\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u001d\u0012\u0006\u0012\u0004\u0018\u00010\u00010\u001c\u00a2\u0006\u0002\u0010!\u00a8\u0006\""}, d2 = {"Lcom/example/sheeps/game/viewmodel/delegates/GameToolDelegate;", "", "<init>", "()V", "handleUseUndo", "", "state", "Lcom/example/sheeps/game/state/GameViewState;", "historyStack", "", "Lkotlin/Triple;", "", "Lcom/example/sheeps/data/model/Tile;", "updateState", "Lkotlin/Function1;", "Lkotlin/ExtensionFunctionType;", "setEffect", "Lcom/example/sheeps/game/state/GameViewEffect;", "onToolUsed", "Lkotlin/Function0;", "handleUseMoveOut", "handleUseShuffle", "handleUseHint", "getLocalizedString", "Lkotlin/Function5;", "", "handleUseBomb", "processSlotMatch", "Lkotlin/Function4;", "Lkotlin/coroutines/Continuation;", "(Lcom/example/sheeps/game/state/GameViewState;Lkotlin/jvm/functions/Function5;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function4;)V", "handleUseJoker", "onAddHistory", "(Lcom/example/sheeps/game/state/GameViewState;Lkotlin/jvm/functions/Function5;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function4;)V", "feature_game_release"})
public final class GameToolDelegate {
    
    @javax.inject.Inject()
    public GameToolDelegate() {
        super();
    }
    
    /**
     * 处理撤销操作
     */
    public final void handleUseUndo(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameViewState state, @org.jetbrains.annotations.NotNull()
    java.util.List<kotlin.Triple<java.util.List<com.example.sheeps.data.model.Tile>, java.util.List<com.example.sheeps.data.model.Tile>, java.util.List<com.example.sheeps.data.model.Tile>>> historyStack, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewState, com.example.sheeps.game.state.GameViewState>, kotlin.Unit> updateState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewEffect, kotlin.Unit> setEffect, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onToolUsed) {
    }
    
    /**
     * 处理移出槽位操作
     */
    public final void handleUseMoveOut(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewState, com.example.sheeps.game.state.GameViewState>, kotlin.Unit> updateState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewEffect, kotlin.Unit> setEffect, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onToolUsed) {
    }
    
    /**
     * 处理洗牌操作
     */
    public final void handleUseShuffle(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewState, com.example.sheeps.game.state.GameViewState>, kotlin.Unit> updateState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewEffect, kotlin.Unit> setEffect, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onToolUsed) {
    }
    
    /**
     * 处理提示操作
     */
    public final void handleUseHint(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function5<? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, java.lang.String> getLocalizedString, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewState, com.example.sheeps.game.state.GameViewState>, kotlin.Unit> updateState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewEffect, kotlin.Unit> setEffect, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onToolUsed) {
    }
    
    /**
     * 处理炸弹操作
     */
    public final void handleUseBomb(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function5<? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, java.lang.String> getLocalizedString, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewState, com.example.sheeps.game.state.GameViewState>, kotlin.Unit> updateState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewEffect, kotlin.Unit> setEffect, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onToolUsed, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function4<? super java.util.List<com.example.sheeps.data.model.Tile>, ? super java.util.List<com.example.sheeps.data.model.Tile>, ? super java.util.List<com.example.sheeps.data.model.Tile>, ? super kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object> processSlotMatch) {
    }
    
    /**
     * 处理万能牌（太极牌）操作
     */
    public final void handleUseJoker(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function5<? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, java.lang.String> getLocalizedString, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewState, com.example.sheeps.game.state.GameViewState>, kotlin.Unit> updateState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewEffect, kotlin.Unit> setEffect, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onToolUsed, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onAddHistory, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function4<? super java.util.List<com.example.sheeps.data.model.Tile>, ? super java.util.List<com.example.sheeps.data.model.Tile>, ? super java.util.List<com.example.sheeps.data.model.Tile>, ? super kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object> processSlotMatch) {
    }
}