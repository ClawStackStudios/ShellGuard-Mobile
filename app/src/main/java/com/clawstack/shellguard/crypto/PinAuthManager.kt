package com.clawstack.shellguard.crypto

import android.util.Base64
import com.clawstack.shellguard.data.local.SettingsRepository
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Manages PIN code generation, salting, PBKDF2-SHA256 derivation, constant-time verification,
 * and failed attempt lockout tracking.
 */
class PinAuthManager(
    private val deviceVault: DeviceVault,
    private val settingsRepository: SettingsRepository
) {
    private val secureRandom = SecureRandom()

    /**
     * Hashes and stores the new PIN using PBKDF2-SHA256 with 100,000 iterations and a fresh 16-byte salt.
     */
    suspend fun setupPin(pin: String) {
        val cleanPin = pin.trim()
        require(cleanPin.length in 4..8 && cleanPin.all { it.isDigit() }) {
            "PIN must be between 4 and 8 numeric digits"
        }

        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)

        val hash = deriveHash(cleanPin, salt)

        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        deviceVault.savePinHash(saltBase64, hashBase64)
        settingsRepository.setHasPinConfigured(true)
    }

    /**
     * Constant-time verification of a candidate PIN against stored salted PBKDF2-SHA256 hash.
     * Increments attempt counter on failure; resets counter on success.
     */
    fun verifyPin(candidatePin: String): Boolean {
        val cleanPin = candidatePin.trim()
        if (cleanPin.isEmpty()) return false

        val saltBase64 = deviceVault.getPinSalt() ?: return false
        val storedHashBase64 = deviceVault.getPinHash() ?: return false

        val salt = try {
            Base64.decode(saltBase64, Base64.NO_WRAP)
        } catch (_: Exception) {
            return false
        }

        val storedHash = try {
            Base64.decode(storedHashBase64, Base64.NO_WRAP)
        } catch (_: Exception) {
            return false
        }

        val candidateHash = deriveHash(cleanPin, salt)

        val isMatch = MessageDigest.isEqual(candidateHash, storedHash)
        if (isMatch) {
            deviceVault.resetFailedPinAttempts()
        } else {
            deviceVault.incrementFailedPinAttempts()
        }
        return isMatch
    }

    /**
     * Checks if a PIN has been successfully configured and stored in the encrypted device vault.
     */
    fun hasPin(): Boolean = deviceVault.hasPinConfigured()

    /**
     * Wipes the stored PIN hash, salt, and failure counter.
     */
    suspend fun clearPin() {
        deviceVault.clearPin()
        settingsRepository.setHasPinConfigured(false)
    }

    /**
     * Returns the remaining attempts before PIN lockout triggers.
     */
    fun getRemainingAttempts(maxAttempts: Int): Int {
        val currentFailed = deviceVault.getFailedPinAttempts()
        return (maxAttempts - currentFailed).coerceAtLeast(0)
    }

    /**
     * Returns true if failed attempts have reached or exceeded the configured lockout threshold.
     */
    fun isLockedOut(maxAttempts: Int): Boolean {
        return deviceVault.getFailedPinAttempts() >= maxAttempts
    }

    /**
     * Resets the failed attempt counter (e.g. after successful Master Key re-authentication).
     */
    fun resetAttempts() {
        deviceVault.resetFailedPinAttempts()
    }

    private fun deriveHash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    companion object {
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_BYTES = 16
    }
}
