package com.voicebridge

import com.voicebridge.data.security.KeyExchangeManager
import org.junit.Assert.*
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

class CryptographyTest {

    private val keyExchangeManager = KeyExchangeManager()

    @Test
    fun testKeyPairGeneration() {
        val keyPair = keyExchangeManager.generateKeyPair()
        assertNotNull(keyPair.public)
        assertNotNull(keyPair.private)
        
        val pubEncoded = keyExchangeManager.getPublicKeyEncoded(keyPair.public)
        val privEncoded = keyExchangeManager.getPrivateKeyEncoded(keyPair.private)
        
        assertTrue(pubEncoded.isNotEmpty())
        assertTrue(privEncoded.isNotEmpty())
    }

    @Test
    fun testKeySerialization() {
        val keyPair = keyExchangeManager.generateKeyPair()
        val pubEncoded = keyExchangeManager.getPublicKeyEncoded(keyPair.public)
        val privEncoded = keyExchangeManager.getPrivateKeyEncoded(keyPair.private)
        
        val loadedPubKey = keyExchangeManager.loadPublicKey(pubEncoded)
        val loadedPrivKey = keyExchangeManager.loadPrivateKey(privEncoded)
        
        assertEquals(keyPair.public, loadedPubKey)
        assertEquals(keyPair.private, loadedPrivKey)
    }

    @Test
    fun testE2eeDiffieHellmanExchange() {
        // 1. Generate Alice and Bob key pairs
        val aliceKeyPair = keyExchangeManager.generateKeyPair()
        val bobKeyPair = keyExchangeManager.generateKeyPair()
        
        // 2. Perform key agreement
        // Alice computes key using Bob's public key
        val aliceSharedKey = keyExchangeManager.deriveSharedKey(aliceKeyPair.private, bobKeyPair.public)
        // Bob computes key using Alice's public key
        val bobSharedKey = keyExchangeManager.deriveSharedKey(bobKeyPair.private, aliceKeyPair.public)
        
        // 3. Verify keys match
        assertArrayEquals(aliceSharedKey.encoded, bobSharedKey.encoded)
        
        // 4. Test E2EE symmetric encryption/decryption
        val originalText = "Hello Bob! This is an offline encrypted mesh message."
        val ciphertext = keyExchangeManager.encryptWithSharedKey(originalText, aliceSharedKey)
        
        // Ensure it is encrypted (not plaintext)
        assertNotEquals(originalText, ciphertext)
        
        // Bob decrypts
        val decryptedText = keyExchangeManager.decryptWithSharedKey(ciphertext, bobSharedKey)
        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testE2eeDecryptionFailureWithIncorrectKey() {
        val aliceKeyPair = keyExchangeManager.generateKeyPair()
        val bobKeyPair = keyExchangeManager.generateKeyPair()
        val eveKeyPair = keyExchangeManager.generateKeyPair() // Intruder
        
        val aliceSharedKey = keyExchangeManager.deriveSharedKey(aliceKeyPair.private, bobKeyPair.public)
        val eveSharedKey = keyExchangeManager.deriveSharedKey(eveKeyPair.private, bobKeyPair.public) // Eve tries to hijack
        
        val originalText = "Top secret message"
        val ciphertext = keyExchangeManager.encryptWithSharedKey(originalText, aliceSharedKey)
        
        // Eve attempts decryption
        val decryptedText = keyExchangeManager.decryptWithSharedKey(ciphertext, eveSharedKey)
        assertEquals("[E2EE Decryption Failure]", decryptedText)
    }
}
