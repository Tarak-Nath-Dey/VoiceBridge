package com.voicebridge.domain.repository;

import android.content.Context;
import com.voicebridge.data.local.dao.ChatDao;
import com.voicebridge.data.local.dao.FriendDao;
import com.voicebridge.data.local.dao.MessageDao;
import com.voicebridge.data.local.dao.UserDao;
import com.voicebridge.data.network.NearbyConnectionManager;
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
public final class ChatRepository_Factory implements Factory<ChatRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<UserDao> userDaoProvider;

  private final Provider<FriendDao> friendDaoProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<EncryptionManager> encryptionManagerProvider;

  private final Provider<KeyExchangeManager> keyExchangeManagerProvider;

  private final Provider<NearbyConnectionManager> nearbyConnectionManagerProvider;

  public ChatRepository_Factory(Provider<Context> contextProvider,
      Provider<UserDao> userDaoProvider, Provider<FriendDao> friendDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ChatDao> chatDaoProvider,
      Provider<EncryptionManager> encryptionManagerProvider,
      Provider<KeyExchangeManager> keyExchangeManagerProvider,
      Provider<NearbyConnectionManager> nearbyConnectionManagerProvider) {
    this.contextProvider = contextProvider;
    this.userDaoProvider = userDaoProvider;
    this.friendDaoProvider = friendDaoProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.encryptionManagerProvider = encryptionManagerProvider;
    this.keyExchangeManagerProvider = keyExchangeManagerProvider;
    this.nearbyConnectionManagerProvider = nearbyConnectionManagerProvider;
  }

  @Override
  public ChatRepository get() {
    return newInstance(contextProvider.get(), userDaoProvider.get(), friendDaoProvider.get(), messageDaoProvider.get(), chatDaoProvider.get(), encryptionManagerProvider.get(), keyExchangeManagerProvider.get(), nearbyConnectionManagerProvider.get());
  }

  public static ChatRepository_Factory create(Provider<Context> contextProvider,
      Provider<UserDao> userDaoProvider, Provider<FriendDao> friendDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<ChatDao> chatDaoProvider,
      Provider<EncryptionManager> encryptionManagerProvider,
      Provider<KeyExchangeManager> keyExchangeManagerProvider,
      Provider<NearbyConnectionManager> nearbyConnectionManagerProvider) {
    return new ChatRepository_Factory(contextProvider, userDaoProvider, friendDaoProvider, messageDaoProvider, chatDaoProvider, encryptionManagerProvider, keyExchangeManagerProvider, nearbyConnectionManagerProvider);
  }

  public static ChatRepository newInstance(Context context, UserDao userDao, FriendDao friendDao,
      MessageDao messageDao, ChatDao chatDao, EncryptionManager encryptionManager,
      KeyExchangeManager keyExchangeManager, NearbyConnectionManager nearbyConnectionManager) {
    return new ChatRepository(context, userDao, friendDao, messageDao, chatDao, encryptionManager, keyExchangeManager, nearbyConnectionManager);
  }
}
