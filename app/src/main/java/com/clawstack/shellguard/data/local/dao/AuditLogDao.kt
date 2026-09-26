package com.clawstack.shellguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.clawstack.shellguard.data.local.entities.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insert(entry: AuditLogEntity)

    @Insert
    suspend fun insertAll(entries: List<AuditLogEntity>)

    @Query("SELECT * FROM audit_logs WHERE is_synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedEntries(): List<AuditLogEntity>

    @Query("UPDATE audit_logs SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAll()
}
