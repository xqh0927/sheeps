package com.example.sheeps.ui.components;

import androidx.compose.animation.core.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.*;
import androidx.compose.ui.graphics.drawscope.Stroke;
import androidx.compose.ui.unit.Dp;
import com.example.sheeps.theme.*;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000\u001e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a#\u0010\u0000\u001a\u00020\u00012\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u0007\u00a2\u0006\u0004\b\u0006\u0010\u0007\u001a\u0012\u0010\b\u001a\u00020\u00012\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u0007\u001a\u001c\u0010\t\u001a\u00020\u00012\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\n\u001a\u00020\u000bH\u0007\u001a\u0012\u0010\f\u001a\u00020\u00012\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u0007\u00a8\u0006\r"}, d2 = {"SheepsLoading", "", "modifier", "Landroidx/compose/ui/Modifier;", "size", "Landroidx/compose/ui/unit/Dp;", "SheepsLoading-3ABfNKs", "(Landroidx/compose/ui/Modifier;F)V", "FullScreenLoading", "ShimmerBox", "shape", "Landroidx/compose/ui/graphics/Shape;", "ShimmerCard", "core_debug"})
public final class LoadingIndicatorKt {
    
    /**
     * 带遮罩的全屏 Loading（用于页面加载中）
     */
    @androidx.compose.runtime.Composable()
    public static final void FullScreenLoading(@org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    /**
     * 骨架屏 Shimmer 效果（用于列表加载中占位）
     * 使用 Brush 渐变模拟流光
     */
    @androidx.compose.runtime.Composable()
    public static final void ShimmerBox(@org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.graphics.Shape shape) {
    }
    
    /**
     * 卡片骨架屏（用于商店列表、排行榜等）
     */
    @androidx.compose.runtime.Composable()
    public static final void ShimmerCard(@org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
}