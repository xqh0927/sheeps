package com.example.sheeps.game.viewmodel

import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.data.model.TileState
import com.example.sheeps.data.model.Tile
import com.example.sheeps.core.game.GameEngine
import com.example.sheeps.data.game.GameLevelGenerator
import com.example.sheeps.data.repository.StagePosition
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.game.viewmodel.delegates.GameLogicDelegate
import com.example.sheeps.game.viewmodel.delegates.GameToolDelegate
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class StageToolRulesTest {
    private val tools = GameToolDelegate()
    private val text: (String, String, String, String, String) -> String = { first, _, _, _, _ -> first }
    private fun tile(id: Int, type: Int) = Tile(id.toString(), type, id.toFloat(), 0f, 0)

    @Test fun hintRequiresAnImmediatelyPlayableMatchAndSpaceForIt() {
        val locked = tile(1, 1).copy(sealedCount = 1)
        val board = listOf(locked, tile(2, 1), tile(3, 1), tile(4, 2), tile(5, 2), tile(6, 2))
        var state = GameViewState(boardTiles = board, hintCount = 2)
        tools.handleUseHint(state, text, { state = state.it() }, {}, {})
        assertEquals(setOf("4", "5", "6"), state.highlightedTileIds)
        state = state.copy(slotTiles = List(6) { tile(20 + it, 3 + it) }, highlightedTileIds = emptySet())
        tools.handleUseHint(state, text, { state = state.it() }, {}, {})
        assertTrue(state.highlightedTileIds.isEmpty())
        assertEquals(1, state.hintCount)
    }

    @Test fun levelTwoHintsHighlightAnAvailableTripleAcrossOneHundredOpeningSeeds() {
        val generator = GameLevelGenerator()
        var checkedMatches = 0
        for (seed in 1L..100L) {
            val board = GameEngine.calculateBlockedStates(generator.generateSolvableLevelLocal(2, seed, 0))
            val available = board.filter { it.state == TileState.NORMAL && it.sealedCount == 0 }
            var state = GameViewState(boardTiles = board, hintCount = 1)
            tools.handleUseHint(state, text, { state = state.it() }, {}, {})
            if (available.groupBy { it.type }.any { it.value.size >= 3 }) {
                val highlighted = available.filter { it.id in state.highlightedTileIds }
                assertEquals("seed $seed", 3, highlighted.size)
                assertEquals("seed $seed", 1, highlighted.distinctBy { it.type }.size)
                assertEquals(0, state.hintCount)
                checkedMatches++
            } else {
                assertEquals(1, state.hintCount)
                assertTrue(state.highlightedTileIds.isEmpty())
            }
        }
        assertTrue(checkedMatches > 0)
    }

    @Test fun jokerFollowsTheSameScoringAndSealThresholdAsAManualTriple() {
        val board = listOf(tile(3, 2), tile(4, 8).copy(sealedCount = 1), tile(5, 6))
        var state = GameViewState(boardTiles = board, slotTiles = listOf(tile(1, 2), tile(2, 2)),
            jokerCount = 1, sealedOrder = listOf("4"), isDoublePointsActive = true)
        tools.handleUseJoker(state, text, { state = state.it() }, {}, {}, {}) {
            GameLogicDelegate().processSlotMatchAndCheckEndGame(state, state.boardTiles, state.slotTiles,
                state.movedOutTiles, state.score, state.isDoublePointsActive, { state = state.it() }, {}, {})
        }
        assertEquals(3, state.sealedClearCount)
        assertEquals(setOf("4"), state.sealedUnlockedIds)
        assertEquals(200, state.score)
        assertEquals(0, state.jokerCount)
    }

    @Test fun undoFromLegacySavedPositionRestoresTheScoreBeforeTheTriple() {
        val previous = Json.decodeFromString<StagePosition>(
            """{"boardTiles":[],"slotTiles":[],"movedOutTiles":[],"sealedClearCount":3,"sealedUnlockedIds":[]}"""
        )
        var state = GameViewState(undoCount = 1, score = 200, sealedClearCount = 6)
        tools.handleUseUndo(state, mutableListOf(previous), { state = state.it() }, {}, {})
        assertEquals(100, state.score)
    }

    @Test fun legacyUndoHandlesAMultiplierEnabledAfterTheMatch() {
        val previous = StagePosition(emptyList(), emptyList(), emptyList(), 3, emptySet())
        var state = GameViewState(undoCount = 1, score = 200, sealedClearCount = 6, isDoublePointsActive = true)
        tools.handleUseUndo(state, mutableListOf(previous), { state = state.it() }, {}, {})
        assertEquals(100, state.score)
    }

    @Test fun undoRecognizesAnOldJokerMatchThatNeverAdvancedTheSealCount() {
        val previous = StagePosition(listOf(tile(3, 1)), listOf(tile(1, 1), tile(2, 1)), emptyList(), 0, emptySet())
        var state = GameViewState(undoCount = 1, score = 100,
            boardTiles = listOf(tile(3, 1).copy(state = TileState.IN_SLOT)))
        tools.handleUseUndo(state, mutableListOf(previous), { state = state.it() }, {}, {})
        assertEquals(0, state.score)
    }

    @Test fun exactSavedScoreTakesPrecedenceOverTheCurrentMultiplier() {
        val before = StagePosition(emptyList(), emptyList(), emptyList(), 3, emptySet(), score = 100)
        val restored = Json.decodeFromString<StagePosition>(Json.encodeToString(before))
        var state = GameViewState(undoCount = 1, score = 200, sealedClearCount = 6, isDoublePointsActive = true)
        tools.handleUseUndo(state, mutableListOf(restored), { state = state.it() }, {}, {})
        assertEquals(100, state.score)
    }

    @Test fun partiallyPublishedSkinKeepsLaterFacesInTheirAssignedPositions() {
        val item = Json.decodeFromString<ShopItem>(
            """{"id":1,"name":"skin","item_type":"SKIN_TEST","points_price":50,"stock":1,"tiles":[null,"second",null]}"""
        )
        assertEquals(listOf(null, "second", null), item.tiles)
    }
}
