package com.example.sheeps.core.game

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import androidx.annotation.MainThread
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.WeakHashMap

/** Menu preloads and all three Canvas boards request the same size and share decoded Coil entries. */
object TileTextureLoader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val remoteLoads = Semaphore(4)
    private val localLoads = Semaphore(2)
    private val emptyFace by lazy { createBitmap(1, 1) }

    // Accessed on Main. A detached board must not be retained by pending downloads.
    private val waiting = mutableMapOf<MemoryCache.Key, MutableSet<() -> Unit>>()
    private val failures = LruCache<MemoryCache.Key, Long>(64)

    @MainThread
    fun preload(context: Context, skin: String) {
        for (url in TileIconProvider.getSkinTileUrls(skin).distinct()) bitmap(context, url, null)
    }

    @MainThread
    fun getTileBitmap(context: Context, skinKey: String, type: Int, onLoaded: () -> Unit): Bitmap {
        val url = TileIconProvider.getTileUrl(skinKey, type)
        if (url != null) bitmap(context, url, onLoaded)?.let { return it }
        return bitmap(context, TileIconProvider.getFallbackResId(context, type), onLoaded) ?: emptyFace
    }

    @MainThread
    private fun bitmap(context: Context, data: Any, onLoaded: (() -> Unit)?): Bitmap? {
        val app = context.applicationContext
        val px = (48f * app.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val key = MemoryCache.Key("tile:$px:$data")
        val loader = Coil.imageLoader(app)
        val cache = loader.memoryCache
        cache?.get(key)?.let { return it.bitmap }
        val failedAt = failures.get(key)
        if (failedAt != null && SystemClock.elapsedRealtime() - failedAt < 30_000) return null
        val existing = waiting[key]
        if (existing != null) {
            if (onLoaded != null) existing.add(onLoaded)
            return null
        }
        val listeners = Collections.newSetFromMap(WeakHashMap<() -> Unit, Boolean>())
        if (onLoaded != null) listeners.add(onLoaded)
        waiting[key] = listeners
        scope.launch {
            var success = false
            try {
                // Local fallback decoding can proceed even when every remote slot is waiting for I/O.
                val permits = if (data is Int) localLoads else remoteLoads
                permits.withPermit {
                    val request = ImageRequest.Builder(app).data(data).size(px).scale(Scale.FIT)
                        .memoryCacheKey(key).allowHardware(false).build()
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val decoded = result.drawable.toBitmap()
                        if (cache?.get(key)?.bitmap !== decoded) cache?.set(key, MemoryCache.Value(decoded))
                        success = true
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("TileTextureLoader", "Could not decode a board texture", e)
            } finally {
                withContext(Dispatchers.Main.immediate) {
                    if (success) failures.remove(key) else failures.put(key, SystemClock.elapsedRealtime())
                    waiting.remove(key)?.forEach { it() }
                }
            }
        }
        return null
    }
}
