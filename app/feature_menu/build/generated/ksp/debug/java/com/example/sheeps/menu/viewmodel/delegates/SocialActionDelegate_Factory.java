package com.example.sheeps.menu.viewmodel.delegates;

import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.local.LocalDao;
import com.example.sheeps.data.network.ApiService;
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
public final class SocialActionDelegate_Factory implements Factory<SocialActionDelegate> {
  private final Provider<ApiService> apiServiceProvider;

  private final Provider<UserPreferences> prefsProvider;

  private final Provider<LocalDao> localDaoProvider;

  private SocialActionDelegate_Factory(Provider<ApiService> apiServiceProvider,
      Provider<UserPreferences> prefsProvider, Provider<LocalDao> localDaoProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.prefsProvider = prefsProvider;
    this.localDaoProvider = localDaoProvider;
  }

  @Override
  public SocialActionDelegate get() {
    return newInstance(apiServiceProvider.get(), prefsProvider.get(), localDaoProvider.get());
  }

  public static SocialActionDelegate_Factory create(Provider<ApiService> apiServiceProvider,
      Provider<UserPreferences> prefsProvider, Provider<LocalDao> localDaoProvider) {
    return new SocialActionDelegate_Factory(apiServiceProvider, prefsProvider, localDaoProvider);
  }

  public static SocialActionDelegate newInstance(ApiService apiService, UserPreferences prefs,
      LocalDao localDao) {
    return new SocialActionDelegate(apiService, prefs, localDao);
  }
}
