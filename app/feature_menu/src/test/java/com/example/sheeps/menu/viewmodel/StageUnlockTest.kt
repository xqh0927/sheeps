package com.example.sheeps.menu.viewmodel

import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.model.UnlockLevelRequest
import com.example.sheeps.data.model.UnlockLevelResponse
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.repository.UserRepository
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.menu.viewmodel.delegates.SocialActionDelegate
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StageUnlockTest {
    private val api = mockk<UserRepository>()
    private val prefs = mockk<UserPreferences>(relaxed = true)
    private val dao = mockk<LocalDao>(relaxed = true)
    private val actions = SocialActionDelegate(api, prefs, dao)
    private val order = UnlockLevelRequest(3, 100)

    @Test fun changedQuoteLeavesBalanceAndUnlocksUntouched() = runTest {
        every { prefs.accountId() } returns "alice"
        coEvery { api.unlockLevel(order) } returns ApiResult.Error(409, "Price changed")
        actions.unlockLevel(3, 100) {}
        verify(exactly = 0) { prefs.setPoints(any()) }
        verify(exactly = 0) { prefs.setUnlockedLevel(any()) }
        verify { dao wasNot Called }
    }

    @Test fun successStoresTheActualBalanceFromTheReceipt() = runTest {
        every { prefs.accountId() } returns "alice"
        every { prefs.getUnlockedLevel() } returns 1
        coEvery { api.unlockLevel(order) } returns ApiResult.Success(UnlockLevelResponse(true, 25))
        actions.unlockLevel(3, 100) {}
        coVerify(exactly = 1) { dao.applyUnlockReceipt("alice", 3, 25, any()) }
        verify { prefs.setPoints(25); prefs.setUnlockedLevel(3) }
    }

    @Test fun aLateResponseDoesNotApplyToANewAccount() = runTest {
        every { prefs.accountId() } returnsMany listOf("alice", "bob")
        coEvery { api.unlockLevel(order) } returns ApiResult.Success(UnlockLevelResponse(true, 25))
        actions.unlockLevel(3, 100) {}
        verify { dao wasNot Called }
        verify(exactly = 0) { prefs.setPoints(any()) }
    }
}
