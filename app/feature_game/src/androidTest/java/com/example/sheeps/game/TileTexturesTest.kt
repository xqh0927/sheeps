package com.example.sheeps.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.EventListener
import coil.decode.Decoder
import coil.request.ImageRequest
import coil.request.Options
import coil.ImageLoader
import coil.intercept.Interceptor
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.core.game.TileTextureLoader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class TileTexturesTest {
    @Test
    fun preloadingAndRepeatedBoardDrawsShareEachRequestThenHonorUpdatedUrls() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.cacheDir, UUID.randomUUID().toString()).apply { mkdirs() }
        fun image(name: String, color: Int): String {
            val file = File(dir, name)
            val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            return file.toURI().toString()
        }
        val urls = (1..12).map { image("face-$it.png", Color.RED) }
        val unusedDefault = (1..12).map { image("default-$it.png", Color.BLUE) }
        fun register(faces: List<String>) = TileIconProvider.setShopItems(listOf(
            ShopItem(1, "selected", item_type = "SKIN_SELECTED", points_price = 0, stock = 1, tiles = faces),
            ShopItem(2, "default", item_type = "SKIN_SHUANG", points_price = 0, stock = 1, tiles = unusedDefault)
        ))
        val requests = AtomicInteger()
        val requestedUrls = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val decodes = AtomicInteger()
        val mainDecodes = AtomicInteger()
        val loader = ImageLoader.Builder(context).allowHardware(false).eventListener(object : EventListener {
            override fun decodeStart(request: ImageRequest, decoder: Decoder, options: Options) {
                decodes.incrementAndGet()
                if (Looper.myLooper() == Looper.getMainLooper()) mainDecodes.incrementAndGet()
            }
        }).components {
            add(Interceptor { chain ->
                if (chain.request.data !is String) return@Interceptor chain.proceed(chain.request)
                requestedUrls.add(chain.request.data.toString())
                requests.incrementAndGet()
                peak.accumulateAndGet(active.incrementAndGet(), ::maxOf)
                try {
                    delay(100)
                    chain.proceed(chain.request)
                } finally { active.decrementAndGet() }
            })
        }.build()
        Coil.setImageLoader(loader)
        suspend fun awaitColor(type: Int, color: Int) = withTimeout(4000) {
            while (withContext(Dispatchers.Main) {
                    TileTextureLoader.getTileBitmap(context, "selected", type) {}.getPixel(0, 0) != color
                }) delay(20)
        }
        try {
            register(urls)
            withContext(Dispatchers.Main) {
                repeat(2) { TileTextureLoader.preload(context, "selected") }
                repeat(300) { TileTextureLoader.getTileBitmap(context, "selected", it % 12 + 1) {} }
            }
            for (type in 1..12) awaitColor(type, Color.RED)
            println("tile requests=${requests.get()} peak=${peak.get()}")
            assertEquals(urls.toSet(), requestedUrls.toSet())
            assertEquals(12, requests.get())
            assertEquals(4, peak.get())
            register(listOf(image("updated.png", Color.GREEN)) + urls.drop(1))
            awaitColor(1, Color.GREEN)
            assertEquals(13, requests.get())
            assertTrue(decodes.get() >= 13)
            assertEquals(0, mainDecodes.get())

            // A second skin reuses identical art. Invalid URLs use the packaged face.
            TileIconProvider.setShopItems(listOf(ShopItem(2, "other", item_type = "SKIN_OTHER",
                points_price = 0, stock = 1, tiles = listOf(urls[0], File(dir, "missing.png").toURI().toString()))))
            withContext(Dispatchers.Main) {
                assertEquals(Color.RED, TileTextureLoader.getTileBitmap(context, "other", 1) {}.getPixel(0, 0))
            }
            val fallbackLoaded = CompletableDeferred<Unit>()
            val invalidate: () -> Unit = { fallbackLoaded.complete(Unit) }
            val fallback = withContext(Dispatchers.Main) {
                TileTextureLoader.getTileBitmap(context, "unknown", 2, invalidate)
            }
            if (fallback.width == 1) withTimeout(4000) { fallbackLoaded.await() }
            withContext(Dispatchers.Main) {
                val local = TileTextureLoader.getTileBitmap(context, "unknown", 2, invalidate)
                assertTrue(local.width > 1)
                repeat(300) {
                    assertTrue(local === TileTextureLoader.getTileBitmap(context, "other", 2, invalidate))
                }
            }
            withTimeout(4000) { while (requests.get() != 14 || active.get() != 0) delay(20) }
            withContext(Dispatchers.Main) {
                repeat(300) { TileTextureLoader.getTileBitmap(context, "other", 2, invalidate) }
            }
            delay(150)
            assertEquals(14, requests.get())
        } finally {
            TileIconProvider.setShopItems(emptyList())
            loader.shutdown()
            dir.deleteRecursively()
        }
    }
}
