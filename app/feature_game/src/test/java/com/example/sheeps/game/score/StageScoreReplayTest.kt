package com.example.sheeps.game.score

import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.local.PendingStageStart
import com.example.sheeps.data.model.StageStartRequest
import com.example.sheeps.data.local.PendingStageScore
import com.example.sheeps.data.model.GenericResponse
import com.example.sheeps.data.model.ScoreRequest
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.repository.StageScoreRepository
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.data.result.TokenRefresher
import com.example.sheeps.data.utils.NetworkMonitor
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class StageScoreReplayTest {
    @Test fun guestOpeningKeepsItsOriginalVersionAndTimeUntilItsOwnReceiptArrives() = runTest {
        val dao = mockk<LocalDao>(relaxed = true)
        val api = mockk<ApiService>()
        val prefs = mockk<UserPreferences>()
        val network = mockk<NetworkMonitor>()
        val repo = StageScoreRepository(dao, api, prefs, network, mockk())
        val owner = "uuid-test-guest"
        val run = PendingStageStart(owner, "same-run", 8, "published-v1", 1500L)
        val body = StageStartRequest(owner, "same-run", 8, "published-v1", 1500L)
        every { prefs.getToken() } returns null
        every { prefs.accountId(null) } returns null
        every { prefs.getUserId() } returns owner
        coEvery { dao.pendingStageStarts(owner) } returns listOf(run)
        every { network.isOnline() } returns false
        assertFalse(repo.flushStarts(owner))
        coVerify(exactly = 0) { api.recordStageStart(any(), any()) }
        every { network.isOnline() } returns true
        coEvery { api.recordStageStart("", body) } returnsMany listOf(
            GenericResponse(true, run_id = "other"), GenericResponse(true, run_id = run.runId))
        assertFalse(repo.flushStarts(owner))
        coVerify(exactly = 0) { dao.acknowledgeStageStart(any(), any()) }
        assertTrue(repo.flushStarts("other-account"))
        assertTrue(repo.flushStarts(owner))
        coVerify(exactly = 2) { api.recordStageStart("", body) }
        coVerify(exactly = 1) { dao.acknowledgeStageStart(owner, run.runId) }
    }

    @Test fun interruptedUploadRetainsTheSameRunAndOtherAccountsCannotDrainIt() = runTest {
        val dao = mockk<LocalDao>(relaxed = true)
        val api = mockk<ApiService>()
        val prefs = mockk<UserPreferences>(relaxed = true)
        val sessions = mockk<TokenRefresher>()
        val network = mockk<NetworkMonitor>()
        val pending = mutableListOf<PendingStageScore>()
        val requests = mutableListOf<ScoreRequest>()
        val accepted = mutableSetOf<String>()
        var online = false
        var owner = "alice"
        var supportsReceipts = false
        var loseResponse = true
        every { prefs.getToken() } answers { owner }
        every { prefs.accountId(any()) } answers { firstArg() }
        every { network.isOnline() } answers { online }
        coEvery { dao.recordStageResult(any(), true) } coAnswers { pending.add(firstArg()) }
        coEvery { dao.pendingScores(any()) } coAnswers { pending.filter { it.ownerId == firstArg<String>() } }
        coEvery { dao.acknowledgeScore(any(), any()) } coAnswers {
            pending.removeAll { it.ownerId == firstArg<String>() && it.runId == secondArg<String>() }; Unit
        }
        coEvery { sessions.authenticated<GenericResponse>(any(), any()) } coAnswers {
            val send = secondArg<suspend (String) -> GenericResponse>()
            if (!supportsReceipts) return@coAnswers ApiResult.Error(404, "receipt endpoint unavailable")
            val response = send("Bearer ${firstArg<String>()}")
            if (loseResponse) {
                loseResponse = false
                ApiResult.Error(-1, "response lost")
            } else ApiResult.Success(response)
        }
        coEvery { api.submitScoreOnce("Bearer alice", any()) } coAnswers {
            val score = secondArg<ScoreRequest>()
            requests.add(score)
            accepted.add(requireNotNull(score.run_id))
            GenericResponse(true, run_id = score.run_id)
        }
        val repo = StageScoreRepository(dao, api, prefs, network, sessions)
        for (level in 1..3) {
            repo.record(PendingStageScore("alice", "run-$level", level, 100, 4500, 0, true, 1000L + level))
        }
        assertFalse(repo.flush("alice"))
        assertTrue(requests.isEmpty())
        online = true
        assertFalse(repo.flush("alice"))
        assertEquals(3, pending.size)
        assertTrue(accepted.isEmpty())
        coVerify(exactly = 0) { api.submitScore(any(), any()) }
        supportsReceipts = true
        assertFalse(repo.flush("alice"))
        assertEquals(3, pending.size)
        owner = "bob"
        assertTrue(repo.flush("alice"))
        assertTrue(repo.flush("bob"))
        assertEquals(listOf("run-1"), requests.map { it.run_id })

        owner = "alice"
        val restarted = StageScoreRepository(dao, api, prefs, network, sessions)
        assertTrue(restarted.flush("alice"))
        assertEquals(listOf("run-1", "run-1", "run-2", "run-3"), requests.map { it.run_id })
        assertEquals(requests[0], requests[1])
        assertEquals(3, accepted.size)
        assertTrue(pending.isEmpty())
    }
}
