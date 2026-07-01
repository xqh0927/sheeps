package com.example.sheeps.menu.viewmodel.delegates;

import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.local.BackpackItemEntity;
import com.example.sheeps.data.local.LocalDao;
import com.example.sheeps.data.local.UserProfileEntity;
import com.example.sheeps.data.local.UserProgressEntity;
import com.example.sheeps.data.model.ExchangeRequest;
import com.example.sheeps.data.model.ShopItem;
import com.example.sheeps.data.model.TaskClaimRequest;
import com.example.sheeps.data.model.UnlockLevelRequest;
import com.example.sheeps.data.network.ApiService;
import com.example.sheeps.menu.state.MenuViewEffect;
import javax.inject.Inject;

/**
 * 社交与任务操作逻辑委派类
 * 处理签到、商店兑换、任务领奖、关卡解锁
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000l\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\u0018\u00002\u00020\u0001B!\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0004\b\b\u0010\tJ0\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000f2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u000b0\u0011JN\u0010\u0013\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u00152\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00190\u00182\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000f2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u000b0\u0011JF\u0010\u001a\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u001b\u001a\u00020\u001c2\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001e0\u00182\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000f2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u000b0\u0011J8\u0010\u001f\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010 \u001a\u00020\u00152\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000f2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u000b0\u0011J(\u0010!\u001a\u00020\u000b2\u0006\u0010\"\u001a\u00020\u00152\u0006\u0010#\u001a\u00020$2\b\b\u0002\u0010%\u001a\u00020&H\u0082@\u00a2\u0006\u0002\u0010\'R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006("}, d2 = {"Lcom/example/sheeps/menu/viewmodel/delegates/SocialActionDelegate;", "", "apiService", "Lcom/example/sheeps/data/network/ApiService;", "prefs", "Lcom/example/sheeps/core/preference/UserPreferences;", "localDao", "Lcom/example/sheeps/data/local/LocalDao;", "<init>", "(Lcom/example/sheeps/data/network/ApiService;Lcom/example/sheeps/core/preference/UserPreferences;Lcom/example/sheeps/data/local/LocalDao;)V", "handleSignIn", "", "scope", "Lkotlinx/coroutines/CoroutineScope;", "onComplete", "Lkotlin/Function0;", "setEffect", "Lkotlin/Function1;", "Lcom/example/sheeps/menu/state/MenuViewEffect;", "handleExchangeShopItem", "shopItemId", "", "count", "shopItems", "", "Lcom/example/sheeps/data/model/ShopItem;", "handleClaimTask", "taskId", "", "dailyTasks", "Lcom/example/sheeps/data/model/DailyTask;", "handleUnlockLevelWithPoints", "levelId", "updateLocalPoints", "points", "isDirty", "", "timestamp", "", "(IZJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "feature_menu_release"})
public final class SocialActionDelegate {
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.network.ApiService apiService = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.core.preference.UserPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.local.LocalDao localDao = null;
    
    @javax.inject.Inject()
    public SocialActionDelegate(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.network.ApiService apiService, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.preference.UserPreferences prefs, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.local.LocalDao localDao) {
        super();
    }
    
    /**
     * 处理每日签到
     */
    public final void handleSignIn(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onComplete, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.menu.state.MenuViewEffect, kotlin.Unit> setEffect) {
    }
    
    /**
     * 处理道具兑换
     */
    public final void handleExchangeShopItem(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, int shopItemId, int count, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.ShopItem> shopItems, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onComplete, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.menu.state.MenuViewEffect, kotlin.Unit> setEffect) {
    }
    
    /**
     * 处理任务领奖
     */
    public final void handleClaimTask(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, @org.jetbrains.annotations.NotNull()
    java.lang.String taskId, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.DailyTask> dailyTasks, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onComplete, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.menu.state.MenuViewEffect, kotlin.Unit> setEffect) {
    }
    
    /**
     * 使用积分手动解锁关卡
     */
    public final void handleUnlockLevelWithPoints(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, int levelId, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onComplete, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.menu.state.MenuViewEffect, kotlin.Unit> setEffect) {
    }
    
    private final java.lang.Object updateLocalPoints(int points, boolean isDirty, long timestamp, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}