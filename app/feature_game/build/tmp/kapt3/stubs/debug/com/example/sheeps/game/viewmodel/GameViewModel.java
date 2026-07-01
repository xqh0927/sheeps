package com.example.sheeps.game.viewmodel;

import com.example.sheeps.core.base.BaseMviViewModel;
import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.local.BackpackItemEntity;
import com.example.sheeps.data.local.LocalDao;
import com.example.sheeps.data.model.*;
import com.example.sheeps.data.network.ApiService;
import com.example.sheeps.data.repository.SyncRepository;
import com.example.sheeps.game.state.*;
import com.example.sheeps.game.viewmodel.delegates.GameLogicDelegate;
import com.example.sheeps.game.viewmodel.delegates.GameToolDelegate;
import com.example.sheeps.game.viewmodel.delegates.ScoreDelegate;
import com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.Dispatchers;
import javax.inject.Inject;

/**
 * 游戏主逻辑 ViewModel
 * 核心逻辑已拆分至 Logic、Tool、Score 等 Delegate 中，保持主类逻辑清晰。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u0088\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u000b\n\u0002\u0010$\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0010\b\u0007\u0018\u00002\u0014\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00040\u0001BQ\b\u0007\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\n\u0012\u0006\u0010\u000b\u001a\u00020\f\u0012\u0006\u0010\r\u001a\u00020\u000e\u0012\u0006\u0010\u000f\u001a\u00020\u0010\u0012\u0006\u0010\u0011\u001a\u00020\u0012\u0012\u0006\u0010\u0013\u001a\u00020\u0014\u0012\u0006\u0010\u0015\u001a\u00020\u0016\u00a2\u0006\u0004\b\u0017\u0010\u0018J\u0010\u0010$\u001a\u00020%2\u0006\u0010&\u001a\u00020\u0003H\u0016J\b\u0010\'\u001a\u00020%H\u0002J\b\u0010(\u001a\u00020%H\u0002J\u0010\u0010)\u001a\u00020%2\u0006\u0010*\u001a\u00020\u001cH\u0002J\u001a\u0010+\u001a\u00020%2\u0006\u0010,\u001a\u00020\u001e2\b\u0010-\u001a\u0004\u0018\u00010\u001cH\u0002J2\u0010.\u001a\u00020%2\f\u0010/\u001a\b\u0012\u0004\u0012\u00020#0\"2\u0012\u00100\u001a\u000e\u0012\u0004\u0012\u00020\u001c\u0012\u0004\u0012\u00020\u001e012\u0006\u00102\u001a\u000203H\u0002J8\u00104\u001a\u00020%2\f\u00105\u001a\b\u0012\u0004\u0012\u00020#0\"2\f\u00106\u001a\b\u0012\u0004\u0012\u00020#0\"2\f\u00107\u001a\b\u0012\u0004\u0012\u00020#0\"H\u0082@\u00a2\u0006\u0002\u00108J\b\u00109\u001a\u00020%H\u0002J\b\u0010:\u001a\u00020%H\u0002J\u0010\u0010;\u001a\u00020%2\u0006\u0010,\u001a\u00020\u001eH\u0002J\b\u0010<\u001a\u00020%H\u0002J0\u0010=\u001a\u00020\u001c2\u0006\u0010>\u001a\u00020\u001c2\u0006\u0010?\u001a\u00020\u001c2\u0006\u0010@\u001a\u00020\u001c2\u0006\u0010A\u001a\u00020\u001c2\u0006\u0010B\u001a\u00020\u001cH\u0002R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u001aX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001b\u001a\u0004\u0018\u00010\u001cX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u001eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R8\u0010\u001f\u001a,\u0012(\u0012&\u0012\n\u0012\b\u0012\u0004\u0012\u00020#0\"\u0012\n\u0012\b\u0012\u0004\u0012\u00020#0\"\u0012\n\u0012\b\u0012\u0004\u0012\u00020#0\"0!0 X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006C"}, d2 = {"Lcom/example/sheeps/game/viewmodel/GameViewModel;", "Lcom/example/sheeps/core/base/BaseMviViewModel;", "Lcom/example/sheeps/game/state/GameViewState;", "Lcom/example/sheeps/game/state/GameViewIntent;", "Lcom/example/sheeps/game/state/GameViewEffect;", "apiService", "Lcom/example/sheeps/data/network/ApiService;", "prefs", "Lcom/example/sheeps/core/preference/UserPreferences;", "json", "Lkotlinx/serialization/json/Json;", "localDao", "Lcom/example/sheeps/data/local/LocalDao;", "syncRepository", "Lcom/example/sheeps/data/repository/SyncRepository;", "levelGenerator", "Lcom/example/sheeps/game/viewmodel/helpers/GameLevelGenerator;", "logicDelegate", "Lcom/example/sheeps/game/viewmodel/delegates/GameLogicDelegate;", "toolDelegate", "Lcom/example/sheeps/game/viewmodel/delegates/GameToolDelegate;", "scoreDelegate", "Lcom/example/sheeps/game/viewmodel/delegates/ScoreDelegate;", "<init>", "(Lcom/example/sheeps/data/network/ApiService;Lcom/example/sheeps/core/preference/UserPreferences;Lkotlinx/serialization/json/Json;Lcom/example/sheeps/data/local/LocalDao;Lcom/example/sheeps/data/repository/SyncRepository;Lcom/example/sheeps/game/viewmodel/helpers/GameLevelGenerator;Lcom/example/sheeps/game/viewmodel/delegates/GameLogicDelegate;Lcom/example/sheeps/game/viewmodel/delegates/GameToolDelegate;Lcom/example/sheeps/game/viewmodel/delegates/ScoreDelegate;)V", "levelStartTime", "", "carryItemsJsonStr", "", "itemsUsedCount", "", "historyStack", "", "Lkotlin/Triple;", "", "Lcom/example/sheeps/data/model/Tile;", "handleIntent", "", "intent", "handleInitUser", "handleAgreePrivacy", "handleChangeUsername", "newName", "handleLoadLevel", "levelId", "carryItemsJson", "updateBoardState", "tiles", "carryMap", "", "isOffline", "", "processSlotMatchAndCheckEndGame", "board", "slot", "movedOut", "(Ljava/util/List;Ljava/util/List;Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleRevive", "handleUseDoublePoints", "handleLoadLeaderboard", "saveHistoryState", "getLocalizedString", "zh", "en", "tw", "ja", "ko", "feature_game_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class GameViewModel extends com.example.sheeps.core.base.BaseMviViewModel<com.example.sheeps.game.state.GameViewState, com.example.sheeps.game.state.GameViewIntent, com.example.sheeps.game.state.GameViewEffect> {
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.network.ApiService apiService = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.core.preference.UserPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.serialization.json.Json json = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.local.LocalDao localDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.repository.SyncRepository syncRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator levelGenerator = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.game.viewmodel.delegates.GameLogicDelegate logicDelegate = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.game.viewmodel.delegates.GameToolDelegate toolDelegate = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.game.viewmodel.delegates.ScoreDelegate scoreDelegate = null;
    private long levelStartTime = 0L;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String carryItemsJsonStr;
    private int itemsUsedCount = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<kotlin.Triple<java.util.List<com.example.sheeps.data.model.Tile>, java.util.List<com.example.sheeps.data.model.Tile>, java.util.List<com.example.sheeps.data.model.Tile>>> historyStack = null;
    
    @javax.inject.Inject()
    public GameViewModel(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.network.ApiService apiService, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.preference.UserPreferences prefs, @org.jetbrains.annotations.NotNull()
    kotlinx.serialization.json.Json json, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.local.LocalDao localDao, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.repository.SyncRepository syncRepository, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator levelGenerator, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.viewmodel.delegates.GameLogicDelegate logicDelegate, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.viewmodel.delegates.GameToolDelegate toolDelegate, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.viewmodel.delegates.ScoreDelegate scoreDelegate) {
        super(null);
    }
    
    @java.lang.Override()
    public void handleIntent(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.GameViewIntent intent) {
    }
    
    private final void handleInitUser() {
    }
    
    private final void handleAgreePrivacy() {
    }
    
    private final void handleChangeUsername(java.lang.String newName) {
    }
    
    private final void handleLoadLevel(int levelId, java.lang.String carryItemsJson) {
    }
    
    private final void updateBoardState(java.util.List<com.example.sheeps.data.model.Tile> tiles, java.util.Map<java.lang.String, java.lang.Integer> carryMap, boolean isOffline) {
    }
    
    private final java.lang.Object processSlotMatchAndCheckEndGame(java.util.List<com.example.sheeps.data.model.Tile> board, java.util.List<com.example.sheeps.data.model.Tile> slot, java.util.List<com.example.sheeps.data.model.Tile> movedOut, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void handleRevive() {
    }
    
    private final void handleUseDoublePoints() {
    }
    
    private final void handleLoadLeaderboard(int levelId) {
    }
    
    private final void saveHistoryState() {
    }
    
    private final java.lang.String getLocalizedString(java.lang.String zh, java.lang.String en, java.lang.String tw, java.lang.String ja, java.lang.String ko) {
        return null;
    }
}