package com.example.sheeps.menu.ui.components;

import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import androidx.compose.ui.text.style.TextOverflow;
import com.example.sheeps.core.R;
import com.example.sheeps.core.game.TileIconProvider;
import com.example.sheeps.menu.state.MenuViewState;
import com.hjq.toast.Toaster;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u00000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\u001a2\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0007\u001a\u001e\u0010\t\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u000b2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0003\u001aB\u0010\r\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u000e\u001a\u00020\u000f2\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\bH\u0003\u00a8\u0006\u0011"}, d2 = {"BackpackCard", "", "state", "Lcom/example/sheeps/menu/state/MenuViewState;", "onApplySkin", "Lkotlin/Function1;", "", "onGoToPlay", "Lkotlin/Function0;", "BackpackItem", "item", "Lcom/example/sheeps/data/model/UserItem;", "onClick", "BackpackItemDetailDialog", "isCurrentSkin", "", "onDismiss", "feature_menu_debug"})
public final class BackpackCardKt {
    
    /**
     * 背包物品卡片组件
     * 横向展示用户拥有的道具及其数量，并支持点击交互使用或应用皮肤
     *
     * @param state 界面状态数据，包含背包物品列表
     * @param onApplySkin 应用/切换皮肤回调
     * @param onGoToPlay 前往游戏关卡回调
     */
    @androidx.compose.runtime.Composable()
    public static final void BackpackCard(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.menu.state.MenuViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onApplySkin, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onGoToPlay) {
    }
    
    /**
     * 单个背包物品条目
     */
    @androidx.compose.runtime.Composable()
    private static final void BackpackItem(com.example.sheeps.data.model.UserItem item, kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
    }
    
    /**
     * 背包物品详细信息与使用/应用弹窗
     */
    @androidx.compose.runtime.Composable()
    private static final void BackpackItemDetailDialog(com.example.sheeps.data.model.UserItem item, boolean isCurrentSkin, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function0<kotlin.Unit> onApplySkin, kotlin.jvm.functions.Function0<kotlin.Unit> onGoToPlay) {
    }
}