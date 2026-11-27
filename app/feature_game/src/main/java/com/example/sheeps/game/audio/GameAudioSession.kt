package com.example.sheeps.game.audio

import android.media.AudioManager

internal interface GameAudioOutput {
    fun requestFocus(brief: Boolean, listener: (Int) -> Unit): Boolean
    fun abandonFocus()
    fun startMusic()
    fun stopMusic()
    fun prepareMatch()
    fun playMatch(tileType: Int)
    fun stopMatch()
    fun afterMatch(callback: () -> Unit)
}

/** All operations, including focus callbacks, are serialized on the audio thread. */
internal class GameAudioSession(private val out: GameAudioOutput) {
    private enum class Focus { MUSIC, BRIEF }

    private var music = false
    private var effects = false
    private var focus: Focus? = null
    private var focusGeneration = 0
    private var effectGeneration = 0
    private var audible = false
    private var interrupted = false
    private var loopRequested = false

    fun update(music: Boolean, effects: Boolean) {
        if (music == this.music && effects == this.effects) return
        this.music = music
        if (!music) setLoop(false)
        if (effects != this.effects) {
            this.effects = effects
            if (effects) out.prepareMatch() else out.stopMatch()
        }
        if ((focus == Focus.MUSIC && !music) || (focus == Focus.BRIEF && (music || !effects))) {
            abandon()
            interrupted = false
        }
        if (!music && !effects) interrupted = false
        if (music && !interrupted && focus == null && acquire(Focus.MUSIC)) setLoop(true)
    }

    fun match(tileType: Int) {
        if (!effects || interrupted) return
        if (focus == null && (music || !acquire(Focus.BRIEF))) return
        if (!audible) return
        out.playMatch(tileType)
        if (focus == Focus.BRIEF) {
            val generation = ++effectGeneration
            out.afterMatch {
                if (focus == Focus.BRIEF && !interrupted && generation == effectGeneration) abandon()
            }
        }
    }

    private fun acquire(mode: Focus): Boolean {
        val generation = ++focusGeneration
        if (!out.requestFocus(mode == Focus.BRIEF) { change ->
                if (generation == focusGeneration) onFocus(change)
            }) return false
        focus = mode
        audible = true
        return true
    }

    private fun onFocus(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                val stoppedEffect = focus == Focus.BRIEF && interrupted
                interrupted = false
                audible = true
                if (stoppedEffect) abandon() else if (focus == Focus.MUSIC && music) setLoop(true)
            }
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                interrupted = true
                audible = false
                setLoop(false)
                if (effects) out.stopMatch()
                if (change == AudioManager.AUDIOFOCUS_LOSS) abandon()
            }
        }
    }

    private fun setLoop(play: Boolean) {
        if (loopRequested == play) return
        loopRequested = play
        if (play) out.startMusic() else out.stopMusic()
    }

    private fun abandon() {
        if (focus == null) return
        focusGeneration++
        effectGeneration++
        focus = null
        audible = false
        out.abandonFocus()
    }
}
