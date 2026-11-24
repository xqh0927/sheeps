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
import com.example.sheeps.ui.R
import com.example.sheeps.ui.theme.*

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
        val visible = remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(androidx.compose.animation.core.tween(160)) +
                slideInVertically(androidx.compose.animation.core.tween(240)) { it / 10 }
        ) {
            Surface(
                modifier = modifier.widthIn(max = 480.dp).fillMaxWidth(.92f),
                shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(14.dp))
                    content()
                    if (confirmButton != null || dismissButton != null) {
                        Spacer(Modifier.height(22.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically) {
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
