package com.example.sheeps.ui.components;

import androidx.compose.animation.*;
import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.TextStyle;
import androidx.compose.ui.text.font.FontWeight;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000\u001c\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\u001a$\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u0007\u001a.\u0010\b\u001a\u00020\u00012\u0006\u0010\t\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\n\u001a\u00020\u00072\b\b\u0002\u0010\u000b\u001a\u00020\u0007H\u0007\u00a8\u0006\f"}, d2 = {"AnimatedCounter", "", "count", "", "modifier", "Landroidx/compose/ui/Modifier;", "style", "Landroidx/compose/ui/text/TextStyle;", "PointsDisplay", "points", "numberStyle", "unitStyle", "core_debug"})
public final class AnimatedCounterKt {
    
    /**
     * 数字滚动动画文字
     * 当 count 变化时，旧数字向上飞出，新数字从下方进入
     */
    @androidx.compose.runtime.Composable()
    public static final void AnimatedCounter(int count, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.text.TextStyle style) {
    }
    
    /**
     * 带单位的积分展示组件
     * 例如：「1234 积分」
     */
    @androidx.compose.runtime.Composable()
    public static final void PointsDisplay(int points, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.text.TextStyle numberStyle, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.text.TextStyle unitStyle) {
    }
}