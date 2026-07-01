package com.example.sheeps.menu.ui.screens;

import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Modifier;
import com.example.sheeps.menu.state.MenuViewState;
import com.example.sheeps.menu.ui.components.*;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000\"\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\u001a\u0092\u0001\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u00010\t2\u0012\u0010\u000b\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u00010\t2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u00010\t2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u00a8\u0006\u000f"}, d2 = {"PersonalScreen", "", "state", "Lcom/example/sheeps/menu/state/MenuViewState;", "onLoginClick", "Lkotlin/Function0;", "onLogoutClick", "onSignInClick", "onClaimTask", "Lkotlin/Function1;", "", "onChangeLanguage", "onThemeChange", "onApplySkin", "onGoToPlay", "feature_menu_debug"})
public final class PersonalScreenKt {
    
    /**
     * 个人中心主界面
     * 采用组件化拆分，保持主文件简洁易读
     *
     * @param state 界面状态数据
     * @param onLoginClick 登录点击回调
     * @param onLogoutClick 退出登录点击回调
     * @param onSignInClick 每日签到点击回调
     * @param onClaimTask 领取任务奖励回调
     * @param onChangeLanguage 语言设置变更回调
     * @param onThemeChange 主题变更回调
     * @param onApplySkin 应用/切换皮肤回调
     * @param onGoToPlay 前往游戏关卡回调
     */
    @androidx.compose.runtime.Composable()
    public static final void PersonalScreen(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.menu.state.MenuViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onLoginClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onLogoutClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSignInClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onClaimTask, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onChangeLanguage, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onThemeChange, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onApplySkin, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onGoToPlay) {
    }
}