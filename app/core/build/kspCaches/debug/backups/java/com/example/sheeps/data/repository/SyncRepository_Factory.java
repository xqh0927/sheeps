package com.example.sheeps.data.repository;

import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.core.utils.NetworkMonitor;
import com.example.sheeps.data.local.LocalDao;
import com.example.sheeps.data.network.ApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class SyncRepository_Factory implements Factory<SyncRepository> {
  private final Provider<ApiService> apiServiceProvider;

  private final Provider<LocalDao> localDaoProvider;

  private final Provider<UserPreferences> userPreferencesProvider;

  private final Provider<NetworkMonitor> networkMonitorProvider;

  private SyncRepository_Factory(Provider<ApiService> apiServiceProvider,
      Provider<LocalDao> localDaoProvider, Provider<UserPreferences> userPreferencesProvider,
      Provider<NetworkMonitor> networkMonitorProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.localDaoProvider = localDaoProvider;
    this.userPreferencesProvider = userPreferencesProvider;
    this.networkMonitorProvider = networkMonitorProvider;
  }

  @Override
  public SyncRepository get() {
    return newInstance(apiServiceProvider.get(), localDaoProvider.get(), userPreferencesProvider.get(), networkMonitorProvider.get());
  }

  public static SyncRepository_Factory create(Provider<ApiService> apiServiceProvider,
      Provider<LocalDao> localDaoProvider, Provider<UserPreferences> userPreferencesProvider,
      Provider<NetworkMonitor> networkMonitorProvider) {
    return new SyncRepository_Factory(apiServiceProvider, localDaoProvider, userPreferencesProvider, networkMonitorProvider);
  }

  public static SyncRepository newInstance(ApiService apiService, LocalDao localDao,
      UserPreferences userPreferences, NetworkMonitor networkMonitor) {
    return new SyncRepository(apiService, localDao, userPreferences, networkMonitor);
  }
}
