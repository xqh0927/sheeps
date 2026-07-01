package com.example.sheeps.game.ui.screens;

import androidx.compose.animation.*;
import androidx.compose.animation.core.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import com.example.sheeps.data.model.Tile;
import com.example.sheeps.game.state.*;
import com.example.sheeps.game.ui.components.*;
import com.example.sheeps.ui.components.*;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000>\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\u001a\u00cc\u0001\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0007\u001a5\u0010\u0014\u001a\u00020\u00012\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00170\u00162\u0006\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u001dH\u0003\u00a2\u0006\u0004\b\u001e\u0010\u001f\u001a\b\u0010 \u001a\u00020\u0001H\u0003\u001aV\u0010!\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0003\u00a8\u0006\""}, d2 = {"GameScreen", "", "state", "Lcom/example/sheeps/game/state/GameViewState;", "onTileClick", "Lkotlin/Function1;", "Lcom/example/sheeps/data/model/Tile;", "onUseUndo", "Lkotlin/Function0;", "onUseMoveOut", "onUseShuffle", "onUseHint", "onUseBomb", "onUseJoker", "onUseDouble", "onRevive", "onRestart", "onBack", "onNextLevel", "onShowLeaderboard", "FlyingTilesLayer", "flyingTiles", "", "Lcom/example/sheeps/game/ui/screens/GameFlyingTile;", "currentSkin", "", "screenRootOffset", "Landroidx/compose/ui/geometry/Offset;", "density", "Landroidx/compose/ui/unit/Density;", "FlyingTilesLayer-Rg1IO4c", "(Ljava/util/List;Ljava/lang/String;JLandroidx/compose/ui/unit/Density;)V", "GameBackgroundDecoration", "GameResultSection", "feature_game_release"})
public final class GameScreenKt {
    
    /**
     * 游戏主界面
     * 负责游戏逻辑控制、动画管理以及各功能组件的组合
     */
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void GameScreen(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.data.model.Tile, kotlin.Unit> onTileClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseUndo, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseMoveOut, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseShuffle, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseHint, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseBomb, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseJoker, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onUseDouble, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onRevive, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onRestart, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNextLevel, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onShowLeaderboard) {
    }
    
    /**
     * 游戏背景装饰（微光效果）
     */
    @androidx.compose.runtime.Composable()
    private static final void GameBackgroundDecoration() {
    }
    
    /**
     * 游戏结果处理区
     */
    @androidx.compose.runtime.Composable()
    private static final void GameResultSection(com.example.sheeps.game.state.GameViewState state, kotlin.jvm.functions.Function0<kotlin.Unit> onBack, kotlin.jvm.functions.Function0<kotlin.Unit> onRestart, kotlin.jvm.functions.Function0<kotlin.Unit> onRevive, kotlin.jvm.functions.Function0<kotlin.Unit> onNextLevel, kotlin.jvm.functions.Function0<kotlin.Unit> onShowLeaderboard) {
    }
}