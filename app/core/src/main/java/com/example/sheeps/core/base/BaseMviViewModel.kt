package com.example.sheeps.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 基于 MVI (Model-View-Intent) 架构模式的 ViewModel 基类。
 * 
 * @param State 界面状态，代表单向数据流中的 UI 模型。
 * @param Intent 界面意图，代表用户的操作或事件。
 * @param Effect 界面副作用，代表一次性事件（如弹窗、导航、提示等）。
 */
abstract class BaseMviViewModel<State, Intent, Effect>(initialState: State) : ViewModel() {

    /**
     * 响应式的界面状态流。
     */
    private val _viewState = MutableStateFlow(initialState)
    val viewState: StateFlow<State> = _viewState.asStateFlow()

    /**
     * 用于发送一次性副作用事件的通道。
     */
    private val _viewEffect = Channel<Effect>(Channel.BUFFERED)
    val viewEffect: Flow<Effect> = _viewEffect.receiveAsFlow()

    /**
     * 当前状态的快照。
     */
    protected var currentState: State
        get() = _viewState.value
        set(value) {
            _viewState.value = value
        }

    /**
     * 处理意图的抽象方法，由子类实现具体的业务逻辑。
     * @param intent 用户发出的意图
     */
    abstract fun handleIntent(intent: Intent)

    /**
     * 由外部（UI层）调用以发送意图。
     */
    fun sendIntent(intent: Intent) {
        viewModelScope.launch {
            handleIntent(intent)
        }
    }

    /**
     * 发送一个副作用事件。
     */
    protected fun setEffect(effect: Effect) {
        viewModelScope.launch {
            _viewEffect.send(effect)
        }
    }

    /**
     * 更新当前状态。采用 Reducer 模式，基于旧状态生成新状态。
     * 
     * @param reducer 定义如何从旧 State 转换到新 State 的逻辑块
     */
    protected fun updateState(reducer: State.() -> State) {
        _viewState.update(reducer)
    }
}
