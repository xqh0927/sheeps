package com.example.sheeps.core.di;

import android.content.Context;
import com.example.sheeps.data.local.AppDatabase;
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
public final class StorageModule_ProvideDatabaseFactory implements Factory<AppDatabase> {
  private final Provider<Context> contextProvider;

  private StorageModule_ProvideDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AppDatabase get() {
    return provideDatabase(contextProvider.get());
  }

  public static StorageModule_ProvideDatabaseFactory create(Provider<Context> contextProvider) {
    return new StorageModule_ProvideDatabaseFactory(contextProvider);
  }

  public static AppDatabase provideDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(StorageModule.INSTANCE.provideDatabase(context));
  }
}
