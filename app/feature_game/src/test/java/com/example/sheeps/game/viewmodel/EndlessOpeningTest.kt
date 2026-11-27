package com.example.sheeps.game.viewmodel

import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import com.example.sheeps.data.model.Tile
import com.example.sheeps.game.state.*
import com.example.sheeps.game.viewmodel.helpers.EndlessLevelGenerator
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class EndlessOpeningTest {
    @Test fun repeatedInitPreservesPausedRunAndOnlyRestartStartsOver() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        mockkStatic(SystemClock::class)
        every { SystemClock.uptimeMillis() } answers { testScheduler.currentTime }
        val tiles = (1..6).map { Tile("face$it", it, it.toFloat(), 0f, 0) }
        val generator = mockk<EndlessLevelGenerator>(relaxed = true)
        every { generator.nextRow(any(), any(), any()) } returns tiles
        val vm = EndlessViewModel(mockk(relaxed = true), mockk(relaxed = true), generator,
            mockk(relaxed = true), mockk(relaxed = true))
        try {
            val open = EndlessViewIntent.Init(isDaily = false, seed = 0)
            vm.handleIntent(open)
            advanceTimeBy(vm.viewState.value.dropIntervalMs)
            runCurrent()
            vm.handleIntent(EndlessViewIntent.UseFreeze)
            vm.handleIntent(EndlessViewIntent.Pause)
            val before = vm.viewState.value
            assertTrue(before.columns.any { it.isNotEmpty() })
            assertEquals(EndlessStatus.PAUSED, before.status)

            vm.handleIntent(open)
            assertEquals(before, vm.viewState.value)
            vm.handleIntent(EndlessViewIntent.Restart)
            val restarted = vm.viewState.value
            assertTrue(restarted.columns.all { it.isEmpty() })
            assertEquals(EndlessStatus.PLAYING, restarted.status)
            assertEquals(before.freezeCount + 1, restarted.freezeCount)
        } finally {
            vm.viewModelScope.cancel()
            unmockkStatic(SystemClock::class)
            Dispatchers.resetMain()
        }
    }
}
