package com.example.sheeps.core.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * App版本更新进度状态
 */
data class UpdateDownloadState(
    val status: UpdateStatus = UpdateStatus.Idle,
    val progress: Int = 0,
    val error: String? = null
)

enum class UpdateStatus {
    Idle, Downloading, Completed, Error, NeedPermission
}

/**
 * App 版本更新管理器（内嵌下载 + 自动安装）
 *
 * 使用方式:
 *   val manager = UpdateDownloadManager()
 *   manager.downloadAndInstall(context, apkUrl) { state -> /* 更新 UI */ }
 */
class UpdateDownloadManager {

    private val _state = MutableStateFlow(UpdateDownloadState())
    val state: StateFlow<UpdateDownloadState> = _state.asStateFlow()

    private var downloadJob: Job? = null
    private var apkFile: File? = null

    /**
     * 下载 APK 并通过 FileProvider 触发安装
     *
     * @param context 上下文
     * @param url     APK 下载地址
     * @param fileName 保存的文件名（可选）
     */
    fun downloadAndInstall(
        context: Context,
        url: String,
        fileName: String = "app_update.apk"
    ) {
        downloadJob?.cancel()
        _state.value = UpdateDownloadState(status = UpdateStatus.Downloading, progress = 0)

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.cacheDir
        // 清理历史残留 APK（含文件名不同的旧版本）
        dir.listFiles()?.filter { it.name.endsWith(".apk") }?.forEach { old ->
            if (old.name != fileName) old.delete()
        }
        apkFile = File(dir, fileName)
        apkFile?.delete()

        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null
            var output: FileOutputStream? = null
            try {
                val downloadUrl = URL(url)
                connection = (downloadUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("Accept", "application/vnd.android.package-archive")
                }
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP ${connection.responseCode}")
                }

                val totalBytes = connection.contentLength.toLong()
                val input = connection.inputStream
                output = FileOutputStream(apkFile)

                val buffer = ByteArray(8192)
                var downloadedBytes = 0L
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        val pct = ((downloadedBytes * 100) / totalBytes).toInt()
                        _state.value = UpdateDownloadState(
                            status = UpdateStatus.Downloading,
                            progress = pct.coerceIn(0, 100)
                        )
                    }
                }

                output.flush()
                _state.value = UpdateDownloadState(status = UpdateStatus.Completed, progress = 100)

                // 下载完成后，先检查权限再安装
                withContext(Dispatchers.Main) {
                    tryInstallApk(context)
                }

            } catch (e: CancellationException) {
                // 取消下载
                apkFile?.delete()
            } catch (e: Exception) {
                _state.value = UpdateDownloadState(
                    status = UpdateStatus.Error,
                    error = e.message ?: "下载失败"
                )
            } finally {
                try { output?.close() } catch (_: Exception) {}
                connection?.disconnect()
            }
        }
    }

    /**
     * 尝试安装 APK（公开方法，供 Dialog 重试）
     * @return true = 已启动安装/权限引导；false = 文件不存在
     */
    fun tryInstallApk(context: Context): Boolean {
        val file = apkFile
        if (file == null || !file.exists()) {
            _state.value = UpdateDownloadState(status = UpdateStatus.Error, error = "安装文件不存在")
            return false
        }

        return try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                _state.value = UpdateDownloadState(status = UpdateStatus.NeedPermission, progress = 100)
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return true
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            _state.value = UpdateDownloadState(status = UpdateStatus.Error, error = "安装失败: ${e.message}")
            false
        }
    }

    fun cancel() {
        downloadJob?.cancel()
        cleanup()
        _state.value = UpdateDownloadState(status = UpdateStatus.Idle)
    }

    /**
     * 清理已下载的 APK 文件（安装完成后可调用）
     */
    fun cleanup() {
        downloadJob?.cancel()
        apkFile?.let {
            if (it.exists()) {
                it.delete()
            }
        }
        apkFile = null
    }
}
