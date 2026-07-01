package com.example.sheeps.game.viewmodel.delegates;

import com.example.sheeps.core.multiplayer.model.CommandType;
import com.example.sheeps.core.multiplayer.model.GameCommand;
import com.example.sheeps.data.model.TileState;
import com.example.sheeps.game.state.DuelViewEffect;
import com.example.sheeps.game.state.DuelViewState;
import com.example.sheeps.game.state.GameStatus;
import javax.inject.Inject;

/**
 * 对决模式远程指令处理委派类
 * 负责接收并应用来自对手或服务器的指令（攻击、施法、系统事件）
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\t\b\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0097\u0001\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2#\u0010\u000e\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\u0002\b\u0010\u0012\u0004\u0012\u00020\u00070\u000f2\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u00070\u000f2*\u0010\u0013\u001a&\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u00150\u00142\u0012\u0010\u0016\u001a\u000e\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u00070\u000fJ=\u0010\u0017\u001a\u00020\u00072\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2#\u0010\u000e\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\u0002\b\u0010\u0012\u0004\u0012\u00020\u00070\u000fH\u0002J=\u0010\u0018\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\r2#\u0010\u000e\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\u0002\b\u0010\u0012\u0004\u0012\u00020\u00070\u000fH\u0002JY\u0010\u0019\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\u001a\u001a\u00020\u00152\u0006\u0010\f\u001a\u00020\r2#\u0010\u000e\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\u0002\b\u0010\u0012\u0004\u0012\u00020\u00070\u000f2\u0012\u0010\u0016\u001a\u000e\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u00070\u000fH\u0002JS\u0010\u001b\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u001a\u001a\u00020\u00152#\u0010\u000e\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\u0002\b\u0010\u0012\u0004\u0012\u00020\u00070\u000f2\f\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00070\u001fH\u0002J}\u0010 \u001a\u00020\u00072\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2#\u0010\u000e\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\u0002\b\u0010\u0012\u0004\u0012\u00020\u00070\u000f2\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u00070\u000f2*\u0010\u0013\u001a&\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u00150\u0014H\u0002J-\u0010!\u001a\u00020\u00072#\u0010\u000e\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\u0002\b\u0010\u0012\u0004\u0012\u00020\u00070\u000fH\u0002Ju\u0010\"\u001a\u00020\u00072\u0006\u0010\f\u001a\u00020\r2#\u0010\u000e\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\u0002\b\u0010\u0012\u0004\u0012\u00020\u00070\u000f2\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u00070\u000f2*\u0010\u0013\u001a&\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u00150\u0014H\u0002R\u0010\u0010\u0004\u001a\u0004\u0018\u00010\u0005X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006#"}, d2 = {"Lcom/example/sheeps/game/viewmodel/delegates/DuelCommandHandler;", "", "<init>", "()V", "countdownJob", "Lkotlinx/coroutines/Job;", "handleRemoteCommand", "", "scope", "Lkotlinx/coroutines/CoroutineScope;", "command", "Lcom/example/sheeps/core/multiplayer/model/GameCommand;", "state", "Lcom/example/sheeps/game/state/DuelViewState;", "updateState", "Lkotlin/Function1;", "Lkotlin/ExtensionFunctionType;", "setEffect", "Lcom/example/sheeps/game/state/DuelViewEffect;", "getLocalizedString", "Lkotlin/Function5;", "", "sendSystemEvent", "handleOpponentEliminate", "applyAttackEffect", "applySpellEffect", "spellType", "startSpellCountdown", "seconds", "", "onFinished", "Lkotlin/Function0;", "handleSystemEvent", "handleOpponentReconnected", "handleOpponentTimeout", "feature_game_release"})
public final class DuelCommandHandler {
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job countdownJob;
    
    @javax.inject.Inject()
    public DuelCommandHandler() {
        super();
    }
    
    /**
     * 处理远程指令
     */
    public final void handleRemoteCommand(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.multiplayer.model.GameCommand command, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.DuelViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewState, com.example.sheeps.game.state.DuelViewState>, kotlin.Unit> updateState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewEffect, kotlin.Unit> setEffect, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function5<? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, java.lang.String> getLocalizedString, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> sendSystemEvent) {
    }
    
    private final void handleOpponentEliminate(com.example.sheeps.core.multiplayer.model.GameCommand command, com.example.sheeps.game.state.DuelViewState state, kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewState, com.example.sheeps.game.state.DuelViewState>, kotlin.Unit> updateState) {
    }
    
    private final void applyAttackEffect(kotlinx.coroutines.CoroutineScope scope, com.example.sheeps.game.state.DuelViewState state, kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewState, com.example.sheeps.game.state.DuelViewState>, kotlin.Unit> updateState) {
    }
    
    private final void applySpellEffect(kotlinx.coroutines.CoroutineScope scope, java.lang.String spellType, com.example.sheeps.game.state.DuelViewState state, kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewState, com.example.sheeps.game.state.DuelViewState>, kotlin.Unit> updateState, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> sendSystemEvent) {
    }
    
    private final void startSpellCountdown(kotlinx.coroutines.CoroutineScope scope, int seconds, java.lang.String spellType, kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewState, com.example.sheeps.game.state.DuelViewState>, kotlin.Unit> updateState, kotlin.jvm.functions.Function0<kotlin.Unit> onFinished) {
    }
    
    private final void handleSystemEvent(com.example.sheeps.core.multiplayer.model.GameCommand command, com.example.sheeps.game.state.DuelViewState state, kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewState, com.example.sheeps.game.state.DuelViewState>, kotlin.Unit> updateState, kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewEffect, kotlin.Unit> setEffect, kotlin.jvm.functions.Function5<? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, java.lang.String> getLocalizedString) {
    }
    
    private final void handleOpponentReconnected(kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewState, com.example.sheeps.game.state.DuelViewState>, kotlin.Unit> updateState) {
    }
    
    private final void handleOpponentTimeout(com.example.sheeps.game.state.DuelViewState state, kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewState, com.example.sheeps.game.state.DuelViewState>, kotlin.Unit> updateState, kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.DuelViewEffect, kotlin.Unit> setEffect, kotlin.jvm.functions.Function5<? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, java.lang.String> getLocalizedString) {
    }
}