package com.example.sheeps.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.sheeps.game.R
import java.util.ArrayDeque

/** Owned by the game audio thread. The controller handles foreground state, focus and settings. */
internal class MatchSound(
    private val context: Context,
    private val nowNanos: () -> Long = System::nanoTime,
    private val createPool: () -> SoundPool = {
        SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
    }
) {
    private var pool: SoundPool? = null
    private data class PendingMatch(val tileType: Int, val enqueuedAt: Long)

    // Index zero is the generic sound. SoundPool IDs are assigned at load time.
    private val samples = IntArray(RESOURCES.size)
    private val ready = mutableSetOf<Int>()
    private val awaitingLoad = ArrayDeque<PendingMatch>()
    private val streams = ArrayDeque<Int>()

    fun prepare() {
        if (pool != null) return
        try {
            val created = createPool()
            pool = created
            created.setOnLoadCompleteListener { source, sampleId, status ->
                if (source !== pool || sampleId == 0) return@setOnLoadCompleteListener
                val type = samples.indexOf(sampleId)
                if (type < 0) return@setOnLoadCompleteListener
                if (status == 0) ready.add(sampleId) else samples[type] = 0
                drainReady(source)
            }
            for ((type, resource) in RESOURCES.withIndex()) {
                try {
                    samples[type] = created.load(context, resource, 1)
                } catch (e: RuntimeException) {
                    Log.w("MatchSound", "Could not load match sound for type $type", e)
                }
            }
        } catch (e: RuntimeException) {
            release()
            Log.w("MatchSound", "Could not prepare match sample", e)
        }
    }

    private fun sampleFor(tileType: Int): Int {
        val sample = samples.getOrNull(tileType) ?: 0
        return if (sample != 0) sample else samples[0]
    }

    fun play(tileType: Int) {
        val source = pool ?: return
        val sample = sampleFor(tileType)
        if (sample == 0) return
        if (sample in ready) {
            playLoaded(source, sample)
        } else {
            if (awaitingLoad.size == MAX_STREAMS) awaitingLoad.removeFirst()
            awaitingLoad.addLast(PendingMatch(tileType, nowNanos()))
        }
    }

    private fun drainReady(source: SoundPool) {
        val now = nowNanos()
        val pending = awaitingLoad.iterator()
        while (pending.hasNext()) {
            val match = pending.next()
            val sample = sampleFor(match.tileType)
            if (sample == 0 || now - match.enqueuedAt > MAX_START_DELAY_NS) {
                pending.remove()
            } else if (sample in ready) {
                pending.remove()
                playLoaded(source, sample)
            }
        }
    }

    private fun playLoaded(source: SoundPool, sample: Int) {
        val stream = source.play(sample, 1f, 1f, 1, 0, 1f)
        if (stream == 0) return
        if (streams.size == MAX_STREAMS) streams.removeFirst()
        streams.addLast(stream)
    }

    fun stop() {
        awaitingLoad.clear()
        while (streams.isNotEmpty()) {
            val stream = streams.removeFirst()
            pool?.stop(stream)
        }
    }

    fun release() {
        stop()
        val previous = pool
        pool = null
        samples.fill(0)
        ready.clear()
        previous?.setOnLoadCompleteListener(null)
        previous?.release()
    }

    private companion object {
        val RESOURCES = intArrayOf(
            R.raw.tile_match,
            R.raw.tile_match_01, R.raw.tile_match_02, R.raw.tile_match_03, R.raw.tile_match_04,
            R.raw.tile_match_05, R.raw.tile_match_06, R.raw.tile_match_07, R.raw.tile_match_08,
            R.raw.tile_match_09, R.raw.tile_match_10, R.raw.tile_match_11, R.raw.tile_match_12,
        )
        const val MAX_STREAMS = 4
        const val MAX_START_DELAY_NS = 150_000_000L
    }
}
