package com.example.sheeps.game.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.repository.SyncRepository
import com.example.sheeps.game.state.GameViewEffect
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.game.state.GameViewIntent
import com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors
import kotlin.time.TimeSource

@OptIn(ExperimentalCoroutinesApi::class)
class LevelOpeningTest {
    private val main = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val dao = mockk<LocalDao>(relaxed = true)
    private val api = mockk<ApiService>()
    private val sync = mockk<SyncRepository>()
    private val prefs = mockk<UserPreferences>(relaxed = true)
    private val generator = spyk(GameLevelGenerator())
    private lateinit var game: GameViewModel
    private val custom = listOf(Tile("custom", 2, 0f, 0f, 0))

    @Before fun setup() {
        Dispatchers.setMain(main)
        every { prefs.isPrivacyAccepted() } returns true
        every { prefs.getUserId() } returns "device"
        every { prefs.accountId() } returns "account"
        coEvery { dao.consumeForRun(any(), any(), any(), any()) } returns mapOf("UNDO" to 1)
        coEvery { sync.syncDirtyData() } returns Unit
        runBlocking(main) {
            game = GameViewModel(SavedStateHandle(), api, prefs, Json, dao, sync, generator,
                mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        }
    }

    @After fun tearDown() {
        game.viewModelScope.cancel()
        Dispatchers.resetMain()
        main.close()
    }

    private suspend fun open() = withContext(main) { game.openLevel(68, """{"UNDO":1}""") }
    private suspend fun ready(level: Int) = game.viewState.first {
        it.gameStatus == GameStatus.PLAYING && !it.isLoading && it.currentLevelId == level
    }

    @Test fun customLevel68OpensWhileCloudSyncIsStillPending() = runBlocking {
        val cloud = CompletableDeferred<Unit>()
        val syncStarted = CompletableDeferred<Unit>()
        coEvery { sync.syncDirtyData() } coAnswers { syncStarted.complete(Unit); cloud.await() }
        coEvery { api.getLevel(68, any()) } coAnswers { delay(120); custom }
        val start = TimeSource.Monotonic.markNow()
        try {
            open()
            open() // repeated Activity delivery for the same run
            val state = withTimeout(1500) { ready(68) }
            println("level 68 custom board ready in ${start.elapsedNow()}, sync held")
            assertEquals(custom.map { it.id }, state.boardTiles.map { it.id })
            assertEquals(1, state.undoCount)
            withTimeout(500) { syncStarted.await() }
            assertFalse(cloud.isCompleted)
            coVerify(exactly = 1) { dao.consumeForRun(any(), any(), any(), any()) }
        } finally { cloud.complete(Unit) }
    }

    @Test fun level68FallsBackOnBudgetWithTheSameSeed() = runBlocking {
        val seed = slot<Int>()
        val cancelled = CompletableDeferred<Unit>()
        coEvery { api.getLevel(68, capture(seed)) } coAnswers {
            try { delay(12000); custom } finally { cancelled.complete(Unit) }
        }
        val start = TimeSource.Monotonic.markNow()
        open()
        val state = withTimeout(4500) { ready(68) }
        println("level 68 local board ready in ${start.elapsedNow()} (${state.boardTiles.size} tiles)")
        assertTrue(seed.captured in 1..1_000_000)
        assertTrue(state.boardTiles.isNotEmpty())
        verify(exactly = 1) { generator.generateSolvableLevelLocal(68, seed.captured.toLong(), any()) }
        assertTrue(cancelled.isCompleted)
        coVerify(exactly = 1) { dao.consumeForRun(any(), any(), any(), any()) }
    }

    @Test fun leavingWhileLevel68IsPendingDoesNotInstallTheOldBoard() = runBlocking {
        val waiting = CompletableDeferred<Unit>()
        val response = CompletableDeferred<List<Tile>>()
        val cancelled = CompletableDeferred<Unit>()
        coEvery { api.getLevel(68, any()) } coAnswers {
            waiting.complete(Unit)
            try { response.await() } finally { cancelled.complete(Unit) }
        }
        try {
            open()
            withTimeout(1500) { waiting.await() }
            withContext(main) { game.handleIntent(GameViewIntent.GoBackToMenu) }
            withTimeout(800) { cancelled.await() }
            response.complete(custom)
            assertFalse(game.viewState.value.isLoading)
            assertEquals(GameStatus.MENU, game.viewState.value.gameStatus)
            coVerify(exactly = 0) { dao.consumeForRun(any(), any(), any(), any()) }
        } finally { response.complete(custom) }
    }
    @Test fun newLevelSupersedesALateLevel68Response() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val late = CompletableDeferred<Unit>()
        val oldReturned = CompletableDeferred<Unit>()
        coEvery { api.getLevel(68, any()) } coAnswers {
            started.complete(Unit)
            // Simulate an already delivered result whose callback still runs after cancellation.
            withContext(NonCancellable) { late.await() }
            oldReturned.complete(Unit)
            custom
        }
        coEvery { api.getLevel(69, any()) } returns listOf(custom.single().copy(id = "next"))
        try {
            open()
            withTimeout(1500) { started.await() }
            withContext(main) { game.handleIntent(GameViewIntent.LoadLevel(69)) }
            val next = withTimeout(1500) { ready(69) }
            assertEquals("next", next.boardTiles.single().id)
            late.complete(Unit)
            withTimeout(1000) { oldReturned.await() }
            withContext(main) { yield() }
            assertEquals("next", game.viewState.value.boardTiles.single().id)
            coVerify(exactly = 1) { dao.consumeForRun(any(), any(), any(), any()) }
        } finally { late.complete(Unit) }
    }

    @Test fun transportFailureUsesLocalImmediately() = runBlocking {
        val seed = slot<Int>()
        coEvery { api.getLevel(68, capture(seed)) } throws java.io.IOException("offline")
        val start = TimeSource.Monotonic.markNow()
        open()
        val state = withTimeout(1500) { ready(68) }
        println("level 68 immediate failure to playable local board: ${start.elapsedNow()}")
        assertTrue(state.boardTiles.isNotEmpty())
        verify { generator.generateSolvableLevelLocal(68, seed.captured.toLong(), any()) }
    }

    @Test fun failedLocalAllocationEndsLoadingWithoutStartingAnUnfundedGame() = runBlocking {
        coEvery { api.getLevel(68, any()) } returns custom
        coEvery { dao.consumeForRun(any(), any(), any(), any()) } throws IllegalStateException("storage unavailable")
        open()
        withTimeout(1500) { game.viewEffect.first { it is GameViewEffect.LevelLoadFailed } }
        assertFalse(game.viewState.value.isLoading)
        assertNotEquals(GameStatus.PLAYING, game.viewState.value.gameStatus)
        coVerify(exactly = 0) { sync.syncDirtyData() }
    }

    @Test fun confirmingRestartAtLevel68CancelsThePendingOpening() = runBlocking {
        val firstStarted = CompletableDeferred<Unit>()
        val firstCancelled = CompletableDeferred<Unit>()
        coEvery { api.getLevel(68, any()) } coAnswers {
            if (!firstStarted.complete(Unit)) return@coAnswers custom
            try { awaitCancellation() } finally { firstCancelled.complete(Unit) }
        }
        coEvery { dao.getAllItems() } returns emptyList()
        open()
        withTimeout(1500) { firstStarted.await() }
        withContext(main) { game.handleIntent(GameViewIntent.RestartLevel) }
        withTimeout(1000) { game.viewState.first { it.showCarrySelection } }
        withContext(main) {
            game.handleIntent(GameViewIntent.ConfirmRestartWithCarry)
            game.handleIntent(GameViewIntent.ConfirmRestartWithCarry)
        }
        val state = withTimeout(1500) { ready(68) }
        assertEquals("custom", state.boardTiles.single().id)
        assertTrue(firstCancelled.isCompleted)
        coVerify(exactly = 2) { api.getLevel(68, any()) }
        coVerify(exactly = 1) { dao.consumeForRun(any(), any(), any(), any()) }
    }

}
