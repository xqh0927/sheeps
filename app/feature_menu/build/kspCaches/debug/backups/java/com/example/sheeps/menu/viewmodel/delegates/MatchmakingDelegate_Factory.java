package com.example.sheeps.menu.viewmodel.delegates;

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
public final class MatchmakingDelegate_Factory implements Factory<MatchmakingDelegate> {
  private final Provider<ApiService> apiServiceProvider;

  private MatchmakingDelegate_Factory(Provider<ApiService> apiServiceProvider) {
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public MatchmakingDelegate get() {
    return newInstance(apiServiceProvider.get());
  }

  public static MatchmakingDelegate_Factory create(Provider<ApiService> apiServiceProvider) {
    return new MatchmakingDelegate_Factory(apiServiceProvider);
  }

  public static MatchmakingDelegate newInstance(ApiService apiService) {
    return new MatchmakingDelegate(apiService);
  }
}
