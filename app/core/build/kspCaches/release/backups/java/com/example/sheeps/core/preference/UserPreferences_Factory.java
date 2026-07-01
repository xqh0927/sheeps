package com.example.sheeps.core.preference;

import android.content.Context;
import com.tencent.mmkv.MMKV;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class UserPreferences_Factory implements Factory<UserPreferences> {
  private final Provider<MMKV> kvProvider;

  private final Provider<Context> contextProvider;

  private UserPreferences_Factory(Provider<MMKV> kvProvider, Provider<Context> contextProvider) {
    this.kvProvider = kvProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public UserPreferences get() {
    return newInstance(kvProvider.get(), contextProvider.get());
  }

  public static UserPreferences_Factory create(Provider<MMKV> kvProvider,
      Provider<Context> contextProvider) {
    return new UserPreferences_Factory(kvProvider, contextProvider);
  }

  public static UserPreferences newInstance(MMKV kv, Context context) {
    return new UserPreferences(kv, context);
  }
}
