package com.example.sheeps.menu.ui.screens;

import androidx.compose.foundation.layout.*;
import androidx.compose.foundation.lazy.grid.GridCells;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import com.example.sheeps.data.model.ShopItem;
import com.example.sheeps.menu.state.MenuViewState;
import com.example.sheeps.core.R;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000(\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\u001aL\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\u0018\u0010\u0006\u001a\u0014\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00010\u00072\u0012\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u00010\nH\u0007\u00a8\u0006\f"}, d2 = {"ShopScreen", "", "state", "Lcom/example/sheeps/menu/state/MenuViewState;", "onLoginClick", "Lkotlin/Function0;", "onExchangeClick", "Lkotlin/Function2;", "", "onChangeSkin", "Lkotlin/Function1;", "", "feature_menu_debug"})
public final class ShopScreenKt {
    
    /**
     * 道具商店界面。
     * 允许用户使用积分兑换游戏道具（如洗牌、撤销等）以及各种主题皮肤。
     *
     * @param state 菜单界面状态
     * @param onLoginClick 点击登录按钮回调
     * @param onExchangeClick 确认兑换商品回调 (itemId, count)
     * @param onChangeSkin 切换当前应用皮肤回调
     */
    @androidx.compose.runtime.Composable()
    public static final void ShopScreen(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.menu.state.MenuViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onLoginClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super java.lang.Integer, ? super java.lang.Integer, kotlin.Unit> onExchangeClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onChangeSkin) {
    }
}