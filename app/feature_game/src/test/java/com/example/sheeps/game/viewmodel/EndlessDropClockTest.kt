package com.example.sheeps.game.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class EndlessDropClockTest {
    @Test
    fun freezingJustBeforeADropPreservesTheLastHundredMilliseconds() {
        val clock = EndlessDropClock(nowMs = 0, intervalMs = 3_200)
        clock.freeze(nowMs = 3_100)

        assertEquals(3_900L, clock.freezeRemainingMs(3_200))
        assertEquals(4_000L, clock.dropDueInMs(3_200))
        assertEquals(0L, clock.freezeRemainingMs(7_100))
        assertEquals(100L, clock.dropDueInMs(7_100))
        assertEquals(0L, clock.dropDueInMs(7_200))
    }

    @Test
    fun pausingHoldsBothCountdownsUntilPlayResumes() {
        val clock = EndlessDropClock(nowMs = 0, intervalMs = 3_200)
        clock.freeze(nowMs = 1_200)
        clock.pause(nowMs = 2_700)

        assertEquals(2_500L, clock.freezeRemainingMs(20_000))
        assertEquals(4_500L, clock.dropDueInMs(20_000))
        clock.resume(nowMs = 20_000)
        assertEquals(0L, clock.freezeRemainingMs(22_500))
        assertEquals(2_000L, clock.dropDueInMs(22_500))
        assertEquals(0L, clock.dropDueInMs(24_500))
    }

    @Test
    fun restartingWhileFrozenStartsAnIndependentDropInterval() {
        val old = EndlessDropClock(nowMs = 0, intervalMs = 3_200)
        old.freeze(nowMs = 3_000)
        val restarted = EndlessDropClock(nowMs = 4_000, intervalMs = 3_200)

        assertEquals(0L, restarted.freezeRemainingMs(4_000))
        assertEquals(3_200L, restarted.dropDueInMs(4_000))
        restarted.freeze(nowMs = 5_000)
        assertEquals(2_000L, restarted.freezeRemainingMs(7_000))
        assertEquals(0L, old.freezeRemainingMs(7_000))
    }
}
