package com.example.sheeps.game.ui.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb

/** Shared playfield for the three native Canvas renderers. Cache the grid across animation frames. */
internal class BoardBackdrop(density: Float) {
    private val spacing = 22f * density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = density * 1.3f
        strokeCap = Paint.Cap.ROUND
    }
    private val gridBounds = RectF()
    private var points = FloatArray(0)

    fun setColors(colors: ColorScheme) {
        fill.color = colors.surfaceContainerHigh.toArgb()
        dots.color = colors.onSurfaceVariant.copy(alpha = .15f).toArgb()
    }

    fun draw(canvas: Canvas, bounds: RectF, radius: Float) {
        if (bounds.isEmpty) return
        if (gridBounds != bounds) {
            gridBounds.set(bounds)
            val columns = (bounds.width() / spacing).toInt()
            val rows = (bounds.height() / spacing).toInt()
            points = FloatArray(columns * rows * 2)
            val left = bounds.centerX() - (columns - 1) * spacing / 2
            val top = bounds.centerY() - (rows - 1) * spacing / 2
            var i = 0
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    points[i++] = left + column * spacing
                    points[i++] = top + row * spacing
                }
            }
        }
        canvas.drawRoundRect(bounds, radius, radius, fill)
        canvas.drawPoints(points, dots)
    }
}
