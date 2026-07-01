package com.example.sheeps.menu.viewmodel;

import com.example.sheeps.core.base.BaseMviViewModel;
import com.example.sheeps.core.game.SkinConstants;
import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.core.utils.NetworkMonitor;
import com.example.sheeps.data.local.LocalDao;
import com.example.sheeps.data.model.LoginResponse;
import com.example.sheeps.data.model.ShopItem;
import com.example.sheeps.data.model.UserItem;
import com.example.sheeps.data.network.ApiService;
import com.example.sheeps.data.repository.SyncRepository;
import com.example.sheeps.menu.state.MenuViewEffect;
import com.example.sheeps.menu.state.MenuViewIntent;
import com.example.sheeps.menu.state.MenuViewState;
import com.example.sheeps.menu.viewmodel.delegates.AuthDelegate;
import com.example.sheeps.menu.viewmodel.delegates.MatchmakingDelegate;
import com.example.sheeps.menu.viewmodel.delegates.SocialActionDelegate;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/**
 * 菜单主界面 ViewModel
 * 负责状态流转、核心业务调度。具体业务逻辑已委派至各 Delegate 实现，保持此类简洁。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000|\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0006\b\u0007\u0018\u00002\u0014\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00040\u0001B[\b\u0007\u0012\b\b\u0001\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\n\u0012\u0006\u0010\u000b\u001a\u00020\f\u0012\u0006\u0010\r\u001a\u00020\u000e\u0012\u0006\u0010\u000f\u001a\u00020\u0010\u0012\u0006\u0010\u0011\u001a\u00020\u0012\u0012\u0006\u0010\u0013\u001a\u00020\u0014\u0012\u0006\u0010\u0015\u001a\u00020\u0016\u0012\u0006\u0010\u0017\u001a\u00020\u0018\u00a2\u0006\u0004\b\u0019\u0010\u001aJ\b\u0010\u001d\u001a\u00020\u001eH\u0002J\b\u0010\u001f\u001a\u00020\u001eH\u0002J\u0010\u0010 \u001a\u00020\u001e2\u0006\u0010!\u001a\u00020\u0003H\u0016J\b\u0010\"\u001a\u00020\u001eH\u0002J\u0014\u0010#\u001a\b\u0012\u0004\u0012\u00020%0$H\u0082@\u00a2\u0006\u0002\u0010&J\u0010\u0010\'\u001a\u00020\u001e2\u0006\u0010(\u001a\u00020\u001cH\u0002J\u0018\u0010)\u001a\u00020\u001e2\u0006\u0010*\u001a\u00020+2\u0006\u0010,\u001a\u00020-H\u0002J\u0010\u0010.\u001a\u00020\u001e2\u0006\u0010/\u001a\u00020+H\u0002J\u0010\u00100\u001a\u00020\u001e2\u0006\u00101\u001a\u00020+H\u0002J\b\u00102\u001a\u00020\u001eH\u0002R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0018X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001b\u001a\u0004\u0018\u00010\u001cX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u00063"}, d2 = {"Lcom/example/sheeps/menu/viewmodel/MenuViewModel;", "Lcom/example/sheeps/core/base/BaseMviViewModel;", "Lcom/example/sheeps/menu/state/MenuViewState;", "Lcom/example/sheeps/menu/state/MenuViewIntent;", "Lcom/example/sheeps/menu/state/MenuViewEffect;", "context", "Landroid/content/Context;", "apiService", "Lcom/example/sheeps/data/network/ApiService;", "prefs", "Lcom/example/sheeps/core/preference/UserPreferences;", "json", "Lkotlinx/serialization/json/Json;", "localDao", "Lcom/example/sheeps/data/local/LocalDao;", "syncRepository", "Lcom/example/sheeps/data/repository/SyncRepository;", "networkMonitor", "Lcom/example/sheeps/core/utils/NetworkMonitor;", "authDelegate", "Lcom/example/sheeps/menu/viewmodel/delegates/AuthDelegate;", "socialActionDelegate", "Lcom/example/sheeps/menu/viewmodel/delegates/SocialActionDelegate;", "matchmakingDelegate", "Lcom/example/sheeps/menu/viewmodel/delegates/MatchmakingDelegate;", "<init>", "(Landroid/content/Context;Lcom/example/sheeps/data/network/ApiService;Lcom/example/sheeps/core/preference/UserPreferences;Lkotlinx/serialization/json/Json;Lcom/example/sheeps/data/local/LocalDao;Lcom/example/sheeps/data/repository/SyncRepository;Lcom/example/sheeps/core/utils/NetworkMonitor;Lcom/example/sheeps/menu/viewmodel/delegates/AuthDelegate;Lcom/example/sheeps/menu/viewmodel/delegates/SocialActionDelegate;Lcom/example/sheeps/menu/viewmodel/delegates/MatchmakingDelegate;)V", "pendingLoginResponse", "Lcom/example/sheeps/data/model/LoginResponse;", "setupObservers", "", "initData", "handleIntent", "intent", "handleLoadData", "fetchShopItems", "", "Lcom/example/sheeps/data/model/ShopItem;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "saveLoginData", "response", "handleUpdateCarryItem", "itemType", "", "change", "", "handleChangeLanguage", "lang", "handleChangeSkin", "skin", "checkAppUpdateOnce", "feature_menu_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class MenuViewModel extends com.example.sheeps.core.base.BaseMviViewModel<com.example.sheeps.menu.state.MenuViewState, com.example.sheeps.menu.state.MenuViewIntent, com.example.sheeps.menu.state.MenuViewEffect> {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
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
    private final com.example.sheeps.core.utils.NetworkMonitor networkMonitor = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.menu.viewmodel.delegates.AuthDelegate authDelegate = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.menu.viewmodel.delegates.SocialActionDelegate socialActionDelegate = null;
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.menu.viewmodel.delegates.MatchmakingDelegate matchmakingDelegate = null;
    @org.jetbrains.annotations.Nullable()
    private com.example.sheeps.data.model.LoginResponse pendingLoginResponse;
    
    @javax.inject.Inject()
    public MenuViewModel(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.network.ApiService apiService, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.preference.UserPreferences prefs, @org.jetbrains.annotations.NotNull()
    kotlinx.serialization.json.Json json, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.local.LocalDao localDao, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.repository.SyncRepository syncRepository, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.utils.NetworkMonitor networkMonitor, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.menu.viewmodel.delegates.AuthDelegate authDelegate, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.menu.viewmodel.delegates.SocialActionDelegate socialActionDelegate, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.menu.viewmodel.delegates.MatchmakingDelegate matchmakingDelegate) {
        super(null);
    }
    
    private final void setupObservers() {
    }
    
    private final void initData() {
    }
    
    @java.lang.Override()
    public void handleIntent(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.menu.state.MenuViewIntent intent) {
    }
    
    private final void handleLoadData() {
    }
    
    private final java.lang.Object fetchShopItems(kotlin.coroutines.Continuation<? super java.util.List<com.example.sheeps.data.model.ShopItem>> $completion) {
        return null;
    }
    
    private final void saveLoginData(com.example.sheeps.data.model.LoginResponse response) {
    }
    
    private final void handleUpdateCarryItem(java.lang.String itemType, int change) {
    }
    
    private final void handleChangeLanguage(java.lang.String lang) {
    }
    
    private final void handleChangeSkin(java.lang.String skin) {
    }
    
    private final void checkAppUpdateOnce() {
    }
}