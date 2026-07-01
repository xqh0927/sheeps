package com.example.sheeps.core.base;

import androidx.lifecycle.ViewModel;
import kotlinx.coroutines.flow.*;

/**
 * 基于 MVI (Model-View-Intent) 架构模式的 ViewModel 基类。
 *
 * @param State 界面状态，代表单向数据流中的 UI 模型。
 * @param Intent 界面意图，代表用户的操作或事件。
 * @param Effect 界面副作用，代表一次性事件（如弹窗、导航、提示等）。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b&\u0018\u0000*\u0004\b\u0000\u0010\u0001*\u0004\b\u0001\u0010\u0002*\u0004\b\u0002\u0010\u00032\u00020\u0004B\u000f\u0012\u0006\u0010\u0005\u001a\u00028\u0000\u00a2\u0006\u0004\b\u0006\u0010\u0007J\u0015\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00028\u0001H&\u00a2\u0006\u0002\u0010\u0007J\u0013\u0010\u001c\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00028\u0001\u00a2\u0006\u0002\u0010\u0007J\u0015\u0010\u001d\u001a\u00020\u001a2\u0006\u0010\u001e\u001a\u00028\u0002H\u0004\u00a2\u0006\u0002\u0010\u0007J!\u0010\u001f\u001a\u00020\u001a2\u0017\u0010 \u001a\u0013\u0012\u0004\u0012\u00028\u0000\u0012\u0004\u0012\u00028\u00000!\u00a2\u0006\u0002\b\"H\u0004R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00028\u00000\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00028\u00000\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00028\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00028\u00020\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R$\u0010\u0015\u001a\u00028\u00002\u0006\u0010\u0014\u001a\u00028\u00008D@DX\u0084\u000e\u00a2\u0006\f\u001a\u0004\b\u0016\u0010\u0017\"\u0004\b\u0018\u0010\u0007\u00a8\u0006#"}, d2 = {"Lcom/example/sheeps/core/base/BaseMviViewModel;", "State", "Intent", "Effect", "Landroidx/lifecycle/ViewModel;", "initialState", "<init>", "(Ljava/lang/Object;)V", "_viewState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "viewState", "Lkotlinx/coroutines/flow/StateFlow;", "getViewState", "()Lkotlinx/coroutines/flow/StateFlow;", "_viewEffect", "Lkotlinx/coroutines/channels/Channel;", "viewEffect", "Lkotlinx/coroutines/flow/Flow;", "getViewEffect", "()Lkotlinx/coroutines/flow/Flow;", "value", "currentState", "getCurrentState", "()Ljava/lang/Object;", "setCurrentState", "handleIntent", "", "intent", "sendIntent", "setEffect", "effect", "updateState", "reducer", "Lkotlin/Function1;", "Lkotlin/ExtensionFunctionType;", "core_debug"})
public abstract class BaseMviViewModel<State extends java.lang.Object, Intent extends java.lang.Object, Effect extends java.lang.Object> extends androidx.lifecycle.ViewModel {
    
    /**
     * 响应式的界面状态流。
     */
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<State> _viewState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<State> viewState = null;
    
    /**
     * 用于发送一次性副作用事件的通道。
     */
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.channels.Channel<Effect> _viewEffect = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<Effect> viewEffect = null;
    
    public BaseMviViewModel(State initialState) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<State> getViewState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<Effect> getViewEffect() {
        return null;
    }
    
    protected final State getCurrentState() {
        return null;
    }
    
    protected final void setCurrentState(State value) {
    }
    
    /**
     * 处理意图的抽象方法，由子类实现具体的业务逻辑。
     * @param intent 用户发出的意图
     */
    public abstract void handleIntent(Intent intent);
    
    /**
     * 由外部（UI层）调用以发送意图。
     */
    public final void sendIntent(Intent intent) {
    }
    
    /**
     * 发送一个副作用事件。
     */
    protected final void setEffect(Effect effect) {
    }
    
    /**
     * 更新当前状态。采用 Reducer 模式，基于旧状态生成新状态。
     *
     * @param reducer 定义如何从旧 State 转换到新 State 的逻辑块
     */
    protected final void updateState(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super State, ? extends State> reducer) {
    }
}