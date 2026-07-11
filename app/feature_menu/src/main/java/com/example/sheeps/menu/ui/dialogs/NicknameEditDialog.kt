package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.core.R

/**
 * 昵称编辑对话框（基于 Material [AlertDialog]）。
 *
 * 通过单行输入框编辑用户昵称，点击"保存"对非空昵称做 trim 后触发 [onSave]。
 *
 * 触发来源：个人中心（ProfileScreen）点击昵称区域弹出。
 * 确认后：由 [onSave] 回传 trim 后的昵称，由上层调用 ViewModel 提交并刷新 UI。
 *
 * 线程约束：昵称状态（[nickname]）由 `remember { mutableStateOf }` 持有于组合作用域，
 * Dialog 关闭后自动释放；[onSave]/[onDismiss] 在主线程（UI 线程）回调，
 * 网络提交由上层负责切到 IO 线程。
 *
 * @param currentNickname 当前昵称，作为输入框初始值。
 * @param onDismiss 关闭对话框的回调（取消按钮或点击外部触发）。
 * @param onSave 点击"保存"且昵称非空时的回调，参数为 trim 后的昵称。
 */
@Composable
fun NicknameEditDialog(
    currentNickname: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nickname by remember { mutableStateOf(currentNickname) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties()
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    stringResource(id = R.string.dialog_title_change_nickname),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Serif
                )
            },
            text = {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text(stringResource(id = R.string.hint_nickname)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nickname.isNotBlank()) {
                            onSave(nickname.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(id = R.string.btn_save), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.btn_cancel), color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
