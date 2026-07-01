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
public final class GameLevelGenerator_Factory implements Factory<GameLevelGenerator> {
  @Override
  public GameLevelGenerator get() {
    return newInstance();
  }

  public static GameLevelGenerator_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GameLevelGenerator newInstance() {
    return new GameLevelGenerator();
  }

  private static final class InstanceHolder {
    static final GameLevelGenerator_Factory INSTANCE = new GameLevelGenerator_Factory();
  }
}
