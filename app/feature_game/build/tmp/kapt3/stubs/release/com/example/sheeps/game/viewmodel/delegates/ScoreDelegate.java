package com.example.sheeps.game.viewmodel.delegates;

import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.local.LocalDao;
import com.example.sheeps.data.model.ScoreRequest;
import com.example.sheeps.data.network.ApiService;
import com.example.sheeps.data.repository.SyncRepository;
import com.example.sheeps.game.state.GameViewEffect;
import kotlinx.coroutines.Dispatchers;
import java.security.MessageDigest;
import javax.inject.Inject;

/**
 * 得分上传与同步逻辑委派类
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B)\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0004\b\n\u0010\u000bJn\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00112\u0006\u0010\u0015\u001a\u00020\u00162*\u0010\u0017\u001a&\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u00190\u00182\u0012\u0010\u001a\u001a\u000e\u0012\u0004\u0012\u00020\u001c\u0012\u0004\u0012\u00020\r0\u001bJ\u0010\u0010\u001d\u001a\u00020\u00192\u0006\u0010\u001e\u001a\u00020\u0019H\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001f"}, d2 = {"Lcom/example/sheeps/game/viewmodel/delegates/ScoreDelegate;", "", "apiService", "Lcom/example/sheeps/data/network/ApiService;", "prefs", "Lcom/example/sheeps/core/preference/UserPreferences;", "localDao", "Lcom/example/sheeps/data/local/LocalDao;", "syncRepository", "Lcom/example/sheeps/data/repository/SyncRepository;", "<init>", "(Lcom/example/sheeps/data/network/ApiService;Lcom/example/sheeps/core/preference/UserPreferences;Lcom/example/sheeps/data/local/LocalDao;Lcom/example/sheeps/data/repository/SyncRepository;)V", "submitScoreOnline", "", "scope", "Lkotlinx/coroutines/CoroutineScope;", "levelId", "", "levelStartTime", "", "itemsUsedCount", "isDoublePointsActive", "", "getLocalizedString", "Lkotlin/Function5;", "", "setEffect", "Lkotlin/Function1;", "Lcom/example/sheeps/game/state/GameViewEffect;", "sha256", "input", "feature_game_release"})
public final class ScoreDelegate {
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.network.ApiService apiService = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.core.preference.UserPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.local.LocalDao localDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.repository.SyncRepository syncRepository = null;
    
    @javax.inject.Inject()
    public ScoreDelegate(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.network.ApiService apiService, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.preference.UserPreferences prefs, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.local.LocalDao localDao, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.repository.SyncRepository syncRepository) {
        super();
    }
    
    /**
     * 提交关卡成绩到云端并同步本地
     */
    public final void submitScoreOnline(@org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope, int levelId, long levelStartTime, int itemsUsedCount, boolean isDoublePointsActive, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function5<? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, ? super java.lang.String, java.lang.String> getLocalizedString, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.example.sheeps.game.state.GameViewEffect, kotlin.Unit> setEffect) {
    }
    
    private final java.lang.String sha256(java.lang.String input) {
        return null;
    }
}