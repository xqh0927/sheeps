package com.example.sheeps.game.viewmodel.helpers;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class DuelLevelGenerator_Factory implements Factory<DuelLevelGenerator> {
  @Override
  public DuelLevelGenerator get() {
    return newInstance();
  }

  public static DuelLevelGenerator_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static DuelLevelGenerator newInstance() {
    return new DuelLevelGenerator();
  }

  private static final class InstanceHolder {
    static final DuelLevelGenerator_Factory INSTANCE = new DuelLevelGenerator_Factory();
  }
}
