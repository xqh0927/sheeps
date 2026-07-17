package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.core.R
import com.example.sheeps.data.model.Notice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 公告详情对话框（基于 Material [AlertDialog]）。
 *
 * 展示单条公告的标题、发布时间（由 [Notice.created_at] 格式化）与正文内容。
 * 点击"关闭"按钮关闭（经由 [onDismiss]）。
 *
 * 触发来源：首页/菜单（MenuScreen）公告列表点击某条公告时弹出。
 * 本对话框为纯只读展示，不回写 ViewModel。
 *
 * 线程约束：[notice] 已在外部加载完成并整体传入，内部不发起网络/IO 请求；
 * 时间格式化使用 [SimpleDateFormat] 在主线程（UI 线程）即时完成（数据量极小，无性能风险）；
 * [onDismiss] 在主线程回调。
 *
 * @param notice 待展示的公告数据（[Notice]），含 title、created_at、content。
 * @param onDismiss 关闭对话框的回调（关闭按钮或点击外部/返回键触发）。
 */
@Composable
fun NoticeDetailDialog(
    notice: Notice,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = notice.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Serif
                )
            },
            text = {
                Column {
                    Text(
                        text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(notice.created_at)),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = notice.content,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color.DarkGray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        stringResource(R.string.dialog_prepare_close),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}
