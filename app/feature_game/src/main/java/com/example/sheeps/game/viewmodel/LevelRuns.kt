package com.example.sheeps.game.viewmodel

import androidx.lifecycle.SavedStateHandle
import java.util.UUID

internal data class LevelRun(
    val id: String,
    val level: Int,
    val carryJson: String?,
    val remaining: Map<String, Int>? = null,
    val requiresSnapshot: Boolean = false
)

/** The ViewModel survives rotation. The saved handle identifies the run after process death. */
internal class LevelRuns(private val saved: SavedStateHandle) {
    private var opened = false

    fun open(level: Int, carryJson: String?, resumeId: String? = saved["resumeRunId"]): LevelRun? {
        if (opened) return null
        opened = true
        val previousId = saved.get<String>("run.id")
        if (previousId == null && resumeId != null) {
            start(level, null, resumeId)
            markInstalled()
        }
        val id = saved.get<String>("run.id") ?: return start(
            level, carryJson, saved.remove<String>("preparedRunId") ?: UUID.randomUUID().toString()
        )
        return LevelRun(
            id, saved.get<Int>("run.level") ?: level, saved["run.carry"],
            saved.get<HashMap<String, Int>>("run.remaining"), saved["run.installed"] ?: false
        )
    }

    fun start(level: Int, carryJson: String?, id: String = UUID.randomUUID().toString()): LevelRun {
        val run = LevelRun(id, level, carryJson)
        saved["run.installed"] = false
        saved["run.id"] = run.id
        saved["run.level"] = level
        saved["run.carry"] = carryJson
        saved.remove<HashMap<String, Int>>("run.remaining")
        return run
    }

    fun markInstalled() { saved["run.installed"] = true }

    fun saveRemaining(counts: Map<String, Int>) {
        saved["run.remaining"] = HashMap(counts)
    }
}
