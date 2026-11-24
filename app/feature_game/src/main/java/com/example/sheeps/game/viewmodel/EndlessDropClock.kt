package com.example.sheeps.game.viewmodel

/** Per-round deadlines, measured in monotonic milliseconds. Only the active game advances them. */
internal class EndlessDropClock(nowMs: Long, intervalMs: Long) {
    private var dropAtMs = nowMs + intervalMs
    private var frozenUntilMs = nowMs
    private var pausedAtMs: Long? = null

    fun dropDueInMs(nowMs: Long): Long = (dropAtMs - (pausedAtMs ?: nowMs)).coerceAtLeast(0)

    fun freezeRemainingMs(nowMs: Long): Long = (frozenUntilMs - (pausedAtMs ?: nowMs)).coerceAtLeast(0)

    fun freeze(nowMs: Long) {
        frozenUntilMs = nowMs + 4_000
        dropAtMs = frozenUntilMs + dropDueInMs(nowMs)
    }

    fun scheduleNextDrop(nowMs: Long, intervalMs: Long) {
        dropAtMs = nowMs + intervalMs
    }

    fun pause(nowMs: Long) {
        pausedAtMs = nowMs
    }

    fun resume(nowMs: Long) {
        val pauseDurationMs = nowMs - (pausedAtMs ?: return)
        dropAtMs += pauseDurationMs
        frozenUntilMs += pauseDurationMs
        pausedAtMs = null
    }
}
