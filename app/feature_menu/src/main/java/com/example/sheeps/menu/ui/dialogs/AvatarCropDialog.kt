package com.example.sheeps.menu.ui.dialogs

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImageView
import com.example.sheeps.core.R

/**
 * 全屏头像裁剪弹窗。
 * 使用 CropImageView（最新推荐 API）实现正方形裁剪。
 *
 * 触发来源：由 [AvatarPickerDialog] 选定图片（拍照/相册）后弹出，
 * 裁剪结果由 [onCropComplete] 回传 [Bitmap]，交由上层上传并更新头像。
 *
 * 线程约束：CropImageView 内部图片解码在后台线程执行，
 * `setImageUriAsync(imageUri)` 异步加载，不阻塞主线程（UI 线程）；
 * [onCropComplete] 回调的 [Bitmap] 在主线程产生。
 *
 * ⚠️ 内存隐患 1：传入的 [imageUri] 指向的图片文件由上层（拍照/相册）创建，
 * 本对话框仅读取不删除，其生命周期由上层负责回收，避免临时文件长期滞留。
 * ⚠️ 内存隐患 2：[onCropComplete] 返回的 [Bitmap] 若不及时回收/上传即释放，
 * 大图易造成内存峰值；确认后应尽快上传并在上层置空引用。
 * ⚠️ 内存隐患 3：`cropImageView` 通过 `remember { CropImageView(context) }` 持有，
 * 包含原生 View 与已加载的 Bitmap；Dialog 关闭后组合销毁会释放引用，
 * 但建议在确认/取消后主动释放（如不再复用），避免长时间持有大图。
 *
 * @param imageUri 待裁剪图片的 Uri。
 * @param onCropComplete 裁剪完成回调，返回裁剪后的 Bitmap。
 * @param onDismiss 取消裁剪。
 */
@Composable
fun AvatarCropDialog(
    imageUri: Uri,
    onCropComplete: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cropImageView = remember { CropImageView(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部操作栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel), color = Color.White, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        val cropped = cropImageView.croppedImage
                        if (cropped != null) {
                            onCropComplete(cropped)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.btn_confirm), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // CropImageView 主体
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = {
                    cropImageView.apply {
                        // 固定正方形 1:1 裁剪
                        setAspectRatio(1, 1)
                        setFixedAspectRatio(true)
                        guidelines = CropImageView.Guidelines.ON
                    }
                }
            ) { view ->
                view.setImageUriAsync(imageUri)
            }
        }
    }
}
