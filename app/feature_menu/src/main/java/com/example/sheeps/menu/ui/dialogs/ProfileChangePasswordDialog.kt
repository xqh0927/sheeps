package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.ui.R

/**
 * 个人资料页"修改密码"对话框（基于 Material [AlertDialog]）。
 *
 * 与 [ChangePasswordDialog] 类似，但手机号由用户自行输入（而非传入），
 * 通过"手机号 + 验证码 + 新密码"完成修改。点击"获取验证码"触发 [onSendCode]，
 * 点击"确认"触发 [onChangePassword]。
 *
 * 触发来源：个人中心（ProfileScreen）点击"修改密码"弹出（未绑定/需手动输入手机号场景）。
 * 确认后：由 [onChangePassword] 回传 (手机号, 验证码, 新密码)，由上层调用 ViewModel 提交修改。
 *
 * 线程约束：所有状态变更在主线程（UI 线程）；[onSendCode]/[onChangePassword]
 * 由上层负责切到 IO 线程发起网络请求。
 *
 * @param onDismiss 关闭对话框的回调（取消按钮或点击外部/返回键触发）。
 * @param onSendCode 点击"获取验证码"的回调，参数为用户输入的手机号（需长度为 11 才发送）。
 * @param onChangePassword 点击"确认"的回调，参数为 (手机号, 验证码, 新密码)；本对话框不做格式校验。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileChangePasswordDialog(
    onDismiss: () -> Unit,
    onSendCode: (String) -> Unit,
    onChangePassword: (String, String, String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(id = R.string.dialog_title_change_password)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text(stringResource(id = R.string.hint_phone)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = code, onValueChange = { code = it },
                            label = { Text(stringResource(id = R.string.hint_code)) }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { if (phone.length == 11) onSendCode(phone) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text(stringResource(id = R.string.btn_get_code)) }
                    }
                    OutlinedTextField(
                        value = newPassword, onValueChange = { newPassword = it },
                        label = { Text(stringResource(id = R.string.hint_new_password)) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { onChangePassword(phone, code, newPassword) }) { Text(stringResource(id = R.string.btn_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.btn_cancel)) }
            }
        )
    }
}
