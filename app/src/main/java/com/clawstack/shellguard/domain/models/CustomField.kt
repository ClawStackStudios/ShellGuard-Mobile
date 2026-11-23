package com.clawstack.shellguard.domain.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

@Serializable
enum class CustomFieldType {
    TEXT,
    HIDDEN,
    BOOLEAN,
    LINKED;

    fun toWebWireType(): String = when (this) {
        TEXT -> "text"
        HIDDEN -> "hidden"
        BOOLEAN -> "checkbox"
        LINKED -> "linked"
    }

    companion object {
        fun fromWire(raw: String?): CustomFieldType = when (raw?.trim()?.lowercase()) {
            "hidden", "1" -> HIDDEN
            "boolean", "checkbox", "2" -> BOOLEAN
            "linked", "3" -> LINKED
            else -> TEXT
        }
    }
}

@Serializable(with = CustomFieldWireSerializer::class)
data class CustomField(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val value: String,
    val type: CustomFieldType = CustomFieldType.TEXT
)

object CustomFieldWireSerializer : KSerializer<CustomField> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CustomField") {
        element<String>("id")
        element<String>("name")
        element<String>("value")
        element<String>("type")
    }

    override fun serialize(encoder: Encoder, value: CustomField) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            val obj = buildJsonObject {
                put("id", JsonPrimitive(value.id))
                put("name", JsonPrimitive(value.label))
                put("value", JsonPrimitive(value.value))
                put("type", JsonPrimitive(value.type.toWebWireType()))
            }
            jsonEncoder.encodeJsonElement(obj)
        } else {
            encoder.encodeString("${value.label}:${value.value}")
        }
    }

    override fun deserialize(decoder: Decoder): CustomField {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalStateException("CustomFieldWireSerializer requires JsonDecoder")
        val element = jsonDecoder.decodeJsonElement()
        val obj = element as? JsonObject
            ?: throw IllegalArgumentException("Expected JsonObject for CustomField")

        val id = obj["id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        val label = obj["name"]?.jsonPrimitive?.contentOrNull
            ?: obj["label"]?.jsonPrimitive?.contentOrNull
            ?: ""
        val value = obj["value"]?.jsonPrimitive?.contentOrNull ?: ""
        val rawType = obj["type"]?.jsonPrimitive?.contentOrNull
        return CustomField(
            id = id,
            label = label,
            value = value,
            type = CustomFieldType.fromWire(rawType)
        )
    }
}

@Serializable
data class PasswordHistoryEntry(
    val password: String,
    val timestamp: Long = System.currentTimeMillis()
)

object CustomFieldSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
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
