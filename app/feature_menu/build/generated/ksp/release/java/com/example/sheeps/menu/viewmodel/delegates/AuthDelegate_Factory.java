package com.example.sheeps.menu.viewmodel.delegates;

import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.local.LocalDao;
import com.example.sheeps.data.network.ApiService;
import com.example.sheeps.data.repository.SyncRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AuthDelegate_Factory implements Factory<AuthDelegate> {
  private final Provider<ApiService> apiServiceProvider;

  private final Provider<UserPreferences> prefsProvider;

  private final Provider<LocalDao> localDaoProvider;

  private final Provider<SyncRepository> syncRepositoryProvider;

  private AuthDelegate_Factory(Provider<ApiService> apiServiceProvider,
      Provider<UserPreferences> prefsProvider, Provider<LocalDao> localDaoProvider,
      Provider<SyncRepository> syncRepositoryProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.prefsProvider = prefsProvider;
    this.localDaoProvider = localDaoProvider;
    this.syncRepositoryProvider = syncRepositoryProvider;
  }

  @Override
  public AuthDelegate get() {
    return newInstance(apiServiceProvider.get(), prefsProvider.get(), localDaoProvider.get(), syncRepositoryProvider.get());
  }

  public static AuthDelegate_Factory create(Provider<ApiService> apiServiceProvider,
      Provider<UserPreferences> prefsProvider, Provider<LocalDao> localDaoProvider,
      Provider<SyncRepository> syncRepositoryProvider) {
    return new AuthDelegate_Factory(apiServiceProvider, prefsProvider, localDaoProvider, syncRepositoryProvider);
  }

  public static AuthDelegate newInstance(ApiService apiService, UserPreferences prefs,
      LocalDao localDao, SyncRepository syncRepository) {
    return new AuthDelegate(apiService, prefs, localDao, syncRepository);
  }
}
