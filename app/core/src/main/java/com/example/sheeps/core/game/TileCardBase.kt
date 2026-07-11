package com.example.sheeps.core.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 卡牌视觉基座 Composable。
 *
 * 负责绘制统一风格的卡牌外框（圆角 + 阴影）与四角装饰折线，并按 [skin] 选择主题色
 * （边框色 / 装饰色）。牌面具体图标由 [content] 插槽注入（通常为 [com.example.sheeps.core.game.TileIconProvider] 提供的 [RemoteImage]）。
 *
 * 线程约束：Composable 默认运行于主线程（@MainThread），仅做 Canvas 绘制，无耗时操作。
 * 重组注意：每次 [skin] 变化会重新计算颜色三元组，开销极小；应避免在调用处高频改变 [skin]。
 *
 * @param skin    皮肤渲染键（如 "shuang"/"electronic"），内部做 `lowercase()` 不区分大小写
 * @param modifier Compose 修饰符
 * @param content 卡牌正面图标内容插槽（@Composable 作用域）
 */
@Composable
fun TileCardBase(
    skin: String, // 皮肤名称
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit // 卡牌正面的图标内容
) {
    val cs = MaterialTheme.colorScheme
    val normalizedSkin = skin.lowercase()
    
    // 根据皮肤类型定义背景色、边框色和装饰线颜色
    val (bgColor, borderColor, decorColor) = when (normalizedSkin) {
        "shuang" -> Triple(cs.surface, Color(0xFF29B6F6), Color(0xFFFFCA28)) // 萌趣竞技：爽爽蓝边框+阳光金装饰
        "electronic" -> Triple(cs.surface, Color(0xFF21D4FD), Color(0xFFB721FF)) // 数码潮玩：科技蓝边框+霓虹紫装饰
        "daily" -> Triple(cs.surface, Color(0xFFFF9800), Color(0xFFFFC107)) // 日常好物：暖橙边框+阳光金装饰
        "vegetable" -> Triple(cs.surface, Color(0xFF4CAF50), Color(0xFF8BC34A)) // 蔬菜园：清新绿边框+嫩绿装饰
        "fruit" -> Triple(cs.surface, Color(0xFFFF5252), Color(0xFFFF8A80)) // 水果盘：鲜红边框+浅红装饰
        else -> Triple(cs.surface, cs.primary, cs.secondary) // 默认经典色调
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

            // 根据皮肤绘制边角的装饰线（所有皮肤统一走经典四角折线装饰）
            val decorLen = 12.dp.toPx()
            // 绘制四个角的折线装饰，颜色从 decorColor 读取
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
