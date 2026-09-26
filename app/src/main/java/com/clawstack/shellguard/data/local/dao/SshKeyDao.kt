package com.clawstack.shellguard.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.clawstack.shellguard.data.local.entities.SshKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SshKeyDao {

    @Query("SELECT * FROM vault_ssh_keys WHERE owner_uuid = :ownerUuid AND sync_state != 'PENDING_DELETE' ORDER BY local_updated_at DESC")
    fun observeAll(ownerUuid: String): Flow<List<SshKeyEntity>>

    @Query("SELECT * FROM vault_ssh_keys WHERE owner_uuid = :ownerUuid AND id = :id LIMIT 1")
    suspend fun getById(ownerUuid: String, id: String): SshKeyEntity?

    @Query("SELECT * FROM vault_ssh_keys WHERE owner_uuid = :ownerUuid AND sync_state != 'PENDING_DELETE' AND (title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%')")
    fun search(ownerUuid: String, query: String): Flow<List<SshKeyEntity>>

    @Query("SELECT * FROM vault_ssh_keys WHERE owner_uuid = :ownerUuid AND sync_state = 'PENDING_SYNC'")
    suspend fun getPendingSyncItems(ownerUuid: String): List<SshKeyEntity>

    @Upsert
    suspend fun upsert(item: SshKeyEntity)

    @Upsert
    suspend fun upsertAll(items: List<SshKeyEntity>)

    @Query("DELETE FROM vault_ssh_keys WHERE id = :id AND owner_uuid = :ownerUuid")
    suspend fun deleteById(ownerUuid: String, id: String)

    @Query("UPDATE vault_ssh_keys SET sync_state = 'PENDING_DELETE', local_updated_at = :timestamp WHERE id = :id AND owner_uuid = :ownerUuid")
    suspend fun markForDeletion(ownerUuid: String, id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM vault_ssh_keys WHERE owner_uuid = :ownerUuid")
    suspend fun clearVault(ownerUuid: String)

    @Query("SELECT COUNT(*) FROM vault_ssh_keys WHERE owner_uuid = :ownerUuid AND sync_state != 'PENDING_DELETE'")
    fun observeItemCount(ownerUuid: String): Flow<Int>

    @Query("DELETE FROM vault_ssh_keys WHERE owner_uuid = :ownerUuid AND id NOT IN (:activeRemoteIds)")
    suspend fun pruneDeletedRemoteItems(ownerUuid: String, activeRemoteIds: List<String>)
}
