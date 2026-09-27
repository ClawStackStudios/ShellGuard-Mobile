package com.clawstack.shellguard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpEngineTest {

    // RFC 6238 Standard Secret: "12345678901234567890" in Base32
    private val rfcSha1Secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    // RFC 6238 Standard SHA256 Secret: "12345678901234567890123456789012" in Base32
    private val rfcSha256Secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA===="

    @Test
    fun testRfc6238Sha1EightDigits() {
        // RFC 6238 Appendix B official test vectors
        assertEquals("94287082", TotpEngine.generateTotp(rfcSha1Secret, timestampMillis = 59_000L, digits = 8))
        assertEquals("07081804", TotpEngine.generateTotp(rfcSha1Secret, timestampMillis = 1111111109_000L, digits = 8))
        assertEquals("14050471", TotpEngine.generateTotp(rfcSha1Secret, timestampMillis = 1111111111_000L, digits = 8))
        assertEquals("89005924", TotpEngine.generateTotp(rfcSha1Secret, timestampMillis = 1234567890_000L, digits = 8))
        assertEquals("69279037", TotpEngine.generateTotp(rfcSha1Secret, timestampMillis = 2000000000_000L, digits = 8))
    }

    @Test
    fun testRfc6238Sha1SixDigits() {
        // Default 6 digits
        assertEquals("287082", TotpEngine.generateTotp(rfcSha1Secret, timestampMillis = 59_000L, digits = 6))
        assertEquals("081804", TotpEngine.generateTotp(rfcSha1Secret, timestampMillis = 1111111109_000L, digits = 6))
        assertEquals("050471", TotpEngine.generateTotp(rfcSha1Secret, timestampMillis = 1111111111_000L, digits = 6))
        assertEquals("005924", TotpEngine.generateTotp(rfcSha1Secret, timestampMillis = 1234567890_000L, digits = 6))
        assertEquals("279037", TotpEngine.generateTotp(rfcSha1Secret, timestampMillis = 2000000000_000L, digits = 6))
    }

    @Test
    fun testRfc6238Sha256EightDigits() {
        assertEquals(
            "46119246",
            TotpEngine.generateTotp(
                rfcSha256Secret,
                timestampMillis = 59_000L,
                digits = 8,
                algorithm = TotpEngine.HashAlgorithm.SHA256
            )
        )
        assertEquals(
            "68084774",
            TotpEngine.generateTotp(
                rfcSha256Secret,
                timestampMillis = 1111111109_000L,
                digits = 8,
                algorithm = TotpEngine.HashAlgorithm.SHA256
            )
        )
        assertEquals(
            "91819424",
            TotpEngine.generateTotp(
                rfcSha256Secret,
                timestampMillis = 1234567890_000L,
                digits = 8,
                algorithm = TotpEngine.HashAlgorithm.SHA256
            )
        )
    }

    @Test
    fun testSteamGuardGeneration() {
        val steamToken = TotpEngine.generateSteamGuard(rfcSha1Secret, timestampMillis = 1234567890_000L)
        assertEquals(5, steamToken.length)

        val allowedChars = "23456789BCDFGHJKMNPQRTVWXY"
        assertTrue(steamToken.all { it in allowedChars })

        // Deterministic check
        val steamToken2 = TotpEngine.generateSteamGuard(rfcSha1Secret, timestampMillis = 1234567890_000L)
        assertEquals(steamToken, steamToken2)
    }

    @Test
    fun testEmptyOrInvalidSecretFallback() {
        assertEquals("------", TotpEngine.generateTotp(""))
        assertEquals("------", TotpEngine.generateTotp("   "))
        assertEquals("-----", TotpEngine.generateSteamGuard(""))
    }
}
