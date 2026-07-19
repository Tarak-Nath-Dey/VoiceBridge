package com.voicebridge.data.network

import com.google.gson.Gson

data class MeshPacket(
    val packetId: String,
    val senderId: String,
    val senderName: String,
    val recipientId: String, // "EVERYONE" for local broadcasts, or specific device ID
    var ttl: Int, // Time-to-Live (e.g., max 5 hops)
    val payloadType: String, // "CHAT_MESSAGE", "FRIEND_REQUEST", "FRIEND_RESPONSE", "TYPING", "READ_RECEIPT", "REACTION", "NODE_HELLO"
    val encryptedPayload: String, // Ciphertext for private, plain JSON for broadcast/system
    val timestamp: Long,
    val senderPublicKey: String? = null,
    val senderAvatar: Int? = null
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): MeshPacket? {
            return try {
                Gson().fromJson(json, MeshPacket::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

// Inner payload for E2EE chat messages
data class ChatMessagePayload(
    val messageId: String,
    val content: String, // Plain text of the message (encrypted inside MeshPacket.encryptedPayload)
    val type: String, // "TEXT", "IMAGE", "VOICE", "FILE"
    val fileExtension: String? = null,
    val fileBytesBase64: String? = null, // Base64 string of file attachment if small enough
    val replyToId: String? = null
) {
    fun toJson(): String = Gson().toJson(this)
    companion object {
        fun fromJson(json: String): ChatMessagePayload? = try {
            Gson().fromJson(json, ChatMessagePayload::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

// Inner payload for friend requests
data class FriendRequestPayload(
    val username: String,
    val avatarIndex: Int,
    val publicKey: String
) {
    fun toJson(): String = Gson().toJson(this)
    companion object {
        fun fromJson(json: String): FriendRequestPayload? = try {
            Gson().fromJson(json, FriendRequestPayload::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

// Inner payload for friend request responses
data class FriendResponsePayload(
    val accepted: Boolean,
    val username: String,
    val avatarIndex: Int,
    val publicKey: String
) {
    fun toJson(): String = Gson().toJson(this)
    companion object {
        fun fromJson(json: String): FriendResponsePayload? = try {
            Gson().fromJson(json, FriendResponsePayload::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

// Inner payload for typing indicators
data class TypingPayload(
    val isTyping: Boolean
) {
    fun toJson(): String = Gson().toJson(this)
    companion object {
        fun fromJson(json: String): TypingPayload? = try {
            Gson().fromJson(json, TypingPayload::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

// Inner payload for emoji reactions
data class ReactionPayload(
    val messageId: String,
    val reaction: String? // e.g. "❤️", null to remove
) {
    fun toJson(): String = Gson().toJson(this)
    companion object {
        fun fromJson(json: String): ReactionPayload? = try {
            Gson().fromJson(json, ReactionPayload::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

// Inner payload for delivery/read status updates
data class ReadReceiptPayload(
    val messageId: String,
    val status: String // "DELIVERED", "READ"
) {
    fun toJson(): String = Gson().toJson(this)
    companion object {
        fun fromJson(json: String): ReadReceiptPayload? = try {
            Gson().fromJson(json, ReadReceiptPayload::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

// Inner payload for NODE_HELLO mesh sync
data class NodeHelloPayload(
    val username: String,
    val avatarIndex: Int,
    val publicKey: String
) {
    fun toJson(): String = Gson().toJson(this)
    companion object {
        fun fromJson(json: String): NodeHelloPayload? = try {
            Gson().fromJson(json, NodeHelloPayload::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
