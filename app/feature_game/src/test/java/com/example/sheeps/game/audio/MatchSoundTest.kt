package com.example.sheeps.game.audio

import android.content.Context
import android.media.SoundPool
import com.example.sheeps.game.R
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MatchSoundTest {
    private class Bank {
        val context = mockk<Context>()
        val pool = mockk<SoundPool>(relaxed = true)
        val listener = slot<SoundPool.OnLoadCompleteListener>()
        val loaded = mutableListOf<Int>()
        val played = mutableListOf<Int>()
        var now = 0L
        val sound = MatchSound(context, nowNanos = { now }) { pool }

        // Compute expectations from resource names, independently of the production type table.
        val resources = (0..12).map { type ->
            val name = if (type == 0) "tile_match" else String.format(Locale.ROOT, "tile_match_%02d", type)
            R.raw::class.java.getField(name).getInt(null)
        }
        fun sample(type: Int) = 1000 - type * 11

        init {
            every { pool.setOnLoadCompleteListener(capture(listener)) } just Runs
            every { pool.load(context, capture(loaded), 1) } answers { sample(resources.indexOf(secondArg<Int>())) }
            every { pool.play(capture(played), 1f, 1f, 1, 0, 1f) } answers { played.size + 500 }
        }
        fun complete(type: Int, status: Int = 0) { listener.captured.onLoadComplete(pool, sample(type), status) }
    }

    @Test fun preloadsEachFaceAndRoutesAllTypesAndUnknownValues() {
        val bank = Bank()
        repeat(2) { bank.sound.prepare() }
        assertEquals(bank.resources.sorted(), bank.loaded.sorted())
        (0..12).reversed().forEach { bank.complete(it) }
        for (type in 1..12) bank.sound.play(type)
        for (type in listOf(0, -1, 13)) bank.sound.play(type)
        assertEquals((1..12).map(bank::sample) + List(3) { bank.sample(0) }, bank.played)
        bank.sound.release()
    }

    @Test fun pendingMatchesKeepTheirTypesAcrossOutOfOrderLoadsAndOneFailedFace() {
        val bank = Bank()
        bank.sound.prepare()
        bank.sound.play(7)
        bank.sound.play(12)
        bank.complete(12)
        assertEquals(listOf(bank.sample(12)), bank.played)
        bank.complete(7, status = 1)
        bank.complete(0)
        bank.sound.play(7)
        bank.sound.play(12)
        assertEquals(listOf(12, 0, 0, 12).map(bank::sample), bank.played)
        bank.sound.release()
    }

    @Test fun unavailableFallbackDoesNotMuteOtherFacesOrReplayStaleOrStoppedMatches() {
        val bank = Bank()
        every { bank.pool.load(bank.context, bank.resources[3], 1) } returns 0
        bank.sound.prepare()
        bank.complete(0, status = 1)
        bank.sound.play(3)
        bank.sound.play(13)
        bank.sound.play(2)
        bank.now += 151_000_000L
        bank.complete(2)
        bank.sound.play(8)
        bank.sound.stop()
        bank.complete(8)
        bank.sound.play(8)
        assertEquals(listOf(bank.sample(8)), bank.played)
        bank.sound.release()
    }

    @Test fun preloadOncePlayOncePerMatchAndStopEveryActiveStream() {
        val context = mockk<Context>()
        val pool = mockk<SoundPool>(relaxed = true)
        val listener = slot<SoundPool.OnLoadCompleteListener>()
        every { pool.setOnLoadCompleteListener(capture(listener)) } just Runs
        every { pool.load(context, R.raw.tile_match, 1) } returns 13
        every { pool.play(13, 1f, 1f, 1, 0, 1f) } returnsMany listOf(21, 22)
        val sound = MatchSound(context) { pool }
        sound.prepare()
        sound.prepare()
        verify(exactly = 1) { pool.load(context, R.raw.tile_match, 1) }
        listener.captured.onLoadComplete(pool, 13, 0)
        sound.play(0)
        sound.play(0)
        sound.stop()
        sound.release()
        sound.release()
        verify(exactly = 2) { pool.play(13, 1f, 1f, 1, 0, 1f) }
        verify(exactly = 1) { pool.stop(21) }
        verify(exactly = 1) { pool.stop(22) }
        verify(exactly = 1) { pool.release() }
    }

    @Test fun pendingSoundIsDiscardedOnPauseAndOldLoadCallbackCannotReplayIt() {
        val context = mockk<Context>()
        val first = mockk<SoundPool>(relaxed = true)
        val second = mockk<SoundPool>(relaxed = true)
        val old = slot<SoundPool.OnLoadCompleteListener>()
        val fresh = slot<SoundPool.OnLoadCompleteListener>()
        every { first.setOnLoadCompleteListener(capture(old)) } just Runs
        every { second.setOnLoadCompleteListener(capture(fresh)) } just Runs
        every { first.load(context, R.raw.tile_match, 1) } returns 1
        every { second.load(context, R.raw.tile_match, 1) } returns 2
        val pools = listOf(first, second).iterator()
        val sound = MatchSound(context) { pools.next() }
        sound.prepare()
        sound.play(0)
        sound.stop()
        old.captured.onLoadComplete(first, 1, 0)
        sound.release()
        sound.prepare()
        old.captured.onLoadComplete(first, 1, 0)
        fresh.captured.onLoadComplete(second, 2, 0)
        verify(exactly = 0) { first.play(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { second.play(any(), any(), any(), any(), any(), any()) }
        sound.play(0)
        verify(exactly = 1) { second.play(2, 1f, 1f, 1, 0, 1f) }
        sound.release()
    }
}
