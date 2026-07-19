package com.voicebridge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.voicebridge.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun getUserFlow(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET status = :status WHERE deviceId = :deviceId")
    suspend fun updateStatus(deviceId: String, status: String)

    @Query("UPDATE users SET username = :username, avatarIndex = :avatarIndex WHERE deviceId = :deviceId")
    suspend fun updateProfile(deviceId: String, username: String, avatarIndex: Int)
}
