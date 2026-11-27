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

data class SmsCodeState(val sending: Boolean = false, val deadlines: Map<String, Long> = emptyMap()) {
    fun secondsLeft(phone: String, now: Long): Int =
        (((deadlines[phone] ?: 0L) - now).coerceAtLeast(0) + 999).div(1000).toInt()
}

/** All account forms share the in-flight gate and the server's per-number resend window. */
@Singleton
class SmsCodeSender @Inject constructor(private val repository: UserRepository) {
    private val gate = Mutex()
    private val mutableState = MutableStateFlow(SmsCodeState())
    val state = mutableState.asStateFlow()

    suspend fun send(phone: String): Int? {
        if (phone.length != 11 || phone.any { it !in '0'..'9' }) return R.string.err_invalid_phone
        if (!gate.tryLock()) return null
        try {
            val now = SystemClock.elapsedRealtime()
            if (state.value.secondsLeft(phone, now) > 0) return null
            val active = state.value.deadlines.filterValues { it > now }
            mutableState.value = SmsCodeState(sending = true, deadlines = active)
            val result = repository.sendCode(phone)
            val message = when (result) {
                is ApiResult.Success -> R.string.toast_send_code_success
                is ApiResult.Error -> when (result.code) {
                    429 -> R.string.sms_rate_limited
                    503 -> R.string.sms_service_unavailable
                    -1 -> R.string.toast_send_code_network_error
                    else -> R.string.toast_send_code_failed
                }
            }
            if (result is ApiResult.Success || (result is ApiResult.Error && result.code == 429)) {
                val until = SystemClock.elapsedRealtime() + 60_000L
                mutableState.value = SmsCodeState(deadlines = active + (phone to until))
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

    fun send(phone: String) {
        viewModelScope.launch { sender.send(phone)?.let { feedback.send(it) } }
    }
}
