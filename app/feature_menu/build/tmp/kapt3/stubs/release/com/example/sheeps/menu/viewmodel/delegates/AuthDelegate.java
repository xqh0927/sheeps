package com.example.sheeps.menu.viewmodel.delegates;

import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.local.BackpackItemEntity;
import com.example.sheeps.data.local.LocalDao;
import com.example.sheeps.data.local.UserProfileEntity;
import com.example.sheeps.data.local.UserProgressEntity;
import com.example.sheeps.data.model.LoginRequest;
import com.example.sheeps.data.model.LoginResponse;
import com.example.sheeps.data.model.SendCodeRequest;
import com.example.sheeps.data.network.ApiService;
import com.example.sheeps.data.repository.SyncRepository;
import com.example.sheeps.menu.state.MenuViewEffect;
import javax.inject.Inject;

/**
 * 用户认证逻辑委派类
 * 处理登录、登出、验证码发送、存档冲突解决
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000j\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0004\u0018\u00002\u00020\u0001B)\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0004\b\n\u0010\u000bJ*\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\u0012\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\r0\u0013J\u0086\u0001\u0010\u0015\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0016\u001a\u00020\u00112\u0012\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\u0018\u0012\u0004\u0012\u00020\r0\u00132*\u0010\u0019\u001a&\u0012\u0004\u0012\u00020\u0018\u0012\u0004\u0012\u00020\u001b\u0012\u0004\u0012\u00020\u001b\u0012\u0004\u0012\u00020\u001b\u0012\u0004\u0012\u00020\u001b\u0012\u0004\u0012\u00020\r0\u001a2\u0012\u0010\u001c\u001a\u000e\u0012\u0004\u0012\u00020\u001d\u0012\u0004\u0012\u00020\r0\u00132\u0012\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\r0\u0013JV\u0010\u001e\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\b\u0010\u001f\u001a\u0004\u0018\u00010\u00182\u0006\u0010 \u001a\u00020\u001d2\f\u0010!\u001a\b\u0012\u0004\u0012\u00020\r0\"2\u0012\u0010\u001c\u001a\u000e\u0012\u0004\u0012\u00020\u001d\u0012\u0004\u0012\u00020\r0\u00132\u0012\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\r0\u0013J\u001e\u0010#\u001a\u00020\r2\u0006\u0010$\u001a\u00020\u00182\u0006\u0010%\u001a\u00020&H\u0082@\u00a2\u0006\u0002\u0010\'J\u001e\u0010(\u001a\u00020\r2\u0006\u0010$\u001a\u00020\u00182\u0006\u0010%\u001a\u00020&H\u0082@\u00a2\u0006\u0002\u0010\'J0\u0010)\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\f\u0010!\u001a\b\u0012\u0004\u0012\u00020\r0\"2\u0012\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\r0\u0013R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006*"}, d2 = {"Lcom/example/sheeps/menu/viewmodel/delegates/AuthDelegate;", "", "apiService", "Lcom/example/sheeps/data/network/ApiService;", "prefs", "Lcom/example/sheeps/core/preference/UserPreferences;", "localDao", "Lcom/example/sheeps/data/local/LocalDao;", "syncRepository", "Lcom/example/sheeps/data/repository/SyncRepository;", "<init>", "(Lcom/example/sheeps/data/network/ApiService;Lcom/example/sheeps/core/preference/UserPreferences;Lcom/example/sheeps/data/local/LocalDao;Lcom/example/sheeps/data/repository/SyncRepository;)V", "handleSendSmsCode", "", "scope", "Lkotlinx/coroutines/CoroutineScope;", "phone", "", "setEffect", "Lkotlin/Function1;", "Lcom/example/sheeps/menu/state/MenuViewEffect;", "handleLoginWithCode", "code", "onSuccess", "Lcom/example/sheeps/data/model/LoginResponse;", "onConflict", "Lkotlin/Function5;", "", "setLoading", "", "handleResolveConflict", "pendingResponse", "useLocal", "onComplete", "Lkotlin/Function0;", "resolveWithLocal", "response", "now", "", "(Lcom/example/sheeps/data/model/LoginResponse;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "resolveWithCloud", "handleLogout", "feature_menu_release"})
public final class AuthDelegate {
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.network.ApiService apiService = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.core.preference.UserPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.local.LocalDao localDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.repository.SyncRepository syncRepository = null;
    
    @javax.inject.Inject()
    public AuthDelegate(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.network.ApiService apiService, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.preference.UserPreferences prefs, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.local.LocalDao localDao, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.repository.SyncRepository syncRepository) {
        super();
    }
    
    /**
     * 发送短信验证码
     */
    public final void handleSendSmsCode(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, @org.jetbrains.annotations.NotNull()
    java.lang.String phone, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.menu.state.MenuViewEffect, kotlin.Unit> setEffect) {
    }
    
    /**
     * 处理登录逻辑
     */
    public final void handleLoginWithCode(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, @org.jetbrains.annotations.NotNull()
    java.lang.String phone, @org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.data.model.LoginResponse, kotlin.Unit> onSuccess, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function5<? super com.example.sheeps.data.model.LoginResponse, ? super java.lang.Integer, ? super java.lang.Integer, ? super java.lang.Integer, ? super java.lang.Integer, kotlin.Unit> onConflict, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> setLoading, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.menu.state.MenuViewEffect, kotlin.Unit> setEffect) {
    }
    
    /**
     * 解决存档冲突
     */
    public final void handleResolveConflict(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, @org.jetbrains.annotations.Nullable()
    com.example.sheeps.data.model.LoginResponse pendingResponse, boolean useLocal, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onComplete, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> setLoading, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.menu.state.MenuViewEffect, kotlin.Unit> setEffect) {
    }
    
    private final java.lang.Object resolveWithLocal(com.example.sheeps.data.model.LoginResponse response, long now, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object resolveWithCloud(com.example.sheeps.data.model.LoginResponse response, long now, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * 处理登出
     */
    public final void handleLogout(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onComplete, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.menu.state.MenuViewEffect, kotlin.Unit> setEffect) {
    }
}