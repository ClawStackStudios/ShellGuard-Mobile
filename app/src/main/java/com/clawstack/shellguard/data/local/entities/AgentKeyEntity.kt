package com.clawstack.shellguard.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_keys")
data class AgentKeyEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,

    @ColumnInfo(name = "owner_uuid") val ownerUuid: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "permissions_json") val permissionsJson: String = "{}",
    @ColumnInfo(name = "rate_limit") val rateLimit: Int = 100,
    @ColumnInfo(name = "expires_at") val expiresAt: Long = 0L,
    @ColumnInfo(name = "is_revoked") val isRevoked: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String
)
