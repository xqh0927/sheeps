package com.example.sheeps.menu.viewmodel

import android.os.SystemClock
import com.example.sheeps.data.di.NetworkModule
import com.example.sheeps.data.model.SendCodeResponse
import com.example.sheeps.data.repository.UserRepository
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.menu.state.MenuViewEffect
import com.example.sheeps.menu.viewmodel.delegates.AuthDelegate
import com.example.sheeps.ui.R
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SmsCodeSenderTest {
    private val repo = mockk<UserRepository>()
    private val sender = SmsCodeSender(repo)
    private val email = "player@example.com"
    private var now = 1000L

    @Before fun clock() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } answers { now }
    }
    @After fun resetClock() = unmockkStatic(SystemClock::class)

    @Test fun newClientReadsOnlyAcceptanceFromNewAndLegacyResponses() {
        val json = NetworkModule.provideJson()
        for (body in listOf("""{"success":true,"code":""}""", """{"success":true,"code":"123456"}""", """{"success":true}""")) {
            assertEquals(SendCodeResponse(true), json.decodeFromString<SendCodeResponse>(body))
        }
    }

    @Test fun unavailableProviderDeliveryFailureAndNetworkFailureAllowImmediateRetry() = runTest {
        for ((status, message) in listOf(503 to R.string.email_service_unavailable, 502 to R.string.toast_send_code_failed,
            -1 to R.string.toast_send_code_network_error)) {
            coEvery { repo.sendCode(email) } returns ApiResult.Error(status, "failure")
            assertEquals(message, sender.send(email))
            assertEquals(0, sender.state.value.secondsLeft(email, now))
            assertFalse(sender.state.value.sending)
        }
        coVerify(exactly = 3) { repo.sendCode(email) }
    }

    @Test fun acceptedAndRateLimitedRequestsStartPerEmailCooldownAndRejectDuplicateTaps() = runTest {
        val accepted = CompletableDeferred<ApiResult<SendCodeResponse>>()
        coEvery { repo.sendCode(email) } coAnswers { accepted.await() }
        val first = async(start = CoroutineStart.UNDISPATCHED) { sender.send("  Player@Example.COM ") }
        assertTrue(sender.state.value.sending)
        assertNull(sender.send(email))
        assertEquals(0, sender.state.value.secondsLeft(email, now))
        accepted.complete(ApiResult.Success(SendCodeResponse(true)))
        assertEquals(R.string.toast_send_code_success, first.await())
        assertEquals(60, sender.state.value.secondsLeft(email, now))
        assertEquals(60, sender.state.value.secondsLeft("PLAYER@EXAMPLE.COM", now))
        assertEquals(0, sender.state.value.secondsLeft("other@example.com", now))
        now += 59999
        assertEquals(1, sender.state.value.secondsLeft(email, now))
        assertNull(sender.send(email))
        coVerify(exactly = 1) { repo.sendCode(email) }
        now++
        coEvery { repo.sendCode(email) } returns ApiResult.Error(429, "limited")
        assertEquals(R.string.sms_rate_limited, sender.send(email))
        assertEquals(60, sender.state.value.secondsLeft(email, now))
    }

    @Test fun menuDelegateReportsAcceptanceWithoutAnyVerificationCode() = runTest {
        coEvery { repo.sendCode(email) } returns ApiResult.Success(SendCodeResponse(true))
        val delegate = AuthDelegate(repo, mockk(), mockk(), sender)
        val shown = CompletableDeferred<MenuViewEffect>()
        delegate.handleSendSmsCode(this, email) { shown.complete(it) }
        assertEquals(MenuViewEffect.ShowToast(resId = R.string.toast_send_code_success), shown.await())
    }

    @Test fun invalidInputAndCancelledRequestsReleaseTheSendButton() = runTest {
        assertEquals(R.string.err_invalid_email, sender.send("13800000000"))
        coVerify(exactly = 0) { repo.sendCode(any()) }
        coEvery { repo.sendCode(email) } coAnswers { awaitCancellation() }
        val request = launch(start = CoroutineStart.UNDISPATCHED) { sender.send(email) }
        request.cancelAndJoin()
        assertFalse(sender.state.value.sending)
        assertEquals(0, sender.state.value.secondsLeft(email, now))
    }
}
