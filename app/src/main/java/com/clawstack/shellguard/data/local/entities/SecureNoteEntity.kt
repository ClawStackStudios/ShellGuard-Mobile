package com.clawstack.shellguard.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_secure_notes")
data class SecureNoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,

    @ColumnInfo(name = "owner_uuid") val ownerUuid: String,
    @ColumnInfo(name = "title") val title: String,

    // Opaque ShellCryption Envelope String
    @ColumnInfo(name = "content") val content: String,

    @ColumnInfo(name = "category") val category: String = "",
    @ColumnInfo(name = "attachments") val attachments: String = "[]",
    @ColumnInfo(name = "custom_fields") val customFields: String = "",
    @ColumnInfo(name = "tags") val tags: String = "[]",

    // Master Key Re-Prompt
    @ColumnInfo(name = "reprompt") val reprompt: Boolean = false,

    @ColumnInfo(name = "sync_state") val syncState: String = "SYNCED",
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "local_updated_at") val localUpdatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "remote_updated_at") val remoteUpdatedAt: Long = 0L
)
