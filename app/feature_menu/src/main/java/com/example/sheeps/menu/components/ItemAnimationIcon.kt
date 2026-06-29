package com.example.sheeps.menu.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sheeps.theme.CrimsonRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ItemAnimationIcon(
    itemType: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "item_icon")
    
    // 旋转动画
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // 漂浮平移动画 (上下)
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    // 缩放/呼吸动画
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // 快速电闪/光芒闪烁动画
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flash"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFCFAF6)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.75f)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val radius = w * 0.45f

            when (itemType.uppercase()) {
                "UNDO" -> { // 乾坤符：慢速旋转的太极图
                    rotate(rotation, pivot = Offset(cx, cy)) {
                        // 绘制阴阳底盘
                        drawArc(
                            color = Color(0xFFCBAA6A),
                            startAngle = -90f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(cx - radius, cy - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                        drawArc(
                            color = CrimsonRed,
                            startAngle = 90f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(cx - radius, cy - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                        // 绘制两个衔接的半圆大鱼头
                        drawCircle(
                            color = Color(0xFFCBAA6A),
                            radius = radius / 2f,
                            center = Offset(cx, cy - radius / 2f)
                        )
                        drawCircle(
                            color = CrimsonRed,
                            radius = radius / 2f,
                            center = Offset(cx, cy + radius / 2f)
                        )
                        // 绘制阴阳鱼眼
                        drawCircle(
                            color = CrimsonRed,
                            radius = radius * 0.12f,
                            center = Offset(cx, cy - radius / 2f)
                        )
                        drawCircle(
                            color = Color(0xFFCBAA6A),
                            radius = radius * 0.12f,
                            center = Offset(cx, cy + radius / 2f)
                        )
                    }
                }
                "MOVEOUT" -> { // 缩地咒：上下飘动的金色祥云
                    translate(top = floatOffset) {
                        val path = Path().apply {
                            val r = radius * 0.5f
                            // 绘制祥云底部基座
                            moveTo(cx - radius * 0.8f, cy + radius * 0.3f)
                            lineTo(cx + radius * 0.8f, cy + radius * 0.3f)
                            // 绘制云圈拱桥
                            cubicTo(cx + radius * 0.9f, cy - radius * 0.1f, cx + radius * 0.5f, cy - radius * 0.7f, cx + radius * 0.2f, cy - radius * 0.4f)
                            cubicTo(cx, cy - radius * 0.9f, cx - radius * 0.5f, cy - radius * 0.7f, cx - radius * 0.5f, cy - radius * 0.2f)
                            cubicTo(cx - radius * 0.8f, cy - radius * 0.1f, cx - radius * 0.9f, cy + radius * 0.2f, cx - radius * 0.8f, cy + radius * 0.3f)
                            close()
                        }
                        drawPath(path, color = Color(0xFFE5B55F))
                        // 云纹描边
                        drawPath(path, color = Color(0xFFCBAA6A), style = Stroke(width = 3f))
                        // 绘制一条飘带线
                        val ribbon = Path().apply {
                            moveTo(cx - radius * 0.7f, cy + radius * 0.1f)
                            quadraticTo(cx - radius * 0.2f, cy + radius * 0.4f, cx + radius * 0.5f, cy + radius * 0.1f)
                        }
                        drawPath(ribbon, color = CrimsonRed, style = Stroke(width = 3f))
                    }
                }
                "SHUFFLE" -> { // 流沙契：金色星盘与颤动的指针
                    // 外盘
                    drawCircle(
                        color = Color(0xFFCBAA6A),
                        radius = radius,
                        style = Stroke(width = 3f)
                    )
                    // 刻度盘
                    for (angle in 0 until 360 step 45) {
                        val rad = Math.toRadians(angle.toDouble())
                        val sx = cx + (radius * 0.8f) * cos(rad).toFloat()
                        val sy = cy + (radius * 0.8f) * sin(rad).toFloat()
                        val ex = cx + radius * cos(rad).toFloat()
                        val ey = cy + radius * sin(rad).toFloat()
                        drawLine(Color(0xFFCBAA6A), Offset(sx, sy), Offset(ex, ey), strokeWidth = 2f)
                    }
                    // 星盘背景
                    drawCircle(
                        color = Color(0xFFE5B55F).copy(alpha = 0.15f),
                        radius = radius * 0.8f
                    )
                    // 颤动的指针
                    val shake = rotation * 2.5f
                    rotate(shake, pivot = Offset(cx, cy)) {
                        drawLine(
                            color = CrimsonRed,
                            start = Offset(cx, cy + radius * 0.2f),
                            end = Offset(cx, cy - radius * 0.75f),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                        drawCircle(CrimsonRed, radius * 0.15f, Offset(cx, cy))
                        drawCircle(Color.White, radius * 0.06f, Offset(cx, cy))
                    }
                }
                "REVIVE" -> { // 还魂丹：呼吸发光的赤金仙丹
                    val pulseRadius = radius * scale
                    // 绘制呼吸光晕
                    drawCircle(
                        color = Color(0xFFFFD60A).copy(alpha = 0.2f * (2f - scale)),
                        radius = pulseRadius * 1.3f
                    )
                    // 仙丹球体
                    val grad = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF099), Color(0xFFE5B55F), CrimsonRed),
                        center = Offset(cx - pulseRadius * 0.2f, cy - pulseRadius * 0.2f),
                        radius = pulseRadius
                    )
                    drawCircle(brush = grad, radius = pulseRadius)
                    // 外发光圈
                    drawCircle(
                        color = Color(0xFFCBAA6A),
                        radius = pulseRadius,
                        style = Stroke(width = 2.5f)
                    )
                }
                "HINT" -> { // 天眼符：左右横扫神光的太极八卦镜/天眼
                    // 绘制眼眶
                    val eyePath = Path().apply {
                        moveTo(cx - radius, cy)
                        quadraticTo(cx, cy - radius * 0.6f, cx + radius, cy)
                        quadraticTo(cx, cy + radius * 0.6f, cx - radius, cy)
                        close()
                    }
                    drawPath(eyePath, color = Color(0xFFE5B55F).copy(alpha = 0.2f))
                    drawPath(eyePath, color = Color(0xFFCBAA6A), style = Stroke(width = 3f))
                    
                    // 天眼瞳孔（太极珠，闪烁）
                    drawCircle(
                        color = CrimsonRed.copy(alpha = flashAlpha),
                        radius = radius * 0.35f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color(0xFFFFFDF9),
                        radius = radius * 0.12f,
                        center = Offset(cx - radius * 0.08f, cy - radius * 0.08f)
                    )
                    
                    // 金光放射线
                    for (i in 0 until 8) {
                        val angle = i * 45f
                        val rad = Math.toRadians(angle.toDouble())
                        val sx = cx + (radius * 0.65f) * cos(rad).toFloat()
                        val sy = cy + (radius * 0.65f) * sin(rad).toFloat()
                        val ex = cx + (radius * 0.95f) * cos(rad).toFloat()
                        val ey = cy + (radius * 0.95f) * sin(rad).toFloat()
                        drawLine(
                            color = Color(0xFFE5B55F).copy(alpha = flashAlpha * 0.8f),
                            start = Offset(sx, sy),
                            end = Offset(ex, ey),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                "BOMB" -> { // 雷震子：环绕雷击闪电的炸弹
                    // 炸弹本体
                    drawCircle(
                        color = Color(0xFF2C2F33),
                        radius = radius * 0.75f,
                        center = Offset(cx, cy + radius * 0.1f)
                    )
                    drawCircle(
                        color = Color(0xFFCBAA6A),
                        radius = radius * 0.75f,
                        center = Offset(cx, cy + radius * 0.1f),
                        style = Stroke(width = 3f)
                    )
                    // 引信与花火
                    drawLine(
                        color = Color(0xFF8B5A2B),
                        start = Offset(cx, cy - radius * 0.65f),
                        end = Offset(cx + radius * 0.3f, cy - radius * 0.9f),
                        strokeWidth = 4f
                    )
                    drawCircle(
                        color = Color(0xFFFFD60A).copy(alpha = flashAlpha),
                        radius = radius * 0.18f,
                        center = Offset(cx + radius * 0.3f, cy - radius * 0.9f)
                    )
                    // 炸弹表面雷电裂纹（闪烁）
                    val lightning = Path().apply {
                        moveTo(cx - radius * 0.4f, cy - radius * 0.1f)
                        lineTo(cx - radius * 0.1f, cy + radius * 0.2f)
                        lineTo(cx + radius * 0.1f, cy - radius * 0.1f)
                        lineTo(cx + radius * 0.4f, cy + radius * 0.3f)
                    }
                    drawPath(
                        path = lightning,
                        color = Color(0xFFFFD60A).copy(alpha = flashAlpha),
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                "JOKER" -> { // 太极牌：流光溢彩的炫彩太极星盘
                    rotate(-rotation * 1.5f, pivot = Offset(cx, cy)) {
                        val brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFF2A6D),
                                Color(0xFF05D9E8),
                                Color(0xFFFFD60A),
                                Color(0xFF30D158),
                                Color(0xFFFF2A6D)
                            ),
                            center = Offset(cx, cy)
                        )
                        drawCircle(brush = brush, radius = radius)
                        drawCircle(Color.White.copy(alpha = 0.3f), radius = radius * 0.7f)
                        drawCircle(Color(0xFF2C2F33), radius = radius, style = Stroke(width = 3f))
                    }
                }
                "DOUBLE_POINTS" -> { // 双倍符：微微重合的闪烁金色铜钱
                    translate(top = floatOffset * 0.5f) {
                        // 绘制第一枚铜钱 (后)
                        drawCoin(cx - radius * 0.25f, cy + radius * 0.15f, radius * 0.65f, scale, flashAlpha)
                        // 绘制第二枚铜钱 (前)
                        drawCoin(cx + radius * 0.25f, cy - radius * 0.15f, radius * 0.65f, scale, 1.0f)
                    }
                }
                "SKIN_INK" -> { // 水墨江山：写意墨松与群山
                    translate(top = floatOffset * 0.4f) {
                        // 远山
                        val mountain1 = Path().apply {
                            moveTo(cx - radius * 0.9f, cy + radius * 0.5f)
                            lineTo(cx - radius * 0.4f, cy - radius * 0.3f)
                            lineTo(cx + radius * 0.2f, cy + radius * 0.5f)
                            close()
                        }
                        val mountain2 = Path().apply {
                            moveTo(cx - radius * 0.2f, cy + radius * 0.5f)
                            lineTo(cx + radius * 0.3f, cy - radius * 0.5f)
                            lineTo(cx + radius * 0.9f, cy + radius * 0.5f)
                            close()
                        }
                        drawPath(mountain1, color = Color(0xFF5A6065))
                        drawPath(mountain2, color = Color(0xFF2C2F31))
                        
                        // 金乌红日
                        drawCircle(
                            color = Color(0xFFCBAA6A).copy(alpha = flashAlpha),
                            radius = radius * 0.25f,
                            center = Offset(cx - radius * 0.3f, cy - radius * 0.4f)
                        )
                    }
                }
                "SKIN_CYBER" -> { // 赛博霓虹：炫光集成芯片网格
                    rotate(rotation * 0.5f, pivot = Offset(cx, cy)) {
                        val rectSize = radius * 0.5f
                        drawRect(
                            color = Color(0xFF00F2FE),
                            topLeft = Offset(cx - rectSize / 2f, cy - rectSize / 2f),
                            size = Size(rectSize, rectSize),
                            style = Stroke(width = 3.5f)
                        )
                        for (i in 0 until 4) {
                            val angle = i * 90f
                            rotate(angle, pivot = Offset(cx, cy)) {
                                drawLine(
                                    color = Color(0xFF05D9E8).copy(alpha = flashAlpha),
                                    start = Offset(cx, cy - rectSize / 2f),
                                    end = Offset(cx, cy - radius * 0.95f),
                                    strokeWidth = 3f
                                )
                                drawCircle(
                                    color = Color(0xFFFF2A6D).copy(alpha = flashAlpha),
                                    radius = radius * 0.08f,
                                    center = Offset(cx, cy - radius * 0.95f)
                                )
                            }
                        }
                    }
                }
                "CLASSIC" -> { // 经典国风：红金大灯笼
                    val lampPath = Path().apply {
                        moveTo(cx - radius * 0.4f, cy - radius * 0.5f)
                        lineTo(cx + radius * 0.4f, cy - radius * 0.5f)
                        quadraticTo(cx + radius * 0.6f, cy, cx + radius * 0.4f, cy + radius * 0.5f)
                        lineTo(cx - radius * 0.4f, cy + radius * 0.5f)
                        quadraticTo(cx - radius * 0.6f, cy, cx - radius * 0.4f, cy - radius * 0.5f)
                        close()
                    }
                    drawPath(lampPath, color = CrimsonRed)
                    drawPath(lampPath, color = Color(0xFFCBAA6A), style = Stroke(width = 3f))
                    drawLine(Color(0xFFCBAA6A), Offset(cx, cy - radius * 0.8f), Offset(cx, cy - radius * 0.5f), strokeWidth = 3f)
                    drawLine(CrimsonRed, Offset(cx, cy + radius * 0.5f), Offset(cx, cy + radius * 0.85f), strokeWidth = 4f)
                }
                else -> { // 默认显示红色圆环
                    drawCircle(
                        color = CrimsonRed,
                        radius = radius,
                        style = Stroke(width = 4f)
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCoin(
    x: Float,
    y: Float,
    r: Float,
    scale: Float,
    alpha: Float
) {
    // 外圆
    drawCircle(
        color = Color(0xFFE5B55F).copy(alpha = alpha),
        radius = r,
        center = Offset(x, y)
    )
    drawCircle(
        color = Color(0xFFCBAA6A).copy(alpha = alpha),
        radius = r,
        center = Offset(x, y),
        style = Stroke(width = 2.5f)
    )
    // 内方孔
    val size = r * 0.35f
    drawRect(
        color = Color(0xFFFCFAF6).copy(alpha = alpha),
        topLeft = Offset(x - size, y - size),
        size = Size(size * 2, size * 2)
    )
    drawRect(
        color = Color(0xFFCBAA6A).copy(alpha = alpha),
        topLeft = Offset(x - size, y - size),
        size = Size(size * 2, size * 2),
        style = Stroke(width = 2f)
    )
}
