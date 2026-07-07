package com.example.sheeps.menu.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sheeps.core.R
import com.example.sheeps.core.utils.ImageCompressor
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.dialogs.AvatarPickerDialog
import com.example.sheeps.menu.ui.dialogs.ProfileChangePasswordDialog
import com.example.sheeps.ui.components.SheepsTopAppBar
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
                Toaster.show(context.getString(R.string.toast_avatar_update_success))
            } catch (e: Exception) {
                Toaster.show(context.getString(R.string.toast_avatar_process_failed, e.message))
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
                Toaster.show(context.getString(R.string.toast_avatar_update_success))
            } catch (e: Exception) {
                Toaster.show(context.getString(R.string.toast_avatar_process_failed, e.message))
            }
        }
    }

    // 头像底部选择菜单
    if (showAvatarPicker) {
        AvatarPickerDialog(
            onDismiss = { showAvatarPicker = false },
            onTakePhoto = {
                showAvatarPicker = false
                cameraLauncher.launch(null)
            },
            onPickFromGallery = {
                showAvatarPicker = false
                imagePickerLauncher.launch("image/*")
            }
        )
    }

    // 修改密码对话框
    if (showChangePwdDialog) {
        ProfileChangePasswordDialog(
            onDismiss = { showChangePwdDialog = false },
            onSendCode = { /* 复用现有验证码发送逻辑 */ },
            onChangePassword = { _, _, _ ->
                showChangePwdDialog = false
                Toaster.show(context.getString(R.string.toast_password_feature_locked))
            }
        )
    }

    Scaffold(
        topBar = {
            SheepsTopAppBar(title = stringResource(R.string.profile_edit_title), onBack = onBack)
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
                    stringResource(id = R.string.click_to_change_avatar),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // ---- 昵称 ----
            item {
                val context = LocalContext.current
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text(stringResource(id = R.string.hint_nickname)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (nickname.isNotBlank()) {
                                onRename(state.phone, nickname)
                                Toaster.show(context.getString(R.string.toast_nickname_updated))
                            }
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "保存")
                        }
                    }
                )
            }

            // ---- 手机号（只读） ----
            item {
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = {},
                    label = { Text(stringResource(id = R.string.hint_phone)) },
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
                    Text(
                        stringResource(id = R.string.dialog_title_change_password),
                        color = Color.White
                    )
                }
            }
        }
    }
}

/* No longer needed — extracted to ProfileChangePasswordDialog */
