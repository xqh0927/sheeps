package com.example.sheeps.data.repository;

import com.apkfuns.logutils.LogUtils;
import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.core.utils.NetworkMonitor;
import com.example.sheeps.data.local.*;
import com.example.sheeps.data.model.*;
import com.example.sheeps.data.network.ApiService;
import kotlinx.coroutines.Dispatchers;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B)\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0004\b\n\u0010\u000bJ\b\u0010\f\u001a\u00020\rH\u0002J\u000e\u0010\u000e\u001a\u00020\u000fH\u0086@\u00a2\u0006\u0002\u0010\u0010J.\u0010\u0011\u001a\u00020\u000f2\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00132\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0013H\u0086@\u00a2\u0006\u0002\u0010\u0018J\u001e\u0010\u0019\u001a\u00020\u000f2\u0006\u0010\u001a\u001a\u00020\r2\u0006\u0010\u001b\u001a\u00020\u0013H\u0086@\u00a2\u0006\u0002\u0010\u001cJ\u0010\u0010\u001d\u001a\u0004\u0018\u00010\u001eH\u0086@\u00a2\u0006\u0002\u0010\u0010R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001f"}, d2 = {"Lcom/example/sheeps/data/repository/SyncRepository;", "", "apiService", "Lcom/example/sheeps/data/network/ApiService;", "localDao", "Lcom/example/sheeps/data/local/LocalDao;", "userPreferences", "Lcom/example/sheeps/core/preference/UserPreferences;", "networkMonitor", "Lcom/example/sheeps/core/utils/NetworkMonitor;", "<init>", "(Lcom/example/sheeps/data/network/ApiService;Lcom/example/sheeps/data/local/LocalDao;Lcom/example/sheeps/core/preference/UserPreferences;Lcom/example/sheeps/core/utils/NetworkMonitor;)V", "getAuthHeader", "", "syncDirtyData", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "saveProgressAndPointsLocally", "levelId", "", "score", "clearTime", "", "pointsGained", "(IIJILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "saveItemLocally", "itemType", "newCount", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "pullCloudProfile", "Lcom/example/sheeps/data/model/UserProfileResponse;", "core_release"})
public final class SyncRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.network.ApiService apiService = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.local.LocalDao localDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.core.preference.UserPreferences userPreferences = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.core.utils.NetworkMonitor networkMonitor = null;
    
    @javax.inject.Inject()
    public SyncRepository(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.network.ApiService apiService, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.local.LocalDao localDao, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.preference.UserPreferences userPreferences, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.utils.NetworkMonitor networkMonitor) {
        super();
    }
    
    private final java.lang.String getAuthHeader() {
        return null;
    }
    
    /**
     * Sync local dirty data to the cloud.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object syncDirtyData(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Save level clear score locally (first) and trigger sync background.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveProgressAndPointsLocally(int levelId, int score, long clearTime, int pointsGained, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Save item count locally (first) and trigger sync background.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveItemLocally(@org.jetbrains.annotations.NotNull()
    java.lang.String itemType, int newCount, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Download the entire cloud state and populate local database (overwriting existing).
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object pullCloudProfile(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.UserProfileResponse> $completion) {
        return null;
    }
}