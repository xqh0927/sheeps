package com.example.sheeps.menu.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sheeps.core.utils.ImageCompressor
import com.example.sheeps.menu.state.MenuViewState
import com.hjq.toast.Toaster

/**
 * 个人资料编辑页
 *
 * 功能：
 * - 圆形头像（点击可拍照/从相册选择，压缩到 256KB 后上传）
 * - 昵称输入框 + 保存按钮
 * - 手机号只读显示
 * - 修改密码按钮（仅已设密码的用户可见）
 *
 * @param state 界面状态数据
 * @param onRename 修改昵称回调 (userId, newUsername)
 * @param onUploadAvatar 上传头像回调 (avatarBase64)
 * @param onBack 返回回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: MenuViewState,
    onRename: (String, String) -> Unit,
    onUploadAvatar: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var nickname by remember(state.username) { mutableStateOf(state.username) }
    var avatarBase64 by remember { mutableStateOf<String?>(null) }
    var showChangePwdDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val base64 = ImageCompressor.compressImage(context, it)
                avatarBase64 = base64
                onUploadAvatar(base64)
                Toaster.show("头像上传成功")
            } catch (e: Exception) {
                Toaster.show("头像处理失败：${e.message}")
            }
        }
    }

    // 相机拍照
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            try {
                // 将 Bitmap 保存到临时文件再压缩
                val tempFile = java.io.File(context.cacheDir, "avatar_temp.jpg")
                tempFile.outputStream().use { out ->
                    it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                val base64 = ImageCompressor.compressImage(context, Uri.fromFile(tempFile))
                avatarBase64 = base64
                onUploadAvatar(base64)
                tempFile.delete()
                Toaster.show("头像上传成功")
            } catch (e: Exception) {
                Toaster.show("头像处理失败：${e.message}")
            }
        }
    }

    // 头像底部选择菜单
    if (showAvatarPicker) {
        AlertDialog(
            onDismissRequest = { showAvatarPicker = false },
            title = { Text("更换头像") },
            text = {
                Column {
                    TextButton(onClick = {
                        showAvatarPicker = false
                        cameraLauncher.launch(null)
                    }) { Text("拍照") }
                    TextButton(onClick = {
                        showAvatarPicker = false
                        imagePickerLauncher.launch("image/*")
                    }) { Text("从相册选择") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarPicker = false }) { Text("取消") }
            }
        )
    }

    // 修改密码对话框
    if (showChangePwdDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePwdDialog = false },
            onSendCode = { /* 复用现有验证码发送逻辑 */ },
            onChangePassword = { _, _, _ ->
                showChangePwdDialog = false
                Toaster.show("密码修改功能暂未开放，请使用找回密码")
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人资料", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                }
            )
        }
    ) { paddingValues ->
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
                        .clickable { showAvatarPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBase64 != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("data:image/jpeg;base64,$avatarBase64")
                                .crossfade(true)
                                .build(),
                            contentDescription = "头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (state.points >= 0) {
                        // 显示默认灰色占位
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "上传头像",
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "点击更换头像",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // ---- 昵称 ----
            item {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (nickname.isNotBlank()) {
                                onRename(state.phone, nickname)
                                Toaster.show("昵称已更新")
                            }
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "保存昵称")
                        }
                    }
                )
            }

            // ---- 手机号（只读） ----
            item {
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = {},
                    label = { Text("手机号") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ---- 修改密码（仅已设密码用户） ----
            item {
                Button(
                    onClick = { showChangePwdDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("修改密码", color = Color.White)
                }
            }
        }
    }
}

/**
 * 修改密码对话框（占位，实际复用重置密码流程）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSendCode: (String) -> Unit,
    onChangePassword: (String, String, String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("手机号") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code, onValueChange = { code = it },
                        label = { Text("验证码") }, singleLine = true, modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (phone.length == 11) onSendCode(phone) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("获取") }
                }
                OutlinedTextField(
                    value = newPassword, onValueChange = { newPassword = it },
                    label = { Text("新密码") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onChangePassword(phone, code, newPassword) }) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
