package com.example.sheeps.game.audio

import com.example.sheeps.data.model.Tile
import com.example.sheeps.game.state.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicBoardTest {
    @Test fun stageAndDuelRequireAPlayableLoadedBoardAndStopAtSettlement() {
        val tiles = listOf(Tile("a", 1, 0f, 0f, 0))
        val stage = GameViewState(gameStatus = GameStatus.PLAYING, boardTiles = tiles)
        val duel = DuelViewState(gameStatus = GameStatus.PLAYING, boardTiles = tiles)
        assertTrue(stage.hasMusicBoard())
        assertTrue(duel.hasMusicBoard())
        assertFalse(stage.copy(isLoading = true).hasMusicBoard())
        assertFalse(stage.copy(showCarrySelection = true).hasMusicBoard())
        assertFalse(duel.copy(boardTiles = emptyList()).hasMusicBoard())
        for (status in listOf(GameStatus.INIT, GameStatus.WON, GameStatus.LOST)) {
            assertFalse(stage.copy(gameStatus = status).hasMusicBoard())
            assertFalse(duel.copy(gameStatus = status).hasMusicBoard())
        }
    }

    @Test fun endlessMusicTracksStartPauseResumeAndGameOver() {
        for (status in EndlessStatus.entries) {
            val board = EndlessViewState(status = status)
            if (status == EndlessStatus.PLAYING) assertTrue(board.hasMusicBoard())
            else assertFalse(board.hasMusicBoard())
            assertFalse(board.copy(isLoading = true).hasMusicBoard())
        }
    }
}
