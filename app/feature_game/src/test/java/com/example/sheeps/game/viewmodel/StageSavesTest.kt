package com.example.sheeps.game.viewmodel

import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.data.repository.StageSaveSession
import com.example.sheeps.data.repository.StageSaves
import com.example.sheeps.game.state.elapsedAt
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.game.state.GameViewState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class StageSavesTest {
    @Test fun fullBoardAndUndoFitInARoomRowAndSurviveANewProcess() = runBlocking {
        val dao = InMemoryStageSaveDao()
        val store = StageSaves(dao, Json)
        val session = StageSaveSession("alice", "run-68", 68)
        val board = (0 until 300).map {
            Tile("tile-$it", it % 12 + 1, (it % 10) * 0.5f, (it / 10) * 0.5f, it % 4,
                isBlind = it % 2 == 0, sealedCount = if (it % 5 == 0) 1 else 0)
        }
        val state = GameViewState(boardTiles = board, slotTiles = board.take(3).map { it.copy(state = TileState.IN_SLOT) },
            movedOutTiles = board.drop(3).take(3).map { it.copy(state = TileState.MOVED_OUT) }, undoCount = 300,
            moveOutCount = 2, reviveCount = 1, hintCount = 1, jokerCount = 2, bombCount = 1, shuffleCount = 1,
            score = 2400, gameStatus = GameStatus.LOST, isDoublePointsActive = true, sealedClearCount = 12,
            sealedOrder = board.filter { it.sealedCount > 0 }.map { it.id }, highlightedTileIds = setOf("tile-2"),
            sealedUnlockedIds = setOf("tile-5"))
        val base = state.snapshot(emptyList(), 3, 54321)
        val history = board.indices.map { click ->
            base.position.copy(boardTiles = board.mapIndexed { idx, tile ->
                if (idx < click) tile.copy(state = TileState.IN_SLOT) else tile
            })
        }
        val snapshot = base.copy(history = history)
        store.begin(session, { _, _ -> fail("No prior game") }) { snapshot }
        assertTrue("SQLite cursor window", dao.read("alice", "run-68")!!.payload.size < 2_000_000)
        val reopened = StageSaves(dao, Json)
        val recovered = reopened.restore(StageSaveSession("alice", "run-68", 68))!!
        assertEquals(snapshot, recovered)
        val restored = GameViewState().restore(recovered, 68, "skin")
        assertEquals(GameStatus.LOST, restored.gameStatus)
        assertEquals(snapshot, restored.snapshot(recovered.history, recovered.itemsUsed, recovered.elapsedMs))
        assertNull(reopened.restore(StageSaveSession("bob", "run-68", 68)))
    }

    @Test fun elapsedTimeUsesOnlyTheAccumulatedMillisAndCurrentMonotonicSegment() {
        val playing = GameViewState(elapsedMs = 3421, activeSinceNanos = 19_000_000_000L,
            levelStartTimestamp = Long.MAX_VALUE)
        assertEquals(6421L, playing.elapsedAt(22_000_000_000L))
        val paused = playing.copy(activeSinceNanos = null, elapsedMs = 6421L)
        assertEquals(6421L, paused.elapsedAt(99_000_000_000L))
    }

    @Test fun oldActivityCannotOverwriteRestoredBoardOrExposeSettlingGame() = runBlocking {
        val dao = InMemoryStageSaveDao()
        val store = StageSaves(dao, Json)
        val oldActivity = StageSaveSession("alice", "run", 1)
        val latestActivity = StageSaveSession("alice", "run", 1)
        val opening = initialSnapshot(listOf(Tile("a", 2, 0f, 0f, 0)), emptyMap())
        store.begin(oldActivity, { _, _ -> fail() }) { opening }
        store.restore(latestActivity)
        store.checkpoint(latestActivity, opening.copy(score = 100)) { fail() }
        store.checkpoint(oldActivity, opening) { fail() }
        assertEquals(100, store.restore(latestActivity)!!.score)
        store.settle(latestActivity, { dao.discard("alice", "run") }, {}, { fail() })
        assertNull(store.latest("alice"))
        assertNull(store.restore(StageSaveSession("alice", "run", 1)))
    }
}
