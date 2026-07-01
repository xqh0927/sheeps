package com.example.sheeps.game.ui.components;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Brush;
import androidx.compose.ui.text.font.FontWeight;
import com.example.sheeps.core.R;
import com.example.sheeps.core.multiplayer.WebSocketManager;
import com.example.sheeps.data.model.TileState;
import com.example.sheeps.game.state.DuelViewState;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000\u001c\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\u001a\u001e\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u001a\u0010\u0010\u0006\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\u0003\u001a\u0010\u0010\u0007\u001a\u00020\u00012\u0006\u0010\b\u001a\u00020\tH\u0003\u00a8\u0006\n"}, d2 = {"DuelHeader", "", "state", "Lcom/example/sheeps/game/state/DuelViewState;", "onLeave", "Lkotlin/Function0;", "DuelProgressBars", "DuelEnergyBar", "currentEnergy", "", "feature_game_release"})
public final class DuelHeaderKt {
    
    /**
     * 对决模式顶部状态栏
     * 展示：对决标题、连接状态、双方进度条、能量条
     *
     * @param state 对决界面状态
     * @param onLeave 离开按钮点击回调
     */
    @androidx.compose.runtime.Composable()
    public static final void DuelHeader(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.DuelViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onLeave) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void DuelProgressBars(com.example.sheeps.game.state.DuelViewState state) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void DuelEnergyBar(int currentEnergy) {
    }
}