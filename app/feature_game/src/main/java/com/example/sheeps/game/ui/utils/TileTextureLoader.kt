package com.example.sheeps.game.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.sheeps.core.game.TileIconProvider
import java.util.concurrent.ConcurrentHashMap
import androidx.core.graphics.createBitmap

/**
 * 针对 SurfaceView 的卡牌纹理异步加载与内存缓存器。
 * 解决多线程渲染中无法直接使用 Compose Painter / coil AsyncImage 渲染的难题。
 */
object TileTextureLoader {

    // 内存纹理缓存：Key = skinKey_type (例如: "shuang_1")
    private val textureCache = ConcurrentHashMap<String, Bitmap>()

    // 正在请求加载中的 Key 集合，避免重复拉取
    private val loadingKeys = ConcurrentHashMap.newKeySet<String>()

    /**
     * 获取指定卡牌的 Bitmap 纹理。
     * - 如果缓存已存在，直接返回；
     * - 如果缓存无且未处于加载中，启动 Coil 异步拉取，并立刻返回本地兜底的默认皮肤 Bitmap；
     * - 加载完成后触发 [onLoaded] 回调以刷新 SurfaceView。
     */
    fun getTileBitmap(
        context: Context,
        skinKey: String,
        type: Int,
        onLoaded: () -> Unit
    ): Bitmap {
        val key = "${skinKey.lowercase()}_$type"
        
        // 1. 命中缓存直接返回
        val cached = textureCache[key]
        if (cached != null) {
            return cached
        }

        // 2. 未命中，若未在加载中则触发异步拉取
        if (loadingKeys.add(key)) {
            val url = TileIconProvider.getTileUrl(skinKey, type)
            if (url != null) {
                val imageLoader = coil.Coil.imageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .target(object : coil.target.Target {
                        override fun onSuccess(result: Drawable) {
                            val bitmap = result.toBitmap()
                            textureCache[key] = bitmap
                            loadingKeys.remove(key)
                            onLoaded() // 通知棋盘刷新
                        }

                        override fun onError(error: Drawable?) {
                            val fallbackResId = TileIconProvider.getFallbackResId(context, type)
                            val fallbackDrawable = ContextCompat.getDrawable(context, fallbackResId)
                            val bitmap = fallbackDrawable?.toBitmap() ?: createBitmap(1, 1)
                            textureCache[key] = bitmap
                            loadingKeys.remove(key)
                            onLoaded()
                        }
                    })
                    .build()
                imageLoader.enqueue(request)
            } else {
                // 如果无远程 URL，直接存本地默认皮肤
                val fallbackResId = TileIconProvider.getFallbackResId(context, type)
                val fallbackDrawable = ContextCompat.getDrawable(context, fallbackResId)
                val bitmap = fallbackDrawable?.toBitmap() ?: createBitmap(1, 1)
                textureCache[key] = bitmap
                loadingKeys.remove(key)
                onLoaded()
            }
        }

        // 3. 正在加载期间，或者未拉取前，始终同步返回默认皮肤（shuang）的本地 Bitmap 保证画面不留白
        val fallbackKey = "shuang_$type"
        val cachedFallback = textureCache[fallbackKey]
        if (cachedFallback != null) {
            return cachedFallback
        }

        val fallbackResId = TileIconProvider.getFallbackResId(context, type)
        val fallbackDrawable = ContextCompat.getDrawable(context, fallbackResId)
        val fallbackBitmap = fallbackDrawable?.toBitmap() ?: createBitmap(1, 1)
        textureCache[fallbackKey] = fallbackBitmap
        return fallbackBitmap
    }

    /**
     * 将 Drawable 转换为可绘制的 Bitmap。
     */
    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable) {
            return bitmap
        }
        val width = intrinsicWidth.coerceAtLeast(1)
        val height = intrinsicHeight.coerceAtLeast(1)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, width, height)
        draw(canvas)
        return bitmap
    }
}
