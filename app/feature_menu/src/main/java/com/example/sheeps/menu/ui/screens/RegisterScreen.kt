package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.sheeps.ui.components.SheepsLoading
import kotlinx.coroutines.delay

/**
 * 全屏注册页面
 *
 * @param isLoading 是否正在加载中
 * @param onBack 返回登录页回调
 * @param onSendCode 发送验证码回调
 * @param onRegister 注册回调 (phone, password, code)
 * @param onRegisterSuccess 注册成功后回调，回到登录页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onSendCode: (String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onRegisterSuccess: () -> Unit
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "注册账号",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Serif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; errorMsg = null },
                        label = { Text("手机号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        label = { Text("密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMsg = null },
                        label = { Text("确认密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("验证码") },
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
                                    errorMsg = "请输入正确的手机号"
                                }
                            },
                            enabled = countdown == 0,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = if (countdown > 0) "${countdown}s" else "获取",
                                color = Color.White
                            )
                        }
                    }

                    // 密码规则提示
                    Text(
                        "6-20位，至少包含字母和数字",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    // 错误提示区
                    if (errorMsg != null) {
                        Text(
                            errorMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 注册按钮
                    Button(
                        onClick = {
                            when {
                                password.length < 6 || password.length > 20 -> errorMsg =
                                    "密码长度需在6-20位之间"

                                !password.any { it.isDigit() } || !password.any { it.isLetter() } -> errorMsg =
                                    "密码需同时包含字母和数字"

                                password != confirmPassword -> errorMsg = "两次输入的密码不一致"
                                code.length != 6 -> errorMsg = "请输入6位验证码"
                                else -> {
                                    onRegister(phone, password, code)
                                    onRegisterSuccess()
                                }
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("注册", color = Color.White)
                    }

                    // 已有账号？去登录
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = onBack) {
                            Text(
                                "已有账号？去登录",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                }
            }

            // 加载遮罩（覆盖整个页面，含 TopAppBar）
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center
                    ) {
                        SheepsLoading(size = 44.dp)
                    }
                }
            }
        }
    }
}
