package com.example.sheeps.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * 图片压缩工具
 *
 * 将相册或相机选中的图片压缩到指定大小并转为 Base64 字符串。
 * 通过等比缩放 + JPEG 渐进式压缩实现目标大小控制。
 */
object ImageCompressor {

    /** 默认最大输出大小（KB） */
    private const val DEFAULT_MAX_SIZE_KB = 256

    /** 渐进压缩质量起始值和递减步长 */
    private const val QUALITY_START = 90
    private const val QUALITY_STEP = 10
    private const val QUALITY_MIN = 10

    /**
     * 压缩图片并返回 Base64 字符串
     *
     * @param context Android Context
     * @param uri 图片的 Uri
     * @param maxSizeKB 最大输出大小（KB），默认 256
     * @return Base64 编码的 JPEG 图片字符串
     */
    fun compressImage(context: Context, uri: Uri, maxSizeKB: Int = DEFAULT_MAX_SIZE_KB): String {
        val maxSizeBytes = maxSizeKB * 1024

        // 1. 读取原始图片尺寸（仅解码尺寸，不解码像素）
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        // 2. 计算缩放比例
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        // 基准最大宽度 512px（头像显示不需要过高的分辨率）
        val maxDimension = 512
        var scaleFactor = 1
        if (originalWidth > maxDimension || originalHeight > maxDimension) {
            scaleFactor = maxOf(
                originalWidth / maxDimension,
                originalHeight / maxDimension
            )
        }

        // 3. 读取并缩放图片
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = scaleFactor
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalArgumentException("Failed to decode image")

        // 4. 渐进式 JPEG 压缩直到满足大小要求
        var quality = QUALITY_START
        var outputBytes: ByteArray

        do {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputBytes = outputStream.toByteArray()

            if (outputBytes.size <= maxSizeBytes) break

            quality -= QUALITY_STEP
        } while (quality >= QUALITY_MIN)

        // 5. 如果最低质量仍超限，再次缩小尺寸
        if (outputBytes.size > maxSizeBytes) {
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY_MIN, outputStream)
            outputBytes = outputStream.toByteArray()
            scaledBitmap.recycle()
        }

        bitmap.recycle()

        // 6. 转为 Base64
        return Base64.encodeToString(outputBytes, Base64.NO_WRAP)
    }
}
