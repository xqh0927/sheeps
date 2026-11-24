package com.example.sheeps.game.viewmodel

import androidx.lifecycle.SavedStateHandle
import java.util.UUID

internal data class LevelRun(
    val id: String,
    val level: Int,
    val carryJson: String?,
    val remaining: Map<String, Int>? = null
)

/** The ViewModel survives rotation. The saved handle identifies the run after process death. */
internal class LevelRuns(private val saved: SavedStateHandle) {
    private var opened = false

    fun open(level: Int, carryJson: String?): LevelRun? {
        if (opened) return null
        opened = true
        val id = saved.get<String>("run.id") ?: return start(level, carryJson)
        return LevelRun(
            id, saved.get<Int>("run.level") ?: level, saved["run.carry"],
            saved.get<HashMap<String, Int>>("run.remaining")
        )
    }

    fun start(level: Int, carryJson: String?): LevelRun {
        val run = LevelRun(UUID.randomUUID().toString(), level, carryJson)
        saved["run.id"] = run.id
        saved["run.level"] = level
        saved["run.carry"] = carryJson
        saved.remove<HashMap<String, Int>>("run.remaining")
        return run
    }

    fun saveRemaining(counts: Map<String, Int>) {
        saved["run.remaining"] = HashMap(counts)
    }
}
