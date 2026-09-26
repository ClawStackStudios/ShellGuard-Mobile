package com.clawstack.shellguard.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.clawstack.shellguard.data.local.entities.AgentKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentKeyDao {

    @Query("SELECT * FROM agent_keys WHERE owner_uuid = :ownerUuid AND is_revoked = 0 ORDER BY created_at DESC")
    fun observeActiveKeys(ownerUuid: String): Flow<List<AgentKeyEntity>>

    @Query("SELECT * FROM agent_keys WHERE owner_uuid = :ownerUuid AND id = :id LIMIT 1")
    suspend fun getById(ownerUuid: String, id: String): AgentKeyEntity?

    @Upsert
    suspend fun upsert(key: AgentKeyEntity)

    @Upsert
    suspend fun upsertAll(keys: List<AgentKeyEntity>)

    @Query("UPDATE agent_keys SET is_revoked = 1 WHERE id = :id AND owner_uuid = :ownerUuid")
    suspend fun revokeKey(ownerUuid: String, id: String)

    @Query("DELETE FROM agent_keys WHERE owner_uuid = :ownerUuid")
    suspend fun clear(ownerUuid: String)
}
