package com.example.sheeps.game.viewmodel

import com.example.sheeps.core.game.GameEngine
import com.example.sheeps.data.model.TileState
import com.example.sheeps.data.game.GameLevelGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialLevelTest {
    @Test fun theOpeningOffersAChoiceThenUncoversFourPlayableTriples() {
        val generator = GameLevelGenerator()
        val board = generator.generateSolvableLevelLocal(1, seed = 7, userId = 42)
        assertEquals(board, generator.generateSolvableLevelLocal(1, seed = 119, userId = 55))
        assertEquals(12, board.size)
        assertTrue(board.all { !it.isBlind && it.sealedCount == 0 })
        val remaining = board.toMutableList()
        val opening = GameEngine.calculateBlockedStates(remaining)
        assertEquals(listOf(1, 1, 1, 2), opening.filter { it.state == TileState.NORMAL }.map { it.type }.sorted())
        for ((round, type) in listOf(1, 2, 3, 1).withIndex()) {
            val exposed = GameEngine.calculateBlockedStates(remaining)
                .filter { it.state == TileState.NORMAL && it.type == type }
            assertEquals(3, exposed.size)
            if (round == 1) {
                assertEquals(2, exposed.count { t -> opening.any { it.id == t.id && it.state == TileState.BLOCKED } })
            }
            for (tile in exposed) {
                assertTrue(!GameEngine.isTileBlocked(tile, remaining))
                assertTrue(remaining.removeIf { it.id == tile.id })
            }
        }
        assertTrue(remaining.isEmpty())
    }
}
