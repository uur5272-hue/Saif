package com.example.motoai.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.BuildConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurePreferences(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        ensureMasterKey()
    }

    private fun ensureMasterKey() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return ""
            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decrypted = cipher.doFinal(cipherText)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    fun saveGeminiApiKey(apiKey: String) {
        val encrypted = encrypt(apiKey.trim())
        prefs.edit().putString(KEY_GEMINI_API_KEY, encrypted).apply()
    }

    fun getGeminiApiKey(): String {
        val encrypted = prefs.getString(KEY_GEMINI_API_KEY, null)
        if (!encrypted.isNullOrEmpty()) {
            val decrypted = decrypt(encrypted)
            if (decrypted.isNotEmpty()) return decrypted
        }
        // Fallback to BuildConfig if provided at build time via Secrets Gradle plugin
        return try {
            val buildConfigKey = BuildConfig.GEMINI_API_KEY
            if (buildConfigKey.isNotEmpty() && !buildConfigKey.contains("MY_GEMINI_API_KEY")) {
                buildConfigKey
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun hasCustomGeminiApiKey(): Boolean {
        return !prefs.getString(KEY_GEMINI_API_KEY, null).isNullOrEmpty()
    }

    fun clearGeminiApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    // Provider configs
    fun saveImageProvider(provider: String) {
        prefs.edit().putString(KEY_IMAGE_PROVIDER, provider).apply()
    }

    fun getImageProvider(): String = prefs.getString(KEY_IMAGE_PROVIDER, "gemini") ?: "gemini"

    fun saveVideoProvider(provider: String) {
        prefs.edit().putString(KEY_VIDEO_PROVIDER, provider).apply()
    }

    fun getVideoProvider(): String = prefs.getString(KEY_VIDEO_PROVIDER, "veo") ?: "veo"

    // Assistant settings
    fun saveVoiceLanguage(lang: String) {
        prefs.edit().putString(KEY_VOICE_LANG, lang).apply()
    }

    fun getVoiceLanguage(): String = prefs.getString(KEY_VOICE_LANG, "en-US") ?: "en-US"

    fun saveVoicePitch(pitch: Float) {
        prefs.edit().putFloat(KEY_VOICE_PITCH, pitch).apply()
    }

    fun getVoicePitch(): Float = prefs.getFloat(KEY_VOICE_PITCH, 0.95f)

    fun saveVoiceSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_VOICE_SPEED, speed).apply()
    }

    fun getVoiceSpeed(): Float = prefs.getFloat(KEY_VOICE_SPEED, 1.05f)

    fun saveWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD, enabled).apply()
    }

    fun isWakeWordEnabled(): Boolean = prefs.getBoolean(KEY_WAKE_WORD, true)

    fun saveConfirmSensitiveActions(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_ACTIONS, enabled).apply()
    }

    fun isConfirmSensitiveActions(): Boolean = prefs.getBoolean(KEY_CONFIRM_ACTIONS, true)

    fun saveUiLanguage(lang: String) {
        prefs.edit().putString(KEY_UI_LANG, lang).apply()
    }

    fun getUiLanguage(): String = prefs.getString(KEY_UI_LANG, "en") ?: "en"

    companion object {
        private const val PREFS_NAME = "moto_ai_secure_prefs"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "MotoAiMasterKeyAlias"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        private const val KEY_GEMINI_API_KEY = "encrypted_gemini_api_key"
        private const val KEY_IMAGE_PROVIDER = "image_provider"
        private const val KEY_VIDEO_PROVIDER = "video_provider"
        private const val KEY_VOICE_LANG = "voice_language"
        private const val KEY_VOICE_PITCH = "voice_pitch"
        private const val KEY_VOICE_SPEED = "voice_speed"
        private const val KEY_WAKE_WORD = "wake_word_enabled"
        private const val KEY_CONFIRM_ACTIONS = "confirm_sensitive_actions"
        private const val KEY_UI_LANG = "ui_language"
    }
}
