package com.voicebridge.ui.viewmodel;

import android.content.Context;
import com.voicebridge.data.local.dao.FriendDao;
import com.voicebridge.data.network.NearbyConnectionManager;
import com.voicebridge.domain.repository.ChatRepository;
import com.voicebridge.domain.repository.UserRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<FriendDao> friendDaoProvider;

  private final Provider<NearbyConnectionManager> nearbyConnectionManagerProvider;

  public ChatViewModel_Factory(Provider<Context> contextProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider, Provider<FriendDao> friendDaoProvider,
      Provider<NearbyConnectionManager> nearbyConnectionManagerProvider) {
    this.contextProvider = contextProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.friendDaoProvider = friendDaoProvider;
    this.nearbyConnectionManagerProvider = nearbyConnectionManagerProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(contextProvider.get(), userRepositoryProvider.get(), chatRepositoryProvider.get(), friendDaoProvider.get(), nearbyConnectionManagerProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<Context> contextProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<ChatRepository> chatRepositoryProvider, Provider<FriendDao> friendDaoProvider,
      Provider<NearbyConnectionManager> nearbyConnectionManagerProvider) {
    return new ChatViewModel_Factory(contextProvider, userRepositoryProvider, chatRepositoryProvider, friendDaoProvider, nearbyConnectionManagerProvider);
  }

  public static ChatViewModel newInstance(Context context, UserRepository userRepository,
      ChatRepository chatRepository, FriendDao friendDao,
      NearbyConnectionManager nearbyConnectionManager) {
    return new ChatViewModel(context, userRepository, chatRepository, friendDao, nearbyConnectionManager);
  }
}
