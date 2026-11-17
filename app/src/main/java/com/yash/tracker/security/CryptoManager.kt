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
 * The password hash is compared in-app. Derived keys live only in volatile RAM.
 * Nothing is persisted to disk.
 */
object CryptoManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val KEY_LENGTH_BIT = 256

    // Encryption key derivation uses higher iterations with a random session salt
    private const val ENCRYPT_PBKDF2_ITERATIONS = 600_000

    // Password verification constants — must match the Python script that generated the hash:
    //   hashlib.pbkdf2_hmac('sha256', password, salt, 100000, dklen=32)
    private const val VERIFY_PBKDF2_ITERATIONS = 100_000
    private const val STORED_PASSWORD_HASH = "ZYzuWyU+tt+eW7YM50Siq5LpEte+/uIukGNWBzI+kw4="
    private val VERIFY_SALT = "TrackerFixedSalt01".toByteArray(Charsets.UTF_8)

    // --- Volatile (RAM-only) state ---
    @Volatile
    private var activeKey: ByteArray? = null

    @Volatile
    private var sessionSalt: ByteArray? = null

    /**
     * Computes PBKDF2-HMAC-SHA256 of the given password with the fixed salt
     * and compares against the stored hash. Matches the Python generation script exactly.
     */
    fun verifyPassword(password: String): Boolean {
        val spec = PBEKeySpec(password.toCharArray(), VERIFY_SALT, VERIFY_PBKDF2_ITERATIONS, KEY_LENGTH_BIT)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        val inputHash = Base64.encodeToString(hashBytes, Base64.NO_WRAP)
        return inputHash == STORED_PASSWORD_HASH
    }

    /**
     * Derives a 256-bit AES key from the password using PBKDF2 with a random
     * session salt. Both the key and salt are held only in volatile memory.
     */
    fun deriveAndHoldKey(password: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(password.toCharArray(), salt, ENCRYPT_PBKDF2_ITERATIONS, KEY_LENGTH_BIT)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec).encoded

        // Wipe previous key if any
        activeKey?.let { Arrays.fill(it, 0.toByte()) }
        sessionSalt?.let { Arrays.fill(it, 0.toByte()) }

        activeKey = key
        sessionSalt = salt

        // Wipe the password chars from the PBEKeySpec
        spec.clearPassword()
    }

    /**
     * Wipes the active key and session salt from memory.
     */
    fun wipeKey() {
        activeKey?.let { Arrays.fill(it, 0.toByte()) }
        sessionSalt?.let { Arrays.fill(it, 0.toByte()) }
        activeKey = null
        sessionSalt = null
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
}
