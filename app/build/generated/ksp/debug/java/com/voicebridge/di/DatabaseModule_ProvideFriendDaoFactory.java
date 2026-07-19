package com.voicebridge.di;

import com.voicebridge.data.local.VoiceBridgeDatabase;
import com.voicebridge.data.local.dao.FriendDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
    "cast"
})
public final class DatabaseModule_ProvideFriendDaoFactory implements Factory<FriendDao> {
  private final Provider<VoiceBridgeDatabase> dbProvider;

  public DatabaseModule_ProvideFriendDaoFactory(Provider<VoiceBridgeDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public FriendDao get() {
    return provideFriendDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideFriendDaoFactory create(
      Provider<VoiceBridgeDatabase> dbProvider) {
    return new DatabaseModule_ProvideFriendDaoFactory(dbProvider);
  }

  public static FriendDao provideFriendDao(VoiceBridgeDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideFriendDao(db));
  }
}
