package com.example.sheeps.game.ui.components;

import androidx.compose.animation.core.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import com.example.sheeps.data.model.Tile;
import com.example.sheeps.game.state.GameViewState;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u00006\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\u0010\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\u001a8\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00070\u00052\u0012\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u00010\tH\u0007\u001a2\u0010\u000b\u001a\u00020\u00012\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\n0\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0012\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u00010\tH\u0003\u001a$\u0010\u0010\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00070\u0005H\u0003\u00a8\u0006\u0011"}, d2 = {"GameDock", "", "state", "Lcom/example/sheeps/game/state/GameViewState;", "slotGlobalPositions", "", "", "Landroidx/compose/ui/geometry/Offset;", "onTileClick", "Lkotlin/Function1;", "Lcom/example/sheeps/data/model/Tile;", "MovedOutTray", "tiles", "", "currentSkin", "", "MatchingSlot", "feature_game_release"})
public final class GameDockKt {
    
    /**
     * 游戏底部托盘/消除槽组件
     * 包含：置物架（移出功能）、消除槽（Slot）
     *
     * @param state 游戏界面状态
     * @param slotGlobalPositions 用于记录槽位全局位置，供动画使用
     * @param onTileClick 置物架中卡牌点击回调
     */
    @androidx.compose.runtime.Composable()
    public static final void GameDock(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameViewState state, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.Integer, androidx.compose.ui.geometry.Offset> slotGlobalPositions, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.data.model.Tile, kotlin.Unit> onTileClick) {
    }
    
    /**
     * 移出置物架组件
     */
    @androidx.compose.runtime.Composable()
    private static final void MovedOutTray(java.util.List<com.example.sheeps.data.model.Tile> tiles, java.lang.String currentSkin, kotlin.jvm.functions.Function1<? super com.example.sheeps.data.model.Tile, kotlin.Unit> onTileClick) {
    }
    
    /**
     * 消除槽（七格槽位）组件
     */
    @androidx.compose.runtime.Composable()
    private static final void MatchingSlot(com.example.sheeps.game.state.GameViewState state, java.util.Map<java.lang.Integer, androidx.compose.ui.geometry.Offset> slotGlobalPositions) {
    }
}