package com.example.sheeps.game

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sheeps.core.game.GameEngine.calculateBlockedStates
import com.example.sheeps.data.game.GameLevelGenerator
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.state.BoardBounds
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.game.ui.components.GameBoardSurfaceView
import com.example.sheeps.game.viewmodel.delegates.GameLogicDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.ceil

@RunWith(AndroidJUnit4::class)
class FirstStagePlayTest {
    @Test
    fun fourTriplesRevealLowerTilesAndClearTheLocalTutorial() = runBlocking {
        val generator = GameLevelGenerator()
        val tiles = generator.generateSolvableLevelLocal(1, 1234L, 42)
        assertEquals(tiles, generator.generateSolvableLevelLocal(1, 900001L, 17))
        assertEquals(12, tiles.size)
        assertEquals(3, tiles.distinctBy { it.type }.size)
        assertTrue(tiles.none { it.isBlind || it.sealedCount != 0 })

        val game = GameLogicDelegate()
        val bounds = BoardBounds(tiles.minOf { it.x }, tiles.maxOf { it.x }, tiles.minOf { it.y }, tiles.maxOf { it.y })
        var state = GameViewState(boardTiles = calculateBlockedStates(tiles), boardBounds = bounds, gameStatus = GameStatus.PLAYING)
        val initiallyBlocked = state.boardTiles.filter { it.state == TileState.BLOCKED }.map { it.id }.toSet()
        val opening = state.boardTiles.filter { it.state == TileState.NORMAL }.groupBy { it.type }
        assertTrue(opening.size >= 2 && opening.values.any { it.size >= 3 })
        var revealed = false
        var victories = 0
        val update: (GameViewState.() -> GameViewState) -> Unit = { change -> state = state.change() }

        withContext(Dispatchers.Main) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val board = GameBoardSurfaceView(context)
            val spacing = 48f * context.resources.displayMetrics.density
            // Give the native board exactly its unscaled content area. Touch each exposed face at its center.
            board.layout(0, 0, ceil((bounds.maxX - bounds.minX + 1) * spacing).toInt(),
                ceil((bounds.maxY - bounds.minY + 1) * spacing).toInt())
            repeat(4) { round ->
                val visible = state.boardTiles.filter { it.state == TileState.NORMAL }
                val triple = visible.groupBy { it.type }.values.firstOrNull { it.size >= 3 }
                    ?: throw AssertionError("No exposed triple at round ${round + 1}: ${visible.map { it.type }}")
                for (tile in triple.take(3)) {
                    var hit: Tile? = null
                    board.updateData(state, emptySet(), { hit = it }, mutableMapOf())
                    val now = SystemClock.uptimeMillis()
                    val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN,
                        (tile.x - bounds.minX + 0.5f) * spacing, (tile.y - bounds.minY + 0.5f) * spacing, 0)
                    try {
                        assertTrue(board.dispatchTouchEvent(event))
                    } finally {
                        event.recycle()
                    }
                    assertEquals(tile.id, hit?.id)
                    game.handleClickTile(requireNotNull(hit), state, {}, update, {}) {
                        game.processSlotMatchAndCheckEndGame(
                            state, state.boardTiles, state.slotTiles, state.movedOutTiles, state.score,
                            false, update, {}, { victories++ }
                        )
                    }
                    revealed = revealed || state.boardTiles.any { it.id in initiallyBlocked && it.state == TileState.NORMAL }
                }
                assertEquals(0, state.slotTiles.size)
                assertEquals((round + 1) * 100, state.score)
            }
        }
        assertTrue("A legal click should uncover a lower tile", revealed)
        assertEquals(1, victories)
        assertEquals(GameStatus.WON, state.gameStatus)
        assertTrue(state.boardTiles.none { it.state == TileState.NORMAL || it.state == TileState.BLOCKED })
    }
}
