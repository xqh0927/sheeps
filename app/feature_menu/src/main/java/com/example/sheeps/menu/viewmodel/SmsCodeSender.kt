package com.example.sheeps.menu.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sheeps.data.repository.UserRepository
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

internal fun normalizeEmailInput(input: String): String = input.trim().lowercase()

internal fun isValidEmailInput(input: String): Boolean {
    val normalized = normalizeEmailInput(input)
    return normalized.length in 5..254 && EMAIL_REGEX.matches(normalized)
}

data class SmsCodeState(val sending: Boolean = false, val deadlines: Map<String, Long> = emptyMap()) {
    fun secondsLeft(email: String, now: Long): Int {
        val key = normalizeEmailInput(email)
        return (((deadlines[key] ?: 0L) - now).coerceAtLeast(0) + 999).div(1000).toInt()
    }
}

/** All account forms share the in-flight gate and the server's per-email resend window. */
@Singleton
class SmsCodeSender @Inject constructor(private val repository: UserRepository) {
    private val gate = Mutex()
    private val mutableState = MutableStateFlow(SmsCodeState())
    val state = mutableState.asStateFlow()

    suspend fun send(email: String): Int? {
        val normalized = normalizeEmailInput(email)
        if (!isValidEmailInput(normalized)) return R.string.err_invalid_email
        if (!gate.tryLock()) return null
        try {
            val now = SystemClock.elapsedRealtime()
            if (state.value.secondsLeft(normalized, now) > 0) return null
            val active = state.value.deadlines.filterValues { it > now }
            mutableState.value = SmsCodeState(sending = true, deadlines = active)
            val result = repository.sendCode(normalized)
            val message = when (result) {
                is ApiResult.Success -> R.string.toast_send_code_success
                is ApiResult.Error -> when (result.code) {
                    429 -> R.string.sms_rate_limited
                    503 -> R.string.email_service_unavailable
                    -1 -> R.string.toast_send_code_network_error
                    else -> R.string.toast_send_code_failed
                }
            }
            if (result is ApiResult.Success || (result is ApiResult.Error && result.code == 429)) {
                val until = SystemClock.elapsedRealtime() + 60_000L
                mutableState.value = SmsCodeState(deadlines = active + (normalized to until))
            }
            return message
        } finally {
            mutableState.value = state.value.copy(sending = false)
            gate.unlock()
        }
    }
}

@HiltViewModel
class SmsCodeViewModel @Inject constructor(private val sender: SmsCodeSender) : ViewModel() {
    val state = sender.state
    private val feedback = Channel<Int>(Channel.BUFFERED)
    val messages = feedback.receiveAsFlow()

    fun send(email: String) {
        viewModelScope.launch { sender.send(email)?.let { feedback.send(it) } }
    }
}
