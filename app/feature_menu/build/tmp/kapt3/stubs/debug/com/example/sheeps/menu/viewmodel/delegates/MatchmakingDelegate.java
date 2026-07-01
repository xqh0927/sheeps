package com.example.sheeps.menu.viewmodel.delegates;

import com.example.sheeps.data.model.MatchJoinRequest;
import com.example.sheeps.data.network.ApiService;
import com.example.sheeps.menu.state.MenuViewState;
import javax.inject.Inject;

/**
 * 匹配系统逻辑委派类
 * 处理在线匹配的加入、取消以及状态轮询
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u0011\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005JC\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2#\u0010\u000e\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\u0002\b\u0010\u0012\u0004\u0012\u00020\u00070\u000fJK\u0010\u0011\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2#\u0010\u000e\u001a\u001f\u0012\u0015\u0012\u0013\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\u0002\b\u0010\u0012\u0004\u0012\u00020\u00070\u000fH\u0082@\u00a2\u0006\u0002\u0010\u0012J\u0016\u0010\u0013\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/example/sheeps/menu/viewmodel/delegates/MatchmakingDelegate;", "", "apiService", "Lcom/example/sheeps/data/network/ApiService;", "<init>", "(Lcom/example/sheeps/data/network/ApiService;)V", "handleJoinMatch", "", "scope", "Lkotlinx/coroutines/CoroutineScope;", "playerId", "", "currentState", "Lcom/example/sheeps/menu/state/MenuViewState;", "updateState", "Lkotlin/Function1;", "Lkotlin/ExtensionFunctionType;", "pollMatchStatus", "(Lkotlinx/coroutines/CoroutineScope;Ljava/lang/String;Lcom/example/sheeps/menu/state/MenuViewState;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleLeaveMatch", "feature_menu_debug"})
public final class MatchmakingDelegate {
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.network.ApiService apiService = null;
    
    @javax.inject.Inject()
    public MatchmakingDelegate(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.network.ApiService apiService) {
        super();
    }
    
    /**
     * 处理加入匹配
     *
     * @param scope 协程作用域
     * @param playerId 玩家ID
     * @param currentState 当前界面状态
     * @param updateState 状态更新回调
     */
    public final void handleJoinMatch(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, @org.jetbrains.annotations.NotNull()
    java.lang.String playerId, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.menu.state.MenuViewState currentState, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.menu.state.MenuViewState, com.example.sheeps.menu.state.MenuViewState>, kotlin.Unit> updateState) {
    }
    
    /**
     * 轮询匹配状态
     */
    private final java.lang.Object pollMatchStatus(kotlinx.coroutines.CoroutineScope scope, java.lang.String playerId, com.example.sheeps.menu.state.MenuViewState currentState, kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function1<? super com.example.sheeps.menu.state.MenuViewState, com.example.sheeps.menu.state.MenuViewState>, kotlin.Unit> updateState, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * 处理离开匹配
     */
    public final void handleLeaveMatch(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, @org.jetbrains.annotations.NotNull()
    java.lang.String playerId) {
    }
}