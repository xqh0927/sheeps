package com.example.sheeps.core.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun TileCardBase(
    skin: String, // 皮肤名称
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit // 卡牌正面的图标内容
) {
    val normalizedSkin = skin.lowercase()
    
    // 根据皮肤类型定义背景色、边框色和装饰线颜色
    val (bgColor, borderColor, decorColor) = when (normalizedSkin) {
        "ink" -> Triple(Color(0xFFF2EFE9), Color(0xFF333333), Color(0xFF888888)) // 水墨风格
        "cyber" -> Triple(Color(0xFF0D0D0D), Color(0xFF00F2FE), Color(0xFFFF2A6D)) // 赛博朋克风格
        "classic" -> Triple(Color(0xFFFFFDF9), Color(0xFFCBAA6A), Color(0xFFD4AF37)) // 经典中国风
        else -> Triple(Color(0xFFFFFDF9), Color(0xFFCBAA6A), Color(0xFFD4AF37)) // 默认经典色调
    }

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeWidth = 2.dp.toPx()

            // 绘制卡牌外边框
            drawRoundRect(
                color = borderColor,
                size = Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )

            // 根据皮肤绘制边角的装饰线（区分经典风格和赛博风格）
            if (normalizedSkin == "classic" || normalizedSkin != "cyber") {
                val decorLen = 12.dp.toPx()
                // 绘制四个角的折线装饰
                // 左上角
                drawLine(decorColor, Offset(0f, decorLen), Offset(0f, 0f), strokeWidth)
                drawLine(decorColor, Offset(0f, 0f), Offset(decorLen, 0f), strokeWidth)
                // 右上角
                drawLine(decorColor, Offset(w - decorLen, 0f), Offset(w, 0f), strokeWidth)
                drawLine(decorColor, Offset(w, 0f), Offset(w, decorLen), strokeWidth)
                // 左下角
                drawLine(decorColor, Offset(0f, h - decorLen), Offset(0f, h), strokeWidth)
                drawLine(decorColor, Offset(0f, h), Offset(decorLen, h), strokeWidth)
                // 右下角
                drawLine(decorColor, Offset(w - decorLen, h), Offset(w, h), strokeWidth)
                drawLine(decorColor, Offset(w, h), Offset(w, h - decorLen), strokeWidth)
            } else {
                // 赛博风格装饰：绘制一条霓虹渐变线
                drawLine(
                    brush = Brush.linearGradient(listOf(Color(0xFF00F2FE), Color(0xFFFF2A6D))),
                    start = Offset(0f, h * 0.8f),
                    end = Offset(w, h * 0.8f),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        // 在卡牌基座上层叠加具体的图标内容
        content()
    }
}
