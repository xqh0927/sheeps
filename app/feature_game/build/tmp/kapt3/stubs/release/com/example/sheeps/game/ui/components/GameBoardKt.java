package com.example.sheeps.game.ui.components;

import androidx.compose.foundation.layout.*;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import com.example.sheeps.data.model.Tile;
import com.example.sheeps.data.model.TileState;
import com.example.sheeps.game.state.GameViewState;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000,\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\"\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010%\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\u001aF\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\u0012\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\t0\b2\u0012\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\u00010\u000bH\u0007\u00a8\u0006\r"}, d2 = {"GameBoard", "", "state", "Lcom/example/sheeps/game/state/GameViewState;", "flyingTileIds", "", "", "tileGlobalPositions", "", "Landroidx/compose/ui/geometry/Offset;", "onTileClick", "Lkotlin/Function1;", "Lcom/example/sheeps/data/model/Tile;", "feature_game_release"})
public final class GameBoardKt {
    
    /**
     * 游戏主棋盘组件
     * 负责渲染棋盘背景、层级排列的所有卡牌（Tile）
     *
     * @param state 游戏界面状态
     * @param flyingTileIds 当前正在飞向槽位的卡牌ID集合（用于隐藏原始位置卡牌）
     * @param tileGlobalPositions 用于记录卡牌在屏幕上的全局位置，供动画使用
     * @param onTileClick 卡牌点击事件回调
     */
    @androidx.compose.runtime.Composable()
    public static final void GameBoard(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameViewState state, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> flyingTileIds, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, androidx.compose.ui.geometry.Offset> tileGlobalPositions, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.data.model.Tile, kotlin.Unit> onTileClick) {
    }
}