package com.example.sheeps.leaderboard;

import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.network.ApiService;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class LeaderboardActivity_MembersInjector implements MembersInjector<LeaderboardActivity> {
  private final Provider<ApiService> apiServiceProvider;

  private final Provider<UserPreferences> userPrefsProvider;

  private LeaderboardActivity_MembersInjector(Provider<ApiService> apiServiceProvider,
      Provider<UserPreferences> userPrefsProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.userPrefsProvider = userPrefsProvider;
  }

  @Override
  public void injectMembers(LeaderboardActivity instance) {
    injectApiService(instance, apiServiceProvider.get());
    injectUserPrefs(instance, userPrefsProvider.get());
  }

  public static MembersInjector<LeaderboardActivity> create(Provider<ApiService> apiServiceProvider,
      Provider<UserPreferences> userPrefsProvider) {
    return new LeaderboardActivity_MembersInjector(apiServiceProvider, userPrefsProvider);
  }

  @InjectedFieldSignature("com.example.sheeps.leaderboard.LeaderboardActivity.apiService")
  public static void injectApiService(LeaderboardActivity instance, ApiService apiService) {
    instance.apiService = apiService;
  }

  @InjectedFieldSignature("com.example.sheeps.leaderboard.LeaderboardActivity.userPrefs")
  public static void injectUserPrefs(LeaderboardActivity instance, UserPreferences userPrefs) {
    instance.userPrefs = userPrefs;
  }
}
