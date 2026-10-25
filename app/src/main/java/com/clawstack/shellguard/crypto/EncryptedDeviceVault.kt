package com.clawstack.shellguard.crypto

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedDeviceVault(private val context: Context) : DeviceVault {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "shellguard_device_vault",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            throw SecurityException(
                "Failed to initialize EncryptedSharedPreferences: hardware keystore or encryption unavailable",
                e
            )
        }
    }

    @Volatile
    private var inMemoryShellKey: ByteArray? = null

    override fun saveSession(
        token: String,
        serverUrl: String,
        ownerUuid: String,
        username: String,
        hashedKey: String,
        shellKey: ByteArray?
    ) {
        val editor = prefs.edit()
            .putString(KEY_SESSION_TOKEN, token)
            .putString(KEY_SERVER_URL, serverUrl)
            .putString(KEY_OWNER_UUID, ownerUuid)
            .putString(KEY_USERNAME, username)
            .putString(KEY_HASHED_KEY, hashedKey)

        if (shellKey != null) {
            inMemoryShellKey = shellKey.copyOf()
            editor.putString(KEY_SHELL_KEY, android.util.Base64.encodeToString(shellKey, android.util.Base64.NO_WRAP))
        } else {
            editor.remove(KEY_SHELL_KEY)
        }
        editor.apply()
    }

    override fun getSessionToken(): String? = prefs.getString(KEY_SESSION_TOKEN, null)

    override fun getServerUrl(): String? = prefs.getString(KEY_SERVER_URL, null)

    override fun getOwnerUuid(): String? = prefs.getString(KEY_OWNER_UUID, null)

    override fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    override fun getHashedKey(): String? = prefs.getString(KEY_HASHED_KEY, null)

    override fun hasActiveSession(): Boolean {
        val token = getSessionToken()
        val server = getServerUrl()
        val owner = getOwnerUuid()
        val key = getInMemoryShellKey()
        return !token.isNullOrBlank() && !server.isNullOrBlank() && !owner.isNullOrBlank() && key != null
    }

    override fun setInMemoryShellKey(key: ByteArray) {
        inMemoryShellKey = key.copyOf()
        prefs.edit().putString(KEY_SHELL_KEY, android.util.Base64.encodeToString(key, android.util.Base64.NO_WRAP)).apply()
    }

    override fun getInMemoryShellKey(): ByteArray? {
        inMemoryShellKey?.let { return it }
        val stored = prefs.getString(KEY_SHELL_KEY, null)
        if (!stored.isNullOrBlank()) {
            return try {
                val decoded = android.util.Base64.decode(stored, android.util.Base64.NO_WRAP)
                inMemoryShellKey = decoded
                decoded
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    override fun zeroizeMemory() {
        inMemoryShellKey?.fill(0.toByte())
        inMemoryShellKey = null
    }

    override fun clearPersistentShellKey() {
        prefs.edit().remove(KEY_SHELL_KEY).apply()
    }

    override fun clearSession() {
        zeroizeMemory()
        clearPersistentShellKey()
        prefs.edit().clear().apply()
    }

    override fun savePinHash(saltBase64: String, hashBase64: String) {
        prefs.edit()
            .putString(KEY_PIN_SALT, saltBase64)
            .putString(KEY_PIN_HASH, hashBase64)
            .putInt(KEY_FAILED_PIN_ATTEMPTS, 0)
            .apply()
    }

    override fun getPinSalt(): String? = prefs.getString(KEY_PIN_SALT, null)
    override fun getPinHash(): String? = prefs.getString(KEY_PIN_HASH, null)

    override fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .remove(KEY_FAILED_PIN_ATTEMPTS)
            .apply()
    }

    override fun hasPinConfigured(): Boolean = !getPinSalt().isNullOrBlank() && !getPinHash().isNullOrBlank()

    override fun getFailedPinAttempts(): Int = prefs.getInt(KEY_FAILED_PIN_ATTEMPTS, 0)

    override fun incrementFailedPinAttempts(): Int {
        val attempts = getFailedPinAttempts() + 1
        prefs.edit().putInt(KEY_FAILED_PIN_ATTEMPTS, attempts).apply()
        return attempts
    }

    override fun resetFailedPinAttempts() {
        prefs.edit().putInt(KEY_FAILED_PIN_ATTEMPTS, 0).apply()
    }

    override fun verifyMasterKey(huKey: String): Boolean {
        val storedHash = getHashedKey() ?: return false
        val computedHash = ClawCrypto.hashHumanKey(huKey)
        return java.security.MessageDigest.isEqual(
            computedHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    override fun verifyClawKeyJson(jsonContent: String): Boolean {
        val extractedKey = ClawCrypto.extractKey(jsonContent) ?: return false
        return verifyMasterKey(extractedKey)
    }

    companion object {
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_OWNER_UUID = "owner_uuid"
        private const val KEY_USERNAME = "username"
        private const val KEY_HASHED_KEY = "hashed_key"
        private const val KEY_SHELL_KEY = "shell_key"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_FAILED_PIN_ATTEMPTS = "failed_pin_attempts"
    }
}
