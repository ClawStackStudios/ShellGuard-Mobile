package com.clawstack.shellguard.crypto

/**
 * Interface contract for hardware-backed device vault key and session persistence.
 */
interface DeviceVault {
    fun saveSession(
        token: String,
        serverUrl: String,
        ownerUuid: String,
        username: String,
        hashedKey: String,
        shellKey: ByteArray? = null
    )

    fun getSessionToken(): String?
    fun getServerUrl(): String?
    fun getOwnerUuid(): String?
    fun getUsername(): String?
    fun getHashedKey(): String?
    fun hasActiveSession(): Boolean
    fun setInMemoryShellKey(key: ByteArray)
    fun getInMemoryShellKey(): ByteArray?
    fun zeroizeMemory()
    fun clearPersistentShellKey()
    fun clearSession()
    fun savePinHash(saltBase64: String, hashBase64: String)
    fun getPinSalt(): String?
    fun getPinHash(): String?
    fun clearPin()
    fun hasPinConfigured(): Boolean
    fun getFailedPinAttempts(): Int
    fun incrementFailedPinAttempts(): Int
    fun resetFailedPinAttempts()
    fun verifyMasterKey(huKey: String): Boolean
    fun verifyClawKeyJson(jsonContent: String): Boolean
}
