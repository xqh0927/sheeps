package com.example.sheeps.core.di;

import com.tencent.mmkv.MMKV;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class StorageModule_ProvideMmkvFactory implements Factory<MMKV> {
  @Override
  public MMKV get() {
    return provideMmkv();
  }

  public static StorageModule_ProvideMmkvFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MMKV provideMmkv() {
    return Preconditions.checkNotNullFromProvides(StorageModule.INSTANCE.provideMmkv());
  }

  private static final class InstanceHolder {
    static final StorageModule_ProvideMmkvFactory INSTANCE = new StorageModule_ProvideMmkvFactory();
  }
}
