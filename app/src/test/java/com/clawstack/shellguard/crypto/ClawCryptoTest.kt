package com.clawstack.shellguard.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class ClawCryptoTest {

    @Test
    fun testEmptyStringSha256Hash() {
        val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val actual = ClawCrypto.hashHumanKey("")
        assertEquals(expected, actual)
    }

    @Test
    fun testClawKeyFormatValidation() {
        val validKey = "hu-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        assertTrue(ClawCrypto.isValidClawKey(validKey))

        // Invalid: missing prefix
        assertFalse(ClawCrypto.isValidClawKey("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))

        // Invalid: wrong prefix (lb- is for agent keys, not human master keys)
        assertFalse(ClawCrypto.isValidClawKey("lb-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))

        // Invalid: too short
        assertFalse(ClawCrypto.isValidClawKey("hu-12345"))

        // Invalid: too long
        assertFalse(ClawCrypto.isValidClawKey("hu-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef00"))

        // Invalid: non-hex characters
        assertFalse(ClawCrypto.isValidClawKey("hu-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdeg"))
    }

    @Test
    fun testGeneratedClawKeyIsValid() {
        val key = ClawCrypto.generateClawKey()
        assertTrue(key.startsWith("hu-"))
        assertEquals(67, key.length)
        assertTrue(ClawCrypto.isValidClawKey(key))
    }

    @Test
    fun testHmacSha256DeterministicOutput() {
        val key = "secret-key".toByteArray(StandardCharsets.UTF_8)
        val data = "test-message".toByteArray(StandardCharsets.UTF_8)
        val hmac1 = ClawCrypto.hmac("HmacSHA256", key, data)
        val hmac2 = ClawCrypto.hmac("HmacSHA256", key, data)
        assertEquals(32, hmac1.size)
        assertTrue(ClawCrypto.constantTimeEquals(hmac1, hmac2))
    }

    @Test
    fun testConstantTimeEquals() {
        val a = byteArrayOf(1, 2, 3, 4)
        val b = byteArrayOf(1, 2, 3, 4)
        val c = byteArrayOf(1, 2, 3, 5)
        assertTrue(ClawCrypto.constantTimeEquals(a, b))
        assertFalse(ClawCrypto.constantTimeEquals(a, c))
    }
}
