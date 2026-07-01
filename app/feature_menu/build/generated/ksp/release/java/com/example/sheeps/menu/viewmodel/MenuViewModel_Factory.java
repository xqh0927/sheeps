package com.example.sheeps.menu.viewmodel;

import android.content.Context;
import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.core.utils.NetworkMonitor;
import com.example.sheeps.data.local.LocalDao;
import com.example.sheeps.data.network.ApiService;
import com.example.sheeps.data.repository.SyncRepository;
import com.example.sheeps.menu.viewmodel.delegates.AuthDelegate;
import com.example.sheeps.menu.viewmodel.delegates.MatchmakingDelegate;
import com.example.sheeps.menu.viewmodel.delegates.SocialActionDelegate;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class MenuViewModel_Factory implements Factory<MenuViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<ApiService> apiServiceProvider;

  private final Provider<UserPreferences> prefsProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<LocalDao> localDaoProvider;

  private final Provider<SyncRepository> syncRepositoryProvider;

  private final Provider<NetworkMonitor> networkMonitorProvider;

  private final Provider<AuthDelegate> authDelegateProvider;

  private final Provider<SocialActionDelegate> socialActionDelegateProvider;

  private final Provider<MatchmakingDelegate> matchmakingDelegateProvider;

  private MenuViewModel_Factory(Provider<Context> contextProvider,
      Provider<ApiService> apiServiceProvider, Provider<UserPreferences> prefsProvider,
      Provider<Json> jsonProvider, Provider<LocalDao> localDaoProvider,
      Provider<SyncRepository> syncRepositoryProvider,
      Provider<NetworkMonitor> networkMonitorProvider, Provider<AuthDelegate> authDelegateProvider,
      Provider<SocialActionDelegate> socialActionDelegateProvider,
      Provider<MatchmakingDelegate> matchmakingDelegateProvider) {
    this.contextProvider = contextProvider;
    this.apiServiceProvider = apiServiceProvider;
    this.prefsProvider = prefsProvider;
    this.jsonProvider = jsonProvider;
    this.localDaoProvider = localDaoProvider;
    this.syncRepositoryProvider = syncRepositoryProvider;
    this.networkMonitorProvider = networkMonitorProvider;
    this.authDelegateProvider = authDelegateProvider;
    this.socialActionDelegateProvider = socialActionDelegateProvider;
    this.matchmakingDelegateProvider = matchmakingDelegateProvider;
  }

  @Override
  public MenuViewModel get() {
    return newInstance(contextProvider.get(), apiServiceProvider.get(), prefsProvider.get(), jsonProvider.get(), localDaoProvider.get(), syncRepositoryProvider.get(), networkMonitorProvider.get(), authDelegateProvider.get(), socialActionDelegateProvider.get(), matchmakingDelegateProvider.get());
  }

  public static MenuViewModel_Factory create(Provider<Context> contextProvider,
      Provider<ApiService> apiServiceProvider, Provider<UserPreferences> prefsProvider,
      Provider<Json> jsonProvider, Provider<LocalDao> localDaoProvider,
      Provider<SyncRepository> syncRepositoryProvider,
      Provider<NetworkMonitor> networkMonitorProvider, Provider<AuthDelegate> authDelegateProvider,
      Provider<SocialActionDelegate> socialActionDelegateProvider,
      Provider<MatchmakingDelegate> matchmakingDelegateProvider) {
    return new MenuViewModel_Factory(contextProvider, apiServiceProvider, prefsProvider, jsonProvider, localDaoProvider, syncRepositoryProvider, networkMonitorProvider, authDelegateProvider, socialActionDelegateProvider, matchmakingDelegateProvider);
  }

  public static MenuViewModel newInstance(Context context, ApiService apiService,
      UserPreferences prefs, Json json, LocalDao localDao, SyncRepository syncRepository,
      NetworkMonitor networkMonitor, AuthDelegate authDelegate,
      SocialActionDelegate socialActionDelegate, MatchmakingDelegate matchmakingDelegate) {
    return new MenuViewModel(context, apiService, prefs, json, localDao, syncRepository, networkMonitor, authDelegate, socialActionDelegate, matchmakingDelegate);
  }
}
