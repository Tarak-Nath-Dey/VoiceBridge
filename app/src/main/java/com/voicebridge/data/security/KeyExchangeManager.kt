package com.voicebridge.data.security

import java.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyExchangeManager @Inject constructor() {

    private val ecCurve = "secp256r1"
    private val keyAgreementAlgorithm = "ECDH"
    private val symmetricAlgorithm = "AES"
    private val aesTransformation = "AES/GCM/NoPadding"
    private val ivSize = 12
    private val tagSize = 128

    /**
     * Generates a new EC (secp256r1) KeyPair
     */
    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        val ecSpec = ECGenParameterSpec(ecCurve)
        keyPairGenerator.initialize(ecSpec, SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Converts a PublicKey to a Base64 encoded string
     */
    fun getPublicKeyEncoded(publicKey: PublicKey): String {
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    /**
     * Converts a PrivateKey to a Base64 encoded string
     */
    fun getPrivateKeyEncoded(privateKey: PrivateKey): String {
        return Base64.getEncoder().encodeToString(privateKey.encoded)
    }

    /**
     * Loads a PublicKey from a Base64 encoded string
     */
    fun loadPublicKey(base64Str: String): PublicKey {
        val keyBytes = Base64.getDecoder().decode(base64Str)
        val spec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePublic(spec)
    }

    /**
     * Loads a PrivateKey from a Base64 encoded string
     */
    fun loadPrivateKey(base64Str: String): PrivateKey {
        val keyBytes = Base64.getDecoder().decode(base64Str)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePrivate(spec)
    }

    /**
     * Generates a shared symmetric AES-256 key from a local private key and peer's public key
     */
    fun deriveSharedKey(localPrivateKey: PrivateKey, peerPublicKey: PublicKey): SecretKeySpec {
        val keyAgreement = KeyAgreement.getInstance(keyAgreementAlgorithm)
        keyAgreement.init(localPrivateKey)
        keyAgreement.doPhase(peerPublicKey, true)
        val rawSharedSecret = keyAgreement.generateSecret()
        
        // Use SHA-256 to hash the shared secret into a clean 256-bit AES key
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedKeyBytes = digest.digest(rawSharedSecret)
        return SecretKeySpec(hashedKeyBytes, symmetricAlgorithm)
    }

    /**
     * Encrypts plaintext string using the derived shared secret key
     */
    fun encryptWithSharedKey(plainText: String, sharedKey: SecretKeySpec): String {
        val cipher = Cipher.getInstance(aesTransformation)
        val iv = ByteArray(ivSize)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(tagSize, iv)
        cipher.init(Cipher.ENCRYPT_MODE, sharedKey, spec)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        
        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Decrypts ciphertext string using the derived shared secret key
     */
    fun decryptWithSharedKey(cipherText: String, sharedKey: SecretKeySpec): String {
        return try {
            val combined = Base64.getDecoder().decode(cipherText)
            if (combined.size <= ivSize) return ""
            
            val iv = ByteArray(ivSize)
            System.arraycopy(combined, 0, iv, 0, ivSize)
            
            val encryptedBytes = ByteArray(combined.size - ivSize)
            System.arraycopy(combined, ivSize, encryptedBytes, 0, encryptedBytes.size)
            
            val cipher = Cipher.getInstance(aesTransformation)
            val spec = GCMParameterSpec(tagSize, iv)
            cipher.init(Cipher.DECRYPT_MODE, sharedKey, spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            "[E2EE Decryption Failure]"
        }
    }
}
