package com.example.sheeps.menu.ui.dialogs

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.canhub.cropper.CropImageView
import com.example.sheeps.ui.R

@Composable
fun AvatarCropDialog(imageUri: Uri, onCropComplete: (Bitmap) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val cropView = remember(context, imageUri) { CropImageView(context) }
    val colors = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(colors.surface).safeDrawingPadding()) {
            Text(stringResource(R.string.dialog_title_change_avatar), Modifier.padding(18.dp),
                color = colors.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            key(cropView) {
                AndroidView(modifier = Modifier.fillMaxWidth().weight(1f).background(colors.scrim), factory = {
                    cropView.apply {
                        setAspectRatio(1, 1)
                        setFixedAspectRatio(true)
                        guidelines = CropImageView.Guidelines.ON
                        setImageUriAsync(imageUri)
                    }
                })
            }
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onDismiss) { Text(stringResource(R.string.btn_cancel)) }
                Button(onClick = { cropView.croppedImage?.let(onCropComplete) }) { Text(stringResource(R.string.btn_confirm)) }
            }
        }
    }
}
