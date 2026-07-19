package com.voicebridge.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicebridge.data.local.dao.FriendDao
import com.voicebridge.data.local.entity.ChatEntity
import com.voicebridge.data.local.entity.FriendEntity
import com.voicebridge.data.local.entity.MessageEntity
import com.voicebridge.data.network.ConnectedDevice
import com.voicebridge.data.network.DiscoveredDevice
import com.voicebridge.data.network.NearbyConnectionManager
import com.voicebridge.domain.repository.ChatRepository
import com.voicebridge.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.voicebridge.data.network.MeshNetworkService
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val friendDao: FriendDao,
    private val nearbyConnectionManager: NearbyConnectionManager
) : ViewModel() {

    val userFlow = userRepository.getUserFlow()

    // Discovered devices in range
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = nearbyConnectionManager.discoveredDevices

    // Connected endpoints in mesh (online neighbours)
    val connectedPeers: StateFlow<List<String>> = nearbyConnectionManager.connectedPeers
    
    // Connected devices details for UI
    val connectedPeersList: StateFlow<List<com.voicebridge.data.network.ConnectedDevice>> = nearbyConnectionManager.connectedPeersList

    // Chat threads
    val chats: Flow<List<ChatEntity>> = chatRepository.getChats()

    val unreadEveryoneBadge: Flow<Boolean> = chats.map { chatsList ->
        chatsList.any { it.chatId == "EVERYONE" && it.unreadCount > 0 }
    }

    // Friends list
    val friends: Flow<List<FriendEntity>> = friendDao.getFriendsFlow()

    // Incoming pending requests
    val pendingRequests: Flow<List<FriendEntity>> = friendDao.getPendingRequestsFlow()

    // Typing statuses map: DeviceId -> Boolean
    val typingStatuses: StateFlow<Map<String, Boolean>> = chatRepository.typingStatuses

    init {
        // Ensure the background service is running when ViewModel is created
        startOfflineMesh()
    }

    fun sendNodeHello() {
        chatRepository.sendNodeHello()
    }

    // --- Offline Mesh Lifecycle ---

    fun startOfflineMesh() {
        val intent = Intent(context, MeshNetworkService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun restartOfflineMesh() {
        val intent = Intent(context, MeshNetworkService::class.java)
        context.stopService(intent)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopOfflineMesh() {
        val intent = Intent(context, MeshNetworkService::class.java)
        context.stopService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        // We can keep it running in background or stop it on application kill.
        // For a seamless background service, keep it running or bound.
    }

    // --- Messaging operations ---

    fun getMessages(chatId: String): Flow<List<MessageEntity>> {
        return chatRepository.getMessages(chatId)
    }

    fun sendMessage(recipientId: String, content: String, type: String = "TEXT", fileBytesBase64: String? = null, fileExtension: String? = null, replyToId: String? = null) {
        viewModelScope.launch {
            chatRepository.sendMessage(recipientId, content, type, fileBytesBase64, fileExtension, replyToId)
        }
    }

    fun sendTypingStatus(recipientId: String, isTyping: Boolean) {
        chatRepository.sendTypingStatus(recipientId, isTyping)
    }

    fun sendReaction(chatId: String, messageId: String, reaction: String?) {
        viewModelScope.launch {
            chatRepository.sendReaction(chatId, messageId, reaction)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId)
        }
    }

    fun clearUnreadCount(chatId: String) {
        viewModelScope.launch {
            chatRepository.clearUnreadCount(chatId)
        }
    }

    fun togglePinChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.togglePinChat(chatId)
        }
    }

    fun toggleFavoriteChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.toggleFavoriteChat(chatId)
        }
    }

    // --- Friend operations ---

    fun sendFriendRequest(discoveredDevice: DiscoveredDevice) {
        viewModelScope.launch {
            nearbyConnectionManager.connectToDevice(discoveredDevice.endpointId)
            chatRepository.sendFriendRequest(
                discoveredDevice.deviceId,
                discoveredDevice.username,
                avatarIndex = (1..6).random()
            )
        }
    }

    fun sendFriendRequest(connectedDevice: ConnectedDevice) {
        viewModelScope.launch {
            chatRepository.sendFriendRequest(
                connectedDevice.deviceId,
                connectedDevice.username,
                avatarIndex = (1..6).random()
            )
        }
    }

    fun respondToFriendRequest(friendDeviceId: String, accept: Boolean) {
        viewModelScope.launch {
            chatRepository.respondToFriendRequest(friendDeviceId, accept)
        }
    }

    // QR pairing adding
    fun addFriendFromQr(friendDeviceId: String, username: String, publicKey: String, avatarIndex: Int) {
        viewModelScope.launch {
            val user = userRepository.getLocalUser() ?: return@launch
            
            // Insert friend record
            val friendEntity = FriendEntity(
                deviceId = friendDeviceId,
                username = username,
                avatarIndex = avatarIndex,
                publicKey = publicKey,
                sharedKey = null,
                isFriend = true,
                isBlocked = false,
                isPendingRequest = false,
                isOutgoingRequest = false,
                lastSeen = System.currentTimeMillis(),
                isOnline = true
            )
            friendDao.insertFriend(friendEntity)

            // Cache shared E2EE key immediately
            chatRepository.respondToFriendRequest(friendDeviceId, accept = true)
        }
    }
}
