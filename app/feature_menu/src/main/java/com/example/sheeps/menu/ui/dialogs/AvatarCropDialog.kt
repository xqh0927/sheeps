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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImageView

/**
 * 全屏头像裁剪弹窗
 * 使用 CropImageView（最新推荐 API）实现正方形裁剪
 *
 * @param imageUri 待裁剪图片的 Uri
 * @param onCropComplete 裁剪完成回调，返回 Bitmap
 * @param onDismiss 取消裁剪
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
                    Text("取消", color = Color.White, fontSize = 16.sp)
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
                    Text("确定", color = Color.White, fontWeight = FontWeight.Bold)
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
