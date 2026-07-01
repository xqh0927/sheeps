package com.example.sheeps.game.viewmodel;

import com.example.sheeps.core.multiplayer.WebSocketManager;
import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.local.LocalDao;
import com.example.sheeps.data.network.ApiService;
import com.example.sheeps.game.viewmodel.delegates.DuelActionDelegate;
import com.example.sheeps.game.viewmodel.delegates.DuelCommandHandler;
import com.example.sheeps.game.viewmodel.helpers.DuelLevelGenerator;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;

@ScopeMetadata
@QualifierMetadata
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
public final class DuelViewModel_Factory implements Factory<DuelViewModel> {
  private final Provider<ApiService> apiServiceProvider;

  private final Provider<UserPreferences> prefsProvider;

  private final Provider<WebSocketManager> wsManagerProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<LocalDao> localDaoProvider;

  private final Provider<DuelLevelGenerator> levelGeneratorProvider;

  private final Provider<DuelActionDelegate> actionDelegateProvider;

  private final Provider<DuelCommandHandler> commandHandlerProvider;

  private DuelViewModel_Factory(Provider<ApiService> apiServiceProvider,
      Provider<UserPreferences> prefsProvider, Provider<WebSocketManager> wsManagerProvider,
      Provider<Json> jsonProvider, Provider<LocalDao> localDaoProvider,
      Provider<DuelLevelGenerator> levelGeneratorProvider,
      Provider<DuelActionDelegate> actionDelegateProvider,
      Provider<DuelCommandHandler> commandHandlerProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.prefsProvider = prefsProvider;
    this.wsManagerProvider = wsManagerProvider;
    this.jsonProvider = jsonProvider;
    this.localDaoProvider = localDaoProvider;
    this.levelGeneratorProvider = levelGeneratorProvider;
    this.actionDelegateProvider = actionDelegateProvider;
    this.commandHandlerProvider = commandHandlerProvider;
  }

  @Override
  public DuelViewModel get() {
    return newInstance(apiServiceProvider.get(), prefsProvider.get(), wsManagerProvider.get(), jsonProvider.get(), localDaoProvider.get(), levelGeneratorProvider.get(), actionDelegateProvider.get(), commandHandlerProvider.get());
  }

  public static DuelViewModel_Factory create(Provider<ApiService> apiServiceProvider,
      Provider<UserPreferences> prefsProvider, Provider<WebSocketManager> wsManagerProvider,
      Provider<Json> jsonProvider, Provider<LocalDao> localDaoProvider,
      Provider<DuelLevelGenerator> levelGeneratorProvider,
      Provider<DuelActionDelegate> actionDelegateProvider,
      Provider<DuelCommandHandler> commandHandlerProvider) {
    return new DuelViewModel_Factory(apiServiceProvider, prefsProvider, wsManagerProvider, jsonProvider, localDaoProvider, levelGeneratorProvider, actionDelegateProvider, commandHandlerProvider);
  }

  public static DuelViewModel newInstance(ApiService apiService, UserPreferences prefs,
      WebSocketManager wsManager, Json json, LocalDao localDao, DuelLevelGenerator levelGenerator,
      DuelActionDelegate actionDelegate, DuelCommandHandler commandHandler) {
    return new DuelViewModel(apiService, prefs, wsManager, json, localDao, levelGenerator, actionDelegate, commandHandler);
  }
}
