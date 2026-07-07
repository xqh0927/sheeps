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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.R
import com.example.sheeps.ui.components.SheepsLoading
import com.example.sheeps.ui.components.SheepsTopAppBar
import com.hjq.toast.Toaster
import kotlinx.coroutines.delay

/**
 * 全屏登录页面 — 支持验证码登录 / 密码登录双模式
 *
 * @param isLoading 是否正在加载中
 * @param onBack 返回回调
 * @param onSendCode 发送验证码回调
 * @param onLogin 验证码登录回调 (phone, code)
 * @param onPasswordLogin 密码登录回调 (phone, password)
 * @param onNavigateToRegister 跳转注册页回调
 * @param onNavigateToResetPassword 跳转找回密码页回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onSendCode: (String) -> Unit,
    onLogin: (String, String) -> Unit,
    onPasswordLogin: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToResetPassword: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: 验证码登录, 1: 密码登录
    val tabTitles = listOf(
        stringResource(R.string.tab_login_code),
        stringResource(R.string.tab_login_password)
    )

    // ----- 验证码登录状态 -----
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(0) }

    // ----- 密码登录状态 -----
    var pwPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Scaffold(
        topBar = {
            SheepsTopAppBar(title = stringResource(id = R.string.btn_login), onBack = onBack)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tab 切换
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
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

                    // 表单内容区固定最小高度，防止切换 tab 时布局跳动
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
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
                                                text = if (countdown > 0) "${countdown}s" else stringResource(
                                                    id = R.string.btn_get_otp
                                                ),
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = { onLogin(phone, code) },
                                        enabled = !isLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            stringResource(id = R.string.dialog_login_btn),
                                            color = Color.White
                                        )
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
                                        label = { Text(stringResource(R.string.label_password)) },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // 忘记密码
                                    TextButton(
                                        onClick = onNavigateToResetPassword,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text(
                                            stringResource(R.string.login_forgot_password),
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = { onPasswordLogin(pwPhone, password) },
                                        enabled = !isLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            stringResource(id = R.string.dialog_login_btn),
                                            color = Color.White
                                        )
                                    }

                                    // 底部：没有账号？立即注册
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        TextButton(onClick = onNavigateToRegister) {
                                            Text(
                                                stringResource(R.string.login_no_account),
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
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
