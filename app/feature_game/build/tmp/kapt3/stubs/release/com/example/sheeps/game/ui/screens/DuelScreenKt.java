package com.example.sheeps.game.ui.screens;

import androidx.compose.animation.*;
import androidx.compose.animation.core.*;
import androidx.compose.foundation.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.*;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import com.example.sheeps.core.R;
import com.example.sheeps.data.model.Tile;
import com.example.sheeps.data.model.TileState;
import com.example.sheeps.game.state.*;
import com.example.sheeps.game.ui.components.*;
import com.example.sheeps.theme.*;
import com.example.sheeps.ui.components.*;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000L\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010%\n\u0002\u0010\b\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\u001aT\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\u0012\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u001a$\u0010\f\u001a\u00020\u00012\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0003\u001a\u0010\u0010\u000f\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\u0003\u001a$\u0010\u0010\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u001a$\u0010\u0011\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00150\u0013H\u0003\u001a\b\u0010\u0016\u001a\u00020\u0001H\u0003\u001a-\u0010\u0017\u001a\u00020\u00012\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u001a0\u00192\u0006\u0010\u001b\u001a\u00020\u00152\u0006\u0010\u001c\u001a\u00020\u001dH\u0003\u00a2\u0006\u0004\b\u001e\u0010\u001f\u00a8\u0006 "}, d2 = {"DuelScreen", "", "state", "Lcom/example/sheeps/game/state/DuelViewState;", "onTileClick", "Lkotlin/Function1;", "Lcom/example/sheeps/data/model/Tile;", "onLeave", "Lkotlin/Function0;", "onRestart", "onCastSpell", "", "ExitConfirmDialog", "onConfirm", "onDismiss", "SpellMessageBar", "DuelMovedOutTray", "DuelMatchingSlot", "slotGlobalPositions", "", "", "Landroidx/compose/ui/geometry/Offset;", "FullScreenLoadingOverlay", "DuelFlyingTilesLayer", "flyingTiles", "", "Lcom/example/sheeps/game/ui/screens/DuelFlyingTile;", "screenRootOffset", "density", "Landroidx/compose/ui/unit/Density;", "DuelFlyingTilesLayer-d-4ec7I", "(Ljava/util/List;JLandroidx/compose/ui/unit/Density;)V", "feature_game_release"})
public final class DuelScreenKt {
    
    /**
     * 对决模式主界面
     */
    @androidx.compose.runtime.Composable()
    public static final void DuelScreen(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.DuelViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.data.model.Tile, kotlin.Unit> onTileClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onLeave, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onRestart, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onCastSpell) {
    }
    
    /**
     * 退出确认对话框
     */
    @androidx.compose.runtime.Composable()
    private static final void ExitConfirmDialog(kotlin.jvm.functions.Function0<kotlin.Unit> onConfirm, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    /**
     * 施法/受击消息提示条
     */
    @androidx.compose.runtime.Composable()
    private static final void SpellMessageBar(com.example.sheeps.game.state.DuelViewState state) {
    }
    
    /**
     * 对决模式移出置物架
     */
    @androidx.compose.runtime.Composable()
    private static final void DuelMovedOutTray(com.example.sheeps.game.state.DuelViewState state, kotlin.jvm.functions.Function1<? super com.example.sheeps.data.model.Tile, kotlin.Unit> onTileClick) {
    }
    
    /**
     * 对决模式消除槽位（支持缩减槽位诅咒）
     */
    @androidx.compose.runtime.Composable()
    private static final void DuelMatchingSlot(com.example.sheeps.game.state.DuelViewState state, java.util.Map<java.lang.Integer, androidx.compose.ui.geometry.Offset> slotGlobalPositions) {
    }
    
    /**
     * 加载全屏遮罩
     */
    @androidx.compose.runtime.Composable()
    private static final void FullScreenLoadingOverlay() {
    }
}