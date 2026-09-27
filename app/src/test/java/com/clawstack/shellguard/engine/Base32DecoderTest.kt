package com.clawstack.shellguard.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.StandardCharsets

class Base32DecoderTest {

    @Test
    fun testEmptyInput() {
        val result = Base32Decoder.decode("")
        assertEquals(0, result.size)
    }

    @Test
    fun testRfc4648TestVectors() {
        // RFC 4648 Section 10 Test Vectors
        // BASE32("") = ""
        // BASE32("f") = "MY======"
        // BASE32("fo") = "MZXQ===="
        // BASE32("foo") = "MZXW6==="
        // BASE32("foob") = "MZXW6YQ="
        // BASE32("fooba") = "MZXW6YTB"
        // BASE32("foobar") = "MZXW6YTBOI======"

        assertEquals("f", String(Base32Decoder.decode("MY======"), StandardCharsets.UTF_8))
        assertEquals("fo", String(Base32Decoder.decode("MZXQ===="), StandardCharsets.UTF_8))
        assertEquals("foo", String(Base32Decoder.decode("MZXW6==="), StandardCharsets.UTF_8))
        assertEquals("foob", String(Base32Decoder.decode("MZXW6YQ="), StandardCharsets.UTF_8))
        assertEquals("fooba", String(Base32Decoder.decode("MZXW6YTB"), StandardCharsets.UTF_8))
        assertEquals("foobar", String(Base32Decoder.decode("MZXW6YTBOI======"), StandardCharsets.UTF_8))
    }

    @Test
    fun testSanitizationSpacesAndHyphens() {
        val withSeparators = "MZXW-6=== =="
        val decoded = Base32Decoder.decode(withSeparators)
        assertEquals("foo", String(decoded, StandardCharsets.UTF_8))
    }

    @Test
    fun testCaseInsensitiveDecoding() {
        val lowercase = "mzxw6==="
        val decoded = Base32Decoder.decode(lowercase)
        assertEquals("foo", String(decoded, StandardCharsets.UTF_8))
    }

    @Test
    fun testWithoutPadding() {
        val unpadded = "MZXW6"
        val decoded = Base32Decoder.decode(unpadded)
        assertEquals("foo", String(decoded, StandardCharsets.UTF_8))
    }
}
