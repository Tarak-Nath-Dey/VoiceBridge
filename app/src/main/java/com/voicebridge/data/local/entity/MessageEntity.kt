package com.voicebridge.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val recipientId: String,
    val content: String, // Locally encrypted
    val timestamp: Long,
    val isRead: Boolean,
    val deliveryStatus: String, // PENDING, DELIVERED, READ
    val type: String, // TEXT, IMAGE, VOICE, FILE
    val filePath: String?, // Path to local attachment
    val reaction: String?, // Emoji reaction (e.g. "👍")
    val replyToId: String? // Message ID being replied to
)
