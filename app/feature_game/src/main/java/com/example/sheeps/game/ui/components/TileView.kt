package com.example.sheeps.game.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.theme.*
import kotlin.math.*

// =============================================================================
// 秘境消消乐 · 卡牌组件（全面重设计）
// 12种中式图案：龙/凤/莲/鹤/锦鲤/如意/葫芦/符/剑/扇/琴/棋
// 使用 Compose Canvas 自绘，完全不依赖图片资源，风格100%统一
// =============================================================================

@Composable
fun TileView(
    tile: Tile,
    onClick: () -> Unit,
    currentSkin: String = "classic",
    modifier: Modifier = Modifier,
    tileSize: Dp = 52.dp
) {
    val isBlocked = tile.state == TileState.BLOCKED
    val isBlind   = tile.isBlind && isBlocked
    val isSealed  = tile.sealedCount > 0

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 按压弹性缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isBlocked) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "tileScale"
    )

    // 封印波动动画
    val infiniteTransition = rememberInfiniteTransition(label = "sealed")
    val sealAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue  = 0.75f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sealAlpha"
    )

    // 阴影深度（可点击卡片稍高）
    val elevation = if (isBlocked) 0.dp else 4.dp

    Box(
        modifier = modifier
            .graphicsLayer(
                scaleX           = scale,
                scaleY           = scale,
                shadowElevation  = if (!isBlocked) elevation.value else 0f,
                shape            = RoundedCornerShape(10.dp),
                clip             = false
            )
            .size(tileSize)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = true,
                onClick           = { if (!isBlocked) onClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isBlind) {
            // 神秘牌：显示问号背面
            BlindTileCanvas(size = tileSize)
        } else {
            // 普通/封印牌：自绘卡牌
            TileCanvas(
                type      = tile.type,
                isBlocked = isBlocked,
                isSealed  = isSealed,
                currentSkin = currentSkin,
                size      = tileSize
            )
            // 封印叠加层（在此也设为透明不可见，使其表现为空白卡）
            if (isSealed) {
                SealedOverlayCanvas(
                    size  = tileSize,
                    alpha = 0f
                )
            }
        }
    }
}

// =============================================================================
// 卡牌底层 Canvas 绘制
// =============================================================================

@Composable
private fun TileCanvas(
    type: Int,
    isBlocked: Boolean,
    isSealed: Boolean,
    currentSkin: String,
    size: Dp
) {
    val bgAlpha = if (isBlocked) 0.45f else 1f

    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val r = 10.dp.toPx()

        // --- 卡牌底色（根据皮肤选择）---
        val bgBrush = if (isBlocked) {
            Brush.linearGradient(
                colors = if (currentSkin == "cyber") listOf(Color(0xFF1F2535), Color(0xFF161A26))
                         else listOf(Color(0xFF2A2D3A), Color(0xFF1E2130)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        } else {
            when (currentSkin) {
                "ink" -> Brush.linearGradient(
                    colors = listOf(Color(0xFFFAF8F5), Color(0xFFF3EFE6)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
                "cyber" -> Brush.linearGradient(
                    colors = listOf(Color(0xFF121824), Color(0xFF090D16)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
                else -> Brush.linearGradient(
                    colors = listOf(Color(0xFFF5E6C8), Color(0xFFEDD9A3), Color(0xFFE5CC8A)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            }
        }
        drawRoundRect(brush = bgBrush, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))

        // --- 卡牌边框（根据皮肤选择）---
        val borderColor = if (isBlocked) {
            if (currentSkin == "cyber") Color(0xFF2B3346) else Color(0xFF3A4050)
        } else {
            when (currentSkin) {
                "ink" -> Color(0xFF4C4942)
                "cyber" -> Color(0xFF00D2FF)
                else -> Color(0xFFCBAA6A)
            }
        }
        val finalBorderColor = if (isSealed) borderColor.copy(alpha = 0f) else borderColor
        
        // 赛博霓虹边框发光特殊绘制（双边框效果）
        if (currentSkin == "cyber" && !isBlocked && !isSealed) {
            drawRoundRect(
                color       = Color(0xFF00F2FE).copy(alpha = 0.3f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                style       = Stroke(width = 3.5f)
            )
        }
        
        drawRoundRect(
            color       = finalBorderColor,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            style       = Stroke(width = 1.5f)
        )

        // --- 内框装饰线（根据皮肤选择，水墨款较暗，赛博款为荧光绿）---
        val inset = 4f
        val lineColor = if (isBlocked) {
            if (currentSkin == "cyber") Color(0xFF374158) else Color(0xFF4A5060)
        } else {
            when (currentSkin) {
                "ink" -> Color(0xFF8C867A)
                "cyber" -> Color(0xFF05D9E8).copy(alpha = 0.4f)
                else -> Color(0xFFDEC07A)
            }
        }
        val lineAlpha = if (isSealed) 0f else (if (isBlocked) 0.3f else 0.5f)
        withTransform({ /* 内框 */ }) {
            // 四个角的短线装饰
            val c = 8f
            val i = inset + 2f
            // 左上
            drawLine(lineColor.copy(alpha = lineAlpha), Offset(i, i + c), Offset(i, i), strokeWidth = 1f)
            drawLine(lineColor.copy(alpha = lineAlpha), Offset(i, i), Offset(i + c, i), strokeWidth = 1f)
            // 右上
            drawLine(lineColor.copy(alpha = lineAlpha), Offset(w - i, i + c), Offset(w - i, i), strokeWidth = 1f)
            drawLine(lineColor.copy(alpha = lineAlpha), Offset(w - i, i), Offset(w - i - c, i), strokeWidth = 1f)
            // 左下
            drawLine(lineColor.copy(alpha = lineAlpha), Offset(i, h - i - c), Offset(i, h - i), strokeWidth = 1f)
            drawLine(lineColor.copy(alpha = lineAlpha), Offset(i, h - i), Offset(i + c, h - i), strokeWidth = 1f)
            // 右下
            drawLine(lineColor.copy(alpha = lineAlpha), Offset(w - i, h - i - c), Offset(w - i, h - i), strokeWidth = 1f)
            drawLine(lineColor.copy(alpha = lineAlpha), Offset(w - i, h - i), Offset(w - i - c, h - i), strokeWidth = 1f)
        }

        // --- 中心图案 ---
        val iconColor = if (isBlocked) {
            if (currentSkin == "cyber") Color(0xFF47526E) else Color(0xFF5A6070)
        } else {
            when (currentSkin) {
                "ink" -> Color(0xFF2E3133)
                "cyber" -> getCyberIconColor(type)
                else -> getTileIconColor(type)
            }
        }
        val finalIconColor = if (isSealed) iconColor.copy(alpha = 0f) else iconColor
        val cx = w / 2f
        val cy = h / 2f
        val iconSize = w * 0.45f

        translate(cx - iconSize / 2f, cy - iconSize / 2f) {
            drawTileIcon(type = type, size = iconSize, color = finalIconColor, isBlocked = isBlocked)
        }
    }
}

@Composable
private fun BlindTileCanvas(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val r = 10.dp.toPx()

        // 暗色底色
        val bgBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF1A1E2A), Color(0xFF242836)),
            start  = Offset(0f, 0f),
            end    = Offset(w, h)
        )
        drawRoundRect(brush = bgBrush, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))

        // 隐藏的神秘边框（透明度设为 0f）
        drawRoundRect(
            color        = Color(0xFF7B5EA7).copy(alpha = 0f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            style        = Stroke(width = 1.5f)
        )

        // 问号（用路径绘制简化版，透明度设为 0f 使其不可见）
        val cx = w / 2f
        val cy = h / 2f
        val qSize = w * 0.35f

        // 问号上半圆弧
        drawArc(
            color      = Color(0xFF9B7ED4).copy(alpha = 0f),
            startAngle = 200f,
            sweepAngle = 220f,
            useCenter  = false,
            topLeft    = Offset(cx - qSize / 2f, cy - qSize * 0.7f),
            size       = Size(qSize, qSize * 0.9f),
            style      = Stroke(width = qSize * 0.18f, cap = StrokeCap.Round)
        )
        // 问号竖线
        drawLine(
            color       = Color(0xFF9B7ED4).copy(alpha = 0f),
            start       = Offset(cx, cy - qSize * 0.05f),
            end         = Offset(cx, cy + qSize * 0.18f),
            strokeWidth = qSize * 0.18f,
            cap         = StrokeCap.Round
        )
        // 问号下点
        drawCircle(
            color  = Color(0xFF9B7ED4).copy(alpha = 0f),
            radius = qSize * 0.1f,
            center = Offset(cx, cy + qSize * 0.38f)
        )
    }
}

@Composable
private fun SealedOverlayCanvas(size: Dp, alpha: Float) {
    Canvas(modifier = Modifier.size(size).alpha(alpha)) {
        val w = this.size.width
        val h = this.size.height
        val r = 10.dp.toPx()

        // 金色封印叠加（渐变透明）
        val sealBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE6A23C).copy(alpha = 0.7f),
                Color(0xFFE6A23C).copy(alpha = 0.3f),
                Color(0xFFE6A23C).copy(alpha = 0.05f)
            ),
            center = Offset(w / 2f, h / 2f),
            radius = w * 0.7f
        )
        drawRoundRect(brush = sealBrush, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))

        // 中心符文（简化五角星形）
        val cx = w / 2f
        val cy = h / 2f
        val starR = w * 0.22f
        val path = Path()
        for (i in 0 until 5) {
            val angle = Math.toRadians((-90.0 + 72.0 * i))
            val x = cx + starR * cos(angle).toFloat()
            val y = cy + starR * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(
            path  = path,
            color = Color(0xFFE6A23C).copy(alpha = 0.5f),
            style = Stroke(width = 1.5f, cap = StrokeCap.Round)
        )
    }
}

// =============================================================================
// 各类型图案绘制函数（12种中式图案）
// 1=龙 2=凤 3=莲 4=鹤 5=锦鲤 6=如意 7=葫芦 8=符 9=剑 10=扇 11=琴 12=棋
// =============================================================================

private fun DrawScope.drawTileIcon(type: Int, size: Float, color: Color, isBlocked: Boolean) {
    when (type) {
        1  -> drawDragon(size, color)
        2  -> drawPhoenix(size, color)
        3  -> drawLotus(size, color)
        4  -> drawCrane(size, color)
        5  -> drawKoi(size, color)
        6  -> drawRuyi(size, color)
        7  -> drawGourd(size, color)
        8  -> drawTalisman(size, color)
        9  -> drawSword(size, color)
        10 -> drawFan(size, color)
        11 -> drawQin(size, color)
        12 -> drawWeiqi(size, color)
        else -> drawDefaultCircle(size, color)
    }
}

fun getTileIconColor(type: Int): Color = when (type) {
    1  -> Color(0xFFE63946)  // 龙 - 朱红
    2  -> Color(0xFFE8A838)  // 凤 - 金橙
    3  -> Color(0xFFE85D9A)  // 莲 - 桃红
    4  -> Color(0xFF4A90D9)  // 鹤 - 天青
    5  -> Color(0xFFFF6B35)  // 锦鲤 - 橘红
    6  -> Color(0xFF8B4513)  // 如意 - 檀木棕
    7  -> Color(0xFF5C9E4A)  // 葫芦 - 翡翠
    8  -> Color(0xFF8B5CF6)  // 符 - 紫
    9  -> Color(0xFF94A3B8)  // 剑 - 银
    10 -> Color(0xFFF59E0B)  // 扇 - 琥珀
    11 -> Color(0xFF6B7280)  // 琴 - 乌木灰
    12 -> Color(0xFF1F2937)  // 棋 - 乌木黑
    else -> Color(0xFF6B7280)
}

// --- 龙（简化：S形曲线+鳞片）---
private fun DrawScope.drawDragon(size: Float, color: Color) {
    val stroke = size * 0.14f
    val path = Path().apply {
        moveTo(size * 0.2f, size * 0.8f)
        cubicTo(size * 0.1f, size * 0.5f, size * 0.9f, size * 0.5f, size * 0.8f, size * 0.2f)
    }
    drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    // 龙头圆（头部）
    drawCircle(color, radius = stroke * 0.9f, center = Offset(size * 0.8f, size * 0.2f))
    // 四个装饰点（爪）
    val pts = listOf(
        Offset(size * 0.25f, size * 0.55f),
        Offset(size * 0.35f, size * 0.65f),
        Offset(size * 0.65f, size * 0.35f),
        Offset(size * 0.75f, size * 0.45f)
    )
    pts.forEach { drawCircle(color.copy(alpha = 0.6f), radius = stroke * 0.4f, center = it) }
}

// --- 凤（尾羽展开）---
private fun DrawScope.drawPhoenix(size: Float, color: Color) {
    val stroke = size * 0.12f
    // 身体曲线
    val body = Path().apply {
        moveTo(size * 0.5f, size * 0.7f)
        cubicTo(size * 0.3f, size * 0.5f, size * 0.4f, size * 0.3f, size * 0.5f, size * 0.25f)
    }
    drawPath(body, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    // 尾羽（三条弧线）
    val tailPts = listOf(-20f, 0f, 20f)
    tailPts.forEach { angle ->
        val tailPath = Path().apply {
            moveTo(size * 0.5f, size * 0.7f)
            val rad = Math.toRadians(90.0 + angle)
            val ex = size * 0.5f + size * 0.35f * cos(rad).toFloat()
            val ey = size * 0.7f + size * 0.35f * sin(rad).toFloat()
            cubicTo(
                size * 0.5f + size * 0.1f * cos(rad - 0.5).toFloat(),
                size * 0.7f + size * 0.15f * sin(rad - 0.5).toFloat(),
                ex - size * 0.05f, ey - size * 0.05f,
                ex, ey
            )
        }
        drawPath(tailPath, color.copy(alpha = 0.7f), style = Stroke(width = stroke * 0.7f, cap = StrokeCap.Round))
    }
    // 头部
    drawCircle(color, radius = size * 0.08f, center = Offset(size * 0.5f, size * 0.18f))
}

// --- 莲花（六瓣）---
private fun DrawScope.drawLotus(size: Float, color: Color) {
    val cx = size / 2f
    val cy = size / 2f
    val petalR = size * 0.22f
    val stroke = size * 0.1f
    for (i in 0 until 6) {
        val angle = Math.toRadians(60.0 * i)
        val px = cx + petalR * cos(angle).toFloat()
        val py = cy + petalR * sin(angle).toFloat()
        drawCircle(color.copy(alpha = 0.7f), radius = size * 0.14f, center = Offset(px, py))
    }
    drawCircle(color, radius = size * 0.12f, center = Offset(cx, cy))
    // 茎
    drawLine(color.copy(alpha = 0.5f), start = Offset(cx, cy + size * 0.28f), end = Offset(cx, cy + size * 0.45f), strokeWidth = stroke * 0.6f)
}

// --- 仙鹤（简化：圆头+长颈+展翅）---
private fun DrawScope.drawCrane(size: Float, color: Color) {
    val stroke = size * 0.1f
    // 头
    drawCircle(color, radius = size * 0.09f, center = Offset(size * 0.5f, size * 0.2f))
    // 红顶
    drawCircle(Color(0xFFE63946), radius = size * 0.05f, center = Offset(size * 0.5f, size * 0.13f))
    // 颈
    val neck = Path().apply {
        moveTo(size * 0.5f, size * 0.28f)
        lineTo(size * 0.5f, size * 0.55f)
    }
    drawPath(neck, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    // 左翅
    drawArc(color.copy(alpha = 0.8f), startAngle = 200f, sweepAngle = 80f, useCenter = false,
        topLeft = Offset(size * 0.05f, size * 0.35f), size = Size(size * 0.45f, size * 0.25f),
        style = Stroke(width = stroke * 0.8f, cap = StrokeCap.Round))
    // 右翅
    drawArc(color.copy(alpha = 0.8f), startAngle = -60f, sweepAngle = 80f, useCenter = false,
        topLeft = Offset(size * 0.5f, size * 0.35f), size = Size(size * 0.45f, size * 0.25f),
        style = Stroke(width = stroke * 0.8f, cap = StrokeCap.Round))
    // 腿
    drawLine(color.copy(alpha = 0.6f), start = Offset(size * 0.45f, size * 0.55f), end = Offset(size * 0.4f, size * 0.85f), strokeWidth = stroke * 0.6f)
    drawLine(color.copy(alpha = 0.6f), start = Offset(size * 0.55f, size * 0.55f), end = Offset(size * 0.6f, size * 0.85f), strokeWidth = stroke * 0.6f)
}

// --- 锦鲤（S形鱼身+尾巴）---
private fun DrawScope.drawKoi(size: Float, color: Color) {
    val stroke = size * 0.14f
    // 鱼身
    val body = Path().apply {
        moveTo(size * 0.3f, size * 0.2f)
        cubicTo(size * 0.65f, size * 0.15f, size * 0.65f, size * 0.85f, size * 0.3f, size * 0.8f)
        cubicTo(size * 0.1f, size * 0.78f, size * 0.1f, size * 0.22f, size * 0.3f, size * 0.2f)
    }
    drawPath(body, color.copy(alpha = 0.85f), style = Fill)
    // 尾巴
    val tail = Path().apply {
        moveTo(size * 0.65f, size * 0.5f)
        lineTo(size * 0.95f, size * 0.2f)
        lineTo(size * 0.95f, size * 0.8f)
        close()
    }
    drawPath(tail, color.copy(alpha = 0.6f), style = Fill)
    // 眼睛
    drawCircle(Color.White, radius = size * 0.05f, center = Offset(size * 0.3f, size * 0.35f))
    drawCircle(Color.Black, radius = size * 0.03f, center = Offset(size * 0.3f, size * 0.35f))
}

// --- 如意（U形头+长柄）---
private fun DrawScope.drawRuyi(size: Float, color: Color) {
    val stroke = size * 0.12f
    // 如意头（三个圆弧）
    drawArc(color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(size * 0.1f, size * 0.1f), size = Size(size * 0.35f, size * 0.35f),
        style = Stroke(width = stroke, cap = StrokeCap.Round))
    drawArc(color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(size * 0.45f, size * 0.1f), size = Size(size * 0.35f, size * 0.35f),
        style = Stroke(width = stroke, cap = StrokeCap.Round))
    // 手柄
    val handle = Path().apply {
        moveTo(size * 0.5f, size * 0.45f)
        cubicTo(size * 0.4f, size * 0.6f, size * 0.5f, size * 0.75f, size * 0.45f, size * 0.9f)
    }
    drawPath(handle, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    // 底部如意云头
    drawCircle(color, radius = size * 0.1f, center = Offset(size * 0.45f, size * 0.9f))
}

// --- 葫芦（两个圆+连接线）---
private fun DrawScope.drawGourd(size: Float, color: Color) {
    val stroke = size * 0.11f
    // 上圆（小）
    drawCircle(color, radius = size * 0.2f, center = Offset(size * 0.5f, size * 0.28f), style = Stroke(width = stroke))
    // 下圆（大）
    drawCircle(color, radius = size * 0.28f, center = Offset(size * 0.5f, size * 0.65f), style = Stroke(width = stroke))
    // 顶部藤茎
    drawLine(color.copy(alpha = 0.7f), start = Offset(size * 0.5f, size * 0.08f), end = Offset(size * 0.5f, size * 0.07f), strokeWidth = stroke)
    drawLine(color.copy(alpha = 0.7f), start = Offset(size * 0.5f, size * 0.07f), end = Offset(size * 0.35f, size * 0.07f), strokeWidth = stroke * 0.7f)
    // 腰部填色
    drawLine(color, start = Offset(size * 0.5f, size * 0.48f), end = Offset(size * 0.5f, size * 0.37f), strokeWidth = stroke * 1.5f)
}

// --- 符（符文简化：方框+横线纹路）---
private fun DrawScope.drawTalisman(size: Float, color: Color) {
    val stroke = size * 0.1f
    // 外框
    drawRoundRect(color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f), topLeft = Offset(size * 0.15f, size * 0.1f),
        size = Size(size * 0.7f, size * 0.85f), style = Stroke(width = stroke * 0.8f))
    // 横线纹路（符文笔画）
    val lines = listOf(0.28f, 0.42f, 0.56f, 0.7f, 0.84f)
    lines.forEachIndexed { i, y ->
        val xRatio = if (i % 2 == 0) 0.22f else 0.3f
        drawLine(color.copy(alpha = 0.7f), start = Offset(size * xRatio, size * y),
            end = Offset(size * (1f - xRatio), size * y), strokeWidth = stroke * 0.7f)
    }
    // 顶部标志
    drawCircle(color, radius = size * 0.07f, center = Offset(size * 0.5f, size * 0.18f))
}

// --- 剑（直线+护手）---
private fun DrawScope.drawSword(size: Float, color: Color) {
    val stroke = size * 0.11f
    // 剑身
    val blade = Path().apply {
        moveTo(size * 0.5f, size * 0.05f)
        lineTo(size * 0.5f, size * 0.75f)
    }
    drawPath(blade, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    // 剑尖
    val tip = Path().apply {
        moveTo(size * 0.5f - stroke / 2, size * 0.75f)
        lineTo(size * 0.5f, size * 0.95f)
        lineTo(size * 0.5f + stroke / 2, size * 0.75f)
    }
    drawPath(tip, color, style = Fill)
    // 护手
    drawLine(color, start = Offset(size * 0.22f, size * 0.68f), end = Offset(size * 0.78f, size * 0.68f), strokeWidth = stroke * 1.2f, cap = StrokeCap.Round)
    // 剑柄
    drawLine(color.copy(alpha = 0.7f), start = Offset(size * 0.5f, size * 0.68f), end = Offset(size * 0.5f, size * 0.85f), strokeWidth = stroke * 1.5f, cap = StrokeCap.Round)
}

// --- 折扇（展开扇形）---
private fun DrawScope.drawFan(size: Float, color: Color) {
    val cx = size * 0.5f
    val cy = size * 0.75f
    // 扇形面（多条弧线）
    for (angle in listOf(-50f, -25f, 0f, 25f, 50f)) {
        val rad = Math.toRadians((-90.0 + angle))
        val ex = cx + size * 0.38f * cos(rad).toFloat()
        val ey = cy + size * 0.38f * sin(rad).toFloat()
        drawLine(color.copy(alpha = 0.6f), start = Offset(cx, cy), end = Offset(ex, ey), strokeWidth = size * 0.08f, cap = StrokeCap.Round)
    }
    // 扇弧
    drawArc(color, startAngle = -140f, sweepAngle = 100f, useCenter = false,
        topLeft = Offset(cx - size * 0.38f, cy - size * 0.38f), size = Size(size * 0.76f, size * 0.76f),
        style = Stroke(width = size * 0.1f, cap = StrokeCap.Round))
    // 扇骨握柄
    drawCircle(color, radius = size * 0.07f, center = Offset(cx, cy))
}

// --- 古琴（弦琴简化）---
private fun DrawScope.drawQin(size: Float, color: Color) {
    val stroke = size * 0.09f
    // 琴身轮廓
    val body = Path().apply {
        moveTo(size * 0.3f, size * 0.15f)
        cubicTo(size * 0.1f, size * 0.25f, size * 0.1f, size * 0.75f, size * 0.3f, size * 0.85f)
        lineTo(size * 0.7f, size * 0.85f)
        cubicTo(size * 0.9f, size * 0.75f, size * 0.9f, size * 0.25f, size * 0.7f, size * 0.15f)
        close()
    }
    drawPath(body, color.copy(alpha = 0.15f), style = Fill)
    drawPath(body, color, style = Stroke(width = stroke))
    // 七根琴弦
    for (i in 0 until 7) {
        val x = size * (0.22f + 0.08f * i)
        drawLine(color.copy(alpha = 0.5f), start = Offset(x, size * 0.2f), end = Offset(x, size * 0.8f), strokeWidth = stroke * 0.4f)
    }
    // 岳山（上下横条）
    drawLine(color.copy(alpha = 0.7f), start = Offset(size * 0.2f, size * 0.22f), end = Offset(size * 0.8f, size * 0.22f), strokeWidth = stroke * 0.8f)
    drawLine(color.copy(alpha = 0.7f), start = Offset(size * 0.2f, size * 0.78f), end = Offset(size * 0.8f, size * 0.78f), strokeWidth = stroke * 0.8f)
}

// --- 围棋（棋盘+棋子）---
private fun DrawScope.drawWeiqi(size: Float, color: Color) {
    val stroke = size * 0.07f
    // 棋盘格（3x3简化）
    for (i in 1..3) {
        val x = size * (0.2f + 0.2f * i)
        drawLine(color.copy(alpha = 0.3f), start = Offset(x, size * 0.2f), end = Offset(x, size * 0.8f), strokeWidth = stroke * 0.5f)
        drawLine(color.copy(alpha = 0.3f), start = Offset(size * 0.2f, x), end = Offset(size * 0.8f, x), strokeWidth = stroke * 0.5f)
    }
    // 黑白棋子
    drawCircle(color, radius = size * 0.12f, center = Offset(size * 0.4f, size * 0.4f))
    drawCircle(Color.White, radius = size * 0.12f, center = Offset(size * 0.6f, size * 0.6f))
    drawCircle(color, radius = size * 0.12f, center = Offset(size * 0.6f, size * 0.6f), style = Stroke(width = stroke * 0.8f))
    drawCircle(Color.White, radius = size * 0.1f, center = Offset(size * 0.6f, size * 0.6f))
    drawCircle(color, radius = size * 0.08f, center = Offset(size * 0.6f, size * 0.6f))
}

// --- 默认圆圈 ---
private fun DrawScope.drawDefaultCircle(size: Float, color: Color) {
    drawCircle(color, radius = size * 0.35f, center = Offset(size / 2f, size / 2f), style = Stroke(width = size * 0.12f))
}

// 向后兼容函数
fun getWebpDrawableForTileType(type: Int): Int = 0 // 不再使用，由 Canvas 自绘替代

fun getCyberIconColor(type: Int): Color = when (type) {
    1  -> Color(0xFFFF2A6D)  // 龙 - 荧光红
    2  -> Color(0xFFFF5E00)  // 凤 - 荧光橙
    3  -> Color(0xFFFF007F)  // 莲 - 荧光粉
    4  -> Color(0xFF05D9E8)  // 鹤 - 荧光青
    5  -> Color(0xFFFF9F00)  // 锦鲤 - 荧光黄
    6  -> Color(0xFFBF5AF2)  // 如意 - 荧光紫
    7  -> Color(0xFF30D158)  // 葫芦 - 荧光绿
    8  -> Color(0xFF64D2FF)  // 符 - 荧光蓝
    9  -> Color(0xFFE5E5EA)  // 剑 - 银白
    10 -> Color(0xFFFFD60A)  // 扇 - 荧光金
    11 -> Color(0xFFAED8F2)  // 琴 - 浅靛
    12 -> Color(0xFF007AFF)  // 棋 - 炫蓝
    else -> Color(0xFF05D9E8)
}
