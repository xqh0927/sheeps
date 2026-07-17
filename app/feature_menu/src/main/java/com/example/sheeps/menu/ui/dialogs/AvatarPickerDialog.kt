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

/**
 * 头像来源选择对话框（基于 Material [AlertDialog]）。
 *
 * 提供"拍照"与"从相册选择"两个入口，供用户挑选头像来源。
 * 选择后由上层负责跳转系统相机/相册，并通常将结果交给 [AvatarCropDialog] 裁剪。
 *
 * 触发来源：个人中心（ProfileScreen）点击头像时弹出。
 * 确认后：经 [onTakePhoto]/[onPickFromGallery] 回传意图，由上层启动对应系统 UI，
 * 不直接修改头像数据。
 *
 * 线程约束：三个回调均为主线程（UI 线程）调用；本对话框不持有任何图片/Uri 资源。
 *
 * @param onDismiss 关闭对话框的回调（取消按钮或点击外部触发）。
 * @param onTakePhoto 用户选择"拍照"的回调。
 * @param onPickFromGallery 用户选择"从相册选择"的回调。
 */
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
