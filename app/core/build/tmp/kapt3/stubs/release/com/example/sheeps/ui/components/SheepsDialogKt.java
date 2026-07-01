package com.example.sheeps.ui.components;

import androidx.compose.animation.*;
import androidx.compose.animation.core.Spring;
import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import androidx.compose.ui.window.DialogProperties;
import com.example.sheeps.theme.*;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u00008\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\u001a~\u0010\u0000\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u0006\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\t2\u0015\b\u0002\u0010\n\u001a\u000f\u0012\u0004\u0012\u00020\u0001\u0018\u00010\u0003\u00a2\u0006\u0002\b\u000b2\u0015\b\u0002\u0010\f\u001a\u000f\u0012\u0004\u0012\u00020\u0001\u0018\u00010\u0003\u00a2\u0006\u0002\b\u000b2\u001c\u0010\r\u001a\u0018\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u00010\u000e\u00a2\u0006\u0002\b\u000b\u00a2\u0006\u0002\b\u0010H\u0007\u001ab\u0010\u0011\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0012\u001a\u00020\u00052\b\b\u0002\u0010\u0013\u001a\u00020\u00052\b\b\u0002\u0010\u0014\u001a\u00020\u00052\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\u000e\b\u0002\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\b\b\u0002\u0010\b\u001a\u00020\tH\u0007\u00a8\u0006\u0017"}, d2 = {"SheepsDialog", "", "onDismissRequest", "Lkotlin/Function0;", "title", "", "modifier", "Landroidx/compose/ui/Modifier;", "dismissible", "", "confirmButton", "Landroidx/compose/runtime/Composable;", "dismissButton", "content", "Lkotlin/Function1;", "Landroidx/compose/foundation/layout/ColumnScope;", "Lkotlin/ExtensionFunctionType;", "ConfirmDialog", "message", "confirmText", "dismissText", "onConfirm", "onDismiss", "core_release"})
public final class SheepsDialogKt {
    
    /**
     * 标准 Dialog（替代 AlertDialog）
     * 特性：
     * - 深色表面 + 金色细边框
     * - 标题使用衬线国风字体
     * - 弹性进入/淡出动画
     * - 按钮统一样式
     */
    @androidx.compose.runtime.Composable()
    public static final void SheepsDialog(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismissRequest, @org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, boolean dismissible, @org.jetbrains.annotations.Nullable()
    androidx.compose.runtime.internal.ComposableFunction0<kotlin.Unit> confirmButton, @org.jetbrains.annotations.Nullable()
    androidx.compose.runtime.internal.ComposableFunction0<kotlin.Unit> dismissButton, @org.jetbrains.annotations.NotNull()
    androidx.compose.runtime.internal.ComposableFunction1<? super androidx.compose.foundation.layout.ColumnScope, kotlin.Unit> content) {
    }
    
    /**
     * 简单确认 Dialog（只有文字+两个按钮）
     */
    @androidx.compose.runtime.Composable()
    public static final void ConfirmDialog(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismissRequest, @org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.NotNull()
    java.lang.String confirmText, @org.jetbrains.annotations.NotNull()
    java.lang.String dismissText, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onConfirm, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, boolean dismissible) {
    }
}