package com.voicebridge.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val deviceId: String,
    val username: String,
    val avatarIndex: Int,
    val publicKey: String,
    val privateKey: String, // Encrypted locally in DB
    val status: String
)
