package com.clawstack.shellguard.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Hybrid File-System Vault: Stores metadata in Room; encrypted file payload
 * streams directly to internal disk (context.filesDir/vault_attachments/{id}.enc),
 * completely eliminating SQLite 2MB CursorWindow crashes (CWE-400).
 */
@Entity(tableName = "vault_secure_attachments")
data class SecureAttachmentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,

    @ColumnInfo(name = "owner_uuid") val ownerUuid: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "mime_type") val mimeType: String = "application/octet-stream",
    @ColumnInfo(name = "local_file_path") val localFilePath: String = "",
    @ColumnInfo(name = "category") val category: String = "",
    @ColumnInfo(name = "sync_state") val syncState: String = "SYNCED",
    @ColumnInfo(name = "created_at") val createdAt: String
)
