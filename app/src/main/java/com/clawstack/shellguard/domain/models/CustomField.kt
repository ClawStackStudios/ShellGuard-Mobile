package com.clawstack.shellguard.domain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class CustomFieldType {
    TEXT,
    HIDDEN,
    BOOLEAN,
    LINKED
}

@Serializable
data class CustomField(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val value: String,
    val type: CustomFieldType = CustomFieldType.TEXT
)

@Serializable
data class PasswordHistoryEntry(
    val password: String,
    val timestamp: Long = System.currentTimeMillis()
)

object CustomFieldSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun serializeFields(fields: List<CustomField>): String {
        return json.encodeToString(fields)
    }

    fun deserializeFields(jsonString: String?): List<CustomField> {
        if (jsonString.isNullOrBlank() || jsonString == "[]") return emptyList()
        return try {
            json.decodeFromString<List<CustomField>>(jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeHistory(history: List<PasswordHistoryEntry>): String {
        return json.encodeToString(history)
    }

    fun deserializeHistory(jsonString: String?): List<PasswordHistoryEntry> {
        if (jsonString.isNullOrBlank() || jsonString == "[]") return emptyList()
        return try {
            json.decodeFromString<List<PasswordHistoryEntry>>(jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeTags(tags: List<String>): String {
        return json.encodeToString(tags)
    }

    fun deserializeTags(jsonString: String?): List<String> {
        if (jsonString.isNullOrBlank() || jsonString == "[]") return emptyList()
        return try {
            json.decodeFromString<List<String>>(jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeUris(uris: List<String>): String {
        return json.encodeToString(uris)
    }

    fun deserializeUris(jsonString: String?): List<String> {
        if (jsonString.isNullOrBlank() || jsonString == "[]") return emptyList()
        return try { json.decodeFromString<List<String>>(jsonString) } catch (_: Exception) { emptyList() }
    }

    fun serializeAttachments(attachments: List<String>): String {
        return json.encodeToString(attachments)
    }

    fun deserializeAttachments(jsonString: String?): List<String> {
        if (jsonString.isNullOrBlank() || jsonString == "[]") return emptyList()
        return try { json.decodeFromString<List<String>>(jsonString) } catch (_: Exception) { emptyList() }
    }
}
