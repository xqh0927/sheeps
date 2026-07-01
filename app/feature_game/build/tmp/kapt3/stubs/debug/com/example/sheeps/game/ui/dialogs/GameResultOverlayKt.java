package com.example.sheeps.game.ui.dialogs;

import androidx.compose.animation.core.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import com.example.sheeps.game.state.GameViewState;
import com.example.sheeps.theme.*;
import com.example.sheeps.ui.components.*;
import com.example.sheeps.core.R;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000\u001c\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\u001af\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\u000e\b\u0002\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\u000e\b\u0002\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\u000e\b\u0002\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\u000e\b\u0002\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\u001a:\u0010\f\u001a\u00020\u00012\u0006\u0010\u0004\u001a\u00020\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a:\u0010\r\u001a\u00020\u00012\u0006\u0010\u0004\u001a\u00020\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u00a8\u0006\u000e"}, d2 = {"GameResultOverlay", "", "won", "", "state", "Lcom/example/sheeps/game/state/GameViewState;", "onBack", "Lkotlin/Function0;", "onRestart", "onRevive", "onNextLevel", "onShowLeaderboard", "WonContent", "LostContent", "feature_game_debug"})
public final class GameResultOverlayKt {
    
    /**
     * 游戏结果覆盖层组件（胜利或失败弹窗）
     */
    @androidx.compose.runtime.Composable()
    public static final void GameResultOverlay(boolean won, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onRestart, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onRevive, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNextLevel, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onShowLeaderboard) {
    }
    
    /**
     * 胜利内容展示
     */
    @androidx.compose.runtime.Composable()
    private static final void WonContent(com.example.sheeps.game.state.GameViewState state, kotlin.jvm.functions.Function0<kotlin.Unit> onBack, kotlin.jvm.functions.Function0<kotlin.Unit> onNextLevel, kotlin.jvm.functions.Function0<kotlin.Unit> onShowLeaderboard) {
    }
    
    /**
     * 失败内容展示
     */
    @androidx.compose.runtime.Composable()
    private static final void LostContent(com.example.sheeps.game.state.GameViewState state, kotlin.jvm.functions.Function0<kotlin.Unit> onBack, kotlin.jvm.functions.Function0<kotlin.Unit> onRestart, kotlin.jvm.functions.Function0<kotlin.Unit> onRevive) {
    }
}