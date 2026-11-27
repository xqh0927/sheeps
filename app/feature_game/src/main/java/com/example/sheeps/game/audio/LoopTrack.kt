package com.example.sheeps.game.audio

import android.media.MediaPlayer
import android.os.Handler
import android.util.Log

/** Owned by one audio looper. A paused track keeps only its cursor. */
internal class LoopTrack(
    private val handler: Handler,
    private val open: (MediaPlayer) -> Unit,
    private val newPlayer: () -> MediaPlayer = { MediaPlayer() }
) {
    private var player: MediaPlayer? = null
    private var started = false
    private var positionMs = 0
    private var fade: Runnable? = null

    fun start() {
        if (player != null) return
        try {
            val track = newPlayer()
            player = track
            track.setOnErrorListener { failed, _, _ ->
                if (failed === player) stop()
                true
            }
            track.setOnPreparedListener { prepared ->
                if (prepared === player) {
                    prepared.isLooping = true
                    prepared.setVolume(0f, 0f)
                    if (positionMs == 0) play(prepared) else prepared.seekTo(positionMs)
                }
            }
            track.setOnSeekCompleteListener { ready -> if (ready === player && !started) play(ready) }
            open(track)
            track.prepareAsync()
        } catch (e: Exception) {
            Log.w("GameAudio", "Game music could not open", e)
            stop()
        }
    }

    private fun play(track: MediaPlayer) {
        track.start()
        started = true
        var step = 0
        val ramp = object : Runnable {
            override fun run() {
                if (track !== player || !started) return
                val gain = 0.55f * ++step / 14
                track.setVolume(gain, gain)
                if (step < 14) handler.postDelayed(this, 50)
            }
        }
        fade = ramp
        handler.postDelayed(ramp, 50)
    }

    fun stop() {
        val track = player ?: return
        player = null
        fade?.let(handler::removeCallbacks)
        fade = null
        if (started) {
            try { positionMs = track.currentPosition } catch (_: IllegalStateException) { /* decoder error */ }
        }
        started = false
        track.release()
    }
}
