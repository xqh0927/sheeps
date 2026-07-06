package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AvatarPickerDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties()
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("更换头像") },
            text = {
                Column {
                    TextButton(onClick = onTakePhoto) {
                        Text("拍照")
                    }
                    TextButton(onClick = onPickFromGallery) {
                        Text("从相册选择")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        )
    }
}
