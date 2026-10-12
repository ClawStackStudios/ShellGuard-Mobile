package com.clawstack.shellguard.domain.models

data class PearlDetail(
    val id: String,
    val ownerUuid: String,
    val title: String,
    val secret: String, // Plaintext password in memory
    val username: String = "",
    val url: String = "",
    val type: String = "password",
    val category: String = "",
    val notes: String = "",
    val totpSecret: String = "", // Plaintext TOTP secret
    val customFields: List<CustomField> = emptyList(),
    val uris: List<String> = emptyList(),
    val attachments: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val passwordHistory: List<PasswordHistoryEntry> = emptyList(),
    val reprompt: Boolean = false,
    val syncState: String = "SYNCED",
    val createdAt: String = "",
    val localUpdatedAt: Long = System.currentTimeMillis(),
    val remoteUpdatedAt: Long = 0L
)

data class SecureNoteDetail(
    val id: String,
    val ownerUuid: String,
    val title: String,
    val content: String, // Plaintext note content
    val category: String = "",
    val customFields: List<CustomField> = emptyList(),
    val attachments: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val reprompt: Boolean = false,
    val syncState: String = "SYNCED",
    val createdAt: String = "",
    val localUpdatedAt: Long = System.currentTimeMillis(),
    val remoteUpdatedAt: Long = 0L
)

data class SshKeyDetail(
    val id: String,
    val ownerUuid: String,
    val title: String,
    val keyValue: String, // Plaintext private key
    val username: String = "",
    val category: String = "",
    val customFields: List<CustomField> = emptyList(),
    val tags: List<String> = emptyList(),
    val reprompt: Boolean = false,
    val syncState: String = "SYNCED",
    val createdAt: String = "",
    val localUpdatedAt: Long = System.currentTimeMillis(),
    val remoteUpdatedAt: Long = 0L
)
