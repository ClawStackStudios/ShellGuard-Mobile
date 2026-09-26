package com.clawstack.shellguard.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "owner_uuid") val ownerUuid: String,

    @ColumnInfo(name = "last_sync_timestamp") val lastSyncTimestamp: Long = 0L,
    @ColumnInfo(name = "server_delta_cursor") val serverDeltaCursor: String = "",
    @ColumnInfo(name = "sync_status") val syncStatus: String = "IDLE", // IDLE, SYNCING, FAILED
    @ColumnInfo(name = "last_error") val lastError: String? = null
)
