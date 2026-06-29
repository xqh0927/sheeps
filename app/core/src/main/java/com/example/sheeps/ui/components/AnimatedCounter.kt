package com.example.sheeps.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.theme.Gold_Primary

// =============================================================================
// 秘境消消乐 · 设计系统 - 数字滚动动画组件
// 用于积分显示、统计数字等场景，数字变化时触发垂直滚动效果
// =============================================================================

/**
 * 数字滚动动画文字
 * 当 count 变化时，旧数字向上飞出，新数字从下方进入
 */
@Composable
fun AnimatedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current.copy(
        fontWeight = FontWeight.Bold,
        fontSize   = 22.sp,
        color      = Gold_Primary
    )
) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState > initialState) {
                // 数字增加：新数字从下方滑入，旧数字向上飞出
                (slideInVertically { height -> height } + fadeIn(tween(300))) togetherWith
                        (slideOutVertically { height -> -height } + fadeOut(tween(200)))
            } else {
                // 数字减少：新数字从上方滑入，旧数字向下飞出
                (slideInVertically { height -> -height } + fadeIn(tween(300))) togetherWith
                        (slideOutVertically { height -> height } + fadeOut(tween(200)))
            }
        },
        modifier = modifier,
        label = "animatedCounter"
    ) { targetCount ->
        Text(
            text  = targetCount.toString(),
            style = style
        )
    }
}

/**
 * 带单位的积分展示组件
 * 例如：「1234 积分」
 */
@Composable
fun PointsDisplay(
    points: Int,
    modifier: Modifier = Modifier,
    numberStyle: TextStyle = LocalTextStyle.current.copy(
        fontWeight = FontWeight.Bold,
        fontSize   = 28.sp,
        color      = Gold_Primary
    ),
    unitStyle: TextStyle = LocalTextStyle.current.copy(
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        color      = Gold_Primary.copy(alpha = 0.7f)
    )
) {
    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        AnimatedCounter(count = points, style = numberStyle)
        Spacer(Modifier.width(4.dp))
        Text(text = "积分", style = unitStyle)
    }
}
