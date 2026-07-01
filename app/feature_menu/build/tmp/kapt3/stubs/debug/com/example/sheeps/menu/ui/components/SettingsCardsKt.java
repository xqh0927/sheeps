package com.example.sheeps.menu.ui.components;

import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import com.example.sheeps.core.R;
import com.example.sheeps.menu.state.MenuViewState;
import com.example.sheeps.theme.AppTheme;
import com.example.sheeps.theme.ThemeManager;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u00002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a$\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u001a\u0016\u0010\u0007\u001a\u00020\u00012\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\tH\u0007\u001a0\u0010\n\u001a\u00020\u00012\u0006\u0010\u000b\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\t2\b\b\u0002\u0010\u000f\u001a\u00020\u0010H\u0003\u001a0\u0010\u0011\u001a\u00020\u00012\u0006\u0010\u000b\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\t2\b\b\u0002\u0010\u000f\u001a\u00020\u0010H\u0003\u00a8\u0006\u0012"}, d2 = {"LanguageSettingsCard", "", "state", "Lcom/example/sheeps/menu/state/MenuViewState;", "onChangeLanguage", "Lkotlin/Function1;", "", "ThemeSettingsCard", "onThemeChange", "Lkotlin/Function0;", "LanguageOption", "name", "isSelected", "", "onClick", "modifier", "Landroidx/compose/ui/Modifier;", "ThemeOption", "feature_menu_debug"})
public final class SettingsCardsKt {
    
    /**
     * 语言设置卡片组件
     *
     * @param state 界面状态数据
     * @param onChangeLanguage 语言切换回调
     */
    @androidx.compose.runtime.Composable()
    public static final void LanguageSettingsCard(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.menu.state.MenuViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onChangeLanguage) {
    }
    
    /**
     * 主题设置卡片组件
     *
     * @param onThemeChange 主题切换后的回调
     */
    @androidx.compose.runtime.Composable()
    public static final void ThemeSettingsCard(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onThemeChange) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void LanguageOption(java.lang.String name, boolean isSelected, kotlin.jvm.functions.Function0<kotlin.Unit> onClick, androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ThemeOption(java.lang.String name, boolean isSelected, kotlin.jvm.functions.Function0<kotlin.Unit> onClick, androidx.compose.ui.Modifier modifier) {
    }
}