package com.clawstack.shellguard.data.repository

import android.util.Log
import com.clawstack.shellguard.crypto.EncryptedDeviceVault
import com.clawstack.shellguard.data.local.ShellGuardDatabase
import com.clawstack.shellguard.data.local.entities.SecureNoteEntity
import com.clawstack.shellguard.data.local.entities.SshKeyEntity
import com.clawstack.shellguard.data.local.entities.SyncMetadataEntity
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
import com.clawstack.shellguard.data.remote.ShellGuardClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SyncStatus {
    ONLINE_SYNCED,
    OFFLINE_READ_ONLY,
    SYNCING,
    SYNC_ERROR
}

enum class VaultItemDomain {
    PASSWORD,
    NOTE,
    SSH_KEY
}

data class UnifiedVaultItem(
    val id: String,
    val ownerUuid: String,
    val title: String,
    val subtitle: String,
    val domain: VaultItemDomain,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val reprompt: Boolean = false,
    val localUpdatedAt: Long = 0L,
    val remoteUpdatedAt: Long = 0L
)

class SyncRepository(
    private val database: ShellGuardDatabase,
    private val clientProvider: (baseUrl: String) -> ShellGuardClient,
    private val deviceVault: EncryptedDeviceVault,
    private val connectivityMonitor: ConnectivityMonitor,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _syncStatus = MutableStateFlow(
        if (connectivityMonitor.isOnline.value) SyncStatus.ONLINE_SYNCED else SyncStatus.OFFLINE_READ_ONLY
    )
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        // Automatically probe and pull when connectivity returns
        coroutineScope.launch {
            connectivityMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    val ownerUuid = deviceVault.getOwnerUuid()
                    if (!ownerUuid.isNullOrBlank() && deviceVault.hasActiveSession()) {
                        syncAll(ownerUuid)
                    }
                } else {
                    _syncStatus.value = SyncStatus.OFFLINE_READ_ONLY
                }
            }
        }
    }

    /**
     * Unified reactive stream of all vault items across domains
     */
    fun observeUnifiedItems(ownerUuid: String): Flow<List<UnifiedVaultItem>> {
        return combine(
            database.vaultPearlDao().observeAll(ownerUuid),
            database.secureNoteDao().observeAll(ownerUuid),
            database.sshKeyDao().observeAll(ownerUuid)
        ) { pearls, notes, sshKeys ->
            val unifiedList = mutableListOf<UnifiedVaultItem>()

            pearls.forEach { pearl ->
                unifiedList.add(
                    UnifiedVaultItem(
                        id = pearl.id,
                        ownerUuid = pearl.ownerUuid,
                        title = pearl.title,
                        subtitle = pearl.username ?: pearl.url ?: "Login",
                        domain = VaultItemDomain.PASSWORD,
                        category = pearl.category,
                        reprompt = pearl.reprompt,
                        localUpdatedAt = pearl.localUpdatedAt,
                        remoteUpdatedAt = pearl.remoteUpdatedAt
                    )
                )
            }

            notes.forEach { note ->
                unifiedList.add(
                    UnifiedVaultItem(
                        id = note.id,
                        ownerUuid = note.ownerUuid,
                        title = note.title,
                        subtitle = note.category ?: "Secure Note",
                        domain = VaultItemDomain.NOTE,
                        category = note.category,
                        reprompt = note.reprompt,
                        localUpdatedAt = note.localUpdatedAt,
                        remoteUpdatedAt = note.remoteUpdatedAt
                    )
                )
            }

            sshKeys.forEach { key ->
                unifiedList.add(
                    UnifiedVaultItem(
                        id = key.id,
                        ownerUuid = key.ownerUuid,
                        title = key.title,
                        subtitle = key.username ?: "SSH Key",
                        domain = VaultItemDomain.SSH_KEY,
                        category = key.category,
                        reprompt = key.reprompt,
                        localUpdatedAt = key.localUpdatedAt,
                        remoteUpdatedAt = key.remoteUpdatedAt
                    )
                )
            }

            unifiedList.sortedByDescending { it.localUpdatedAt.coerceAtLeast(it.remoteUpdatedAt) }
        }
    }

    /**
     * Execute full bidirectional sync (Upstream push + Downstream delta pull)
     */
    suspend fun syncAll(ownerUuid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val serverUrl = deviceVault.getServerUrl()
        val sessionToken = deviceVault.getSessionToken()

        if (serverUrl.isNullOrBlank() || sessionToken.isNullOrBlank()) {
            _syncStatus.value = SyncStatus.OFFLINE_READ_ONLY
            return@withContext Result.failure(IllegalStateException("No active server URL or session token"))
        }

        if (!connectivityMonitor.isOnline.value) {
            _syncStatus.value = SyncStatus.OFFLINE_READ_ONLY
            return@withContext Result.success(Unit)
        }

        _syncStatus.value = SyncStatus.SYNCING

        try {
            val client = clientProvider(serverUrl)

            // Health probe
            val healthCheck = client.getHealth()
            if (healthCheck.isFailure || healthCheck.getOrNull() != true) {
                Log.w("SyncRepository", "Server health check failed. Entering OfflineReadOnly.")
                _syncStatus.value = SyncStatus.OFFLINE_READ_ONLY
                return@withContext Result.success(Unit)
            }

            // Downstream delta pull
            val pearlsResult = client.fetchVault(sessionToken)
            if (pearlsResult.isSuccess) {
                val remotePearls = pearlsResult.getOrThrow()
                val remoteIds = remotePearls.map { it.id }

                val entities = remotePearls.map { dto ->
                    VaultPearlEntity(
                        id = dto.id,
                        ownerUuid = dto.owner_uuid,
                        title = dto.title,
                        secret = dto.secret,
                        username = dto.username ?: "",
                        url = dto.url ?: "",
                        type = dto.type,
                        category = dto.category ?: "",
                        notes = dto.notes ?: "",
                        totpSecret = dto.totp_secret ?: "",
                        attachments = dto.attachments,
                        customFields = dto.custom_fields ?: "",
                        tags = dto.tags,
                        uris = dto.uris,
                        passwordHistory = dto.password_history ?: "[]",
                        reprompt = dto.reprompt,
                        syncState = "SYNCED",
                        createdAt = dto.created_at.ifBlank { System.currentTimeMillis().toString() },
                        localUpdatedAt = System.currentTimeMillis(),
                        remoteUpdatedAt = System.currentTimeMillis()
                    )
                }
                database.vaultPearlDao().upsertAll(entities)
                if (remoteIds.isNotEmpty()) {
                    database.vaultPearlDao().pruneDeletedRemoteItems(ownerUuid, remoteIds)
                }
            }

            // Downstream notes pull
            val notesResult = client.fetchNotes(sessionToken)
            if (notesResult.isSuccess) {
                val remoteNotes = notesResult.getOrThrow()
                val remoteNoteIds = remoteNotes.map { it.id }

                val noteEntities = remoteNotes.map { dto ->
                    SecureNoteEntity(
                        id = dto.id,
                        ownerUuid = dto.owner_uuid,
                        title = dto.title,
                        content = dto.content,
                        category = dto.category ?: "",
                        attachments = dto.attachments,
                        customFields = dto.custom_fields ?: "",
                        tags = dto.tags,
                        reprompt = dto.reprompt,
                        syncState = "SYNCED",
                        createdAt = dto.created_at.ifBlank { System.currentTimeMillis().toString() },
                        localUpdatedAt = System.currentTimeMillis(),
                        remoteUpdatedAt = System.currentTimeMillis()
                    )
                }
                database.secureNoteDao().upsertAll(noteEntities)
                if (remoteNoteIds.isNotEmpty()) {
                    database.secureNoteDao().pruneDeletedRemoteItems(ownerUuid, remoteNoteIds)
                }
            }

            // Downstream keys pull
            val keysResult = client.fetchKeys(sessionToken)
            if (keysResult.isSuccess) {
                val remoteKeys = keysResult.getOrThrow()
                val remoteKeyIds = remoteKeys.map { it.id }

                val keyEntities = remoteKeys.map { dto ->
                    SshKeyEntity(
                        id = dto.id,
                        ownerUuid = dto.owner_uuid,
                        title = dto.title,
                        keyValue = dto.key_value,
                        username = dto.username ?: "",
                        category = dto.category ?: "",
                        customFields = dto.custom_fields ?: "",
                        tags = dto.tags,
                        reprompt = dto.reprompt,
                        syncState = "SYNCED",
                        createdAt = dto.created_at.ifBlank { System.currentTimeMillis().toString() },
                        localUpdatedAt = System.currentTimeMillis(),
                        remoteUpdatedAt = System.currentTimeMillis()
                    )
                }
                database.sshKeyDao().upsertAll(keyEntities)
                if (remoteKeyIds.isNotEmpty()) {
                    database.sshKeyDao().pruneDeletedRemoteItems(ownerUuid, remoteKeyIds)
                }
            }

            // Update SyncMetadata
            database.syncMetadataDao().upsert(
                SyncMetadataEntity(
                    ownerUuid = ownerUuid,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    syncStatus = "OnlineSynced"
                )
            )

            _syncStatus.value = SyncStatus.ONLINE_SYNCED
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e("SyncRepository", "Sync failed: ${e.message}", e)
            _syncStatus.value = SyncStatus.SYNC_ERROR
            Result.failure(e)
        }
    }
}
