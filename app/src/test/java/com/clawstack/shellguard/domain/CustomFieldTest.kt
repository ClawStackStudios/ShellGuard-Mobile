package com.clawstack.shellguard.domain

import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.domain.models.CustomField
import com.clawstack.shellguard.domain.models.CustomFieldSerializer
import com.clawstack.shellguard.domain.models.CustomFieldType
import com.clawstack.shellguard.domain.models.PasswordHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomFieldTest {

    private val testHuKey = "hu-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private val testUserUuid = "usr_11223344-5566-7788-99aa-bbccddeeff00"

    @Test
    fun testCustomFieldSerializationAndDeserialization() {
        val fields = listOf(
            CustomField(id = "cf-1", label = "Server IP", value = "192.168.1.100", type = CustomFieldType.TEXT),
            CustomField(id = "cf-2", label = "Master PIN", value = "9876", type = CustomFieldType.HIDDEN),
            CustomField(id = "cf-3", label = "MFA Required", value = "true", type = CustomFieldType.BOOLEAN),
            CustomField(id = "cf-4", label = "Linked User", value = "username", type = CustomFieldType.LINKED)
        )

        val json = CustomFieldSerializer.serializeFields(fields)
        assertNotNull(json)
        assertTrue(json.contains("Server IP"))
        assertTrue(json.contains("Master PIN"))

        val decoded = CustomFieldSerializer.deserializeFields(json)
        assertEquals(4, decoded.size)
        assertEquals("Server IP", decoded[0].label)
        assertEquals(CustomFieldType.TEXT, decoded[0].type)
        assertEquals("Master PIN", decoded[1].label)
        assertEquals(CustomFieldType.HIDDEN, decoded[1].type)
        assertEquals("true", decoded[2].value)
        assertEquals(CustomFieldType.BOOLEAN, decoded[2].type)
        assertEquals("Linked User", decoded[3].label)
        assertEquals(CustomFieldType.LINKED, decoded[3].type)
    }

    @Test
    fun testCustomFieldDeserializationGracefulFallback() {
        assertEquals(emptyList<CustomField>(), CustomFieldSerializer.deserializeFields(null))
        assertEquals(emptyList<CustomField>(), CustomFieldSerializer.deserializeFields(""))
        assertEquals(emptyList<CustomField>(), CustomFieldSerializer.deserializeFields("not valid json"))
        assertEquals(emptyList<CustomField>(), CustomFieldSerializer.deserializeFields("{}"))
    }

    @Test
    fun testPasswordHistorySerializationAndDeserialization() {
        val history = listOf(
            PasswordHistoryEntry(password = "OldPass1!", timestamp = 1711000000000L),
            PasswordHistoryEntry(password = "OlderPass2#", timestamp = 1710000000000L)
        )

        val json = CustomFieldSerializer.serializeHistory(history)
        assertNotNull(json)
        assertTrue(json.contains("OldPass1!"))

        val decoded = CustomFieldSerializer.deserializeHistory(json)
        assertEquals(2, decoded.size)
        assertEquals("OldPass1!", decoded[0].password)
        assertEquals(1711000000000L, decoded[0].timestamp)
    }

    @Test
    fun testPasswordHistoryGracefulFallback() {
        assertEquals(emptyList<PasswordHistoryEntry>(), CustomFieldSerializer.deserializeHistory(null))
        assertEquals(emptyList<PasswordHistoryEntry>(), CustomFieldSerializer.deserializeHistory(""))
        assertEquals(emptyList<PasswordHistoryEntry>(), CustomFieldSerializer.deserializeHistory("invalid"))
    }

    @Test
    fun testCustomFieldsShellCryptionRoundTrip() {
        val shellKey = ShellCryptionEngine.deriveShellKey(testHuKey, testUserUuid)
        val itemId = "pearl-custom-test-123"
        val aad = ShellCryptionEngine.AadNamespace.pearlCustomFields(itemId)

        val originalFields = listOf(
            CustomField(id = "1", label = "Recovery Key", value = "XYZ-123-ABC", type = CustomFieldType.HIDDEN),
            CustomField(id = "2", label = "Port", value = "8080", type = CustomFieldType.TEXT)
        )

        val plaintextJson = CustomFieldSerializer.serializeFields(originalFields)
        val envelopeJson = ShellCryptionEngine.encryptField(plaintextJson, shellKey, aad)

        val decryptedJson = ShellCryptionEngine.decryptField(envelopeJson, shellKey, aad)
        val restoredFields = CustomFieldSerializer.deserializeFields(decryptedJson)

        assertEquals(originalFields.size, restoredFields.size)
        assertEquals(originalFields[0].label, restoredFields[0].label)
        assertEquals(originalFields[0].value, restoredFields[0].value)
        assertEquals(originalFields[0].type, restoredFields[0].type)
        assertEquals(originalFields[1].label, restoredFields[1].label)
        assertEquals(originalFields[1].value, restoredFields[1].value)
    }
}
