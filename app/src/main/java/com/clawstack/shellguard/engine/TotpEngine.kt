package com.clawstack.shellguard.engine

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * Deterministic Algorithmic TOTP Generator (RFC 6238 & RFC 4226) for ShellGuard Mobile.
 *
 * Computes time-synchronized numeric OTPs (6 or 8 digits) across SHA-1, SHA-256, and SHA-512,
 * as well as 5-character alphanumeric Steam Guard tokens without requiring network access.
 */
object TotpEngine {
    const val DEFAULT_TIME_STEP_SECONDS = 30L
    const val DEFAULT_DIGITS = 6

    enum class HashAlgorithm(val hmacName: String) {
        SHA1("HmacSHA1"),
        SHA256("HmacSHA256"),
        SHA512("HmacSHA512");

        companion object {
            fun fromString(value: String?): HashAlgorithm {
                return when (value?.uppercase()?.trim()) {
                    "SHA256", "HMACSHA256" -> SHA256
                    "SHA512", "HMACSHA512" -> SHA512
                    else -> SHA1
                }
            }
        }
    }

    /**
     * Computes the current TOTP numeric code for a given Base32 secret.
     */
    fun generateTotp(
        secretBase32: String,
        timestampMillis: Long = System.currentTimeMillis(),
        timeStepSeconds: Long = DEFAULT_TIME_STEP_SECONDS,
        digits: Int = DEFAULT_DIGITS,
        algorithm: HashAlgorithm = HashAlgorithm.SHA1
    ): String {
        val cleanSecret = secretBase32.replace(" ", "").replace("-", "").uppercase()
        if (cleanSecret.isBlank()) return "------"

        return try {
            val keyBytes = Base32Decoder.decode(cleanSecret)
            if (keyBytes.isEmpty()) return "------"

            val timeWindow = (timestampMillis / 1000L) / timeStepSeconds
            val counterBytes = ByteBuffer.allocate(8).putLong(timeWindow).array()

            val mac = Mac.getInstance(algorithm.hmacName)
            mac.init(SecretKeySpec(keyBytes, algorithm.hmacName))
            val hash = mac.doFinal(counterBytes)

            // Dynamic Truncation (RFC 4226 §5.4)
            val offset = hash[hash.size - 1].toInt() and 0x0F
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val modulus = (10.0.pow(digits.toDouble())).toLong()
            val otp = binary % modulus
            otp.toString().padStart(digits, '0')
        } catch (e: Exception) {
            "------"
        }
    }

    /**
     * Computes a 5-character alphanumeric Steam Guard token.
     */
    fun generateSteamGuard(
        secretBase32: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): String {
        val steamChars = "23456789BCDFGHJKMNPQRTVWXY"
        val cleanSecret = secretBase32.replace(" ", "").replace("-", "").uppercase()
        if (cleanSecret.isBlank()) return "-----"

        return try {
            val keyBytes = Base32Decoder.decode(cleanSecret)
            if (keyBytes.isEmpty()) return "-----"

            val timeWindow = (timestampMillis / 1000L) / 30L
            val counterBytes = ByteBuffer.allocate(8).putLong(timeWindow).array()

            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
            val hash = mac.doFinal(counterBytes)

            val offset = hash[hash.size - 1].toInt() and 0x0F
            var fullcode = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val code = StringBuilder()
            for (i in 0 until 5) {
                code.append(steamChars[fullcode % steamChars.length])
                fullcode /= steamChars.length
            }
            code.toString()
        } catch (e: Exception) {
            "-----"
        }
    }
}
