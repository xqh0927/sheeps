package com.example.sheeps.game.viewmodel;

import com.example.sheeps.core.base.BaseMviViewModel;
import com.example.sheeps.core.multiplayer.WebSocketManager;
import com.example.sheeps.core.multiplayer.model.*;
import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.local.LocalDao;
import com.example.sheeps.data.model.*;
import com.example.sheeps.data.network.ApiService;
import com.example.sheeps.game.state.*;
import com.example.sheeps.game.viewmodel.delegates.DuelActionDelegate;
import com.example.sheeps.game.viewmodel.delegates.DuelCommandHandler;
import com.example.sheeps.game.viewmodel.helpers.DuelLevelGenerator;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.Dispatchers;
import javax.inject.Inject;

/**
 * 对决模式 ViewModel
 * 处理 WebSocket 实时对战逻辑。核心业务分发至 ActionDelegate 和 CommandHandler 实现。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000r\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0013\b\u0007\u0018\u00002\u0014\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00040\u0001BI\b\u0007\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\n\u0012\u0006\u0010\u000b\u001a\u00020\f\u0012\u0006\u0010\r\u001a\u00020\u000e\u0012\u0006\u0010\u000f\u001a\u00020\u0010\u0012\u0006\u0010\u0011\u001a\u00020\u0012\u0012\u0006\u0010\u0013\u001a\u00020\u0014\u00a2\u0006\u0004\b\u0015\u0010\u0016J\u0010\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\u0003H\u0016J(\u0010\u001f\u001a\u00020\u001d2\u0006\u0010 \u001a\u00020!2\u0006\u0010\"\u001a\u00020!2\u0006\u0010#\u001a\u00020\u001a2\u0006\u0010$\u001a\u00020\u001aH\u0002J8\u0010%\u001a\u00020\u001d2\f\u0010&\u001a\b\u0012\u0004\u0012\u00020(0\'2\f\u0010)\u001a\b\u0012\u0004\u0012\u00020(0\'2\f\u0010*\u001a\b\u0012\u0004\u0012\u00020(0\'H\u0082@\u00a2\u0006\u0002\u0010+J\u0016\u0010,\u001a\u00020\u001d2\f\u0010-\u001a\b\u0012\u0004\u0012\u00020!0\'H\u0002J\b\u0010.\u001a\u00020\u001dH\u0002J\u0010\u0010/\u001a\u00020\u001d2\u0006\u00100\u001a\u00020!H\u0002J\u0010\u00101\u001a\u00020\u001d2\u0006\u00102\u001a\u00020!H\u0002J\b\u00103\u001a\u00020\u001dH\u0002J\b\u00104\u001a\u00020\u001dH\u0002J0\u00105\u001a\u00020!2\u0006\u00106\u001a\u00020!2\u0006\u00107\u001a\u00020!2\u0006\u00108\u001a\u00020!2\u0006\u00109\u001a\u00020!2\u0006\u0010:\u001a\u00020!H\u0002R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0018X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u001aX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001b\u001a\u00020\u001aX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006;"}, d2 = {"Lcom/example/sheeps/game/viewmodel/DuelViewModel;", "Lcom/example/sheeps/core/base/BaseMviViewModel;", "Lcom/example/sheeps/game/state/DuelViewState;", "Lcom/example/sheeps/game/state/DuelViewIntent;", "Lcom/example/sheeps/game/state/DuelViewEffect;", "apiService", "Lcom/example/sheeps/data/network/ApiService;", "prefs", "Lcom/example/sheeps/core/preference/UserPreferences;", "wsManager", "Lcom/example/sheeps/core/multiplayer/WebSocketManager;", "json", "Lkotlinx/serialization/json/Json;", "localDao", "Lcom/example/sheeps/data/local/LocalDao;", "levelGenerator", "Lcom/example/sheeps/game/viewmodel/helpers/DuelLevelGenerator;", "actionDelegate", "Lcom/example/sheeps/game/viewmodel/delegates/DuelActionDelegate;", "commandHandler", "Lcom/example/sheeps/game/viewmodel/delegates/DuelCommandHandler;", "<init>", "(Lcom/example/sheeps/data/network/ApiService;Lcom/example/sheeps/core/preference/UserPreferences;Lcom/example/sheeps/core/multiplayer/WebSocketManager;Lkotlinx/serialization/json/Json;Lcom/example/sheeps/data/local/LocalDao;Lcom/example/sheeps/game/viewmodel/helpers/DuelLevelGenerator;Lcom/example/sheeps/game/viewmodel/delegates/DuelActionDelegate;Lcom/example/sheeps/game/viewmodel/delegates/DuelCommandHandler;)V", "seqId", "", "currentLevelId", "", "currentGameSeed", "handleIntent", "", "intent", "handleInit", "gameId", "", "playerId", "levelId", "seed", "processSlotMatch", "board", "", "Lcom/example/sheeps/data/model/Tile;", "slot", "movedOut", "(Ljava/util/List;Ljava/util/List;Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendEliminateCommand", "ids", "sendAttackCommand", "sendCastSpellCommand", "spellType", "sendSystemEvent", "msg", "handleRestart", "handleLeave", "getLocalizedString", "zh", "en", "tw", "ja", "ko", "feature_game_release"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class DuelViewModel extends com.example.sheeps.core.base.BaseMviViewModel<com.example.sheeps.game.state.DuelViewState, com.example.sheeps.game.state.DuelViewIntent, com.example.sheeps.game.state.DuelViewEffect> {
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.network.ApiService apiService = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.core.preference.UserPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.core.multiplayer.WebSocketManager wsManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.serialization.json.Json json = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.data.local.LocalDao localDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.game.viewmodel.helpers.DuelLevelGenerator levelGenerator = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.game.viewmodel.delegates.DuelActionDelegate actionDelegate = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.game.viewmodel.delegates.DuelCommandHandler commandHandler = null;
    private long seqId = 0L;
    private int currentLevelId = 2;
    private int currentGameSeed = 0;
    
    @javax.inject.Inject()
    public DuelViewModel(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.network.ApiService apiService, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.preference.UserPreferences prefs, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.multiplayer.WebSocketManager wsManager, @org.jetbrains.annotations.NotNull()
    kotlinx.serialization.json.Json json, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.local.LocalDao localDao, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.viewmodel.helpers.DuelLevelGenerator levelGenerator, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.viewmodel.delegates.DuelActionDelegate actionDelegate, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.viewmodel.delegates.DuelCommandHandler commandHandler) {
        super(null);
    }
    
    @java.lang.Override()
    public void handleIntent(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.DuelViewIntent intent) {
    }
    
    private final void handleInit(java.lang.String gameId, java.lang.String playerId, int levelId, int seed) {
    }
    
    private final java.lang.Object processSlotMatch(java.util.List<com.example.sheeps.data.model.Tile> board, java.util.List<com.example.sheeps.data.model.Tile> slot, java.util.List<com.example.sheeps.data.model.Tile> movedOut, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void sendEliminateCommand(java.util.List<java.lang.String> ids) {
    }
    
    private final void sendAttackCommand() {
    }
    
    private final void sendCastSpellCommand(java.lang.String spellType) {
    }
    
    private final void sendSystemEvent(java.lang.String msg) {
    }
    
    private final void handleRestart() {
    }
    
    private final void handleLeave() {
    }
    
    private final java.lang.String getLocalizedString(java.lang.String zh, java.lang.String en, java.lang.String tw, java.lang.String ja, java.lang.String ko) {
        return null;
    }
}