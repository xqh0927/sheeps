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
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import com.example.sheeps.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/** Three tiles lift in sequence while work is pending. */
@Composable
fun SheepsLoading(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val phase = rememberInfiniteTransition(label = "tileSequence").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1080, easing = LinearEasing)),
        label = "tileLift"
    )
    val colors = MaterialTheme.colorScheme
    Canvas(modifier.size(size).progressSemantics()) {
        val unit = this.size.minDimension
        for (index in 0..2) {
            val cycle = (phase.value - index * 0.14f + 1f) % 1f
            val lift = sin((cycle / 0.55f).coerceAtMost(1f) * Math.PI).toFloat()
            val edge = unit * .2f
            val x = unit * (.1f + index * .30f)
            val y = unit * (.49f - .19f * lift)
            drawRoundRect(
                color = androidx.compose.ui.graphics.lerp(colors.onSurfaceVariant, colors.primary, lift),
                topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(edge, edge),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(edge * .23f)
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
