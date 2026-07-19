package com.voicebridge.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val deviceId: String,
    val username: String,
    val avatarIndex: Int,
    val publicKey: String,
    val sharedKey: String?, // Derived E2EE key, encrypted locally
    val isFriend: Boolean,
    val isBlocked: Boolean,
    val isPendingRequest: Boolean,
    val isOutgoingRequest: Boolean,
    val lastSeen: Long,
    val isOnline: Boolean
)
