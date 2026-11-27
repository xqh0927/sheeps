package com.example.sheeps.game.viewmodel

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test

class LevelRunsTest {
    @Test
    fun preparedRunIsReusedOnceAndRestartGetsAFreshBoard() {
        val prepared = java.util.UUID.randomUUID().toString()
        val runs = LevelRuns(SavedStateHandle(mapOf("preparedRunId" to prepared)))
        assertEquals(prepared, runs.open(3, null)!!.id)
        assertNotEquals(prepared, runs.start(3, null).id)
    }

    @Test
    fun rotationKeepsBoardAndProcessRestoreReopensOnlyTheCurrentRun() {
        val saved = SavedStateHandle()
        val retained = LevelRuns(saved)
        val first = retained.open(1, """{"UNDO":2}""")!!
        assertNull(retained.open(1, first.carryJson))
        val restart = retained.start(1, """{"BOMB":2}""")
        assertNotEquals(first.id, restart.id)
        retained.saveRemaining(mapOf("BOMB" to 1))
        val snapshot = saved.keys().associateWith { saved.get<Any?>(it) }
        val recreated = LevelRuns(SavedStateHandle(snapshot))
        assertEquals(restart.copy(remaining = mapOf("BOMB" to 1)), recreated.open(1, null))
        assertNull(recreated.open(1, first.carryJson))
        val next = recreated.start(2, null)
        assertNotEquals(restart.id, next.id)
        assertNull(next.remaining)
    }
}
