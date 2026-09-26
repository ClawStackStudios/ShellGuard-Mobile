package com.clawstack.shellguard.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.clawstack.shellguard.data.local.entities.SecureAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureAttachmentDao {

    @Query("SELECT * FROM vault_secure_attachments WHERE owner_uuid = :ownerUuid AND sync_state != 'PENDING_DELETE' ORDER BY created_at DESC")
    fun observeAll(ownerUuid: String): Flow<List<SecureAttachmentEntity>>

    @Query("SELECT * FROM vault_secure_attachments WHERE owner_uuid = :ownerUuid AND id = :id LIMIT 1")
    suspend fun getById(ownerUuid: String, id: String): SecureAttachmentEntity?

    @Query("SELECT * FROM vault_secure_attachments WHERE owner_uuid = :ownerUuid AND sync_state = 'PENDING_SYNC'")
    suspend fun getPendingSyncItems(ownerUuid: String): List<SecureAttachmentEntity>

    @Upsert
    suspend fun upsert(item: SecureAttachmentEntity)

    @Upsert
    suspend fun upsertAll(items: List<SecureAttachmentEntity>)

    @Query("DELETE FROM vault_secure_attachments WHERE id = :id AND owner_uuid = :ownerUuid")
    suspend fun deleteById(ownerUuid: String, id: String)

    @Query("DELETE FROM vault_secure_attachments WHERE owner_uuid = :ownerUuid")
    suspend fun clearVault(ownerUuid: String)

    @Query("SELECT COUNT(*) FROM vault_secure_attachments WHERE owner_uuid = :ownerUuid AND sync_state != 'PENDING_DELETE'")
    fun observeItemCount(ownerUuid: String): Flow<Int>
}
