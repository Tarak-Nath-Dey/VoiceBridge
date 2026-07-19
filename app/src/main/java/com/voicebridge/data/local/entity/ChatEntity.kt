package com.voicebridge.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val title: String,
    val avatarIndex: Int,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int,
    val isGroup: Boolean,
    val isPinned: Boolean,
    val isFavorite: Boolean
)
