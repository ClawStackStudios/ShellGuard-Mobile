package com.clawstack.shellguard.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class ShellResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val code: String? = null,
    val details: List<ValidationIssue>? = null,
    val suggestion: String? = null
)

@Serializable
data class ValidationIssue(
    val path: String? = null,
    val message: String
)

@Serializable
data class TokenRequest(
    val type: String = "human",
    val keyHash: String,
    val uuid: String? = null
)

@Serializable
data class UserProfileDto(
    val uuid: String,
    val username: String,
    val displayName: String? = null,
    val email: String? = null
)

@Serializable
data class SessionData(
    val token: String,
    val type: String = "human",
    val createdAt: String = "",
    val expiresAt: String = "",
    val user: UserProfileDto
)

@Serializable
data class TokenResponse(
    val success: Boolean,
    val data: SessionData? = null,
    val error: String? = null
)

@Serializable
data class PearlDto(
    val id: String,
    val owner_uuid: String,
    val title: String,
    val username: String? = null,
    val url: String? = null,
    val category: String? = null,
    val notes: String? = null,
    val secret: String = "",
    val totp_secret: String? = null,
    val attachments: String = "[]",
    val custom_fields: String? = null,
    val tags: String = "[]",
    val uris: String = "[]",
    val password_history: String? = null,
    val type: String = "password",
    val reprompt: Boolean = false,
    val created_at: String = "",
    val updated_at: String? = null
)

@Serializable
data class CreateVaultItemRequest(
    val title: String,
    val username: String? = null,
    val url: String? = null,
    val category: String? = null,
    val notes: String? = null,
    val secret: String = "",
    val totp_secret: String? = null,
    val type: String = "password",
    val custom_fields: String? = null,
    val tags: String? = null,
    val uris: String? = null,
    val reprompt: Boolean = false
)

@Serializable
data class VaultItemResponse(
    val success: Boolean,
    val data: PearlDto? = null,
    val error: String? = null
)

@Serializable
data class VaultResponse(
    val success: Boolean,
    val data: List<PearlDto> = emptyList(),
    val error: String? = null
)

@Serializable
data class SecureNoteDto(
    val id: String,
    val owner_uuid: String,
    val title: String,
    val content: String = "",
    val category: String? = null,
    val attachments: String = "[]",
    val custom_fields: String? = null,
    val tags: String = "[]",
    val reprompt: Boolean = false,
    val created_at: String = "",
    val updated_at: String? = null
)

@Serializable
data class CreateNoteRequest(
    val title: String,
    val content: String = "",
    val category: String? = null,
    val custom_fields: String? = null,
    val tags: String? = null,
    val reprompt: Boolean = false
)

@Serializable
data class NotesResponse(
    val success: Boolean,
    val data: List<SecureNoteDto> = emptyList(),
    val error: String? = null
)

@Serializable
data class NoteItemResponse(
    val success: Boolean,
    val data: SecureNoteDto? = null,
    val error: String? = null
)

@Serializable
data class SshKeyDto(
    val id: String,
    val owner_uuid: String,
    val title: String,
    val key_value: String = "",
    val username: String? = null,
    val category: String? = null,
    val custom_fields: String? = null,
    val tags: String = "[]",
    val reprompt: Boolean = false,
    val created_at: String = "",
    val updated_at: String? = null
)

@Serializable
data class CreateSshKeyRequest(
    val title: String,
    val key_value: String = "",
    val username: String? = null,
    val category: String? = null,
    val custom_fields: String? = null,
    val tags: String? = null,
    val reprompt: Boolean = false
)

@Serializable
data class KeysResponse(
    val success: Boolean,
    val data: List<SshKeyDto> = emptyList(),
    val error: String? = null
)

@Serializable
data class KeyItemResponse(
    val success: Boolean,
    val data: SshKeyDto? = null,
    val error: String? = null
)

