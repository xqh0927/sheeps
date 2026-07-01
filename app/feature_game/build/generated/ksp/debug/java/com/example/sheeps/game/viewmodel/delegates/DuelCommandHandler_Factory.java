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
public final class DuelCommandHandler_Factory implements Factory<DuelCommandHandler> {
  @Override
  public DuelCommandHandler get() {
    return newInstance();
  }

  public static DuelCommandHandler_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static DuelCommandHandler newInstance() {
    return new DuelCommandHandler();
  }

  private static final class InstanceHolder {
    static final DuelCommandHandler_Factory INSTANCE = new DuelCommandHandler_Factory();
  }
}
