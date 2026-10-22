package com.example.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the user's personal Gemini API key encrypted with an Android Keystore AES-GCM key.
 * The key is kept on this device only; it is never written to Firebase or exported with chats.
 */
object GeminiApiKeyStore {
    private const val PREFERENCES = "bloom_secure_settings"
    private const val ENTRY = "gemini_api_key_encrypted"
    private const val KEY_ALIAS = "bloom_gemini_api_key_aes_gcm_v1"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val IV_SIZE = 12
    private const val TAG_SIZE_BITS = 128

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun getStoredKey(): String {
        val context = appContext ?: return ""
        return try {
            val encoded = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString(ENTRY, null) ?: return ""
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            if (packed.size <= IV_SIZE) return ""
            val iv = packed.copyOfRange(0, IV_SIZE)
            val ciphertext = packed.copyOfRange(IV_SIZE, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            // Do not log secrets. If the Keystore key is invalidated, let the user enter it again.
            ""
        }
    }

    fun saveKey(context: Context, value: String): Boolean {
        initialize(context)
        val prefs = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (value.isBlank()) {
            return prefs.edit().remove(ENTRY).commit()
        }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(value.trim().toByteArray(Charsets.UTF_8))
            val packed = ByteBuffer.allocate(cipher.iv.size + encrypted.size)
                .put(cipher.iv)
                .put(encrypted)
                .array()
            prefs.edit().putString(ENTRY, Base64.encodeToString(packed, Base64.NO_WRAP)).commit()
        } catch (_: Exception) {
            false
        }
    }

    fun clearKey(context: Context): Boolean {
        initialize(context)
        return context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .remove(ENTRY)
            .commit()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }
}
