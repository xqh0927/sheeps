package com.example.sheeps.core.di;

import android.content.Context;
import com.example.sheeps.core.preference.UserPreferences;
import com.tencent.mmkv.MMKV;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class StorageModule_ProvideUserPreferencesFactory implements Factory<UserPreferences> {
  private final Provider<MMKV> mmkvProvider;

  private final Provider<Context> contextProvider;

  private StorageModule_ProvideUserPreferencesFactory(Provider<MMKV> mmkvProvider,
      Provider<Context> contextProvider) {
    this.mmkvProvider = mmkvProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public UserPreferences get() {
    return provideUserPreferences(mmkvProvider.get(), contextProvider.get());
  }

  public static StorageModule_ProvideUserPreferencesFactory create(Provider<MMKV> mmkvProvider,
      Provider<Context> contextProvider) {
    return new StorageModule_ProvideUserPreferencesFactory(mmkvProvider, contextProvider);
  }

  public static UserPreferences provideUserPreferences(MMKV mmkv, Context context) {
    return Preconditions.checkNotNullFromProvides(StorageModule.INSTANCE.provideUserPreferences(mmkv, context));
  }
}
