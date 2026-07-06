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
import com.hjq.toast.Toaster
import kotlinx.coroutines.delay
import com.example.sheeps.core.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext

/**
 * 登录对话框 — 支持验证码登录 / 密码登录双模式
 *
 * @param onDismiss 关闭对话框回调
 * @param onSendCode 发送验证码回调
 * @param onLogin 验证码登录回调 (phone, code)
 * @param onPasswordLogin 密码登录回调 (phone, password)
 * @param onRegister 注册回调 (phone, password, code)
 * @param onResetPassword 重置密码回调 (phone, code, newPassword)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onSendCode: (String) -> Unit,
    onLogin: (String, String) -> Unit,
    onPasswordLogin: (String, String) -> Unit = { _, _ -> },
    onRegister: (String, String, String) -> Unit = { _, _, _ -> },
    onResetPassword: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: 验证码登录, 1: 密码登录
    val tabTitles = listOf("验证码登录", "密码登录")

    // ----- 验证码登录状态 -----
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(0) }

    // ----- 密码登录状态 -----
    var pwPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // ----- 子对话框状态 -----
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showResetPwdDialog by remember { mutableStateOf(false) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    // ---- 注册对话框 ----
    if (showRegisterDialog) {
        RegisterDialog(
            onDismiss = { showRegisterDialog = false },
            onSendCode = { p ->
                if (p.length == 11) {
                    onSendCode(p)
                }
            },
            onRegister = { p, pw, c ->
                onRegister(p, pw, c)
                showRegisterDialog = false
            }
        )
    }

    // ---- 找回密码对话框 ----
    if (showResetPwdDialog) {
        ResetPasswordDialog(
            onDismiss = { showResetPwdDialog = false },
            onSendCode = { p ->
                if (p.length == 11) {
                    onSendCode(p)
                }
            },
            onReset = { p, c, np ->
                onResetPassword(p, c, np)
                showResetPwdDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = stringResource(id = R.string.btn_login),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Tab 切换
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (selectedTab) {
                    // ---- 验证码登录 ----
                    0 -> {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(stringResource(id = R.string.dialog_login_phone)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text(stringResource(id = R.string.dialog_login_code)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (phone.length == 11) {
                                        onSendCode(phone)
                                        countdown = 60
                                    } else {
                                        Toaster.show(context.getString(R.string.hint_phone))
                                    }
                                },
                                enabled = countdown == 0,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(
                                    text = if (countdown > 0) "${countdown}s" else stringResource(id = R.string.btn_get_otp),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // ---- 密码登录 ----
                    1 -> {
                        OutlinedTextField(
                            value = pwPhone,
                            onValueChange = { pwPhone = it },
                            label = { Text(stringResource(id = R.string.dialog_login_phone)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("密码") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 忘记密码
                        TextButton(
                            onClick = { showResetPwdDialog = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("忘记密码？", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (selectedTab) {
                        0 -> onLogin(phone, code)
                        1 -> onPasswordLogin(pwPhone, password)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(id = R.string.dialog_login_btn), color = Color.White)
            }
        },
        dismissButton = {
            Row {
                if (selectedTab == 1) {
                    TextButton(onClick = { showRegisterDialog = true }) {
                        Text("没有账号？立即注册", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.dialog_login_cancel), color = Color.Gray)
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * 注册对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterDialog(
    onDismiss: () -> Unit,
    onSendCode: (String) -> Unit,
    onRegister: (String, String, String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableStateOf(0) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("注册账号", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Serif)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it; errorMsg = null },
                    label = { Text("手机号") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it; errorMsg = null },
                    label = { Text("密码") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it; errorMsg = null },
                    label = { Text("确认密码") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code, onValueChange = { code = it },
                        label = { Text("验证码") }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (phone.length == 11) {
                                onSendCode(phone)
                                countdown = 60
                            } else {
                                errorMsg = "请输入正确的手机号"
                            }
                        },
                        enabled = countdown == 0,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp), modifier = Modifier.height(48.dp)
                    ) {
                        Text(if (countdown > 0) "${countdown}s" else "获取", color = Color.White)
                    }
                }
                Text("6-20位，至少包含字母和数字", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        password.length < 6 || password.length > 20 -> errorMsg = "密码长度需在6-20位之间"
                        !password.any { it.isDigit() } || !password.any { it.isLetter() } -> errorMsg = "密码需同时包含字母和数字"
                        password != confirmPassword -> errorMsg = "两次输入的密码不一致"
                        code.length != 6 -> errorMsg = "请输入6位验证码"
                        else -> onRegister(phone, password, code)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("注册", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Color.Gray) }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * 找回密码对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResetPasswordDialog(
    onDismiss: () -> Unit,
    onSendCode: (String) -> Unit,
    onReset: (String, String, String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableStateOf(0) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("找回密码", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Serif)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it; errorMsg = null },
                    label = { Text("手机号") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code, onValueChange = { code = it },
                        label = { Text("验证码") }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (phone.length == 11) {
                                onSendCode(phone)
                                countdown = 60
                            } else {
                                errorMsg = "请输入正确的手机号"
                            }
                        },
                        enabled = countdown == 0,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp), modifier = Modifier.height(48.dp)
                    ) {
                        Text(if (countdown > 0) "${countdown}s" else "获取", color = Color.White)
                    }
                }
                OutlinedTextField(
                    value = newPassword, onValueChange = { newPassword = it; errorMsg = null },
                    label = { Text("新密码") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        newPassword.length < 6 || newPassword.length > 20 -> errorMsg = "密码长度需在6-20位之间"
                        !newPassword.any { it.isDigit() } || !newPassword.any { it.isLetter() } -> errorMsg = "密码需同时包含字母和数字"
                        code.length != 6 -> errorMsg = "请输入6位验证码"
                        else -> onReset(phone, code, newPassword)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("确认", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Color.Gray) }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
