package com.voicebridge.data.network;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
    "cast"
})
public final class NearbyConnectionManager_Factory implements Factory<NearbyConnectionManager> {
  private final Provider<Context> contextProvider;

  public NearbyConnectionManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NearbyConnectionManager get() {
    return newInstance(contextProvider.get());
  }

  public static NearbyConnectionManager_Factory create(Provider<Context> contextProvider) {
    return new NearbyConnectionManager_Factory(contextProvider);
  }

  public static NearbyConnectionManager newInstance(Context context) {
    return new NearbyConnectionManager(context);
  }
}
