package com.example.sheeps.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class BaseMviViewModel<State, Intent, Effect>(initialState: State) : ViewModel() {

    private val _viewState = MutableStateFlow(initialState)
    val viewState: StateFlow<State> = _viewState.asStateFlow()

    private val _viewEffect = Channel<Effect>(Channel.BUFFERED)
    val viewEffect: Flow<Effect> = _viewEffect.receiveAsFlow()

    protected var currentState: State
        get() = _viewState.value
        set(value) {
            _viewState.value = value
        }

    abstract fun handleIntent(intent: Intent)

    fun sendIntent(intent: Intent) {
        viewModelScope.launch {
            handleIntent(intent)
        }
    }

    protected fun setEffect(effect: Effect) {
        viewModelScope.launch {
            _viewEffect.send(effect)
        }
    }

    protected fun updateState(reducer: State.() -> State) {
        _viewState.update(reducer)
    }
}
