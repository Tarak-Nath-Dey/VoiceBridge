package com.voicebridge.domain.repository;

import android.content.Context;
import com.voicebridge.data.local.dao.UserDao;
import com.voicebridge.data.security.EncryptionManager;
import com.voicebridge.data.security.KeyExchangeManager;
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
public final class UserRepository_Factory implements Factory<UserRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<UserDao> userDaoProvider;

  private final Provider<EncryptionManager> encryptionManagerProvider;

  private final Provider<KeyExchangeManager> keyExchangeManagerProvider;

  public UserRepository_Factory(Provider<Context> contextProvider,
      Provider<UserDao> userDaoProvider, Provider<EncryptionManager> encryptionManagerProvider,
      Provider<KeyExchangeManager> keyExchangeManagerProvider) {
    this.contextProvider = contextProvider;
    this.userDaoProvider = userDaoProvider;
    this.encryptionManagerProvider = encryptionManagerProvider;
    this.keyExchangeManagerProvider = keyExchangeManagerProvider;
  }

  @Override
  public UserRepository get() {
    return newInstance(contextProvider.get(), userDaoProvider.get(), encryptionManagerProvider.get(), keyExchangeManagerProvider.get());
  }

  public static UserRepository_Factory create(Provider<Context> contextProvider,
      Provider<UserDao> userDaoProvider, Provider<EncryptionManager> encryptionManagerProvider,
      Provider<KeyExchangeManager> keyExchangeManagerProvider) {
    return new UserRepository_Factory(contextProvider, userDaoProvider, encryptionManagerProvider, keyExchangeManagerProvider);
  }

  public static UserRepository newInstance(Context context, UserDao userDao,
      EncryptionManager encryptionManager, KeyExchangeManager keyExchangeManager) {
    return new UserRepository(context, userDao, encryptionManager, keyExchangeManager);
  }
}
