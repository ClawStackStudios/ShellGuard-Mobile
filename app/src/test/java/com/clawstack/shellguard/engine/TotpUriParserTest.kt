package com.clawstack.shellguard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TotpUriParserTest {

    @Test
    fun testStandardOtpauthUri() {
        val uri = "otpauth://totp/GitHub:octocat?secret=JBSWY3DPEHPK3PXP&issuer=GitHub"
        val parsed = TotpUriParser.parse(uri)

        assertNotNull(parsed)
        parsed?.let {
            assertEquals("octocat", it.title)
            assertEquals("GitHub", it.issuer)
            assertEquals("JBSWY3DPEHPK3PXP", it.secret)
            assertEquals("SHA1", it.algorithm)
            assertEquals(6, it.digits)
            assertEquals(30L, it.period)
        }
    }

    @Test
    fun testUrlEncodedLabelAndIssuer() {
        val uri = "otpauth://totp/Acme%20Corp%3Aalice%40acme.com?secret=JBSWY3DPEHPK3PXP&issuer=Acme%20Corp"
        val parsed = TotpUriParser.parse(uri)

        assertNotNull(parsed)
        parsed?.let {
            assertEquals("alice@acme.com", it.title)
            assertEquals("Acme Corp", it.issuer)
            assertEquals("JBSWY3DPEHPK3PXP", it.secret)
        }
    }

    @Test
    fun testCustomParameters() {
        val uri = "otpauth://totp/SecureService?secret=MZXW6YQ=&digits=8&period=60&algorithm=SHA256"
        val parsed = TotpUriParser.parse(uri)

        assertNotNull(parsed)
        parsed?.let {
            assertEquals("MZXW6YQ", it.secret)
            assertEquals(8, it.digits)
            assertEquals(60L, it.period)
            assertEquals("SHA256", it.algorithm)
        }
    }

    @Test
    fun testRawBase32KeyFallback() {
        val rawKey = "JBSWY3DPEHPK3PXP"
        val parsed = TotpUriParser.parse(rawKey)

        assertNotNull(parsed)
        parsed?.let {
            assertEquals("JBSWY3DPEHPK3PXP", it.secret)
            assertEquals("Verification Code", it.title)
        }
    }

    @Test
    fun testInvalidUri() {
        assertNull(TotpUriParser.parse(""))
        assertNull(TotpUriParser.parse("    "))
        assertNull(TotpUriParser.parse("invalid-secret-890!"))
    }
}
