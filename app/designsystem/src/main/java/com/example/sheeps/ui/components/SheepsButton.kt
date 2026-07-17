package com.example.sheeps.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.ui.theme.ShapeMedium
import com.example.sheeps.ui.theme.ShapeSmall
import com.example.sheeps.ui.theme.Text_Disabled_Dark
import com.example.sheeps.ui.theme.Text_OnPrimary
import com.example.sheeps.ui.theme.Vermilion_Error

// =============================================================================
// 秘境消消乐 · 设计系统 - 按钮组件
// 统一按钮规范：朱砂红渐变主按钮、描边次按钮、幽灵按钮
// =============================================================================

/**
 * 主按钮（Primary）：朱砂红渐变背景，金色文字
 * 带按压弹性缩放反馈
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 48.dp
) {
    // 按钮互动源：用于监听按钮按压状态以实现缩放动画
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 弹簧动画：提供 Q 弹的按压反馈
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "primaryButtonScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(height)
            .scale(scale), // 应用缩放动画
        interactionSource = interactionSource,
        contentPadding = if (icon != null) {
            ButtonDefaults.ButtonWithIconContentPadding
        } else {
            ButtonDefaults.TextButtonContentPadding
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Text_OnPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = Text_Disabled_Dark
        ),
        shape = ShapeMedium,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 0.dp // 按压时取消投影，增加下沉感
        )
    ) {
        // 图标+文字布局
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1
        )
    }
}

/**
 * 次级按钮（Secondary）：金色描边，透明背景
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 48.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "secondaryButtonScale"
    )

    // 使用 OutlinedButton 实现描边效果
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(height)
            .scale(scale),
        interactionSource = interactionSource,
        contentPadding = if (icon != null) {
            ButtonDefaults.ButtonWithIconContentPadding
        } else {
            ButtonDefaults.TextButtonContentPadding
        },
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.secondary,
            disabledContentColor = Text_Disabled_Dark
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (enabled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f) else Text_Disabled_Dark
        ),
        shape = ShapeMedium
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (enabled) MaterialTheme.colorScheme.secondary else Text_Disabled_Dark
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            maxLines = 1
        )
    }
}

/**
 * 幽灵按钮（Ghost）：无背景无边框，仅文字+轻微涟漪
 */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
) {
    // TextButton 本身即为无边框按钮
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            contentColor = color,
            disabledContentColor = Text_Disabled_Dark
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

/**
 * 危险操作按钮（Danger）：深红色，用于退出登录、删除等
 */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dangerScale"
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(48.dp)
            .scale(scale),
        interactionSource = interactionSource,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Vermilion_Error, // 红色错误色
            disabledContentColor = Text_Disabled_Dark
        ),
        border = BorderStroke(1.dp, Vermilion_Error.copy(alpha = 0.7f)),
        shape = ShapeMedium
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
    }
}

/**
 * 道具按钮（Tool）：游戏内道具栏专用，竖排图标+数量
 */
@Composable
fun ToolItemButton(
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "toolButtonScale"
    )

    val containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else Text_Disabled_Dark

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .scale(scale)
            .widthIn(min = 64.dp), // 最小宽度保证视觉统一
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = Text_Disabled_Dark
        ),
        shape = ShapeSmall,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 3.dp)
    ) {
        // 使用 Column 实现图标、名称、数量的纵向排列
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "×$count",
                fontSize = 10.sp,
                color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else Text_Disabled_Dark
            )
        }
    }
}
