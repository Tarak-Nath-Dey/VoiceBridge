package com.voicebridge.domain.repository

import android.content.Context
import android.content.SharedPreferences
import com.voicebridge.data.local.dao.UserDao
import com.voicebridge.data.local.entity.UserEntity
import com.voicebridge.data.security.EncryptionManager
import com.voicebridge.data.security.KeyExchangeManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao,
    private val encryptionManager: EncryptionManager,
    private val keyExchangeManager: KeyExchangeManager
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("voicebridge_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
    }

    /**
     * Gets or generates a stable unique local device ID
     */
    fun getOrCreateDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString().substring(0, 8).uppercase() // 8-char simple unique ID
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    /**
     * Checks if the user profile is already created
     */
    suspend fun isProfileCreated(): Boolean {
        return userDao.getUser() != null
    }

    /**
     * Gets the current local user flow
     */
    fun getUserFlow(): Flow<UserEntity?> {
        return userDao.getUserFlow()
    }

    /**
     * Retrieves the user profile directly
     */
    suspend fun getLocalUser(): UserEntity? {
        return userDao.getUser()
    }

    /**
     * Registers a new user account locally on first launch
     */
    suspend fun createProfile(username: String, avatarIndex: Int) {
        val devId = getOrCreateDeviceId()
        
        // Generate cryptographic keypair for E2EE
        val keyPair = keyExchangeManager.generateKeyPair()
        val publicKeyEncoded = keyExchangeManager.getPublicKeyEncoded(keyPair.public)
        val privateKeyEncoded = keyExchangeManager.getPrivateKeyEncoded(keyPair.private)
        
        // Local database storage: encrypt the private key
        val encryptedPrivateKey = encryptionManager.encrypt(privateKeyEncoded)
        
        val user = UserEntity(
            deviceId = devId,
            username = username,
            avatarIndex = avatarIndex,
            publicKey = publicKeyEncoded,
            privateKey = encryptedPrivateKey,
            status = "Hey there! I am using VoiceBridge offline."
        )
        userDao.insertUser(user)
    }

    /**
     * Update user profile status
     */
    suspend fun updateStatus(status: String) {
        val user = getLocalUser() ?: return
        userDao.updateStatus(user.deviceId, status)
    }

    /**
     * Update user profile username and avatar
     */
    suspend fun updateProfile(username: String, avatarIndex: Int) {
        val user = getLocalUser() ?: return
        userDao.updateProfile(user.deviceId, username, avatarIndex)
    }
}
