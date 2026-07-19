package com.voicebridge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voicebridge.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getChatsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE chatId = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastMessageTimestamp = :timestamp WHERE chatId = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMessage: String, timestamp: Long)

    @Query("UPDATE chats SET title = :title, avatarIndex = :avatarIndex WHERE chatId = :chatId")
    suspend fun updateChatTitleAndAvatar(chatId: String, title: String, avatarIndex: Int)

    @Query("UPDATE chats SET unreadCount = unreadCount + 1 WHERE chatId = :chatId")
    suspend fun incrementUnreadCount(chatId: String)

    @Query("UPDATE chats SET unreadCount = 0 WHERE chatId = :chatId")
    suspend fun clearUnreadCount(chatId: String)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE chatId = :chatId")
    suspend fun updatePinStatus(chatId: String, isPinned: Boolean)

    @Query("UPDATE chats SET isFavorite = :isFavorite WHERE chatId = :chatId")
    suspend fun updateFavoriteStatus(chatId: String, isFavorite: Boolean)

    @Query("DELETE FROM chats WHERE chatId = :chatId")
    suspend fun deleteChatById(chatId: String)
}
