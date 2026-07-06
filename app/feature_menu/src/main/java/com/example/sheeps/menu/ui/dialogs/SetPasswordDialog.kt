package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.core.R
import androidx.compose.ui.res.stringResource

/**
 * 强制设密对话框
 *
 * 登录后若用户尚未设置密码，弹出此对话框引导设置。
 * 不可关闭/不可跳过，设置密码后奖励 50 积分。
 *
 * @param onSetPassword 确认设置密码回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetPasswordDialog(
    onSetPassword: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { /* 不可关闭 */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AlertDialog(
            onDismissRequest = { /* 不可关闭 */ },
            title = {
                Text(
                    text = "\uD83D\uDD10 设置登录密码",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Serif
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "为保障账号安全，请设置密码（后续可用密码快捷登录）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMsg = null
                    },
                    label = { Text("新密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = errorMsg != null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMsg = null
                    },
                    label = { Text("确认密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = errorMsg != null,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "6-20位，至少包含字母和数字",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
            confirmButton = {
                Button(
                onClick = {
                    // 密码校验
                    when {
                        password.length < 6 || password.length > 20 -> {
                            errorMsg = "密码长度需在6-20位之间"
                        }
                        !password.any { it.isDigit() } || !password.any { it.isLetter() } -> {
                            errorMsg = "密码需同时包含字母和数字"
                        }
                        password != confirmPassword -> {
                            errorMsg = "两次输入的密码不一致"
                        }
                        else -> {
                            onSetPassword(password)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("确认设置（完成后奖励 50 积分）", color = Color.White)
            }
        },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
