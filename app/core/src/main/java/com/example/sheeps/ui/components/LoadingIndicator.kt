package com.example.sheeps.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import com.example.sheeps.theme.*
import kotlin.math.cos
import kotlin.math.sin

// =============================================================================
// 秘境消消乐 · 设计系统 - 自定义 Loading 组件
// 替代系统 CircularProgressIndicator，使用国风太极旋转动画
// =============================================================================

/**
 * 太极旋转 Loading 指示器（替代系统 ProgressBar）
 * 由两个半圆旋转组成，金色+朱砂双色
 */
@Composable
fun SheepsLoading(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sheepsLoading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadingRotation"
    )
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = -360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerLoadingRotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor  = MaterialTheme.colorScheme.secondary
    val bgColor      = MaterialTheme.colorScheme.surfaceContainer
    val innerColor   = MaterialTheme.colorScheme.onPrimaryContainer

    Canvas(
        modifier = modifier
            .size(size)
            .rotate(rotation)
    ) {
        val strokeWidth = size.toPx() * 0.12f
        val radius = (size.toPx() - strokeWidth) / 2f
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f

        // 外圈：朱砂弧
        drawArc(
            color      = primaryColor,
            startAngle = 0f,
            sweepAngle = 200f,
            useCenter  = false,
            topLeft    = Offset(cx - radius, cy - radius),
            size       = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        // 外圈：金色弧（另一侧）
        drawArc(
            color      = accentColor,
            startAngle = 200f,
            sweepAngle = 160f,
            useCenter  = false,
            topLeft    = Offset(cx - radius, cy - radius),
            size       = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 内圈：反向旋转小圆弧
        val innerRadius = radius * 0.5f
        rotate(degrees = innerRotation, pivot = Offset(cx, cy)) {
            drawArc(
                color      = innerColor,
                startAngle = 0f,
                sweepAngle = 120f,
                useCenter  = false,
                topLeft    = Offset(cx - innerRadius, cy - innerRadius),
                size       = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
                style      = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * 带遮罩的全屏 Loading（用于页面加载中）
 */
@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Overlay_Dark_Medium)
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .clickable(enabled = false, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            SheepsLoading(size = 48.dp)
        }
    }
}

/**
 * 骨架屏 Shimmer 效果（用于列表加载中占位）
 * 使用 Brush 渐变模拟流光
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue  = 2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surfaceContainer,
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
        MaterialTheme.colorScheme.surfaceContainer,
        MaterialTheme.colorScheme.surfaceVariant
    )

    val brush = Brush.horizontalGradient(
        colors = shimmerColors,
        startX = shimmerProgress * 800f,
        endX   = shimmerProgress * 800f + 400f
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/**
 * 卡片骨架屏（用于商店列表、排行榜等）
 */
@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.75f).height(12.dp))
    }
}
