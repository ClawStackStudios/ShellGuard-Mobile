package com.clawstack.shellguard.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_pearls")
data class VaultPearlEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,

    @ColumnInfo(name = "owner_uuid") val ownerUuid: String,
    @ColumnInfo(name = "title") val title: String,

    // Opaque ShellCryption Envelope String
    @ColumnInfo(name = "secret") val secret: String,

    @ColumnInfo(name = "username") val username: String = "",
    @ColumnInfo(name = "url") val url: String = "",
    @ColumnInfo(name = "type") val type: String = "password",
    @ColumnInfo(name = "category") val category: String = "",
    @ColumnInfo(name = "notes") val notes: String = "",

    // Opaque ShellCryption Envelope String
    @ColumnInfo(name = "totp_secret") val totpSecret: String = "",

    @ColumnInfo(name = "attachments") val attachments: String = "[]",

    // Opaque ShellCryption Envelope String
    @ColumnInfo(name = "custom_fields") val customFields: String = "",

    @ColumnInfo(name = "tags") val tags: String = "[]",

    // JSON array of URI objects: [{"uri": "https://...", "match": "HOST"}]
    @ColumnInfo(name = "uris") val uris: String = "[]",

    // Opaque ShellCryption Envelope String
    @ColumnInfo(name = "password_history") val passwordHistory: String = "[]",

    // Master Key Re-Prompt (Claw Re-Prompt): Requires biometric/PIN confirmation before reveal/copy
    @ColumnInfo(name = "reprompt") val reprompt: Boolean = false,

    @ColumnInfo(name = "sync_state") val syncState: String = "SYNCED", // PENDING_SYNC, SYNCED, PENDING_DELETE
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "local_updated_at") val localUpdatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "remote_updated_at") val remoteUpdatedAt: Long = 0L
)
