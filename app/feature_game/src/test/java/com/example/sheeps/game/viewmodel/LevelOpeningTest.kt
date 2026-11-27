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
import com.example.sheeps.game.viewmodel.delegates.ScoreDelegate
import com.example.sheeps.data.game.GameLevelGenerator
import com.example.sheeps.data.repository.StageBoards
import com.example.sheeps.data.repository.StageSaves
import com.example.sheeps.data.repository.ResumableStage
import com.example.sheeps.data.local.StageSaveDao
import com.example.sheeps.data.local.StageSaveEntity
import com.example.sheeps.game.viewmodel.delegates.GameLogicDelegate
import com.example.sheeps.game.viewmodel.delegates.GameToolDelegate
import retrofit2.Response
import org.junit.Rule
import org.junit.rules.TemporaryFolder
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
    @get:Rule val temp = TemporaryFolder()
    private val main = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val dao = mockk<LocalDao>(relaxed = true)
    private val api = mockk<ApiService>()
    private val sync = mockk<SyncRepository>()
    private val prefs = mockk<UserPreferences>(relaxed = true)
    private val scores = mockk<ScoreDelegate>(relaxed = true)
    private val saveDao = InMemoryStageSaveDao()
    private val saves = StageSaves(saveDao, Json)
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
            game = GameViewModel(SavedStateHandle(), api, prefs, Json, dao, sync, StageBoards(api, Json, generator, temp.root),
                GameLogicDelegate(), GameToolDelegate(), scores, saves)
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

    @Test fun resumingAfterReturningToMenuRestoresTheRackAndUndoWithoutAnotherAllocation() = runBlocking {
        val row = (0..5).map { Tile("t$it", it / 3 + 1, it * 2f, 0f, 0) }
        val runId = slot<String>()
        coEvery { api.getStageBoard(68, any()) } returns Response.success(row)
        coEvery { dao.consumeForRun(capture(runId), any(), any(), any()) } returns mapOf("UNDO" to 1, "MOVEOUT" to 1)
        open()
        ready(68)
        withContext(main) { game.handleIntent(GameViewIntent.ClickTile(row[0])) }
        withContext(main) {
            game.handleIntent(GameViewIntent.UseMoveOut)
            game.handleIntent(GameViewIntent.GoBackToMenu)
        }
        val restored = withContext(main) {
            GameViewModel(SavedStateHandle(mapOf("resumeRunId" to runId.captured)), api, prefs, Json, dao, sync,
                StageBoards(api, Json, generator, temp.root), GameLogicDelegate(),
                GameToolDelegate(), scores, saves).also { it.openLevel(68, null) }
        }
        try {
            val resumed = withTimeout(2000) { restored.viewState.first { it.gameStatus == GameStatus.PLAYING } }
            assertEquals(listOf("t0"), resumed.movedOutTiles.map { it.id })
            assertEquals(0, resumed.moveOutCount)
            withContext(main) { restored.handleIntent(GameViewIntent.UseUndo) }
            assertEquals(0, restored.viewState.value.undoCount)
            assertTrue(restored.viewState.value.movedOutTiles.isEmpty())
            assertEquals(6, restored.viewState.value.boardTiles.count { it.state == com.example.sheeps.data.model.TileState.NORMAL })
            coVerify(exactly = 1) { dao.consumeForRun(any(), any(), any(), any()) }
            coVerify(exactly = 1) { dao.enqueueStageStart(any()) }
        } finally { restored.viewModelScope.cancel() }
    }

    @Test fun rejectedResultReopensPriorMoveAndTheRetriedWinCommitsOnce() = runBlocking {
        val records = mockk<com.example.sheeps.data.repository.StageScoreRepository>(relaxed = true)
        val delegate = spyk(ScoreDelegate(records, mockk(relaxed = true)))
        every { delegate.scheduleUpload(any(), any()) } just Runs
        val attempts = java.util.concurrent.atomic.AtomicInteger()
        coEvery { records.record(any()) } coAnswers {
            if (attempts.incrementAndGet() == 1) throw java.io.IOException("storage unavailable")
            val result = firstArg<com.example.sheeps.data.local.PendingStageScore>()
            saveDao.discard(result.ownerId, result.runId)
        }
        val row = (0..2).map { Tile("t$it", 1, it * 2f, 0f, 0) }
        coEvery { api.getStageBoard(68, any()) } returns Response.success(row)
        game.viewModelScope.cancel()
        withContext(main) {
            game = GameViewModel(SavedStateHandle(), api, prefs, Json, dao, sync, StageBoards(api, Json, generator, temp.root),
                GameLogicDelegate(), GameToolDelegate(), delegate, saves)
        }
        open()
        ready(68)
        val id = saves.latest("account")!!.id
        withContext(main) { row.forEach { game.handleIntent(GameViewIntent.ClickTile(it)) } }
        val recovered = withTimeout(1500) { game.viewState.first { it.gameStatus == GameStatus.PLAYING && it.slotTiles.size == 2 } }
        assertEquals(listOf("t0", "t1"), recovered.slotTiles.map { it.id })
        assertEquals(id, saves.latest("account")!!.id)
        withContext(main) { repeat(2) { game.handleIntent(GameViewIntent.ClickTile(row.last())) } }
        withTimeout(1500) { while (attempts.get() < 2) delay(5) }
        assertEquals(GameStatus.WON, game.viewState.value.gameStatus)
        assertNull(saves.latest("account"))
        coVerify(exactly = 2) { records.record(match { it.runId == id && it.won }) }
    }

    @Test fun lateResultFailureDoesNotInstallOldBoardOverNewOpening() = runBlocking {
        val writing = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        coEvery { scores.record(any()) } coAnswers {
            writing.complete(Unit)
            release.await()
            throw java.io.IOException("old result failed")
        }
        coEvery { api.getStageBoard(68, any()) } returns Response.success(custom)
        coEvery { api.getStageBoard(69, any()) } returns Response.success(listOf(custom.single().copy(id = "new")))
        open()
        ready(68)
        withContext(main) { game.handleIntent(GameViewIntent.ClickTile(custom.single())) }
        withTimeout(1500) { writing.await() }
        try {
            withContext(main) { game.handleIntent(GameViewIntent.LoadLevel(69)) }
        } finally { release.complete(Unit) }
        assertEquals("new", withTimeout(1500) { ready(69) }.boardTiles.single().id)
        assertEquals(69, saves.latest("account")?.level)
    }

    @Test fun missingExplicitResumeFailsWithoutOpeningABoardOrChargingItems() = runBlocking {
        withContext(main) { game.openLevel(68, """{"UNDO":1}""", "expired-run") }
        withTimeout(1500) { game.viewEffect.first { it is GameViewEffect.LevelLoadFailed } }
        coVerify(exactly = 0) { api.getStageBoard(any(), any()) }
        coVerify(exactly = 0) { dao.consumeForRun(any(), any(), any(), any()) }
        coVerify(exactly = 0) { dao.enqueueStageStart(any()) }
    }

    @Test fun tripleSettlesBeforeBackgroundCheckpointAndTimerFreezes() = runBlocking {
        val row = (0..5).map { Tile("t$it", it / 3 + 1, it * 2f, 0f, 0) }
        coEvery { api.getStageBoard(68, any()) } returns Response.success(row)
        open()
        ready(68)
        withContext(main) {
            row.take(3).forEach { game.handleIntent(GameViewIntent.ClickTile(it)) }
            game.pausePlay()
        }
        val sessionId = saves.latest("account")!!.id
        val freshProcess = StageSaves(saveDao, Json)
        val paused = freshProcess.restore(com.example.sheeps.data.repository.StageSaveSession("account", sessionId, 68))!!
        assertTrue(paused.position.slotTiles.isEmpty())
        assertEquals(100, paused.score)
        assertEquals(3, paused.position.sealedClearCount)
        delay(60)
        withContext(main) { game.pausePlay() }
        saves.latest("account")
        val stillPaused = freshProcess.restore(com.example.sheeps.data.repository.StageSaveSession("account", sessionId, 68))!!
        assertEquals(paused.elapsedMs, stillPaused.elapsedMs)
        withContext(main) {
            game.resumePlay()
            row.drop(3).forEach { game.handleIntent(GameViewIntent.ClickTile(it)) }
            game.handleIntent(GameViewIntent.ClickTile(row.last()))
        }
        assertEquals(GameStatus.WON, game.viewState.value.gameStatus)
        assertNull(saves.latest("account"))
        val displayedTime = game.viewState.value.elapsedMs
        coVerify(exactly = 1) { scores.record(match { it.runId == sessionId && it.levelId == 68 && it.won && it.itemsUsed == 0 && it.clearTimeMs == displayedTime }) }
    }

    @Test fun lossWithReviveCanResumeAndFinalLossClosesItOnce() = runBlocking {
        val row = (0..10).map { Tile("t$it", it + 1, it * 2f, 0f, 0) }
        coEvery { api.getStageBoard(68, any()) } returns Response.success(row)
        coEvery { dao.consumeForRun(any(), any(), any(), any()) } returns mapOf("REVIVE" to 1)
        open()
        ready(68)
        withContext(main) { row.take(7).forEach { game.handleIntent(GameViewIntent.ClickTile(it)) } }
        assertEquals(GameStatus.LOST, game.viewState.value.gameStatus)
        val saved = saves.latest("account")!!
        val resumed = withContext(main) {
            GameViewModel(SavedStateHandle(), api, prefs, Json, dao, sync, StageBoards(api, Json, generator, temp.root),
                GameLogicDelegate(), GameToolDelegate(), scores, saves).also { it.openLevel(saved.level, null, saved.id) }
        }
        try {
            val loss = withTimeout(1500) { resumed.viewState.first { it.gameStatus == GameStatus.LOST } }
            assertEquals(7, loss.slotTiles.size)
            assertEquals(1, loss.reviveCount)
            withContext(main) {
                resumed.handleIntent(GameViewIntent.Revive)
                assertEquals(3, resumed.viewState.value.movedOutTiles.size)
                row.drop(7).forEach { resumed.handleIntent(GameViewIntent.ClickTile(it)) }
                resumed.handleIntent(GameViewIntent.Revive)
            }
            assertEquals(GameStatus.LOST, resumed.viewState.value.gameStatus)
            assertEquals(0, resumed.viewState.value.reviveCount)
            assertNull(saves.latest("account"))
            coVerify(exactly = 1) { scores.record(match { it.runId == saved.id && it.levelId == 68 && !it.won && !it.abandoned && it.itemsUsed == 1 }) }
        } finally { resumed.viewModelScope.cancel() }
    }

    @Test fun cancellingRestartKeepsTheRunAndConfirmingOnceRecordsOneAbandonment() = runBlocking {
        coEvery { api.getStageBoard(68, any()) } returns Response.success(custom)
        open()
        val initial = withTimeout(2000) { ready(68) }
        withContext(main) { game.handleIntent(GameViewIntent.RestartLevel) }
        withTimeout(2000) { game.viewState.first { it.showCarrySelection } }
        withContext(main) { game.handleIntent(GameViewIntent.DismissCarrySelection) }
        assertEquals(initial.boardTiles, game.viewState.value.boardTiles)
        coVerify(exactly = 0) { dao.recordStageResult(any(), any()) }
        withContext(main) { game.handleIntent(GameViewIntent.TriggerRestartFlow) }
        withTimeout(2000) { game.viewState.first { it.showCarrySelection } }
        withContext(main) { repeat(2) { game.handleIntent(GameViewIntent.ConfirmRestartWithCarry) } }
        withTimeout(2000) { ready(68) }
        coVerify(exactly = 1) { dao.recordStageResult(match { it.levelId == 68 && !it.won && it.abandoned }, true) }
    }

    @Test fun customLevel68OpensWhileCloudSyncIsStillPending() = runBlocking {
        val cloud = CompletableDeferred<Unit>()
        val syncStarted = CompletableDeferred<Unit>()
        coEvery { sync.syncDirtyData() } coAnswers { syncStarted.complete(Unit); cloud.await() }
        coEvery { api.getStageBoard(68, any()) } coAnswers { delay(120); Response.success(custom) }
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
        coEvery { api.getStageBoard(68, capture(seed)) } coAnswers {
            try { delay(12000); Response.success(custom) } finally { cancelled.complete(Unit) }
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
        coEvery { api.getStageBoard(68, any()) } coAnswers {
            waiting.complete(Unit)
            try { Response.success(response.await()) } finally { cancelled.complete(Unit) }
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
        coEvery { api.getStageBoard(68, any()) } coAnswers {
            started.complete(Unit)
            // Simulate an already delivered result whose callback still runs after cancellation.
            withContext(NonCancellable) { late.await() }
            oldReturned.complete(Unit)
            Response.success(custom)
        }
        coEvery { api.getStageBoard(69, any()) } returns Response.success(listOf(custom.single().copy(id = "next")))
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
        coEvery { api.getStageBoard(68, capture(seed)) } throws java.io.IOException("offline")
        val start = TimeSource.Monotonic.markNow()
        open()
        val state = withTimeout(1500) { ready(68) }
        println("level 68 immediate failure to playable local board: ${start.elapsedNow()}")
        assertTrue(state.boardTiles.isNotEmpty())
        verify { generator.generateSolvableLevelLocal(68, seed.captured.toLong(), any()) }
    }

    @Test fun failedLocalAllocationEndsLoadingWithoutStartingAnUnfundedGame() = runBlocking {
        coEvery { api.getStageBoard(68, any()) } returns Response.success(custom)
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
        coEvery { api.getStageBoard(68, any()) } coAnswers {
            if (!firstStarted.complete(Unit)) return@coAnswers Response.success(custom)
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
        coVerify(exactly = 2) { api.getStageBoard(68, any()) }
        coVerify(exactly = 1) { dao.consumeForRun(any(), any(), any(), any()) }
    }

}

internal class InMemoryStageSaveDao : StageSaveDao {
    private val rows = java.util.concurrent.ConcurrentHashMap<String, StageSaveEntity>()
    override fun observeLatest(owner: String) = kotlinx.coroutines.flow.flow { emit(latest(owner)) }
    override suspend fun latest(owner: String) = rows[owner]?.let { ResumableStage(it.runId, it.levelId) }
    override suspend fun read(owner: String, id: String) = rows[owner]?.takeIf { it.runId == id }
    override suspend fun begin(row: StageSaveEntity) { rows[row.ownerId] = row }
    override suspend fun checkpoint(owner: String, id: String, payload: ByteArray) {
        read(owner, id)?.let { rows[owner] = it.copy(payload = payload) }
    }
    override suspend fun discard(owner: String, id: String) {
        read(owner, id)?.let { rows.remove(owner, it) }
    }
}
