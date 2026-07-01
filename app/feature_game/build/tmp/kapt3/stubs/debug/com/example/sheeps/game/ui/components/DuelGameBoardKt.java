package com.example.sheeps.game.ui.components;

import androidx.compose.foundation.layout.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.*;
import com.example.sheeps.data.model.Tile;
import com.example.sheeps.data.model.TileState;
import com.example.sheeps.game.state.DuelViewState;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u00004\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\"\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010%\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u001aP\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\u0012\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\t0\b2\u0012\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\u00010\u000b2\b\b\u0002\u0010\r\u001a\u00020\u000eH\u0007\u001a\b\u0010\u000f\u001a\u00020\u0001H\u0003\u00a8\u0006\u0010"}, d2 = {"DuelGameBoard", "", "state", "Lcom/example/sheeps/game/state/DuelViewState;", "flyingTileIds", "", "", "tileGlobalPositions", "", "Landroidx/compose/ui/geometry/Offset;", "onTileClick", "Lkotlin/Function1;", "Lcom/example/sheeps/data/model/Tile;", "modifier", "Landroidx/compose/ui/Modifier;", "FogEffectOverlay", "feature_game_debug"})
public final class DuelGameBoardKt {
    
    /**
     * 对决模式游戏棋盘
     * 负责渲染卡牌排列以及特殊的“迷雾障眼”干扰特效
     */
    @androidx.compose.runtime.Composable()
    public static final void DuelGameBoard(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.DuelViewState state, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> flyingTileIds, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, androidx.compose.ui.geometry.Offset> tileGlobalPositions, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.data.model.Tile, kotlin.Unit> onTileClick, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    /**
     * 迷雾效果遮罩
     * 用户可以通过手指触摸来临时“擦除”一小块迷雾以观察下方卡牌
     */
    @androidx.compose.runtime.Composable()
    private static final void FogEffectOverlay() {
    }
}