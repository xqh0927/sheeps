package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.core.R

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
            title = { Text(stringResource(id = R.string.dialog_title_change_avatar)) },
            text = {
                Column {
                    TextButton(onClick = onTakePhoto) {
                        Text(stringResource(id = R.string.btn_take_photo))
                    }
                    TextButton(onClick = onPickFromGallery) {
                        Text(stringResource(id = R.string.btn_choose_gallery))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.btn_cancel)) }
            }
        )
    }
}
