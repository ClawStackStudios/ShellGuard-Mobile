package com.clawstack.shellguard.crypto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.GeneralSecurityException

class ShellCryptionEngineTest {

    private val testHuKey = "hu-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private val testUserUuid = "usr_99887766-5544-3322-1100-aabbccddeeff"
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testHkdfKeyDerivationDeterministic() {
        val key1 = ShellCryptionEngine.deriveShellKey(testHuKey, testUserUuid)
        val key2 = ShellCryptionEngine.deriveShellKey(testHuKey, testUserUuid)

        assertEquals(32, key1.size)
        assertArrayEquals(key1, key2)

        val differentUuidKey = ShellCryptionEngine.deriveShellKey(testHuKey, "usr_different_uuid")
        assertFalse(key1.contentEquals(differentUuidKey))
    }

    @Test
    fun testEncryptAndDecryptRoundTrip() {
        val shellKey = ShellCryptionEngine.deriveShellKey(testHuKey, testUserUuid)
        val plainText = "VaultMasterP@ssw0rd!2026#Reef"
        val aad = ShellCryptionEngine.AadNamespace.pearlSecret("pearl-12345")

        val envelopeJson = ShellCryptionEngine.encryptField(plainText, shellKey, aad)
        assertNotNull(envelopeJson)

        val envelope = json.decodeFromString<ShellCryptionEnvelope>(envelopeJson)
        assertEquals(1, envelope.v)
        assertEquals("AES-GCM-256", envelope.alg)
        assertEquals(aad, envelope.aad)
        assertTrue(envelope.iv.isNotBlank())
        assertTrue(envelope.ct.isNotBlank())

        val decryptedText = ShellCryptionEngine.decryptField(envelopeJson, shellKey, aad)
        assertEquals(plainText, decryptedText)
    }

    @Test
    fun testAadMismatchThrowsSecurityException() {
        val shellKey = ShellCryptionEngine.deriveShellKey(testHuKey, testUserUuid)
        val plainText = "Sensitive Content"
        val envelopeJson = ShellCryptionEngine.encryptField(plainText, shellKey, "vault_pearls:id_1")

        try {
            ShellCryptionEngine.decryptField(envelopeJson, shellKey, "vault_pearls:id_2")
            fail("Expected SecurityException on AAD mismatch")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("AAD mismatch") == true)
        }
    }

    @Test
    fun testTamperedCiphertextThrowsException() {
        val shellKey = ShellCryptionEngine.deriveShellKey(testHuKey, testUserUuid)
        val plainText = "Tamper Proof Data"
        val aad = "vault_secure_notes:note_1"
        val envelopeJson = ShellCryptionEngine.encryptField(plainText, shellKey, aad)

        val envelope = json.decodeFromString<ShellCryptionEnvelope>(envelopeJson)
        val originalCt = envelope.ct
        // Tamper with ciphertext by altering last characters
        val tamperedCt = originalCt.substring(0, originalCt.length - 2) + "=="
        val tamperedEnvelope = envelope.copy(ct = tamperedCt)
        val tamperedJson = json.encodeToString(tamperedEnvelope)

        try {
            ShellCryptionEngine.decryptField(tamperedJson, shellKey, aad)
            fail("Expected GeneralSecurityException on tampered ciphertext")
        } catch (e: GeneralSecurityException) {
            // Expected AES-GCM tag verification failure
        } catch (e: Exception) {
            // Expected catch
        }
    }

    @Test
    fun testAll10AadNamespacesRoundTrip() {
        val shellKey = ShellCryptionEngine.deriveShellKey(testHuKey, testUserUuid)
        val id = "entity-abc-987"
        val namespaces = listOf(
            ShellCryptionEngine.AadNamespace.pearlSecret(id),
            ShellCryptionEngine.AadNamespace.pearlTotp(id),
            ShellCryptionEngine.AadNamespace.pearlCustomFields(id),
            ShellCryptionEngine.AadNamespace.pearlPasswordHistory(id),
            ShellCryptionEngine.AadNamespace.secureNoteContent(id),
            ShellCryptionEngine.AadNamespace.secureNoteCustomFields(id),
            ShellCryptionEngine.AadNamespace.sshKeyPrivate(id),
            ShellCryptionEngine.AadNamespace.sshKeyCustomFields(id),
            ShellCryptionEngine.AadNamespace.secureAttachment(id),
            ShellCryptionEngine.AadNamespace.totpBackup(testUserUuid)
        )

        for (aad in namespaces) {
            val samplePayload = "Secret Payload for $aad"
            val envelope = ShellCryptionEngine.encryptField(samplePayload, shellKey, aad)
            val decrypted = ShellCryptionEngine.decryptField(envelope, shellKey, aad)
            assertEquals("Failed for AAD: $aad", samplePayload, decrypted)
        }
    }
}
