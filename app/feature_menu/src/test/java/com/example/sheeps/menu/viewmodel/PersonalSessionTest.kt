package com.example.sheeps.menu.viewmodel

import com.example.sheeps.data.model.RefreshResponse
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.data.result.TokenRefresher
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class PersonalSessionTest {
    private val api = mockk<ApiService>()
    private val prefs = mockk<UserPreferences>()
    private var access = "A-old"
    private var refresh = "A-refresh"
    private val tokens = TokenRefresher(api, prefs)

    init {
        every { prefs.getToken() } answers { access }
        every { prefs.getRefreshToken() } answers { refresh }
        every { prefs.accountId(any()) } answers { firstArg<String>().substringBefore('-') }
        every { prefs.replaceTokens(any(), any(), any(), any()) } answers {
            if (access == firstArg<String>() && refresh == secondArg<String>()) {
                access = thirdArg()
                refresh = arg(3)
                true
            } else false
        }
    }

    private fun failure(code: Int) = HttpException(Response.error<Unit>(code, "".toResponseBody()))

    @Test fun concurrentReadersShareAnUnavailableRefreshAndCanRetryLater() = runTest {
        val finish = CompletableDeferred<Unit>()
        coEvery { api.refreshToken(any()) } coAnswers { finish.await(); throw failure(503) }
        val reads = List(4) {
            async(start = CoroutineStart.UNDISPATCHED) { tokens.authenticated(access) { throw failure(401) } }
        }
        finish.complete(Unit)
        assertTrue(reads.awaitAll().all { it is ApiResult.Error && it.code == 503 })
        coVerify(exactly = 1) { api.refreshToken(any()) }

        coEvery { api.refreshToken(any()) } returns RefreshResponse(true, "A-new", "A-rotated")
        assertEquals(ApiResult.Success("Bearer A-new"), tokens.authenticated(access) { auth ->
            if (auth.endsWith("old")) throw failure(401)
            auth
        })
    }

    @Test fun logoutOrAnotherLoginDuringRenewalCannotReplaceTheNewSession() = runTest {
        val finish = CompletableDeferred<Unit>()
        coEvery { api.refreshToken(any()) } coAnswers { finish.await(); RefreshResponse(true, "A-new", "A-rotated") }
        val pending = async(start = CoroutineStart.UNDISPATCHED) { tokens.authenticated(access) { throw failure(401) } }
        access = "B-access"
        refresh = "B-refresh"
        finish.complete(Unit)
        assertEquals(401, (pending.await() as ApiResult.Error).code)
        assertEquals("B-access", access)
        assertEquals("B-refresh", refresh)
    }
}
