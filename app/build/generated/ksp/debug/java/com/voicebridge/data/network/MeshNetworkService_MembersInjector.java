package com.voicebridge.data.network;

import com.voicebridge.data.local.dao.UserDao;
import com.voicebridge.domain.repository.ChatRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MeshNetworkService_MembersInjector implements MembersInjector<MeshNetworkService> {
  private final Provider<NearbyConnectionManager> nearbyConnectionManagerProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<UserDao> userDaoProvider;

  public MeshNetworkService_MembersInjector(
      Provider<NearbyConnectionManager> nearbyConnectionManagerProvider,
      Provider<ChatRepository> chatRepositoryProvider, Provider<UserDao> userDaoProvider) {
    this.nearbyConnectionManagerProvider = nearbyConnectionManagerProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.userDaoProvider = userDaoProvider;
  }

  public static MembersInjector<MeshNetworkService> create(
      Provider<NearbyConnectionManager> nearbyConnectionManagerProvider,
      Provider<ChatRepository> chatRepositoryProvider, Provider<UserDao> userDaoProvider) {
    return new MeshNetworkService_MembersInjector(nearbyConnectionManagerProvider, chatRepositoryProvider, userDaoProvider);
  }

  @Override
  public void injectMembers(MeshNetworkService instance) {
    injectNearbyConnectionManager(instance, nearbyConnectionManagerProvider.get());
    injectChatRepository(instance, chatRepositoryProvider.get());
    injectUserDao(instance, userDaoProvider.get());
  }

  @InjectedFieldSignature("com.voicebridge.data.network.MeshNetworkService.nearbyConnectionManager")
  public static void injectNearbyConnectionManager(MeshNetworkService instance,
      NearbyConnectionManager nearbyConnectionManager) {
    instance.nearbyConnectionManager = nearbyConnectionManager;
  }

  @InjectedFieldSignature("com.voicebridge.data.network.MeshNetworkService.chatRepository")
  public static void injectChatRepository(MeshNetworkService instance,
      ChatRepository chatRepository) {
    instance.chatRepository = chatRepository;
  }

  @InjectedFieldSignature("com.voicebridge.data.network.MeshNetworkService.userDao")
  public static void injectUserDao(MeshNetworkService instance, UserDao userDao) {
    instance.userDao = userDao;
  }
}
