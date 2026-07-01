package com.example.sheeps.game.viewmodel;

import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.local.LocalDao;
import com.example.sheeps.data.network.ApiService;
import com.example.sheeps.data.repository.SyncRepository;
import com.example.sheeps.game.viewmodel.delegates.GameLogicDelegate;
import com.example.sheeps.game.viewmodel.delegates.GameToolDelegate;
import com.example.sheeps.game.viewmodel.delegates.ScoreDelegate;
import com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator;
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
public final class GameViewModel_Factory implements Factory<GameViewModel> {
  private final Provider<ApiService> apiServiceProvider;

  private final Provider<UserPreferences> prefsProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<LocalDao> localDaoProvider;

  private final Provider<SyncRepository> syncRepositoryProvider;

  private final Provider<GameLevelGenerator> levelGeneratorProvider;

  private final Provider<GameLogicDelegate> logicDelegateProvider;

  private final Provider<GameToolDelegate> toolDelegateProvider;

  private final Provider<ScoreDelegate> scoreDelegateProvider;

  private GameViewModel_Factory(Provider<ApiService> apiServiceProvider,
      Provider<UserPreferences> prefsProvider, Provider<Json> jsonProvider,
      Provider<LocalDao> localDaoProvider, Provider<SyncRepository> syncRepositoryProvider,
      Provider<GameLevelGenerator> levelGeneratorProvider,
      Provider<GameLogicDelegate> logicDelegateProvider,
      Provider<GameToolDelegate> toolDelegateProvider,
      Provider<ScoreDelegate> scoreDelegateProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.prefsProvider = prefsProvider;
    this.jsonProvider = jsonProvider;
    this.localDaoProvider = localDaoProvider;
    this.syncRepositoryProvider = syncRepositoryProvider;
    this.levelGeneratorProvider = levelGeneratorProvider;
    this.logicDelegateProvider = logicDelegateProvider;
    this.toolDelegateProvider = toolDelegateProvider;
    this.scoreDelegateProvider = scoreDelegateProvider;
  }

  @Override
  public GameViewModel get() {
    return newInstance(apiServiceProvider.get(), prefsProvider.get(), jsonProvider.get(), localDaoProvider.get(), syncRepositoryProvider.get(), levelGeneratorProvider.get(), logicDelegateProvider.get(), toolDelegateProvider.get(), scoreDelegateProvider.get());
  }

  public static GameViewModel_Factory create(Provider<ApiService> apiServiceProvider,
      Provider<UserPreferences> prefsProvider, Provider<Json> jsonProvider,
      Provider<LocalDao> localDaoProvider, Provider<SyncRepository> syncRepositoryProvider,
      Provider<GameLevelGenerator> levelGeneratorProvider,
      Provider<GameLogicDelegate> logicDelegateProvider,
      Provider<GameToolDelegate> toolDelegateProvider,
      Provider<ScoreDelegate> scoreDelegateProvider) {
    return new GameViewModel_Factory(apiServiceProvider, prefsProvider, jsonProvider, localDaoProvider, syncRepositoryProvider, levelGeneratorProvider, logicDelegateProvider, toolDelegateProvider, scoreDelegateProvider);
  }

  public static GameViewModel newInstance(ApiService apiService, UserPreferences prefs, Json json,
      LocalDao localDao, SyncRepository syncRepository, GameLevelGenerator levelGenerator,
      GameLogicDelegate logicDelegate, GameToolDelegate toolDelegate, ScoreDelegate scoreDelegate) {
    return new GameViewModel(apiService, prefs, json, localDao, syncRepository, levelGenerator, logicDelegate, toolDelegate, scoreDelegate);
  }
}
