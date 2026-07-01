package com.example.sheeps.menu;

import com.example.sheeps.core.preference.UserPreferences;
import com.example.sheeps.data.network.ApiService;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;

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
public final class MenuActivity_MembersInjector implements MembersInjector<MenuActivity> {
  private final Provider<Json> jsonProvider;

  private final Provider<ApiService> apiServiceProvider;

  private final Provider<UserPreferences> userPrefsProvider;

  private MenuActivity_MembersInjector(Provider<Json> jsonProvider,
      Provider<ApiService> apiServiceProvider, Provider<UserPreferences> userPrefsProvider) {
    this.jsonProvider = jsonProvider;
    this.apiServiceProvider = apiServiceProvider;
    this.userPrefsProvider = userPrefsProvider;
  }

  @Override
  public void injectMembers(MenuActivity instance) {
    injectJson(instance, jsonProvider.get());
    injectApiService(instance, apiServiceProvider.get());
    injectUserPrefs(instance, userPrefsProvider.get());
  }

  public static MembersInjector<MenuActivity> create(Provider<Json> jsonProvider,
      Provider<ApiService> apiServiceProvider, Provider<UserPreferences> userPrefsProvider) {
    return new MenuActivity_MembersInjector(jsonProvider, apiServiceProvider, userPrefsProvider);
  }

  @InjectedFieldSignature("com.example.sheeps.menu.MenuActivity.json")
  public static void injectJson(MenuActivity instance, Json json) {
    instance.json = json;
  }

  @InjectedFieldSignature("com.example.sheeps.menu.MenuActivity.apiService")
  public static void injectApiService(MenuActivity instance, ApiService apiService) {
    instance.apiService = apiService;
  }

  @InjectedFieldSignature("com.example.sheeps.menu.MenuActivity.userPrefs")
  public static void injectUserPrefs(MenuActivity instance, UserPreferences userPrefs) {
    instance.userPrefs = userPrefs;
  }
}
