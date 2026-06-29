package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.sheeps.theme.CrimsonRed

@Composable
fun CompassCanvas() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val r = w / 2f
        // 外圆
        drawCircle(color = CrimsonRed, radius = r, style = Stroke(width = 1.5.dp.toPx()))
        // 内八卦线
        drawCircle(color = CrimsonRed.copy(alpha = 0.4f), radius = r * 0.6f, style = Stroke(width = 1.dp.toPx()))
        // 中心点
        drawCircle(color = CrimsonRed, radius = r * 0.18f)
        // 指针
        drawLine(color = CrimsonRed, start = Offset(w/2f, h/2f - r * 0.65f), end = Offset(w/2f, h/2f + r * 0.65f), strokeWidth = 2.dp.toPx())
        drawLine(color = CrimsonRed, start = Offset(w/2f - r * 0.65f, h/2f), end = Offset(w/2f + r * 0.65f, h/2f), strokeWidth = 1.dp.toPx())
    }
}

@Composable
fun GourdCanvas() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val stroke = 1.5.dp.toPx()
        // 上圆
        drawCircle(color = CrimsonRed, radius = w * 0.16f, center = Offset(cx, h * 0.35f), style = Stroke(width = stroke))
        // 下圆
        drawCircle(color = CrimsonRed, radius = w * 0.23f, center = Offset(cx, h * 0.68f), style = Stroke(width = stroke))
        // 腰线
        drawLine(color = CrimsonRed, start = Offset(cx, h * 0.46f), end = Offset(cx, h * 0.54f), strokeWidth = stroke * 1.5f)
    }
}

@Composable
fun StarCanvas() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w * 0.42f
        
        val path = Path().apply {
            moveTo(cx, cy - r)
            quadraticTo(cx, cy, cx + r, cy)
            quadraticTo(cx, cy, cx, cy + r)
            quadraticTo(cx, cy, cx - r, cy)
            quadraticTo(cx, cy, cx, cy - r)
        }
        drawPath(path, color = CrimsonRed)
    }
}

@Composable
fun CoinCanvas() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val r = w / 2f
        val stroke = 1.5.dp.toPx()
        
        drawCircle(color = CrimsonRed, radius = r * 0.8f, style = Stroke(width = stroke))
        drawRect(
            color = CrimsonRed,
            topLeft = Offset(w/2f - r * 0.25f, h/2f - r * 0.25f),
            size = Size(r * 0.5f, r * 0.5f),
            style = Stroke(width = stroke)
        )
    }
}
