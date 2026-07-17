package com.example.sheeps.game.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.core.R

/**
 * 退出确认对话框（对决模式）。
 *
 * 以 `Dialog` + 内部 `AlertDialog` 呈现，标题/正文来自字符串资源，提供
 * “确认离开”与“取消”两个按钮，分别对应 [onConfirm] / [onDismiss]。
 *
 * 线程约束：纯 Composable，运行于主线程；`Dialog` 的 `onDismissRequest`
 * 同时绑定外层 [onDismiss]，允许通过系统返回/点按外部关闭。
 * ⚠️ 内存隐患：弹窗由 `DuelScreen` 在 `showExitConfirmDialog` 为 true 时组合，
 * 关闭后随即从组合树移除并释放，无静态引用或回调悬挂。
 *
 * @param onConfirm 用户确认离开对决时的回调
 * @param onDismiss 取消关闭弹窗的回调
 */
@Composable
fun DuelExitDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(id = R.string.dialog_duel_exit_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.dialog_duel_exit_text)) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(id = R.string.dialog_duel_exit_confirm), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.dialog_duel_exit_cancel))
                }
            }
        )
    }
}
