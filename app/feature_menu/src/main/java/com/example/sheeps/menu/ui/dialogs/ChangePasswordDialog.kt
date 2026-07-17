package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.core.R
import kotlinx.coroutines.delay

/**
 * 修改密码对话框（基于 Material [AlertDialog]）。
 *
 * 通过短信验证码 + 新密码完成密码修改：用户输入验证码、新密码与确认密码，
 * 点击"获取验证码"触发 [onSendCode]，点击"确认"经本地校验后触发 [onChangePassword]。
 *
 * 触发来源：个人中心（ProfileScreen）点击"修改密码"弹出。
 * 确认后：由 [onChangePassword] 回传 (验证码, 新密码)，由上层调用 ViewModel 提交修改。
 *
 * 线程约束：本对话框所有状态变更发生在主线程（UI 线程）；[onSendCode]/[onChangePassword]
 * 由上层负责切到 IO 线程发起网络请求。
 *
 * ⚠️ 内存隐患：下方的 [LaunchedEffect] 倒计时协程由组合作用域（Composition）持有，
 * 随 Dialog 关闭自动取消，不会泄漏；但若后续改为 `rememberCoroutineScope().launch { ... }`
 * 的全局作用域，则需在 onDispose 中手动取消，否则 Dialog 关闭后延迟任务仍会执行。
 *
 * @param currentPhone 当前登录手机号，仅用于展示账号前缀，不可编辑。
 * @param onDismiss 关闭对话框的回调（取消按钮或点击外部触发）。
 * @param onSendCode 点击"获取验证码"的回调，参数为当前手机号 [currentPhone]。
 * @param onChangePassword 点击"确认"且本地校验通过后的回调，参数为 (验证码, 新密码)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(
    currentPhone: String,
    onDismiss: () -> Unit,
    onSendCode: (String) -> Unit,
    onChangePassword: (String, String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val pwdErrLength = stringResource(id = R.string.pwd_err_length)
    val pwdErrNoAlphaDigit = stringResource(id = R.string.pwd_err_no_alpha_digit)
    val pwdErrNotMatch = stringResource(id = R.string.pwd_err_not_match)
    val pwdErrCodeLength = stringResource(id = R.string.pwd_err_code_length)

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties()
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    stringResource(id = R.string.dialog_title_change_password),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Serif
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(id = R.string.current_account_prefix, currentPhone),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it; errorMsg = null },
                            label = { Text(stringResource(id = R.string.hint_code)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSendCode(currentPhone)
                                countdown = 60
                            },
                            enabled = countdown == 0,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = if (countdown > 0) "${countdown}s" else stringResource(id = R.string.btn_get_code),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorMsg = null },
                        label = { Text(stringResource(id = R.string.hint_new_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMsg = null },
                        label = { Text(stringResource(id = R.string.hint_confirm_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(id = R.string.pwd_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    if (errorMsg != null) {
                        Text(
                            errorMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            newPassword.length < 6 || newPassword.length > 20 -> errorMsg = pwdErrLength
                            !newPassword.any { it.isDigit() } || !newPassword.any { it.isLetter() } -> errorMsg = pwdErrNoAlphaDigit
                            newPassword != confirmPassword -> errorMsg = pwdErrNotMatch
                            code.length != 6 -> errorMsg = pwdErrCodeLength
                            else -> onChangePassword(code, newPassword)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(id = R.string.btn_confirm), color = Color.White)
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
