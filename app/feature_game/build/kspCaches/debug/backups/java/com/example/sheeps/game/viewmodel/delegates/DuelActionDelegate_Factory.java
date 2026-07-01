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
public final class DuelActionDelegate_Factory implements Factory<DuelActionDelegate> {
  @Override
  public DuelActionDelegate get() {
    return newInstance();
  }

  public static DuelActionDelegate_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static DuelActionDelegate newInstance() {
    return new DuelActionDelegate();
  }

  private static final class InstanceHolder {
    static final DuelActionDelegate_Factory INSTANCE = new DuelActionDelegate_Factory();
  }
}
