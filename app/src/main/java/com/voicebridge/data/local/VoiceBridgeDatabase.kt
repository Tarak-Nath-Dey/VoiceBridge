package com.voicebridge.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.voicebridge.data.local.dao.ChatDao
import com.voicebridge.data.local.dao.FriendDao
import com.voicebridge.data.local.dao.MessageDao
import com.voicebridge.data.local.dao.UserDao
import com.voicebridge.data.local.entity.ChatEntity
import com.voicebridge.data.local.entity.FriendEntity
import com.voicebridge.data.local.entity.MessageEntity
import com.voicebridge.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        FriendEntity::class,
        MessageEntity::class,
        ChatEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VoiceBridgeDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun friendDao(): FriendDao
    abstract fun messageDao(): MessageDao
    abstract fun chatDao(): ChatDao
}
