package com.clawstack.shellguard.crypto

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Core cryptographic utilities for ClawKey generation, validation, SHA-256 hashing, and HMAC calculation.
 */
object ClawCrypto {

    private val CLAW_KEY_REGEX = Regex("^hu-[0-9a-f]{64}$")
    private val secureRandom = SecureRandom()

    /**
     * Validates whether a given key matches the sovereign ClawKey format (hu- followed by 64 hex characters).
     */
    fun isValidClawKey(key: String): Boolean {
        return CLAW_KEY_REGEX.matches(key.trim())
    }

    /**
     * Hashes a raw ClawKey or human secret via SHA-256, returning a 64-character lowercase hex string.
     * Used for zero-knowledge server authentication (the server only ever sees SHA-256(hu-)).
     */
    fun hashHumanKey(rawKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawKey.trim().toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a new cryptographically secure ClawKey (`hu-` + 64 random hex characters).
     */
    fun generateClawKey(): String {
        val randomBytes = ByteArray(32)
        secureRandom.nextBytes(randomBytes)
        val hex = randomBytes.joinToString("") { "%02x".format(it) }
        return "hu-$hex"
    }

    /**
     * Generates a specified number of cryptographically secure random bytes.
     */
    fun generateSecureBytes(length: Int = 32): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    /**
     * Computes an HMAC digest using the specified algorithm (e.g. HmacSHA256, HmacSHA1).
     */
    fun hmac(algorithm: String, key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance(algorithm)
        val secretKeySpec = SecretKeySpec(key, algorithm)
        mac.init(secretKeySpec)
        return mac.doFinal(data)
    }

    /**
     * Constant-time comparison between two byte arrays to protect against timing attacks.
     */
    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        return MessageDigest.isEqual(a, b)
    }
}
