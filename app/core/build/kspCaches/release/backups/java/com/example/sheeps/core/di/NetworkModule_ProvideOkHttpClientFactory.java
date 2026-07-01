package com.example.sheeps.core.di;

import com.example.sheeps.core.preference.UserPreferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;

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
public final class NetworkModule_ProvideOkHttpClientFactory implements Factory<OkHttpClient> {
  private final Provider<UserPreferences> prefsProvider;

  private final Provider<Json> jsonProvider;

  private NetworkModule_ProvideOkHttpClientFactory(Provider<UserPreferences> prefsProvider,
      Provider<Json> jsonProvider) {
    this.prefsProvider = prefsProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideOkHttpClient(prefsProvider.get(), jsonProvider.get());
  }

  public static NetworkModule_ProvideOkHttpClientFactory create(
      Provider<UserPreferences> prefsProvider, Provider<Json> jsonProvider) {
    return new NetworkModule_ProvideOkHttpClientFactory(prefsProvider, jsonProvider);
  }

  public static OkHttpClient provideOkHttpClient(UserPreferences prefs, Json json) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideOkHttpClient(prefs, json));
  }
}
