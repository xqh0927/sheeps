package com.example.sheeps.menu.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.core.update.UpdateDownloadManager
import com.example.sheeps.core.update.UpdateStatus
import com.example.sheeps.data.model.AppUpdateResponse
import androidx.compose.ui.res.stringResource
import com.example.sheeps.core.R

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateResponse,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val downloadManager = remember { UpdateDownloadManager() }
    val downloadState by downloadManager.state.collectAsState()
    var showProgress by remember { mutableStateOf(false) }

    // Android 8+ 首次安装需授权"未知来源"。
    // 用 remembered 标记是否需在 onResume 时自动重试安装。
    val needsRetryInstall = remember { mutableStateOf(false) }
    if (needsRetryInstall.value &&
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
        context.packageManager.canRequestPackageInstalls()
    ) {
        needsRetryInstall.value = false
        LaunchedEffect(Unit) { downloadManager.tryInstallApk(context) }
    }
    DisposableEffect(downloadState.status) {
        if (downloadState.status == UpdateStatus.NeedPermission) {
            // 跳转到系统设置后，下一次 recompose 时检测权限
            needsRetryInstall.value = true
        }
        onDispose {}
    }

    // 对话框关闭时：取消下载；仅在没有启动安装流程时清理 APK
    DisposableEffect(Unit) {
        onDispose {
            val current = downloadManager.state.value
            // 已完成/已授权 = APK 已交由安装器，不删；其余情况清理
            if (current.status != UpdateStatus.Completed &&
                current.status != UpdateStatus.NeedPermission
            ) {
                downloadManager.cleanup()
            } else {
                downloadManager.cancel() // 仅取消下载 job，保留文件
            }
        }
    }
    }

    Dialog(
        onDismissRequest = {
            if (!updateInfo.force_update) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.force_update,
            dismissOnClickOutside = !updateInfo.force_update
        )
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 图标区
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Upgrade",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 标题
                Text(
                    text = stringResource(id = R.string.update_discovered, updateInfo.version_name ?: ""),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Serif
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 更新日志卡片
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.update_log_title),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = updateInfo.update_log ?: stringResource(id = R.string.update_default_log),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                }

                // 下载进度区
                AnimatedVisibility(
                    visible = showProgress,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 进度条
                        LinearProgressIndicator(
                            progress = { downloadState.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // 进度文字
                        Text(
                            text = when (downloadState.status) {
                                UpdateStatus.Downloading -> "下载中 ${downloadState.progress}%"
                                UpdateStatus.Completed -> "下载完成，准备安装..."
                                UpdateStatus.NeedPermission -> "请授权\"允许安装未知应用\"后点击安装"
                                UpdateStatus.Error -> downloadState.error ?: "下载失败"
                                else -> ""
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (downloadState.status) {
                                UpdateStatus.Completed -> MaterialTheme.colorScheme.primary
                                UpdateStatus.NeedPermission -> Color(0xFFFF9800)
                                UpdateStatus.Error -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        // 错误时显示重试，需要权限时显示安装按钮
                        when (downloadState.status) {
                            UpdateStatus.Error -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = {
                                    downloadManager.downloadAndInstall(context, updateInfo.apk_url ?: "")
                                }) {
                                    Text("重新下载", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            UpdateStatus.NeedPermission -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { downloadManager.tryInstallApk(context) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("安装更新", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            else -> {}
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (showProgress) 12.dp else 24.dp))

                // 底部按钮区
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 非强制更新时显示"以后再说"
                    if (!updateInfo.force_update && downloadState.status != UpdateStatus.Downloading) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(id = R.string.update_btn_later), fontSize = 14.sp)
                        }
                    }

                    // 主按钮：下载 / 下载中(禁用) / 已完成 / 需授权重试
                    val btnEnabled = downloadState.status != UpdateStatus.Downloading
                    Button(
                        onClick = {
                            when {
                                downloadState.status == UpdateStatus.NeedPermission -> {
                                    downloadManager.tryInstallApk(context)
                                }
                                downloadState.status == UpdateStatus.Completed -> {
                                    // 已完成但未安装（可能用户没注意到）
                                }
                                else -> {
                                    val url = updateInfo.apk_url
                                    if (!url.isNullOrEmpty() && !showProgress) {
                                        showProgress = true
                                        downloadManager.downloadAndInstall(context, url)
                                    }
                                }
                            }
                        },
                        enabled = btnEnabled,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when {
                                downloadState.status == UpdateStatus.Downloading ->
                                    "${downloadState.progress}%"
                                downloadState.status == UpdateStatus.Completed ->
                                    stringResource(id = R.string.update_install_success)
                                downloadState.status == UpdateStatus.NeedPermission ->
                                    "授权后安装"
                                else -> stringResource(id = R.string.update_btn_now)
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 下载中强制更新提示
                if (updateInfo.force_update && downloadState.status == UpdateStatus.Downloading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "下载更新中，请勿退出",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
