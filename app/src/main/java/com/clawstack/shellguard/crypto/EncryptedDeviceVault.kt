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
        prefs.edit()
            .putString(KEY_SESSION_TOKEN, token)
            .putString(KEY_SERVER_URL, serverUrl)
            .putString(KEY_OWNER_UUID, ownerUuid)
            .putString(KEY_USERNAME, username)
            .putString(KEY_HASHED_KEY, hashedKey)
            .apply()

        shellKey?.let {
            inMemoryShellKey = it.copyOf()
        }
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
        return !token.isNullOrBlank() && !server.isNullOrBlank() && !owner.isNullOrBlank()
    }

    fun setInMemoryShellKey(key: ByteArray) {
        inMemoryShellKey = key.copyOf()
    }

    fun getInMemoryShellKey(): ByteArray? = inMemoryShellKey

    fun zeroizeMemory() {
        inMemoryShellKey?.fill(0.toByte())
        inMemoryShellKey = null
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
    }
}
