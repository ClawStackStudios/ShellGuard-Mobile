package com.clawstack.shellguard.crypto

import java.util.concurrent.ConcurrentHashMap

class FakeDeviceVault : DeviceVault {

    private val storage = ConcurrentHashMap<String, Any>()

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
        storage["KEY_SESSION_TOKEN"] = token
        storage["KEY_SERVER_URL"] = serverUrl
        storage["KEY_OWNER_UUID"] = ownerUuid
        storage["KEY_USERNAME"] = username
        storage["KEY_HASHED_KEY"] = hashedKey

        if (shellKey != null) {
            inMemoryShellKey = shellKey.copyOf()
            storage["KEY_SHELL_KEY"] = android.util.Base64.encodeToString(shellKey, android.util.Base64.NO_WRAP)
        } else {
            storage.remove("KEY_SHELL_KEY")
        }
    }

    override fun getSessionToken(): String? = storage["KEY_SESSION_TOKEN"] as? String

    override fun getServerUrl(): String? = storage["KEY_SERVER_URL"] as? String

    override fun getOwnerUuid(): String? = storage["KEY_OWNER_UUID"] as? String

    override fun getUsername(): String? = storage["KEY_USERNAME"] as? String

    override fun getHashedKey(): String? = storage["KEY_HASHED_KEY"] as? String

    override fun hasActiveSession(): Boolean {
        val token = getSessionToken()
        val server = getServerUrl()
        val owner = getOwnerUuid()
        val key = getInMemoryShellKey()
        return !token.isNullOrBlank() && !server.isNullOrBlank() && !owner.isNullOrBlank() && key != null
    }

    override fun setInMemoryShellKey(key: ByteArray) {
        inMemoryShellKey = key.copyOf()
        storage["KEY_SHELL_KEY"] = android.util.Base64.encodeToString(key, android.util.Base64.NO_WRAP)
    }

    override fun getInMemoryShellKey(): ByteArray? {
        inMemoryShellKey?.let { return it }
        val stored = storage["KEY_SHELL_KEY"] as? String
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
        storage.remove("KEY_SHELL_KEY")
    }

    override fun clearSession() {
        zeroizeMemory()
        clearPersistentShellKey()
        storage.clear()
    }

    override fun savePinHash(saltBase64: String, hashBase64: String) {
        storage["KEY_PIN_SALT"] = saltBase64
        storage["KEY_PIN_HASH"] = hashBase64
        storage["KEY_FAILED_PIN_ATTEMPTS"] = 0
    }

    override fun getPinSalt(): String? = storage["KEY_PIN_SALT"] as? String

    override fun getPinHash(): String? = storage["KEY_PIN_HASH"] as? String

    override fun clearPin() {
        storage.remove("KEY_PIN_SALT")
        storage.remove("KEY_PIN_HASH")
        storage.remove("KEY_FAILED_PIN_ATTEMPTS")
    }

    override fun hasPinConfigured(): Boolean = !getPinSalt().isNullOrBlank() && !getPinHash().isNullOrBlank()

    override fun getFailedPinAttempts(): Int = (storage["KEY_FAILED_PIN_ATTEMPTS"] as? Int) ?: 0

    override fun incrementFailedPinAttempts(): Int {
        val attempts = getFailedPinAttempts() + 1
        storage["KEY_FAILED_PIN_ATTEMPTS"] = attempts
        return attempts
    }

    override fun resetFailedPinAttempts() {
        storage["KEY_FAILED_PIN_ATTEMPTS"] = 0
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
}
