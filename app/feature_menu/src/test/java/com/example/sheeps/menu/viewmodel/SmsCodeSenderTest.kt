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
    private val phone = "13800000000"
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
        for ((status, message) in listOf(503 to R.string.sms_service_unavailable, 502 to R.string.toast_send_code_failed,
            -1 to R.string.toast_send_code_network_error)) {
            coEvery { repo.sendCode(phone) } returns ApiResult.Error(status, "failure")
            assertEquals(message, sender.send(phone))
            assertEquals(0, sender.state.value.secondsLeft(phone, now))
            assertFalse(sender.state.value.sending)
        }
        coVerify(exactly = 3) { repo.sendCode(phone) }
    }

    @Test fun acceptedAndRateLimitedRequestsStartPerPhoneCooldownAndRejectDuplicateTaps() = runTest {
        val accepted = CompletableDeferred<ApiResult<SendCodeResponse>>()
        coEvery { repo.sendCode(phone) } coAnswers { accepted.await() }
        val first = async(start = CoroutineStart.UNDISPATCHED) { sender.send(phone) }
        assertTrue(sender.state.value.sending)
        assertNull(sender.send(phone))
        assertEquals(0, sender.state.value.secondsLeft(phone, now))
        accepted.complete(ApiResult.Success(SendCodeResponse(true)))
        assertEquals(R.string.toast_send_code_success, first.await())
        assertEquals(60, sender.state.value.secondsLeft(phone, now))
        assertEquals(0, sender.state.value.secondsLeft("13900000000", now))
        now += 59999
        assertEquals(1, sender.state.value.secondsLeft(phone, now))
        assertNull(sender.send(phone))
        coVerify(exactly = 1) { repo.sendCode(phone) }
        now++
        coEvery { repo.sendCode(phone) } returns ApiResult.Error(429, "limited")
        assertEquals(R.string.sms_rate_limited, sender.send(phone))
        assertEquals(60, sender.state.value.secondsLeft(phone, now))
    }

    @Test fun menuDelegateReportsAcceptanceWithoutAnyVerificationCode() = runTest {
        coEvery { repo.sendCode(phone) } returns ApiResult.Success(SendCodeResponse(true))
        val delegate = AuthDelegate(repo, mockk(), mockk(), sender)
        val shown = CompletableDeferred<MenuViewEffect>()
        delegate.handleSendSmsCode(this, phone) { shown.complete(it) }
        assertEquals(MenuViewEffect.ShowToast(resId = R.string.toast_send_code_success), shown.await())
    }

    @Test fun invalidInputAndCancelledRequestsReleaseTheSendButton() = runTest {
        assertEquals(R.string.err_invalid_phone, sender.send("123"))
        coVerify(exactly = 0) { repo.sendCode(any()) }
        coEvery { repo.sendCode(phone) } coAnswers { awaitCancellation() }
        val request = launch(start = CoroutineStart.UNDISPATCHED) { sender.send(phone) }
        request.cancelAndJoin()
        assertFalse(sender.state.value.sending)
        assertEquals(0, sender.state.value.secondsLeft(phone, now))
    }
}
