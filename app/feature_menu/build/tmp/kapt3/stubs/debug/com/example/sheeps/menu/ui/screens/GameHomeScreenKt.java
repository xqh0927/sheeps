package com.example.sheeps.menu.ui.screens;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.graphics.drawscope.Stroke;
import androidx.compose.ui.text.font.FontWeight;
import com.example.sheeps.menu.state.MenuViewState;
import com.example.sheeps.theme.*;
import com.example.sheeps.core.R;
import com.hjq.toast.Toaster;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000:\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u001a\u00a4\u0001\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00030\u00072\u0012\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00030\u00072\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00030\u000b2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00030\u000b2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00030\u000b2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00030\u000b2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00030\u000b2$\u0010\u0010\u001a \u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00030\u0011H\u0007\u001a$\u0010\u0013\u001a\u00020\u0003*\u00020\u00142\u0006\u0010\u0015\u001a\u00020\b2\b\b\u0002\u0010\u0016\u001a\u00020\bH\u0082@\u00a2\u0006\u0002\u0010\u0017\"\u000e\u0010\u0000\u001a\u00020\u0001X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"isColdStartAutoScrolled", "", "GameHomeScreen", "", "state", "Lcom/example/sheeps/menu/state/MenuViewState;", "onLevelClick", "Lkotlin/Function1;", "", "onShowLeaderboard", "onNoticeClick", "Lkotlin/Function0;", "onLoginClick", "onJoinMatch", "onLeaveMatch", "onResetMatch", "onNavigateToDuel", "Lkotlin/Function4;", "", "animateScrollToItemSmoothly", "Landroidx/compose/foundation/lazy/LazyListState;", "index", "scrollOffset", "(Landroidx/compose/foundation/lazy/LazyListState;IILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "feature_menu_debug"})
public final class GameHomeScreenKt {
    private static boolean isColdStartAutoScrolled = false;
    
    @androidx.compose.runtime.Composable()
    public static final void GameHomeScreen(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.menu.state.MenuViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> onLevelClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> onShowLeaderboard, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNoticeClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onLoginClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onJoinMatch, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onLeaveMatch, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onResetMatch, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function4<? super java.lang.String, ? super java.lang.String, ? super java.lang.Integer, ? super java.lang.Integer, kotlin.Unit> onNavigateToDuel) {
    }
    
    /**
     * 平滑且低卡顿滚动到目标 Item 的扩展方法
     * 针对长距离滚动进行优化：如果跨度大，则先进行无感知 Snap 跳转到附近，再启动平滑动画滚动
     */
    private static final java.lang.Object animateScrollToItemSmoothly(androidx.compose.foundation.lazy.LazyListState $this$animateScrollToItemSmoothly, int index, int scrollOffset, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}