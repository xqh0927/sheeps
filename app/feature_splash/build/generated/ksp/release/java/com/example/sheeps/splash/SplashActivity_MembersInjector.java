package com.example.sheeps.splash;

import com.example.sheeps.core.preference.UserPreferences;
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
public final class SplashActivity_MembersInjector implements MembersInjector<SplashActivity> {
  private final Provider<UserPreferences> userPrefsProvider;

  private SplashActivity_MembersInjector(Provider<UserPreferences> userPrefsProvider) {
    this.userPrefsProvider = userPrefsProvider;
  }

  @Override
  public void injectMembers(SplashActivity instance) {
    injectUserPrefs(instance, userPrefsProvider.get());
  }

  public static MembersInjector<SplashActivity> create(
      Provider<UserPreferences> userPrefsProvider) {
    return new SplashActivity_MembersInjector(userPrefsProvider);
  }

  @InjectedFieldSignature("com.example.sheeps.splash.SplashActivity.userPrefs")
  public static void injectUserPrefs(SplashActivity instance, UserPreferences userPrefs) {
    instance.userPrefs = userPrefs;
  }
}
