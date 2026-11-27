package com.example.sheeps.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sheeps.data.game.GameLevelGenerator
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.network.ApiService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class StageBoardHandoffTest {
    @Test
    fun previewMatchesOpeningAndPublishedBoardSurvivesOfflineRestart() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.cacheDir, UUID.randomUUID().toString())
        val online = AtomicBoolean(true)
        val requests = AtomicInteger()
        // Level 5's generated layout has neither kind of special tile.
        val published = (0 until 6).map { index ->
            Tile("editor-$index", index / 3 + 1, index.toFloat(), 0f, 0,
                isBlind = index == 4, sealedCount = if (index == 5) 1 else 0)
        }
        val media = "application/json".toMediaType()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            check(request.url.encodedPath == "/api/level" && request.url.queryParameter("id") == "5")
            requests.incrementAndGet()
            if (!online.get()) throw IOException("Test transport is offline")
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .header("X-Stage-Source", "published")
                .header("X-Stage-Version", "a".repeat(64))
                .body(Json.encodeToString(published).toResponseBody(media)).build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://stage.test/").client(client)
            .addConverterFactory(Json.asConverterFactory(media)).build().create(ApiService::class.java)
        fun repository() = StageBoards(api, Json, GameLevelGenerator(), directory)

        try {
            val runId = UUID.randomUUID().toString()
            val boards = repository()
            val preview = boards.resolve(5, runId, 17)
            assertFalse(preview.offline)
            assertTrue(preview.hasBlind && preview.hasSeals)
            assertEquals(published, preview.tiles)
            assertEquals(1, requests.get())

            online.set(false)
            val opening = boards.resolve(5, runId, 17)
            assertEquals(preview.tiles, opening.tiles)
            assertEquals(1, requests.get())

            val replay = repository().resolve(5, UUID.randomUUID().toString(), 17)
            assertTrue(replay.offline)
            assertTrue(replay.hasBlind && replay.hasSeals)
            assertEquals(published, replay.tiles)
            assertTrue(requests.get() > 1)
        } finally {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
            directory.deleteRecursively()
        }
    }
}
