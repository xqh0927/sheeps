package com.example.sheeps.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.core.R
import com.example.sheeps.theme.*

// =============================================================================
// 秘境消消乐 · 设计系统 - 统一 Dialog 组件
// 统一弹窗样式：深色毛玻璃背景 + 金色边框 + 弹性进入动画
// =============================================================================

/**
 * 标准 Dialog（替代 AlertDialog）
 * 特性：
 * - 深色表面 + 金色细边框
 * - 标题使用衬线国风字体
 * - 弹性进入/淡出动画
 * - 按钮统一样式
 */
@Composable
fun SheepsDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismissRequest() },
        properties = DialogProperties(
            dismissOnBackPress  = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false
        )
    ) {
        AnimatedVisibility(
            visible = true,
            enter   = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium
                ),
                initialScale = 0.85f
            ) + fadeIn(androidx.compose.animation.core.tween(250)),
            exit    = scaleOut(targetScale = 0.9f) + fadeOut(androidx.compose.animation.core.tween(200))
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .clip(ShapeLarge)
                    .background(MaterialTheme.colorScheme.surface)
                    .then(
                        Modifier.padding(1.dp) // 金色细边框效果（通过外层 padding+内层 background 模拟）
                    )
            ) {
                // 金色边框
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(ShapeLarge)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                            )
                        )
                )

                // 内容区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeLarge)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(1.5.dp)
                        .clip(ShapeLarge)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(24.dp)
                ) {
                    // 标题
                    Text(
                        text       = title,
                        style      = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary,
                        modifier   = Modifier.padding(bottom = 16.dp)
                    )

                    // 自定义内容区域
                    content()

                    // 按钮区域
                    if (confirmButton != null || dismissButton != null) {
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            dismissButton?.invoke()
                            confirmButton?.invoke()
                        }
                    }
                }
            }
        }
    }
}

/**
 * 简单确认 Dialog（只有文字+两个按钮）
 */
@Composable
fun ConfirmDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmText: String = stringResource(R.string.btn_confirm),
    dismissText: String = stringResource(R.string.btn_cancel),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = onDismissRequest,
    dismissible: Boolean = true
) {
    SheepsDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        dismissible = dismissible,
        confirmButton = {
            PrimaryButton(
                text    = confirmText,
                onClick = onConfirm,
                height  = 40.dp,
                modifier = Modifier.widthIn(min = 80.dp)
            )
        },
        dismissButton = {
            GhostButton(
                text    = dismissText,
                onClick = onDismiss
            )
        }
    ) {
        Text(
            text     = message,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            lineHeight = 22.sp
        )
    }
}
