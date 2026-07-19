package com.voicebridge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voicebridge.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends WHERE isFriend = 1 AND isBlocked = 0")
    fun getFriendsFlow(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE isPendingRequest = 1 AND isBlocked = 0")
    fun getPendingRequestsFlow(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends")
    fun getAllDiscoveredFlow(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getFriendById(deviceId: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Query("UPDATE friends SET isOnline = :isOnline, lastSeen = :lastSeen WHERE deviceId = :deviceId")
    suspend fun updateOnlineStatus(deviceId: String, isOnline: Boolean, lastSeen: Long)

    @Query("UPDATE friends SET isFriend = :isFriend, isPendingRequest = :isPending, isOutgoingRequest = :isOutgoing WHERE deviceId = :deviceId")
    suspend fun updateFriendshipState(deviceId: String, isFriend: Boolean, isPending: Boolean, isOutgoing: Boolean)

    @Query("UPDATE friends SET isBlocked = :isBlocked WHERE deviceId = :deviceId")
    suspend fun updateBlockedStatus(deviceId: String, isBlocked: Boolean)

    @Query("UPDATE friends SET sharedKey = :sharedKey WHERE deviceId = :deviceId")
    suspend fun updateSharedKey(deviceId: String, sharedKey: String)
}
