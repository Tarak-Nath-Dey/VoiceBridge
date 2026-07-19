package com.voicebridge.di

import android.content.Context
import androidx.room.Room
import com.voicebridge.data.local.VoiceBridgeDatabase
import com.voicebridge.data.local.dao.ChatDao
import com.voicebridge.data.local.dao.FriendDao
import com.voicebridge.data.local.dao.MessageDao
import com.voicebridge.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VoiceBridgeDatabase {
        return Room.databaseBuilder(
            context,
            VoiceBridgeDatabase::class.java,
            "voicebridge_db"
        )
        .fallbackToDestructiveMigration() // Destructive migration for simple local reset
        .build()
    }

    @Provides
    fun provideUserDao(db: VoiceBridgeDatabase): UserDao = db.userDao()

    @Provides
    fun provideFriendDao(db: VoiceBridgeDatabase): FriendDao = db.friendDao()

    @Provides
    fun provideMessageDao(db: VoiceBridgeDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideChatDao(db: VoiceBridgeDatabase): ChatDao = db.chatDao()
}
