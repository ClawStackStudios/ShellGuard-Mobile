package com.clawstack.shellguard.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,

    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "item_id") val itemId: String? = null,
    @ColumnInfo(name = "actor_type") val actorType: String = "HUMAN",
    @ColumnInfo(name = "details") val details: String = "",
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false
)
