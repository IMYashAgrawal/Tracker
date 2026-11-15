package com.yash.tracker.security

import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * Handles all zero-knowledge encryption operations.
 * Derived keys are stored in volatile memory and can be wiped.
 */
object CryptoManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val PBKDF2_ITERATIONS = 600_000
    private const val KEY_LENGTH_BIT = 256

    private var activeKey: ByteArray? = null

    /**
     * Derives a 256-bit AES key from a password and salt using PBKDF2.
     * The resulting key is stored in memory until [wipeKey] is called.
     */
    fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BIT)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec).encoded
        activeKey = key
        return key
    }

    /**
     * Wipes the active key from memory by filling it with zeros.
     */
    fun wipeKey() {
        activeKey?.let {
            Arrays.fill(it, 0.toByte())
        }
        activeKey = null
    }

    fun isKeyLoaded(): Boolean = activeKey != null

    /**
     * Encrypts plaintext using AES-GCM with the active key.
     * Returns a Base64 string containing: IV + Ciphertext + Tag
     */
    fun encrypt(plaintext: String): String {
        val key = activeKey ?: throw IllegalStateException("Vault is locked")
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)
        
        val secretKey = SecretKeySpec(key, "AES")
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts a Base64 string (IV + Ciphertext + Tag) using the active key.
     */
    fun decrypt(encryptedPayload: String): String {
        val key = activeKey ?: throw IllegalStateException("Vault is locked")
        val combined = Base64.decode(encryptedPayload, Base64.NO_WRAP)
        
        val iv = ByteArray(IV_LENGTH_BYTE)
        System.arraycopy(combined, 0, iv, 0, iv.size)
        
        val ciphertext = ByteArray(combined.size - iv.size)
        System.arraycopy(combined, iv.size, ciphertext, 0, ciphertext.size)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(key, "AES")
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        
        val decrypted = cipher.doFinal(ciphertext)
        return String(decrypted, Charsets.UTF_8)
    }
    
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }
}
