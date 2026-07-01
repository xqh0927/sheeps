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
public final class GameToolDelegate_Factory implements Factory<GameToolDelegate> {
  @Override
  public GameToolDelegate get() {
    return newInstance();
  }

  public static GameToolDelegate_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GameToolDelegate newInstance() {
    return new GameToolDelegate();
  }

  private static final class InstanceHolder {
    static final GameToolDelegate_Factory INSTANCE = new GameToolDelegate_Factory();
  }
}
