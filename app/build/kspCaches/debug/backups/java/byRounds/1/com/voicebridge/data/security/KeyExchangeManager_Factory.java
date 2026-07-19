package com.voicebridge.data.security;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
    "cast"
})
public final class KeyExchangeManager_Factory implements Factory<KeyExchangeManager> {
  @Override
  public KeyExchangeManager get() {
    return newInstance();
  }

  public static KeyExchangeManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static KeyExchangeManager newInstance() {
    return new KeyExchangeManager();
  }

  private static final class InstanceHolder {
    private static final KeyExchangeManager_Factory INSTANCE = new KeyExchangeManager_Factory();
  }
}
