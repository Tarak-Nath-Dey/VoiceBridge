package com.voicebridge.domain.repository

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.voicebridge.data.local.dao.ChatDao
import com.voicebridge.data.local.dao.FriendDao
import com.voicebridge.data.local.dao.MessageDao
import com.voicebridge.data.local.dao.UserDao
import com.voicebridge.data.local.entity.ChatEntity
import com.voicebridge.data.local.entity.FriendEntity
import com.voicebridge.data.local.entity.MessageEntity
import com.voicebridge.data.local.entity.UserEntity
import com.voicebridge.data.network.*
import com.voicebridge.data.security.EncryptionManager
import com.voicebridge.data.security.KeyExchangeManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao,
    private val friendDao: FriendDao,
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val encryptionManager: EncryptionManager,
    private val keyExchangeManager: KeyExchangeManager,
    private val nearbyConnectionManager: NearbyConnectionManager
) {
    private val TAG = "ChatRepository"
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Flow for current active typing statuses: Friend Device ID -> Boolean
    private val _typingStatuses = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingStatuses: StateFlow<Map<String, Boolean>> = _typingStatuses

    init {
        // Collect incoming packets from Nearby Connection mesh
        repositoryScope.launch {
            nearbyConnectionManager.onConnectedNodeCallback = { _, _ -> sendNodeHello() }
            nearbyConnectionManager.incomingPackets.collect { packet ->
                try {
                    handleIncomingPacket(packet)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling incoming packet: ${e.message}", e)
                }
            }
        }
    }

    private fun showNotification(title: String, text: String, chatId: String? = null) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val builder = NotificationCompat.Builder(context, "voicebridge_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            if (chatId != null) {
                val intent = android.content.Intent(context, com.voicebridge.ui.MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("chatId", chatId)
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                builder.setContentIntent(pendingIntent)
            }

            try {
                NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    fun sendNodeHello() {
        repositoryScope.launch {
            val user = userDao.getUser() ?: return@launch
            val helloPayload = NodeHelloPayload(
                username = user.username,
                avatarIndex = user.avatarIndex,
                publicKey = user.publicKey
            )
            val packet = MeshPacket(
                packetId = UUID.randomUUID().toString(),
                senderId = user.deviceId,
                senderName = user.username,
                recipientId = "EVERYONE",
                ttl = 5,
                payloadType = "NODE_HELLO",
                encryptedPayload = helloPayload.toJson(),
                timestamp = System.currentTimeMillis(),
                senderPublicKey = user.publicKey,
                senderAvatar = user.avatarIndex
            )
            nearbyConnectionManager.sendPacket(packet)
        }
    }

    /**
     * Exposes chats flow, decrypting last message for preview
     */
    fun getChats(): Flow<List<ChatEntity>> {
        return chatDao.getChatsFlow().map { list ->
            list.map { chat ->
                if (chat.chatId == "EVERYONE") {
                    chat
                } else {
                    // Decrypt last message preview
                    val decrypted = encryptionManager.decrypt(chat.lastMessage)
                    chat.copy(lastMessage = decrypted)
                }
            }
        }
    }

    /**
     * Exposes messages flow for a chat, decrypting message content on the fly
     */
    fun getMessages(chatId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForChatFlow(chatId).map { list ->
            list.map { msg ->
                if (msg.chatId == "EVERYONE") {
                    // Broadcast messages are not E2EE encrypted, only locally DB-encrypted
                    val decryptedContent = encryptionManager.decrypt(msg.content)
                    msg.copy(content = decryptedContent)
                } else {
                    // Private chats are E2EE encrypted. We stored them in the DB in encrypted form.
                    // Actually, to make DB search/backup easy and keep it uniform,
                    // we decrypt the local DB content (which was encrypted with Keystore key).
                    val decryptedContent = encryptionManager.decrypt(msg.content)
                    msg.copy(content = decryptedContent)
                }
            }
        }
    }

    /**
     * Retries sending all pending messages for a recipient who just came online
     */
    private fun retryPendingMessages(recipientId: String) {
        repositoryScope.launch {
            val user = userDao.getUser() ?: return@launch
            val pendingMessages = messageDao.getPendingMessagesForRecipient(recipientId)
            
            for (msg in pendingMessages) {
                // Decrypt local db content to re-encrypt with mesh key
                val decryptedContent = encryptionManager.decrypt(msg.content)
                val targetFriend = friendDao.getFriendById(recipientId)
                
                val chatMessagePayload = ChatMessagePayload(
                    messageId = msg.id,
                    content = decryptedContent,
                    type = msg.type,
                    fileBytesBase64 = null, // Skip file retries in simple store & forward for now
                    fileExtension = null,
                    replyToId = msg.replyToId
                )
                
                val sharedKey = if (targetFriend != null) getOrDeriveSharedKey(user, targetFriend) else null
                if (sharedKey == null) continue // Still can't encrypt, wait for proper exchange
                
                val encryptedPayloadJson = keyExchangeManager.encryptWithSharedKey(chatMessagePayload.toJson(), sharedKey)
                
                val packet = MeshPacket(
                    packetId = UUID.randomUUID().toString(),
                    senderId = user.deviceId,
                    senderName = user.username,
                    recipientId = recipientId,
                    ttl = 5,
                    payloadType = "CHAT_MESSAGE",
                    encryptedPayload = encryptedPayloadJson,
                    timestamp = msg.timestamp,
                    senderPublicKey = user.publicKey,
                    senderAvatar = user.avatarIndex
                )
                nearbyConnectionManager.sendPacket(packet)
                messageDao.updateDeliveryStatus(msg.id, "DELIVERED")
            }
        }
    }

    /**
     * Sends a direct or broadcast chat message
     */
    suspend fun sendMessage(recipientId: String, content: String, type: String = "TEXT", fileBytesBase64: String? = null, fileExtension: String? = null, replyToId: String? = null) {
        val user = userDao.getUser() ?: return
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // Write local file bytes to disk if present
        if (!fileBytesBase64.isNullOrEmpty() && !fileExtension.isNullOrEmpty()) {
            try {
                val fileBytes = java.util.Base64.getDecoder().decode(fileBytesBase64)
                val file = File(context.cacheDir, "$messageId.$fileExtension")
                val fos = FileOutputStream(file)
                fos.write(fileBytes)
                fos.flush()
                fos.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 1. Save to local database FIRST (encrypted using local Keystore key) so sender always sees outgoing message instantly
        val encryptedContent = encryptionManager.encrypt(content)
        val localMessage = MessageEntity(
            id = messageId,
            chatId = recipientId,
            senderId = user.deviceId,
            senderName = user.username,
            recipientId = recipientId,
            content = encryptedContent,
            timestamp = timestamp,
            isRead = true,
            deliveryStatus = if (recipientId == "EVERYONE") "DELIVERED" else "PENDING",
            type = type,
            filePath = if (!fileExtension.isNullOrEmpty()) File(context.cacheDir, "$messageId.$fileExtension").absolutePath else null,
            reaction = null,
            replyToId = replyToId
        )
        messageDao.insertMessage(localMessage)

        // 2. Update Chat conversation cache
        val previewText = when (type) {
            "IMAGE" -> "📷 Image"
            "VOICE" -> "🎤 Voice note"
            "FILE" -> "📁 File"
            else -> content
        }
        val targetChat = chatDao.getChatById(recipientId)
        if (targetChat == null) {
            val discoveredName = nearbyConnectionManager.discoveredDevices.value.find { it.deviceId == recipientId }?.username
            val friend = friendDao.getFriendById(recipientId)
            
            val newChat = ChatEntity(
                chatId = recipientId,
                title = if (recipientId == "EVERYONE") "Everyone Broadcast" else friend?.username ?: discoveredName ?: "Private Chat",
                avatarIndex = if (recipientId == "EVERYONE") 99 else friend?.avatarIndex ?: 0,
                lastMessage = encryptionManager.encrypt(previewText),
                lastMessageTimestamp = timestamp,
                unreadCount = 0,
                isGroup = recipientId == "EVERYONE",
                isPinned = false,
                isFavorite = false
            )
            chatDao.insertChat(newChat)
        } else {
            chatDao.updateLastMessage(recipientId, encryptionManager.encrypt(previewText), timestamp)
        }

        val chatMessagePayload = ChatMessagePayload(
            messageId = messageId,
            content = content,
            type = type,
            fileBytesBase64 = fileBytesBase64,
            fileExtension = fileExtension,
            replyToId = replyToId
        )

        // 3. Prepare and send packet across mesh
        val packet = if (recipientId == "EVERYONE") {
            MeshPacket(
                packetId = UUID.randomUUID().toString(),
                senderId = user.deviceId,
                senderName = user.username,
                recipientId = "EVERYONE",
                ttl = 5,
                payloadType = "CHAT_MESSAGE",
                encryptedPayload = chatMessagePayload.toJson(),
                timestamp = timestamp,
                senderPublicKey = user.publicKey
            )
        } else {
            // E2EE Private Chat message
            var friend = friendDao.getFriendById(recipientId)
            if (friend == null) {
                // Auto create contact if not existing yet
                val connectedPeer = nearbyConnectionManager.connectedPeersList.value.find { it.deviceId == recipientId }
                val discoveredPeer = nearbyConnectionManager.discoveredDevices.value.find { it.deviceId == recipientId }
                val peerName = connectedPeer?.username ?: discoveredPeer?.username ?: "Unknown User"
                friend = FriendEntity(
                    deviceId = recipientId,
                    username = peerName,
                    avatarIndex = 0,
                    publicKey = "",
                    sharedKey = null,
                    isFriend = false,
                    isBlocked = false,
                    isPendingRequest = false,
                    isOutgoingRequest = false,
                    lastSeen = timestamp,
                    isOnline = true
                )
                friendDao.insertFriend(friend)
            }

            val sharedKey = getOrDeriveSharedKey(user, friend)
            if (sharedKey == null) {
                Log.w(TAG, "Public key missing for recipient $recipientId. Sending NODE_HELLO and queueing packet.")
                sendNodeHello()
                return
            }
            val encryptedPayloadJson = keyExchangeManager.encryptWithSharedKey(chatMessagePayload.toJson(), sharedKey)
            
            MeshPacket(
                packetId = UUID.randomUUID().toString(),
                senderId = user.deviceId,
                senderName = user.username,
                recipientId = recipientId,
                ttl = 5,
                payloadType = "CHAT_MESSAGE",
                encryptedPayload = encryptedPayloadJson,
                timestamp = timestamp,
                senderPublicKey = user.publicKey
            )
        }

        nearbyConnectionManager.sendPacket(packet)
    }

    /**
     * Send typing status to a friend
     */
    fun sendTypingStatus(recipientId: String, isTyping: Boolean) {
        val user = repositoryScope.run {
            launch {
                val u = userDao.getUser() ?: return@launch
                val packet = MeshPacket(
                    packetId = UUID.randomUUID().toString(),
                    senderId = u.deviceId,
                    senderName = u.username,
                    recipientId = recipientId,
                    ttl = 3,
                    payloadType = "TYPING",
                    encryptedPayload = TypingPayload(isTyping).toJson(),
                    timestamp = System.currentTimeMillis()
                )
                nearbyConnectionManager.sendPacket(packet)
            }
        }
    }

    /**
     * Send reaction to a message
     */
    suspend fun sendReaction(chatId: String, messageId: String, reaction: String?) {
        val user = userDao.getUser() ?: return
        messageDao.updateReaction(messageId, reaction)
        
        val packet = MeshPacket(
            packetId = UUID.randomUUID().toString(),
            senderId = user.deviceId,
            senderName = user.username,
            recipientId = chatId,
            ttl = 4,
            payloadType = "REACTION",
            encryptedPayload = ReactionPayload(messageId, reaction).toJson(),
            timestamp = System.currentTimeMillis()
        )
        nearbyConnectionManager.sendPacket(packet)
    }

    /**
     * Send friend request to a discovered user
     */
    suspend fun sendFriendRequest(friendDeviceId: String, username: String, avatarIndex: Int) {
        val user = userDao.getUser() ?: return
        
        // Save as outgoing request
        val friendEntity = FriendEntity(
            deviceId = friendDeviceId,
            username = username,
            avatarIndex = avatarIndex,
            publicKey = "", // Loaded once accepted or via scan
            sharedKey = null,
            isFriend = false,
            isBlocked = false,
            isPendingRequest = false,
            isOutgoingRequest = true,
            lastSeen = System.currentTimeMillis(),
            isOnline = true
        )
        friendDao.insertFriend(friendEntity)

        val payload = FriendRequestPayload(
            username = user.username,
            avatarIndex = user.avatarIndex,
            publicKey = user.publicKey
        )

        val packet = MeshPacket(
            packetId = UUID.randomUUID().toString(),
            senderId = user.deviceId,
            senderName = user.username,
            recipientId = friendDeviceId,
            ttl = 5,
            payloadType = "FRIEND_REQUEST",
            encryptedPayload = payload.toJson(),
            timestamp = System.currentTimeMillis(),
            senderPublicKey = user.publicKey,
            senderAvatar = user.avatarIndex
        )
        nearbyConnectionManager.sendPacket(packet)
    }

    /**
     * Respond to friend request (Accept/Reject)
     */
    suspend fun respondToFriendRequest(friendDeviceId: String, accept: Boolean) {
        val user = userDao.getUser() ?: return
        val friend = friendDao.getFriendById(friendDeviceId) ?: return
        
        if (accept) {
            // Update friend record
            val updatedFriend = friend.copy(
                isFriend = true,
                isPendingRequest = false,
                isOutgoingRequest = false
            )
            friendDao.insertFriend(updatedFriend)
            
            // Derive & cache shared key
            getOrDeriveSharedKey(user, updatedFriend)

            // Add chat thread
            val newChat = ChatEntity(
                chatId = friendDeviceId,
                title = friend.username,
                avatarIndex = friend.avatarIndex,
                lastMessage = encryptionManager.encrypt("Friend request accepted!"),
                lastMessageTimestamp = System.currentTimeMillis(),
                unreadCount = 0,
                isGroup = false,
                isPinned = false,
                isFavorite = false
            )
            chatDao.insertChat(newChat)
        } else {
            // Rejected
            friendDao.updateFriendshipState(friendDeviceId, isFriend = false, isPending = false, isOutgoing = false)
        }

        // Notify friend
        val responsePayload = FriendResponsePayload(
            accepted = accept,
            username = user.username,
            avatarIndex = user.avatarIndex,
            publicKey = user.publicKey
        )
        val packet = MeshPacket(
            packetId = UUID.randomUUID().toString(),
            senderId = user.deviceId,
            senderName = user.username,
            recipientId = friendDeviceId,
            ttl = 5,
            payloadType = "FRIEND_RESPONSE",
            encryptedPayload = responsePayload.toJson(),
            timestamp = System.currentTimeMillis(),
            senderAvatar = user.avatarIndex
        )
        nearbyConnectionManager.sendPacket(packet)
    }

    /**
     * Internal processing of packets coming from the mesh network
     */
    private suspend fun handleIncomingPacket(packet: MeshPacket) {
        val user = userDao.getUser() ?: return
        
        if (!packet.senderPublicKey.isNullOrEmpty() && packet.senderId != user.deviceId) {
            val existing = friendDao.getFriendById(packet.senderId)
            if (existing == null) {
                val newContact = FriendEntity(
                    deviceId = packet.senderId,
                    username = packet.senderName,
                    avatarIndex = 0,
                    publicKey = packet.senderPublicKey,
                    sharedKey = null,
                    isFriend = false,
                    isBlocked = false,
                    isPendingRequest = false,
                    isOutgoingRequest = false,
                    lastSeen = packet.timestamp,
                    isOnline = true
                )
                friendDao.insertFriend(newContact)
                getOrDeriveSharedKey(user, newContact)
            } else if (existing.publicKey != packet.senderPublicKey || !existing.isOnline) {
                val updated = existing.copy(
                    username = if (existing.username.isEmpty()) packet.senderName else existing.username,
                    publicKey = packet.senderPublicKey,
                    isOnline = true,
                    lastSeen = packet.timestamp
                )
                friendDao.insertFriend(updated)
                if (existing.sharedKey == null || existing.publicKey != packet.senderPublicKey) {
                    getOrDeriveSharedKey(user, updated)
                }
            }
        }

        when (packet.payloadType) {
            "CHAT_MESSAGE" -> {
                val (payloadJson, isE2EE) = if (packet.recipientId == "EVERYONE") {
                    Pair(packet.encryptedPayload, false)
                } else {
                    // Private message. Retrieve sender's key and decrypt payload
                    var friend = friendDao.getFriendById(packet.senderId)
                    if (friend == null) {
                        if (!packet.senderPublicKey.isNullOrEmpty()) {
                            friend = FriendEntity(
                                deviceId = packet.senderId,
                                username = packet.senderName,
                                avatarIndex = 0,
                                publicKey = packet.senderPublicKey,
                                sharedKey = null,
                                isFriend = false,
                                isBlocked = false,
                                isPendingRequest = false,
                                isOutgoingRequest = false,
                                lastSeen = packet.timestamp,
                                isOnline = true
                            )
                            friendDao.insertFriend(friend)
                        } else {
                            Log.e(TAG, "Private message received from unknown user without public key: ${packet.senderId}. Cannot decrypt.")
                            return
                        }
                    } else if (friend.publicKey.isEmpty() && !packet.senderPublicKey.isNullOrEmpty()) {
                        friend = friend.copy(publicKey = packet.senderPublicKey, isOnline = true, lastSeen = packet.timestamp)
                        friendDao.insertFriend(friend)
                    }
                    val sharedKey = getOrDeriveSharedKey(user, friend) ?: return
                    val decrypted = keyExchangeManager.decryptWithSharedKey(packet.encryptedPayload, sharedKey)
                    Pair(decrypted, true)
                }

                val payload = ChatMessagePayload.fromJson(payloadJson) ?: return
                
                // Write incoming file bytes to disk if present
                if (!payload.fileBytesBase64.isNullOrEmpty() && !payload.fileExtension.isNullOrEmpty()) {
                    try {
                        val fileBytes = java.util.Base64.getDecoder().decode(payload.fileBytesBase64)
                        val file = File(context.cacheDir, "${payload.messageId}.${payload.fileExtension}")
                        val fos = FileOutputStream(file)
                        fos.write(fileBytes)
                        fos.flush()
                        fos.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Save sender to friend table if not exists (as a non-friend contact for Everyone chat)
                val senderFriend = friendDao.getFriendById(packet.senderId)
                if (senderFriend == null) {
                    val newContact = FriendEntity(
                        deviceId = packet.senderId,
                        username = packet.senderName,
                        avatarIndex = packet.senderAvatar ?: 0,
                        publicKey = "",
                        sharedKey = null,
                        isFriend = false,
                        isBlocked = false,
                        isPendingRequest = false,
                        isOutgoingRequest = false,
                        lastSeen = packet.timestamp,
                        isOnline = true
                    )
                    friendDao.insertFriend(newContact)
                } else {
                    friendDao.updateOnlineStatus(packet.senderId, isOnline = true, lastSeen = packet.timestamp)
                }

                // Encrypt payload contents before saving to local Room Database
                val dbEncryptedContent = encryptionManager.encrypt(payload.content)
                
                val localMessage = MessageEntity(
                    id = payload.messageId,
                    chatId = if (packet.recipientId == "EVERYONE") "EVERYONE" else packet.senderId, // "EVERYONE" or sender's deviceId
                    senderId = packet.senderId,
                    senderName = packet.senderName,
                    recipientId = packet.recipientId,
                    content = dbEncryptedContent,
                    timestamp = packet.timestamp,
                    isRead = false,
                    deliveryStatus = "DELIVERED",
                    type = payload.type,
                    filePath = if (!payload.fileExtension.isNullOrEmpty()) File(context.cacheDir, "${payload.messageId}.${payload.fileExtension}").absolutePath else null,
                    reaction = null,
                    replyToId = payload.replyToId
                )
                messageDao.insertMessage(localMessage)

                // Update/Create chat conversation thread
                val chatId = if (packet.recipientId == "EVERYONE") "EVERYONE" else packet.senderId
                val chatTitle = if (packet.recipientId == "EVERYONE") "Everyone Broadcast" else packet.senderName
                val chatAvatar = if (packet.recipientId == "EVERYONE") 99 else packet.senderAvatar ?: senderFriend?.avatarIndex ?: 0
                
                val previewText = when (payload.type) {
                    "IMAGE" -> "📷 Image"
                    "VOICE" -> "🎤 Voice note"
                    "FILE" -> "📁 File"
                    else -> payload.content
                }
                
                val chat = chatDao.getChatById(chatId)
                if (chat == null) {
                    val newChat = ChatEntity(
                        chatId = chatId,
                        title = chatTitle,
                        avatarIndex = chatAvatar,
                        lastMessage = encryptionManager.encrypt(previewText),
                        lastMessageTimestamp = packet.timestamp,
                        unreadCount = 1,
                        isGroup = packet.recipientId == "EVERYONE",
                        isPinned = false,
                        isFavorite = false
                    )
                    chatDao.insertChat(newChat)
                    
                    // Show Notification
                    if (packet.recipientId == "EVERYONE") {
                        showNotification("New Broadcast in Everyone", "${packet.senderName}: $previewText", "EVERYONE")
                    } else {
                        showNotification("New Message from ${packet.senderName}", previewText, packet.senderId)
                    }
                } else {
                    // Update existing chat with new name/avatar if available
                    if (packet.recipientId != "EVERYONE" && packet.senderAvatar != null) {
                        chatDao.updateChatTitleAndAvatar(chatId, packet.senderName, packet.senderAvatar)
                    }
                    chatDao.updateLastMessage(chatId, encryptionManager.encrypt(previewText), packet.timestamp)
                    chatDao.incrementUnreadCount(chatId)
                    
                    // Show Notification
                    if (packet.recipientId == "EVERYONE") {
                        showNotification("New Broadcast in Everyone", "${packet.senderName}: $previewText", "EVERYONE")
                    } else {
                        showNotification("New Message from ${packet.senderName}", previewText, packet.senderId)
                    }
                }

                // Send read/delivery receipt for private messages
                if (isE2EE) {
                    val receiptPacket = MeshPacket(
                        packetId = UUID.randomUUID().toString(),
                        senderId = user.deviceId,
                        senderName = user.username,
                        recipientId = packet.senderId,
                        ttl = 3,
                        payloadType = "READ_RECEIPT",
                        encryptedPayload = ReadReceiptPayload(payload.messageId, "DELIVERED").toJson(),
                        timestamp = System.currentTimeMillis()
                    )
                    nearbyConnectionManager.sendPacket(receiptPacket)
                }
            }

            "FRIEND_REQUEST" -> {
                val payload = FriendRequestPayload.fromJson(packet.encryptedPayload) ?: return
                
                // Add/Update friend record in DB as pending request
                val newFriend = FriendEntity(
                    deviceId = packet.senderId,
                    username = payload.username,
                    avatarIndex = payload.avatarIndex,
                    publicKey = payload.publicKey,
                    sharedKey = null,
                    isFriend = false,
                    isBlocked = false,
                    isPendingRequest = true,
                    isOutgoingRequest = false,
                    lastSeen = packet.timestamp,
                    isOnline = true
                )
                friendDao.insertFriend(newFriend)
                getOrDeriveSharedKey(user, newFriend)

                // Trigger a UI refresh if necessary, or let Flow handle it.
                showNotification("New Friend Request", "${packet.senderName} sent you a friend request.", packet.senderId)

                // Create a temporary Chat in list to show request
                val chat = ChatEntity(
                    chatId = packet.senderId,
                    title = payload.username,
                    avatarIndex = payload.avatarIndex,
                    lastMessage = encryptionManager.encrypt("Sent you a friend request!"),
                    lastMessageTimestamp = packet.timestamp,
                    unreadCount = 1,
                    isGroup = false,
                    isPinned = false,
                    isFavorite = false
                )
                chatDao.insertChat(chat)
            }

            "FRIEND_RESPONSE" -> {
                val payload = FriendResponsePayload.fromJson(packet.encryptedPayload) ?: return
                val friend = friendDao.getFriendById(packet.senderId) ?: return
                
                if (payload.accepted) {
                    // Update friendship
                    val updatedFriend = friend.copy(
                        isFriend = true,
                        isPendingRequest = false,
                        isOutgoingRequest = false,
                        publicKey = payload.publicKey
                    )
                    friendDao.insertFriend(updatedFriend)
                    getOrDeriveSharedKey(user, updatedFriend)

                    // Add/update chat preview
                    val chat = ChatEntity(
                        chatId = packet.senderId,
                        title = payload.username,
                        avatarIndex = payload.avatarIndex,
                        lastMessage = encryptionManager.encrypt("Friend request accepted!"),
                        lastMessageTimestamp = packet.timestamp,
                        unreadCount = 0,
                        isGroup = false,
                        isPinned = false,
                        isFavorite = false
                    )
                    chatDao.insertChat(chat)
                } else {
                    // Rejected
                    friendDao.updateFriendshipState(packet.senderId, isFriend = false, isPending = false, isOutgoing = false)
                    chatDao.deleteChatById(packet.senderId)
                }
            }

            "NODE_HELLO" -> {
                val payload = NodeHelloPayload.fromJson(packet.encryptedPayload) ?: return
                val existing = friendDao.getFriendById(packet.senderId)
                if (existing == null) {
                    val newContact = FriendEntity(
                        deviceId = packet.senderId,
                        username = payload.username,
                        avatarIndex = payload.avatarIndex,
                        publicKey = payload.publicKey,
                        sharedKey = null,
                        isFriend = false,
                        isBlocked = false,
                        isPendingRequest = false,
                        isOutgoingRequest = false,
                        lastSeen = packet.timestamp,
                        isOnline = true
                    )
                    friendDao.insertFriend(newContact)
                    getOrDeriveSharedKey(user, newContact)
                } else {
                    val updated = existing.copy(
                        username = payload.username,
                        avatarIndex = payload.avatarIndex,
                        publicKey = payload.publicKey,
                        isOnline = true,
                        lastSeen = packet.timestamp
                    )
                    friendDao.insertFriend(updated)
                    if (existing.sharedKey == null || existing.publicKey != payload.publicKey) {
                        getOrDeriveSharedKey(user, updated)
                    }
                }
                retryPendingMessages(packet.senderId)
            }

            "TYPING" -> {
                val payload = TypingPayload.fromJson(packet.encryptedPayload) ?: return
                _typingStatuses.value = _typingStatuses.value.toMutableMap().apply {
                    put(packet.senderId, payload.isTyping)
                }
            }

            "READ_RECEIPT" -> {
                val payload = ReadReceiptPayload.fromJson(packet.encryptedPayload) ?: return
                messageDao.updateDeliveryStatus(payload.messageId, payload.status)
            }

            "REACTION" -> {
                val payload = ReactionPayload.fromJson(packet.encryptedPayload) ?: return
                messageDao.updateReaction(payload.messageId, payload.reaction)
            }
        }
    }

    /**
     * Clears unread badge count for a chat
     */
    suspend fun clearUnreadCount(chatId: String) {
        chatDao.clearUnreadCount(chatId)
    }

    /**
     * Delete message for me
     */
    suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessageById(messageId)
    }

    /**
     * Toggle pinning chat
     */
    suspend fun togglePinChat(chatId: String) {
        val chat = chatDao.getChatById(chatId) ?: return
        chatDao.updatePinStatus(chatId, !chat.isPinned)
    }

    /**
     * Toggle favorite friend
     */
    suspend fun toggleFavoriteChat(chatId: String) {
        val chat = chatDao.getChatById(chatId) ?: return
        chatDao.updateFavoriteStatus(chatId, !chat.isFavorite)
    }

    /**
     * Resolves E2EE derived symmetric key (cached or newly computed via ECDH)
     */
    private suspend fun getOrDeriveSharedKey(user: UserEntity, friend: FriendEntity): SecretKeySpec? {
        if (friend.publicKey.isEmpty()) return null
        
        if (friend.sharedKey != null) {
            // Decrypt cached symmetric key using local Keystore key
            val decryptedKeyBase64 = encryptionManager.decrypt(friend.sharedKey)
            val keyBytes = java.util.Base64.getDecoder().decode(decryptedKeyBase64)
            return SecretKeySpec(keyBytes, "AES")
        }

        // Derive new shared key
        return try {
            val decryptedLocalPrivateEncoded = encryptionManager.decrypt(user.privateKey)
            val ownPrivateKey = keyExchangeManager.loadPrivateKey(decryptedLocalPrivateEncoded)
            val friendPublicKey = keyExchangeManager.loadPublicKey(friend.publicKey)
            val sharedKeySpec = keyExchangeManager.deriveSharedKey(ownPrivateKey, friendPublicKey)
            
            // Cache the derived key (encrypted using local Keystore key)
            val keyBase64 = java.util.Base64.getEncoder().encodeToString(sharedKeySpec.encoded)
            val encryptedKeyBase64 = encryptionManager.encrypt(keyBase64)
            friendDao.updateSharedKey(friend.deviceId, encryptedKeyBase64)
            
            sharedKeySpec
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
