package com.example.sheeps.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.game.R
import com.example.sheeps.game.state.DuelViewState
import com.example.sheeps.game.state.EndlessStatus
import com.example.sheeps.game.state.EndlessViewState
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.game.state.GameViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class GameAudio(context: Context) {
    private val commands = Channel<GameAudioSession.() -> Unit>(Channel.UNLIMITED)
    @Volatile private var resumedAt: Long? = null

    init {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val thread = HandlerThread("BoardAudio").apply { start() }
            try {
                // Getting a HandlerThread's looper may wait for startup. Keep that wait off Main too.
                val handler = Handler(thread.looper)
                withContext(handler.asCoroutineDispatcher()) {
                    val output = AndroidGameAudio(app, handler)
                    val session = GameAudioSession(output)
                    try {
                        for (command in commands) session.command()
                    } finally {
                        session.update(false, false)
                        output.release()
                    }
                }
            } finally {
                thread.quitSafely()
            }
        }
    }

    fun resume() { resumedAt = System.nanoTime() }

    fun update(music: Boolean, effects: Boolean) {
        val since = resumedAt ?: return
        commands.trySend { if (resumedAt == since) this.update(music, effects) }
    }

    fun playMatch(tileType: Int, emittedAtNanos: Long) {
        val since = resumedAt ?: return
        if (emittedAtNanos < since) return
        commands.trySend { if (resumedAt == since) match(tileType) }
    }

    fun pause() {
        resumedAt = null
        commands.trySend { this.update(false, false) }
    }

    fun close() {
        resumedAt = null
        commands.close()
    }
}

internal fun GameViewState.hasMusicBoard() = gameStatus == GameStatus.PLAYING &&
    !isLoading && !showCarrySelection && boardTiles.isNotEmpty()

internal fun DuelViewState.hasMusicBoard() = gameStatus == GameStatus.PLAYING && !isLoading && boardTiles.isNotEmpty()

internal fun EndlessViewState.hasMusicBoard() = status == EndlessStatus.PLAYING && !isLoading

internal fun <S> ComponentActivity.bindGameAudio(
    prefs: UserPreferences, states: StateFlow<S>, playable: (S) -> Boolean
): GameAudio {
    val audio = GameAudio(this)
    lifecycleScope.launch {
        try {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                audio.resume()
                try {
                    val music = prefs.isGameMusicEnabled()
                    val effects = prefs.isMatchSoundEnabled()
                    states.map(playable).distinctUntilChanged().collect { audio.update(music && it, effects) }
                } finally {
                    audio.pause()
                }
            }
        } finally {
            audio.close()
        }
    }
    return audio
}

private class AndroidGameAudio(context: Context, private val handler: Handler) : GameAudioOutput {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val track = LoopTrack(handler, open = { player ->
        player.setAudioAttributes(attributes)
        context.resources.openRawResourceFd(R.raw.bamboo_walk).use { asset ->
            player.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
        }
    })
    private val effects = MatchSound(context)
    private var abandon: (() -> Unit)? = null

    @Suppress("DEPRECATION")
    override fun requestFocus(brief: Boolean, listener: (Int) -> Unit): Boolean {
        val callback = AudioManager.OnAudioFocusChangeListener { change ->
            if (Looper.myLooper() == handler.looper) listener(change) else handler.post { listener(change) }
        }
        val gain = if (brief) AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK else AudioManager.AUDIOFOCUS_GAIN
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(gain).setAudioAttributes(attributes)
                .setWillPauseWhenDucked(true).setOnAudioFocusChangeListener(callback, handler).build()
            abandon = { manager.abandonAudioFocusRequest(request) }
            manager.requestAudioFocus(request)
        } else {
            abandon = { manager.abandonAudioFocus(callback) }
            manager.requestAudioFocus(callback, AudioManager.STREAM_MUSIC, gain)
        }
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return true
        abandon = null
        return false
    }

    override fun abandonFocus() { abandon?.invoke(); abandon = null }
    override fun startMusic() = track.start()
    override fun stopMusic() = track.stop()
    override fun prepareMatch() = effects.prepare()
    override fun playMatch(tileType: Int) { effects.play(tileType) }
    override fun stopMatch() = effects.stop()
    override fun afterMatch(callback: () -> Unit) { handler.postDelayed(callback, 620) }
    fun release() = effects.release()
}
