package com.example.sheeps.game.audio

import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import com.example.sheeps.data.model.Tile
import com.example.sheeps.game.state.*
import com.example.sheeps.game.viewmodel.EndlessViewModel
import com.example.sheeps.game.viewmodel.delegates.DuelActionDelegate
import com.example.sheeps.game.viewmodel.delegates.GameLogicDelegate
import com.example.sheeps.game.viewmodel.delegates.GameToolDelegate
import com.example.sheeps.game.viewmodel.helpers.EndlessLevelGenerator
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchEventsTest {
    private fun tray(vararg types: Int) = types.mapIndexed { i, type -> Tile("t$i", type, i.toFloat(), 0f, 0) }

    @Test fun normalSoundsOnceForEachRemovedGroupAndNeverForScoreMultiplierOrUnmatchedTiles() = runTest {
        val cases = listOf(
            tray(5, 5) to emptyList(),
            tray(7, 7, 7, 1) to listOf(7),
            tray(4, 9, 4, 9, 4, 9) to listOf(4, 9),
            tray(3, 3, 3, 3, 3, 3) to listOf(3, 3)
        ) + (1..12).map { type -> tray(type, type, type) to listOf(type) }
        for ((slot, expected) in cases) {
            val played = mutableListOf<Int>()
            val score = GameLogicDelegate().processSlotMatchAndCheckEndGame(
                GameViewState(), emptyList(), slot, emptyList(), 0, true, {},
                { if (it is GameViewEffect.TileMatch) played.add(it.tileType) }, {}
            )
            assertEquals(expected.size * 200, score)
            assertEquals(expected, played)
        }
    }

    @Test fun duelSoundsOnlyOnActualLocalTriple() = runTest {
        var state = DuelViewState()
        val played = mutableListOf<Int>()
        var confirmed = 0
        for (slot in listOf(tray(2, 2), tray(8, 8, 8, 1))) {
            DuelActionDelegate().processSlotMatch(state, emptyList(), slot, emptyList(), { state = state.it() },
                { if (it is DuelViewEffect.TileMatch) played.add(it.tileType) },
                { confirmed++ }, {})
            assertEquals(confirmed, played.size)
        }
        assertEquals(listOf(8), played)
    }

    @Test fun aSuccessfulDuelSpellIsNotATileMatch() {
        var state = DuelViewState(gameStatus = GameStatus.PLAYING, currentEnergy = 5)
        var cast = false
        val effects = mutableListOf<DuelViewEffect>()
        DuelActionDelegate().handleCastSpell(state, "FOG", { zh, _, _, _, _ -> zh }, { state = state.it() },
            effects::add, { cast = true })
        assertTrue(cast)
        assertFalse(effects.any { it is DuelViewEffect.TileMatch })
    }

    @Test fun jokerCompletesOneMatchWhileABombDoesNot() = runTest {
        val tools = GameToolDelegate()
        var state = GameViewState(slotTiles = tray(6, 9), jokerCount = 1, bombCount = 1)
        val played = mutableListOf<Int>()
        val text: (String, String, String, String, String) -> String = { first, _, _, _, _ -> first }
        val sound: (GameViewEffect) -> Unit = { if (it is GameViewEffect.TileMatch) played.add(it.tileType) }
        tools.handleUseJoker(state, text, { state = state.it() }, sound, {}, {}, {})
        assertTrue(played.isEmpty())
        assertEquals(1, state.jokerCount)
        state = state.copy(slotTiles = tray(6, 6, 9, 9, 1))
        tools.handleUseJoker(state, text, { state = state.it() }, sound, {}, {}) {
            GameLogicDelegate().processSlotMatchAndCheckEndGame(state, state.boardTiles, state.slotTiles,
                state.movedOutTiles, state.score, state.isDoublePointsActive, { state = state.it() }, sound, {})
        }
        assertEquals(listOf(9), played)
        assertEquals(listOf(6, 6, 1), state.slotTiles.map { it.type })
        tools.handleUseBomb(state, text, { state = state.it() }, sound, {}, {})
        assertEquals(1, state.slotTiles.size)
        assertEquals(listOf(9), played)
    }

    @Test fun endlessSoundsAfterTheThirdTileArrivesNotOnEarlierClicks() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        mockkStatic(SystemClock::class)
        every { SystemClock.uptimeMillis() } answers { testScheduler.currentTime }
        val row = tray(7, 7, 7, 2, 2, 2)
        val generator = mockk<EndlessLevelGenerator>(relaxed = true)
        every { generator.nextRow(any(), any(), any()) } returns row
        val vm = EndlessViewModel(mockk(relaxed = true), mockk(relaxed = true), generator, mockk(relaxed = true), mockk(relaxed = true))
        val played = mutableListOf<Int>()
        val collect = backgroundScope.launch { vm.viewEffect.collect {
            if (it is EndlessViewEffect.TileMatch) played.add(it.tileType)
        } }
        try {
            vm.handleIntent(EndlessViewIntent.Init(true, 42))
            advanceTimeBy(3250)
            fun click(column: Int) = vm.handleIntent(
                EndlessViewIntent.ClickColumn(column, vm.viewState.value.columns[column].last().id)
            )
            click(3)
            advanceTimeBy(201)
            assertTrue(played.isEmpty())
            // The type-2 click resolves first, but the group actually removed from the tray is type 7.
            click(4)
            for (column in 0..2) click(column)
            advanceTimeBy(201)
            assertEquals(listOf(7), played)
            click(5)
            advanceTimeBy(600)
            assertEquals(listOf(7, 2), played)
        } finally {
            collect.cancel()
            vm.viewModelScope.cancel()
            unmockkStatic(SystemClock::class)
            Dispatchers.resetMain()
        }
    }
}
