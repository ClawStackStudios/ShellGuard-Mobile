package com.clawstack.shellguard.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_ssh_keys")
data class SshKeyEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,

    @ColumnInfo(name = "owner_uuid") val ownerUuid: String,
    @ColumnInfo(name = "title") val title: String,

    // Opaque ShellCryption Envelope String
    @ColumnInfo(name = "key_value") val keyValue: String,

    @ColumnInfo(name = "username") val username: String = "",
    @ColumnInfo(name = "category") val category: String = "",
    @ColumnInfo(name = "custom_fields") val customFields: String = "",
    @ColumnInfo(name = "tags") val tags: String = "[]",

    // Master Key Re-Prompt
    @ColumnInfo(name = "reprompt") val reprompt: Boolean = false,

    @ColumnInfo(name = "sync_state") val syncState: String = "SYNCED",
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "local_updated_at") val localUpdatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "remote_updated_at") val remoteUpdatedAt: Long = 0L
)
