package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sheeps.core.R
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.dialogs.ChangePasswordDialog
import com.example.sheeps.menu.ui.dialogs.NicknameEditDialog
import com.example.sheeps.ui.components.SheepsLoading
import com.example.sheeps.ui.components.SheepsTopAppBar

/**
 * 全屏用户信息页面
 *
 * @param state 界面状态数据
 * @param isLoading 是否正在加载
 * @param onBack 返回回调
 * @param onChangeAvatar 触发图片选择器回调
 * @param onUpdateNickname 更新昵称回调
 * @param onSendCode 发送验证码回调（修改密码用）
 * @param onChangePassword 修改密码回调 (code, newPassword)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    state: MenuViewState,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onChangeAvatar: () -> Unit,
    onUpdateNickname: (String) -> Unit,
    onSendCode: (String) -> Unit,
    onChangePassword: (String, String) -> Unit
) {
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showChangePwdDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SheepsTopAppBar(title = stringResource(R.string.profile_title), onBack = onBack)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 昵称修改弹窗
            if (showNicknameDialog) {
                NicknameEditDialog(
                    currentNickname = state.username,
                    onDismiss = { showNicknameDialog = false },
                    onSave = { newName ->
                        onUpdateNickname(newName)
                        showNicknameDialog = false
                    }
                )
            }

            // 修改密码弹窗
            if (showChangePwdDialog) {
                ChangePasswordDialog(
                    currentPhone = state.phone,
                    onDismiss = { showChangePwdDialog = false },
                    onSendCode = onSendCode,
                    onChangePassword = { code, newPassword ->
                        onChangePassword(code, newPassword)
                        showChangePwdDialog = false
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 24.dp)
            ) {
                // ---- 头像区域 ----
                item {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0))
                            .clickable { onChangeAvatar() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = state.avatarUrl,
                                contentDescription = stringResource(R.string.cd_avatar),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.cd_upload_avatar),
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.click_to_change_avatar),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // ---- 昵称（点击弹窗修改） ----
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNicknameDialog = true }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.label_nickname),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                state.username.ifBlank { stringResource(R.string.label_not_set) },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.cd_edit_nickname),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }

                // ---- 手机号（只读） ----
                item {
                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.label_phone)) },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---- 修改密码 ----
                item {
                    Button(
                        onClick = { showChangePwdDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.dialog_title_change_password), color = Color.White)
                    }
                }
            }

            // 加载遮罩
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
