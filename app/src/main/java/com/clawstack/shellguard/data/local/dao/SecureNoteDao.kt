package com.clawstack.shellguard.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.clawstack.shellguard.data.local.entities.SecureNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureNoteDao {

    @Query("SELECT * FROM vault_secure_notes WHERE owner_uuid = :ownerUuid AND sync_state != 'PENDING_DELETE' ORDER BY local_updated_at DESC")
    fun observeAll(ownerUuid: String): Flow<List<SecureNoteEntity>>

    @Query("SELECT * FROM vault_secure_notes WHERE owner_uuid = :ownerUuid AND id = :id LIMIT 1")
    suspend fun getById(ownerUuid: String, id: String): SecureNoteEntity?

    @Query("SELECT * FROM vault_secure_notes WHERE owner_uuid = :ownerUuid AND sync_state != 'PENDING_DELETE' AND title LIKE '%' || :query || '%'")
    fun search(ownerUuid: String, query: String): Flow<List<SecureNoteEntity>>

    @Query("SELECT * FROM vault_secure_notes WHERE owner_uuid = :ownerUuid AND sync_state != 'PENDING_DELETE'")
    suspend fun getAllActiveNotes(ownerUuid: String): List<SecureNoteEntity>

    @Query("SELECT * FROM vault_secure_notes WHERE owner_uuid = :ownerUuid AND sync_state = 'PENDING_SYNC'")
    suspend fun getPendingSyncItems(ownerUuid: String): List<SecureNoteEntity>

    @Query("SELECT * FROM vault_secure_notes WHERE owner_uuid = :ownerUuid AND sync_state = 'PENDING_DELETE'")
    suspend fun getPendingDeleteItems(ownerUuid: String): List<SecureNoteEntity>

    @Upsert
    suspend fun upsert(item: SecureNoteEntity)

    @Upsert
    suspend fun upsertAll(items: List<SecureNoteEntity>)

    @Query("DELETE FROM vault_secure_notes WHERE id = :id AND owner_uuid = :ownerUuid")
    suspend fun deleteById(ownerUuid: String, id: String)

    @Query("UPDATE vault_secure_notes SET sync_state = 'PENDING_DELETE', local_updated_at = :timestamp WHERE id = :id AND owner_uuid = :ownerUuid")
    suspend fun markForDeletion(ownerUuid: String, id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM vault_secure_notes WHERE owner_uuid = :ownerUuid")
    suspend fun clearVault(ownerUuid: String)

    @Query("SELECT COUNT(*) FROM vault_secure_notes WHERE owner_uuid = :ownerUuid AND sync_state != 'PENDING_DELETE'")
    fun observeItemCount(ownerUuid: String): Flow<Int>

    @Query("SELECT id FROM vault_secure_notes WHERE owner_uuid = :ownerUuid AND sync_state = 'SYNCED'")
    suspend fun getSyncedItemIds(ownerUuid: String): List<String>

    @Query("DELETE FROM vault_secure_notes WHERE owner_uuid = :ownerUuid AND id IN (:ids)")
    suspend fun deleteBatch(ownerUuid: String, ids: List<String>)
}
