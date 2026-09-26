package com.clawstack.shellguard.crypto

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedDeviceVault(private val context: Context) {

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
        } catch (e: Throwable) {
            Log.w("EncryptedDeviceVault", "Fallback to standard preferences (headless/test mode): ${e.message}")
            context.getSharedPreferences("shellguard_device_vault_test", Context.MODE_PRIVATE)
        }
    }

    @Volatile
    private var inMemoryShellKey: ByteArray? = null

    fun saveSession(
        token: String,
        serverUrl: String,
        ownerUuid: String,
        username: String,
        hashedKey: String,
        shellKey: ByteArray? = null
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

    fun getSessionToken(): String? = prefs.getString(KEY_SESSION_TOKEN, null)

    fun getServerUrl(): String? = prefs.getString(KEY_SERVER_URL, null)

    fun getOwnerUuid(): String? = prefs.getString(KEY_OWNER_UUID, null)

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getHashedKey(): String? = prefs.getString(KEY_HASHED_KEY, null)

    fun hasActiveSession(): Boolean {
        val token = getSessionToken()
        val server = getServerUrl()
        val owner = getOwnerUuid()
        val key = getInMemoryShellKey()
        return !token.isNullOrBlank() && !server.isNullOrBlank() && !owner.isNullOrBlank() && key != null
    }

    fun setInMemoryShellKey(key: ByteArray) {
        inMemoryShellKey = key.copyOf()
        prefs.edit().putString(KEY_SHELL_KEY, android.util.Base64.encodeToString(key, android.util.Base64.NO_WRAP)).apply()
    }

    fun getInMemoryShellKey(): ByteArray? {
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

    fun zeroizeMemory() {
        inMemoryShellKey?.fill(0.toByte())
        inMemoryShellKey = null
        prefs.edit().remove(KEY_SHELL_KEY).apply()
    }

    fun clearSession() {
        zeroizeMemory()
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_OWNER_UUID = "owner_uuid"
        private const val KEY_USERNAME = "username"
        private const val KEY_HASHED_KEY = "hashed_key"
        private const val KEY_SHELL_KEY = "shell_key"
    }
}
