package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun CompassCanvas() {
    val c = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val r = w / 2f
        drawCircle(color = c, radius = r, style = Stroke(width = 1.5.dp.toPx()))
        drawCircle(color = c.copy(alpha = 0.4f), radius = r * 0.6f, style = Stroke(width = 1.dp.toPx()))
        drawCircle(color = c, radius = r * 0.18f)
        drawLine(color = c, start = Offset(w/2f, h/2f - r * 0.65f), end = Offset(w/2f, h/2f + r * 0.65f), strokeWidth = 2.dp.toPx())
        drawLine(color = c, start = Offset(w/2f - r * 0.65f, h/2f), end = Offset(w/2f + r * 0.65f, h/2f), strokeWidth = 1.dp.toPx())
    }
}

@Composable
fun GourdCanvas() {
    val c = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val stroke = 1.5.dp.toPx()
        drawCircle(color = c, radius = w * 0.16f, center = Offset(cx, h * 0.35f), style = Stroke(width = stroke))
        drawCircle(color = c, radius = w * 0.23f, center = Offset(cx, h * 0.68f), style = Stroke(width = stroke))
        drawLine(color = c, start = Offset(cx, h * 0.46f), end = Offset(cx, h * 0.54f), strokeWidth = stroke * 1.5f)
    }
}

@Composable
fun StarCanvas() {
    val c = MaterialTheme.colorScheme.primary
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
        drawPath(path, color = c)
    }
}

@Composable
fun CoinCanvas() {
    val c = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val r = w / 2f
        val stroke = 1.5.dp.toPx()
        drawCircle(color = c, radius = r * 0.8f, style = Stroke(width = stroke))
        drawRect(
            color = c,
            topLeft = Offset(w/2f - r * 0.25f, h/2f - r * 0.25f),
            size = Size(r * 0.5f, r * 0.5f),
            style = Stroke(width = stroke)
        )
    }
}
