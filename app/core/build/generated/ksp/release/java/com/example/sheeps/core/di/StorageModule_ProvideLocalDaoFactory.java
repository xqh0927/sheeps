package com.example.sheeps.core.di;

import com.example.sheeps.data.local.AppDatabase;
import com.example.sheeps.data.local.LocalDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class StorageModule_ProvideLocalDaoFactory implements Factory<LocalDao> {
  private final Provider<AppDatabase> dbProvider;

  private StorageModule_ProvideLocalDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public LocalDao get() {
    return provideLocalDao(dbProvider.get());
  }

  public static StorageModule_ProvideLocalDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new StorageModule_ProvideLocalDaoFactory(dbProvider);
  }

  public static LocalDao provideLocalDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(StorageModule.INSTANCE.provideLocalDao(db));
  }
}
