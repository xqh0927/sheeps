package com.example.sheeps.game.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.sheeps.core.multiplayer.WebSocketManager
import com.example.sheeps.core.multiplayer.model.GameCommand
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.repository.GameRepository
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.game.state.DuelViewIntent
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.game.viewmodel.delegates.DuelActionDelegate
import com.example.sheeps.game.viewmodel.delegates.DuelCommandHandler
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DuelSettlementTest {
    @Test fun fullTraySettlesBothPeersOnceAndRematchingOnTheLastSlotStillWorks() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val board = (0 until 24).map { Tile("tile-$it", it % 8 + 1, (it % 6)*2f, (it / 6)*2f, 0) }
        val repo = mockk<GameRepository>()
        val delayedOpponentBoard = CompletableDeferred<Unit>()
        var loads = 0
        coEvery { repo.getLevel(any(), any()) } coAnswers {
            if (++loads == 2) delayedOpponentBoard.await()
            ApiResult.Success(board)
        }
        val sent = mutableListOf<GameCommand>()
        val leftIn = MutableSharedFlow<GameCommand>(extraBufferCapacity = 16)
        val rightIn = MutableSharedFlow<GameCommand>(extraBufferCapacity = 16)
        fun peer(id: String, incoming: MutableSharedFlow<GameCommand>, outgoing: MutableSharedFlow<GameCommand>): DuelViewModel {
            val ws = mockk<WebSocketManager>(relaxed = true)
            every { ws.connectionState } returns MutableStateFlow(WebSocketManager.ConnectionState.Connected)
            every { ws.messageFlow } returns incoming
            every { ws.sendCommand(any()) } answers { val cmd = firstArg<GameCommand>(); sent.add(cmd); outgoing.tryEmit(cmd) }
            return DuelViewModel(repo, mockk(relaxed = true), ws, Json, mockk(relaxed = true),
                mockk(), DuelActionDelegate(), DuelCommandHandler()).also {
                it.handleIntent(DuelViewIntent.Init("match", id, 9, 42))
            }
        }
        val left = peer("left", leftIn, rightIn)
        val right = peer("right", rightIn, leftIn)
        try {
            runCurrent()
            fun click(index: Int) { left.handleIntent(DuelViewIntent.ClickTile(board[index])); runCurrent() }
            // Six occupied slots; putting a third matching face into slot seven clears the triplet.
            for (i in listOf(0, 8, 1, 2, 3, 4, 16)) click(i)
            assertEquals(4, left.viewState.value.slotTiles.size)
            assertEquals(GameStatus.PLAYING, left.viewState.value.gameStatus)
            assertTrue(sent.none { it.payload.systemMessage != null })
            for (i in listOf(5, 6, 7)) click(i)
            assertEquals(GameStatus.LOST, left.viewState.value.gameStatus)
            assertEquals(GameStatus.WON, right.viewState.value.gameStatus)
            delayedOpponentBoard.complete(Unit)
            runCurrent()
            assertEquals(GameStatus.WON, right.viewState.value.gameStatus)
            // Replay of the settled result and another tap cannot create a second result.
            val loss = sent.single { it.payload.systemMessage == "LOST" }
            rightIn.emit(loss)
            click(9)
            left.handleIntent(DuelViewIntent.Leave)
            runCurrent()
            assertEquals("right", right.viewState.value.winnerId)
            assertEquals(listOf("LOST"), sent.mapNotNull { it.payload.systemMessage })
        } finally {
            left.viewModelScope.cancel(); right.viewModelScope.cancel(); Dispatchers.resetMain()
        }
    }
}
