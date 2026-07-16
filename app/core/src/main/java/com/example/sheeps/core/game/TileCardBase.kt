package com.example.sheeps.core.game

import android.content.Context
import android.util.TypedValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt

/**
 * 皮肤对应的边框与装饰色实体类。
 */
data class SkinColors(val borderColor: Int, val decorColor: Int)

/**
 * 辅助获取 Android 主题颜色。
 */
fun getThemeColor(context: Context, attrRes: Int, fallbackColor: Int): Int {
    val typedValue = TypedValue()
    return if (context.theme.resolveAttribute(attrRes, typedValue, true)) {
        typedValue.data
    } else fallbackColor
}

/**
 * 非 Composable 环境（如自绘 View 兜底或 AS 布局预览）下的皮肤颜色解析函数。
 */
fun getSkinColors(context: Context, skin: String): SkinColors {
    return when (skin.lowercase()) {
        "shuang" -> SkinColors("#0288D1".toColorInt(), "#FBC02D".toColorInt())
        "electronic" -> SkinColors("#00E5FF".toColorInt(), "#D500F9".toColorInt())
        "daily" -> SkinColors("#FF6D00".toColorInt(), "#FFD600".toColorInt())
        "vegetable" -> SkinColors("#2E7D32".toColorInt(), "#AEEA00".toColorInt())
        "fruit" -> SkinColors("#D50000".toColorInt(), "#FF80AB".toColorInt())
        "henan" -> SkinColors("#8D6E63".toColorInt(), "#FFB300".toColorInt())
        "sichuan" -> SkinColors("#C62828".toColorInt(), "#00897B".toColorInt())
        else -> {
            val primaryColor = getThemeColor(context, android.R.attr.colorPrimary, "#6200EE".toColorInt())
            val secondaryColor = getThemeColor(context, android.R.attr.colorSecondary, "#FFD54F".toColorInt())
            SkinColors(primaryColor, secondaryColor)
        }
    }
}

/**
 * 在 Composable 环境中按 TileCardBase 的算法解析并缓存当前皮肤的边框/装饰色。
 */
@Composable
fun rememberTileCardBorderColors(skin: String): Pair<Int, Int> {
    val cs = MaterialTheme.colorScheme
    return remember(skin, cs.primary, cs.secondary) {
        val borderColor = when (skin.lowercase()) {
            "shuang" -> Color(0xFF0288D1)
            "electronic" -> Color(0xFF00E5FF)
            "daily" -> Color(0xFFFF6D00)
            "vegetable" -> Color(0xFF2E7D32)
            "fruit" -> Color(0xFFD50000)
            "henan" -> Color(0xFF8D6E63)
            "sichuan" -> Color(0xFFC62828)
            else -> cs.primary
        }
        val decorColor = when (skin.lowercase()) {
            "shuang" -> Color(0xFFFBC02D)
            "electronic" -> Color(0xFFD500F9)
            "daily" -> Color(0xFFFFD600)
            "vegetable" -> Color(0xFFAEEA00)
            "fruit" -> Color(0xFFFF80AB)
            "henan" -> Color(0xFFFFB300)
            "sichuan" -> Color(0xFF00897B)
            else -> cs.secondary
        }
        borderColor.toArgb() to decorColor.toArgb()
    }
}

/**
 * 卡牌视觉基座 Composable。
 *
 * 负责绘制统一风格的卡牌外框（圆角 + 阴影）与四角装饰折线，并按 [skin] 选择主题色
 * （边框色 / 装饰色）。牌面具体图标由 [content] 插槽注入。
 */
@Composable
fun TileCardBase(
    skin: String, // 皮肤名称
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit // 卡牌正面的图标内容
) {
    val cs = MaterialTheme.colorScheme
    val normalizedSkin = skin.lowercase()
    
    val bgColor = cs.surface
    val (borderColorInt, decorColorInt) = rememberTileCardBorderColors(normalizedSkin)
    val borderColor = Color(borderColorInt)
    val decorColor = Color(decorColorInt)

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

            // 根据皮肤绘制边角的装饰线
            val decorLen = 12.dp.toPx()
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
        }
        // 在卡牌基座上层叠加具体的图标内容
        content()
    }
}
