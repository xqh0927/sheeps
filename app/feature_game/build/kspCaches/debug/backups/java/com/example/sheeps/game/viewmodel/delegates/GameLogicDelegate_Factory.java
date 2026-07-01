package com.example.sheeps.game.viewmodel.delegates;

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
public final class GameLogicDelegate_Factory implements Factory<GameLogicDelegate> {
  @Override
  public GameLogicDelegate get() {
    return newInstance();
  }

  public static GameLogicDelegate_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GameLogicDelegate newInstance() {
    return new GameLogicDelegate();
  }

  private static final class InstanceHolder {
    static final GameLogicDelegate_Factory INSTANCE = new GameLogicDelegate_Factory();
  }
}
