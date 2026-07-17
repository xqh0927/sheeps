package com.example.sheeps.menu.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.sheeps.ui.R
import com.example.sheeps.lib_base.base.BaseActivity
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.local.UserProfileEntity
import com.example.sheeps.data.model.RenameRequest
import com.example.sheeps.data.model.ResetPasswordRequest
import com.example.sheeps.data.model.SendCodeRequest
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.menu.ui.dialogs.AvatarCropDialog
import com.example.sheeps.menu.ui.screens.UserInfoScreen
import com.example.sheeps.ui.theme.SheepsTheme
import com.hjq.toast.Toaster
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import top.zibin.luban.api.compressTo
import java.io.File
import javax.inject.Inject

/**
 * 全屏用户信息 Activity。
 * 支持头像更换（相机/相册 → Luban 压缩 → R2 上传）、昵称修改、密码修改。
 */
@Route(path = "/menu/userinfo")
@AndroidEntryPoint
class UserInfoActivity : BaseActivity() {

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var prefs: UserPreferences

    @Inject
    lateinit var localDao: LocalDao

    override fun initView(savedInstanceState: Bundle?) {
        setContent {
            SheepsTheme {
                BackHandler { finish() }
                var isLoading by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                val context = this

                // 构建响应式 MenuViewState，修改后即时更新 UI
                var userInfoState by remember {
                    mutableStateOf(
                        MenuViewState(
                            username = prefs.getUsername(),
                            avatarUrl = prefs.getAvatarUrl(),
                            phone = prefs.getPhone() ?: ""
                        )
                    )
                }

                // 待裁剪的图片 Uri（必须在 pickLauncher 之前声明）
                var cropUri by remember { mutableStateOf<android.net.Uri?>(null) }

                // 步骤1：选择图片（图库）
                val pickLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let { selectedUri ->
                        cropUri = selectedUri
                    }
                }

                UserInfoScreen(
                    state = userInfoState,
                    isLoading = isLoading,
                    onBack = { finish() },
                    onChangeAvatar = { pickLauncher.launch("image/*") },
                    onUpdateNickname = { nickname ->
                        handleUpdateNickname(nickname, scope, { isLoading = it }) { name ->
                            userInfoState = userInfoState.copy(username = name)
                        }
                    },
                    onSendCode = { phone -> handleSendCode(phone, scope) },
                    onChangePassword = { code, newPassword ->
                        handleChangePassword(code, newPassword, scope) { isLoading = it }
                    }
                )

                // 步骤2：头像裁剪（CropImageView — 最新 API）
                cropUri?.let { uri ->
                    AvatarCropDialog(
                        imageUri = uri,
                        onCropComplete = { bitmap ->
                            cropUri = null
                            scope.launch(Dispatchers.IO) {
                                try {
                                    // 将 Bitmap 保存为临时文件再压缩
                                    val tmpFile = File(
                                        context.cacheDir,
                                        "avatar_crop_${System.currentTimeMillis()}.jpg"
                                    )
                                    tmpFile.outputStream().use { fos ->
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                                    }
                                    val file = Uri.fromFile(tmpFile).compressTo(context).getOrNull()
                                    if (file != null && file.exists() && file.canRead()) {
                                        val bytes = file.readBytes()
                                        withContext(Dispatchers.Main) {
                                            uploadAvatar(bytes, file.name, scope) { url ->
                                                userInfoState = userInfoState.copy(avatarUrl = url)
                                            }
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toaster.show(context.getString(R.string.toast_image_process_failed))
                                        }
                                    }
                                    tmpFile.delete()
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toaster.show(context.getString(R.string.toast_image_process_failed))
                                    }
                                }
                            }
                        },
                        onDismiss = { cropUri = null }
                    )
                }
            }
        }
    }

    override fun initData() {}

    // -------- 业务逻辑 --------

    private fun handleSendCode(phone: String, scope: CoroutineScope) {
        if (phone.length != 11) {
            Toaster.show(getString(R.string.err_invalid_phone))
            return
        }
        scope.launch {
            try {
                val response = apiService.sendCode(SendCodeRequest(phone))
                if (response.success) {
                    Toaster.show(getString(R.string.toast_send_code_success, response.code))
                } else {
                    Toaster.show(getString(R.string.toast_send_code_failed))
                }
            } catch (e: Exception) {
                Toaster.show(getString(R.string.toast_send_code_network_error))
            }
        }
    }

    /**
     * 上传头像：Luban 压缩后的字节数组以 multipart/form-data 上传至 R2
     */
    private fun uploadAvatar(
        imageBytes: ByteArray,
        fileName: String,
        scope: kotlinx.coroutines.CoroutineScope,
        onSuccess: (String) -> Unit
    ) {
        scope.launch {
            try {
                val authHeader = "Bearer ${prefs.getToken()}"
                val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("avatar", fileName, requestBody)
                val uploadResponse = apiService.uploadAvatar(authHeader, part)
                val url = uploadResponse.avatarUrl ?: ""
                prefs.setAvatarUrl(url)
                onSuccess(url)
                Toaster.show(getString(R.string.toast_avatar_update_success))
            } catch (e: Exception) {
                Toaster.show(getString(R.string.toast_avatar_upload_failed))
            }
        }
    }

    private fun handleUpdateNickname(
        nickname: String,
        scope: kotlinx.coroutines.CoroutineScope,
        setLoading: (Boolean) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        setLoading(true)
        scope.launch {
            try {
                apiService.rename(RenameRequest(nickname))
                prefs.setUsername(nickname)
                // 同步更新本地 DB 中的 profile，MenuViewModel observer 立即感知变更
                val userId = prefs.getUserId()
                localDao.deleteProfile()
                localDao.insertProfile(
                    UserProfileEntity(
                        userId = userId,
                        username = nickname,
                        points = prefs.getPoints(),
                        isDirty = false,
                        updateTimestamp = System.currentTimeMillis()
                    )
                )
                onSuccess(nickname)
                Toaster.show(getString(R.string.toast_nickname_update_success))
            } catch (e: Exception) {
                Toaster.show(getString(R.string.toast_nickname_update_failed))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun handleChangePassword(
        code: String,
        newPassword: String,
        scope: kotlinx.coroutines.CoroutineScope,
        setLoading: (Boolean) -> Unit
    ) {
        setLoading(true)
        scope.launch {
            try {
                val phone = prefs.getPhone() ?: ""
                val response = apiService.resetPassword(
                    ResetPasswordRequest(phone, code, newPassword)
                )
                if (response.success) {
                    // 清空本地登录态
                    prefs.logout()
                    Toaster.show(getString(R.string.toast_password_change_success))
                    // 跳转到登录页
                    com.therouter.TheRouter.build("/auth/login").navigation(this@UserInfoActivity)
                    finish()
                } else {
                    Toaster.show(getString(R.string.toast_password_change_failed))
                    setLoading(false)
                }
            } catch (e: Exception) {
                Toaster.show(getString(R.string.toast_password_change_failed))
                setLoading(false)
            }
        }
    }
}
