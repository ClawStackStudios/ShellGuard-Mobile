package com.clawstack.shellguard.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.clawstack.shellguard.data.local.entities.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE owner_uuid = :ownerUuid LIMIT 1")
    fun observeMetadata(ownerUuid: String): Flow<SyncMetadataEntity?>

    @Query("SELECT * FROM sync_metadata WHERE owner_uuid = :ownerUuid LIMIT 1")
    suspend fun getMetadata(ownerUuid: String): SyncMetadataEntity?

    @Upsert
    suspend fun upsert(metadata: SyncMetadataEntity)

    @Query("UPDATE sync_metadata SET last_sync_timestamp = :timestamp, sync_status = 'IDLE', last_error = null WHERE owner_uuid = :ownerUuid")
    suspend fun recordSuccessfulSync(ownerUuid: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE sync_metadata SET sync_status = 'FAILED', last_error = :errorMessage WHERE owner_uuid = :ownerUuid")
    suspend fun recordFailedSync(ownerUuid: String, errorMessage: String)

    @Query("DELETE FROM sync_metadata WHERE owner_uuid = :ownerUuid")
    suspend fun clear(ownerUuid: String)
}
