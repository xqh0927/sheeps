package com.example.sheeps.game.ui.components;

import androidx.compose.foundation.layout.*;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import com.example.sheeps.game.state.GameViewState;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000$\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\u001ar\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u001ar\u0010\f\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u001a\u0010\u0010\r\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\u0003\u001a \u0010\u000e\u001a\u00020\u00012\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00102\u0006\u0010\u0012\u001a\u00020\u0013H\u0003\u00a8\u0006\u0014"}, d2 = {"GameTools", "", "state", "Lcom/example/sheeps/game/state/GameViewState;", "onUseMoveOut", "Lkotlin/Function0;", "onUseUndo", "onUseShuffle", "onUseHint", "onUseBomb", "onUseJoker", "onUseDouble", "ToolButtonRow", "CarriedItemsSection", "CarriedItemIcon", "type", "", "name", "count", "", "feature_game_release"})
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
    
    /**
     * 道具交互按钮行
     */
    @androidx.compose.runtime.Composable()
    private static final void ToolButtonRow(com.example.sheeps.game.state.GameViewState state, kotlin.jvm.functions.Function0<kotlin.Unit> onUseMoveOut, kotlin.jvm.functions.Function0<kotlin.Unit> onUseUndo, kotlin.jvm.functions.Function0<kotlin.Unit> onUseShuffle, kotlin.jvm.functions.Function0<kotlin.Unit> onUseHint, kotlin.jvm.functions.Function0<kotlin.Unit> onUseBomb, kotlin.jvm.functions.Function0<kotlin.Unit> onUseJoker, kotlin.jvm.functions.Function0<kotlin.Unit> onUseDouble) {
    }
    
    /**
     * 已携带法宝展示区（展示当前关卡携带的所有法宝及其剩余次数）
     */
    @androidx.compose.runtime.Composable()
    private static final void CarriedItemsSection(com.example.sheeps.game.state.GameViewState state) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void CarriedItemIcon(java.lang.String type, java.lang.String name, int count) {
    }
}