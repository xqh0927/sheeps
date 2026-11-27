package com.example.sheeps.game.audio

import android.media.MediaPlayer
import android.os.Handler
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Test

class LoopTrackTest {
    @Test fun stalePreparationCannotStartAndAResumedTrackSeeksBeforeFadingIn() {
        val first = mockk<MediaPlayer>(relaxed = true)
        val second = mockk<MediaPlayer>(relaxed = true)
        val pending = ArrayDeque(listOf(first, second))
        val prepared = mutableListOf<MediaPlayer.OnPreparedListener>()
        val sought = slot<MediaPlayer.OnSeekCompleteListener>()
        val gains = mutableListOf<Float>()
        for (player in pending) {
            every { player.setOnPreparedListener(capture(prepared)) } just Runs
            every { player.setOnSeekCompleteListener(capture(sought)) } just Runs
            every { player.setVolume(capture(gains), any()) } just Runs
        }
        val handler = mockk<Handler>(relaxed = true)
        val ramps = ArrayDeque<Runnable>()
        every { handler.postDelayed(any<Runnable>(), 50L) } answers { ramps.addLast(firstArg()); true }
        every { handler.removeCallbacks(any()) } answers { ramps.remove(firstArg<Runnable>()); Unit }

        val loop = LoopTrack(handler, open = {}, newPlayer = { pending.removeFirst() })
        loop.start()
        loop.start()
        verify(exactly = 1) { first.prepareAsync() }
        verify(exactly = 0) { first.start() }
        prepared.first().onPrepared(first)
        repeat(14) { ramps.removeFirst().run() }
        assertEquals(0f, gains.first())
        assertEquals(0.55f, gains.last(), 0.00001f)
        every { first.currentPosition } returns 8200
        loop.stop()
        verify(exactly = 1) { first.release() }
        loop.start()
        prepared.first().onPrepared(first)
        verify(exactly = 1) { first.start() }
        prepared.last().onPrepared(second)
        verify { second.seekTo(8200) }
        verify(exactly = 0) { second.start() }
        sought.captured.onSeekComplete(second)
        verify(exactly = 1) { second.start() }
        loop.stop()
        assertEquals(0, ramps.size)
        verify(exactly = 1) { second.release() }
    }

    @Test fun leavingDuringPreparationReleasesWithoutPlaying() {
        val player = mockk<MediaPlayer>(relaxed = true)
        val prepared = slot<MediaPlayer.OnPreparedListener>()
        every { player.setOnPreparedListener(capture(prepared)) } just Runs
        val loop = LoopTrack(mockk(relaxed = true), open = {}, newPlayer = { player })
        loop.start()
        loop.stop()
        prepared.captured.onPrepared(player)
        verify(exactly = 1) { player.release() }
        verify(exactly = 0) { player.start() }
    }
}
