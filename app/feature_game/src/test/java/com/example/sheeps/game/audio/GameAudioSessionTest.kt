package com.example.sheeps.game.audio

import android.media.AudioManager.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GameAudioSessionTest {
    private val events = mutableListOf<String>()
    private var change: (Int) -> Unit = {}
    private var timeout: () -> Unit = {}
    private var granted = true
    private val output = object : GameAudioOutput {
        override fun requestFocus(brief: Boolean, listener: (Int) -> Unit): Boolean {
            events += if (brief) "short focus" else "music focus"
            change = listener
            return granted
        }
        override fun abandonFocus() { events += "abandon" }
        override fun startMusic() { events += "start" }
        override fun stopMusic() { events += "stop" }
        override fun prepareMatch() { events += "prepare effect" }
        override fun playMatch(tileType: Int) { events += "match:$tileType" }
        override fun stopMatch() { events += "stop effect" }
        override fun afterMatch(callback: () -> Unit) { timeout = callback }
    }
    private val audio = GameAudioSession(output)

    @Test fun repeatedBoardStatesAndFocusGainsNeverStackPlayback() {
        audio.update(true, true)
        repeat(5) { audio.update(true, true); change(AUDIOFOCUS_GAIN) }
        audio.match(3)
        change(AUDIOFOCUS_LOSS_TRANSIENT)
        audio.match(3)
        change(AUDIOFOCUS_GAIN)
        val oldListener = change
        audio.update(false, false)
        oldListener(AUDIOFOCUS_GAIN)
        assertEquals(listOf("prepare effect", "music focus", "start", "match:3", "stop", "stop effect",
            "start", "stop", "stop effect", "abandon"), events)
    }

    @Test fun matchOnlyUsesBriefFocusAndPreviousTimeoutCannotCutOffTheNextMatch() {
        audio.update(false, true)
        audio.match(3)
        val oldTimeout = timeout
        audio.match(12)
        oldTimeout()
        assertFalse("abandon" in events)
        timeout()
        assertEquals(listOf("prepare effect", "short focus", "match:3", "match:12", "abandon"), events)
    }

    @Test fun backgroundAndPermanentFocusLossStaySilentUntilNextForegroundSession() {
        audio.update(true, true)
        change(AUDIOFOCUS_LOSS)
        audio.match(3)
        audio.update(true, true)
        assertEquals(1,events.count { it == "start" })
        assertFalse("match:3" in events)
        audio.update(false, false)
        audio.update(true, true)
        assertEquals(2,events.count { it == "start" })
    }

    @Test fun settlementStopsTheLoopAndTheLastMatchCanStillSoundInForeground() {
        audio.update(true, true)
        audio.update(false, true)
        audio.match(3)
        timeout()
        assertEquals(listOf("prepare effect", "music focus", "start", "stop", "abandon",
            "short focus", "match:3", "abandon"), events)
    }

    @Test fun aShortInterruptionStopsTheEffectAndDoesNotMuteLaterMatches() {
        audio.update(false, true)
        audio.match(3)
        change(AUDIOFOCUS_LOSS_TRANSIENT)
        timeout()
        audio.match(3)
        assertEquals(1, events.count { it == "match:3" })
        change(AUDIOFOCUS_GAIN)
        audio.match(3)
        assertEquals(2, events.count { it == "match:3" })
    }

    @Test fun deniedMusicFocusCannotProduceSound() {
        granted = false
        audio.update(true,true)
        audio.match(3)
        assertFalse("start" in events || "match:3" in events)
    }
}
