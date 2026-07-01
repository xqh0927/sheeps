package com.example.sheeps.game.ui.components;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import com.example.sheeps.game.state.GameViewState;
import com.example.sheeps.core.R;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000*\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\u001ar\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u001a$\u0010\f\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u00010\u000eH\u0003\u001a&\u0010\u0010\u001a\u00020\u00012\u0006\u0010\u0011\u001a\u00020\u000f2\u0006\u0010\u0012\u001a\u00020\u00132\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u00a8\u0006\u0015"}, d2 = {"GameTools", "", "state", "Lcom/example/sheeps/game/state/GameViewState;", "onUseMoveOut", "Lkotlin/Function0;", "onUseUndo", "onUseShuffle", "onUseHint", "onUseBomb", "onUseJoker", "onUseDouble", "CarriedItemsSection", "onToolClick", "Lkotlin/Function1;", "", "CarriedItemIcon", "type", "count", "", "onClick", "feature_game_debug"})
public final class GameToolsKt {
    
    /**
     * 游戏道具栏组件
     * 包含：可使用的道具按钮行、已携带法宝展示区
     */
    @androidx.compose.runtime.Composable()
    public static final void GameTools(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseMoveOut, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseUndo, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseShuffle, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseHint, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseBomb, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseJoker, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseDouble) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void CarriedItemsSection(com.example.sheeps.game.state.GameViewState state, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onToolClick) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void CarriedItemIcon(java.lang.String type, int count, kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
}