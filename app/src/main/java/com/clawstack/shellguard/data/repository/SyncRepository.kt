package com.clawstack.shellguard.data.repository

import android.util.Log
import com.clawstack.shellguard.crypto.EncryptedDeviceVault
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.data.local.ShellGuardDatabase
import com.clawstack.shellguard.data.local.entities.SecureNoteEntity
import com.clawstack.shellguard.data.local.entities.SshKeyEntity
import com.clawstack.shellguard.data.local.entities.SyncMetadataEntity
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
import com.clawstack.shellguard.data.remote.ShellGuardClient
import com.clawstack.shellguard.data.remote.models.CreateNoteRequest
import com.clawstack.shellguard.data.remote.models.CreateSshKeyRequest
import com.clawstack.shellguard.data.remote.models.CreateVaultItemRequest
import com.clawstack.shellguard.domain.models.CustomFieldSerializer
import com.clawstack.shellguard.domain.models.PasswordHistoryEntry
import com.clawstack.shellguard.domain.models.PearlDetail
import com.clawstack.shellguard.domain.models.SecureNoteDetail
import com.clawstack.shellguard.domain.models.SshKeyDetail
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val cryptoEngine: ShellCryptionEngine = ShellCryptionEngine,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val syncMutex = Mutex()
    private val _syncStatus = MutableStateFlow(
        if (connectivityMonitor.isOnline.value) SyncStatus.ONLINE_SYNCED else SyncStatus.OFFLINE_READ_ONLY
    )
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        // Automatically probe and pull when connectivity returns (transition from offline to online)
        coroutineScope.launch {
            var wasOnline = connectivityMonitor.isOnline.value
            connectivityMonitor.isOnline.collect { isOnline ->
                if (isOnline && !wasOnline) {
                    val ownerUuid = deviceVault.getOwnerUuid()
                    if (!ownerUuid.isNullOrBlank() && deviceVault.hasActiveSession()) {
                        syncAll(ownerUuid)
                    }
                } else if (!isOnline) {
                    _syncStatus.value = SyncStatus.OFFLINE_READ_ONLY
                }
                wasOnline = isOnline
            }
        }
    }

    /**
     * Cancel background repository coroutines (for testing teardown or lifecycle termination).
     */
    fun cancelScope() {
        coroutineScope.cancel()
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
        syncMutex.withLock {
            try {
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

            val client = clientProvider(serverUrl)

            // Health probe
            val healthCheck = client.getHealth()
            if (healthCheck.isFailure || healthCheck.getOrNull() != true) {
                Log.w("SyncRepository", "Server health check failed. Entering OfflineReadOnly.")
                _syncStatus.value = SyncStatus.OFFLINE_READ_ONLY
                return@withContext Result.failure(IllegalStateException("Server health probe failed"))
            }

            // Upstream push of pending changes prior to downstream pull
            val shellKey = deviceVault.getInMemoryShellKey()
            pushPendingChanges(ownerUuid, client, sessionToken, shellKey)

            // Downstream delta pull - Pearls
            val pearlsResult = client.fetchVault(sessionToken)
            if (pearlsResult.isFailure) {
                _syncStatus.value = SyncStatus.SYNC_ERROR
                return@withContext Result.failure(pearlsResult.exceptionOrNull() ?: Exception("Failed to fetch pearls"))
            }
            val remotePearls = pearlsResult.getOrThrow()
            val remotePearlIds = remotePearls.map { it.id }.toSet()

            // Conflict check: Don't overwrite local pending changes
            val pendingPearlSyncIds = database.vaultPearlDao().getPendingSyncItems(ownerUuid).map { it.id }.toSet()
            val pendingPearlDeleteIds = database.vaultPearlDao().getPendingDeleteItems(ownerUuid).map { it.id }.toSet()
            val conflictingPearlIds = pendingPearlSyncIds + pendingPearlDeleteIds

            val pearlEntitiesToUpsert = remotePearls
                .filter { it.id !in conflictingPearlIds }
                .map { dto ->
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
                        customFields = dto.custom_fields?.takeIf { it != "[]" } ?: "",
                        tags = dto.tags,
                        uris = dto.uris,
                        passwordHistory = dto.password_history?.takeIf { it != "[]" } ?: "",
                        reprompt = dto.reprompt,
                        syncState = "SYNCED",
                        createdAt = dto.created_at.ifBlank { System.currentTimeMillis().toString() },
                        localUpdatedAt = System.currentTimeMillis(),
                        remoteUpdatedAt = System.currentTimeMillis()
                    )
                }
            if (pearlEntitiesToUpsert.isNotEmpty()) {
                database.vaultPearlDao().upsertAll(pearlEntitiesToUpsert)
            }

            // Prune deleted remote items in safe chunks (prevent SQLite 999 limit)
            val localSyncedPearlIds = database.vaultPearlDao().getSyncedItemIds(ownerUuid).toSet()
            val obsoletePearlIds = localSyncedPearlIds - remotePearlIds
            obsoletePearlIds.chunked(500).forEach { batch ->
                database.vaultPearlDao().deleteBatch(ownerUuid, batch)
            }

            // Downstream notes pull
            val notesResult = client.fetchNotes(sessionToken)
            if (notesResult.isFailure) {
                _syncStatus.value = SyncStatus.SYNC_ERROR
                return@withContext Result.failure(notesResult.exceptionOrNull() ?: Exception("Failed to fetch notes"))
            }
            val remoteNotes = notesResult.getOrThrow()
            val remoteNoteIds = remoteNotes.map { it.id }.toSet()

            val pendingNoteSyncIds = database.secureNoteDao().getPendingSyncItems(ownerUuid).map { it.id }.toSet()
            val pendingNoteDeleteIds = database.secureNoteDao().getPendingDeleteItems(ownerUuid).map { it.id }.toSet()
            val conflictingNoteIds = pendingNoteSyncIds + pendingNoteDeleteIds

            val noteEntitiesToUpsert = remoteNotes
                .filter { it.id !in conflictingNoteIds }
                .map { dto ->
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
            if (noteEntitiesToUpsert.isNotEmpty()) {
                database.secureNoteDao().upsertAll(noteEntitiesToUpsert)
            }

            val localSyncedNoteIds = database.secureNoteDao().getSyncedItemIds(ownerUuid).toSet()
            val obsoleteNoteIds = localSyncedNoteIds - remoteNoteIds
            obsoleteNoteIds.chunked(500).forEach { batch ->
                database.secureNoteDao().deleteBatch(ownerUuid, batch)
            }

            // Downstream keys pull
            val keysResult = client.fetchKeys(sessionToken)
            if (keysResult.isFailure) {
                _syncStatus.value = SyncStatus.SYNC_ERROR
                return@withContext Result.failure(keysResult.exceptionOrNull() ?: Exception("Failed to fetch SSH keys"))
            }
            val remoteKeys = keysResult.getOrThrow()
            val remoteKeyIds = remoteKeys.map { it.id }.toSet()

            val pendingKeySyncIds = database.sshKeyDao().getPendingSyncItems(ownerUuid).map { it.id }.toSet()
            val pendingKeyDeleteIds = database.sshKeyDao().getPendingDeleteItems(ownerUuid).map { it.id }.toSet()
            val conflictingKeyIds = pendingKeySyncIds + pendingKeyDeleteIds

            val keyEntitiesToUpsert = remoteKeys
                .filter { it.id !in conflictingKeyIds }
                .map { dto ->
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
            if (keyEntitiesToUpsert.isNotEmpty()) {
                database.sshKeyDao().upsertAll(keyEntitiesToUpsert)
            }

            val localSyncedKeyIds = database.sshKeyDao().getSyncedItemIds(ownerUuid).toSet()
            val obsoleteKeyIds = localSyncedKeyIds - remoteKeyIds
            obsoleteKeyIds.chunked(500).forEach { batch ->
                database.sshKeyDao().deleteBatch(ownerUuid, batch)
            }

            // Update SyncMetadata only after all operations succeed
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

    private suspend fun pushPendingChanges(
        ownerUuid: String,
        client: ShellGuardClient,
        sessionToken: String,
        shellKey: ByteArray?
    ) {
        // 1. Drain pending deletes first - only remove local tombstone if remote delete succeeded
        val pendingDeletePearls = database.vaultPearlDao().getPendingDeleteItems(ownerUuid)
        for (item in pendingDeletePearls) {
            var remoteDeleted = true
            if (item.remoteUpdatedAt > 0L) {
                try {
                    val res = client.deleteVaultItem(sessionToken, item.id)
                    remoteDeleted = res.isSuccess && (res.getOrNull() == true)
                } catch (e: Exception) {
                    Log.w("SyncRepository", "Failed to delete remote pearl ${item.id}", e)
                    remoteDeleted = false
                }
            }
            if (remoteDeleted) {
                database.vaultPearlDao().deleteById(ownerUuid, item.id)
            }
        }

        val pendingDeleteNotes = database.secureNoteDao().getPendingDeleteItems(ownerUuid)
        for (item in pendingDeleteNotes) {
            var remoteDeleted = true
            if (item.remoteUpdatedAt > 0L) {
                try {
                    val res = client.deleteNote(sessionToken, item.id)
                    remoteDeleted = res.isSuccess && (res.getOrNull() == true)
                } catch (e: Exception) {
                    Log.w("SyncRepository", "Failed to delete remote note ${item.id}", e)
                    remoteDeleted = false
                }
            }
            if (remoteDeleted) {
                database.secureNoteDao().deleteById(ownerUuid, item.id)
            }
        }

        val pendingDeleteKeys = database.sshKeyDao().getPendingDeleteItems(ownerUuid)
        for (item in pendingDeleteKeys) {
            var remoteDeleted = true
            if (item.remoteUpdatedAt > 0L) {
                try {
                    val res = client.deleteSshKey(sessionToken, item.id)
                    remoteDeleted = res.isSuccess && (res.getOrNull() == true)
                } catch (e: Exception) {
                    Log.w("SyncRepository", "Failed to delete remote ssh key ${item.id}", e)
                    remoteDeleted = false
                }
            }
            if (remoteDeleted) {
                database.sshKeyDao().deleteById(ownerUuid, item.id)
            }
        }

        // 2. Drain pending syncs (Creates / Updates) for Pearls
        val pendingSyncPearls = database.vaultPearlDao().getPendingSyncItems(ownerUuid)
        for (item in pendingSyncPearls) {
            try {
                val request = CreateVaultItemRequest(
                    id = item.id,
                    title = item.title,
                    username = item.username.ifBlank { null },
                    url = item.url.ifBlank { null },
                    category = item.category.ifBlank { null },
                    notes = item.notes.ifBlank { null },
                    secret = item.secret,
                    totp_secret = item.totpSecret.ifBlank { null },
                    type = item.type,
                    custom_fields = item.customFields.ifBlank { null },
                    tags = item.tags,
                    reprompt = item.reprompt
                )

                val pushResult = if (item.remoteUpdatedAt > 0L) {
                    client.updateVaultItem(sessionToken, item.id, request)
                } else {
                    client.createVaultItem(sessionToken, request)
                }

                if (pushResult.isSuccess) {
                    val serverDto = pushResult.getOrNull()
                    var finalId = item.id
                    var finalSecret = item.secret
                    var finalTotp = item.totpSecret
                    var finalCustom = item.customFields
                    var finalHistory = item.passwordHistory

                    if (serverDto != null && serverDto.id.isNotBlank() && serverDto.id != item.id && shellKey != null) {
                        val serverId = serverDto.id
                        try {
                            val plainSecret = cryptoEngine.decryptField(item.secret, shellKey, ShellCryptionEngine.AadNamespace.pearlSecret(item.id))
                            finalSecret = cryptoEngine.encryptField(plainSecret, shellKey, ShellCryptionEngine.AadNamespace.pearlSecret(serverId))

                            if (item.totpSecret.isNotBlank()) {
                                val plainTotp = cryptoEngine.decryptField(item.totpSecret, shellKey, ShellCryptionEngine.AadNamespace.pearlTotp(item.id))
                                finalTotp = cryptoEngine.encryptField(plainTotp, shellKey, ShellCryptionEngine.AadNamespace.pearlTotp(serverId))
                            }
                            if (item.customFields.isNotBlank() && item.customFields != "[]" && cryptoEngine.isEncryptedEnvelope(item.customFields)) {
                                val plainCustom = cryptoEngine.decryptField(item.customFields, shellKey, ShellCryptionEngine.AadNamespace.pearlCustomFields(item.id))
                                finalCustom = cryptoEngine.encryptField(plainCustom, shellKey, ShellCryptionEngine.AadNamespace.pearlCustomFields(serverId))
                            }
                            if (item.passwordHistory.isNotBlank() && item.passwordHistory != "[]" && cryptoEngine.isEncryptedEnvelope(item.passwordHistory)) {
                                val plainHistory = cryptoEngine.decryptField(item.passwordHistory, shellKey, ShellCryptionEngine.AadNamespace.pearlPasswordHistory(item.id))
                                finalHistory = cryptoEngine.encryptField(plainHistory, shellKey, ShellCryptionEngine.AadNamespace.pearlPasswordHistory(serverId))
                            }

                            // Re-update server so remote vault stores ciphertext matching serverId
                            val reEncryptReq = request.copy(
                                id = serverId,
                                secret = finalSecret,
                                totp_secret = finalTotp.ifBlank { null },
                                custom_fields = finalCustom.ifBlank { null }
                            )
                            client.updateVaultItem(sessionToken, serverId, reEncryptReq)

                            database.vaultPearlDao().deleteById(ownerUuid, item.id)
                            finalId = serverId
                        } catch (e: Exception) {
                            Log.e("SyncRepository", "Failed to re-key item to server ID $serverId", e)
                        }
                    }

                    database.vaultPearlDao().upsert(
                        item.copy(
                            id = finalId,
                            secret = finalSecret,
                            totpSecret = finalTotp,
                            customFields = finalCustom,
                            passwordHistory = finalHistory,
                            syncState = "SYNCED",
                            remoteUpdatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("SyncRepository", "Failed to push pending pearl ${item.id}", e)
            }
        }

        // 3. Drain pending syncs for Secure Notes
        val pendingSyncNotes = database.secureNoteDao().getPendingSyncItems(ownerUuid)
        for (item in pendingSyncNotes) {
            try {
                val request = CreateNoteRequest(
                    id = item.id,
                    title = item.title,
                    content = item.content,
                    category = item.category.ifBlank { null },
                    custom_fields = item.customFields.ifBlank { null },
                    tags = item.tags,
                    reprompt = item.reprompt
                )

                val pushResult = if (item.remoteUpdatedAt > 0L) {
                    client.updateNote(sessionToken, item.id, request)
                } else {
                    client.createNote(sessionToken, request)
                }

                if (pushResult.isSuccess) {
                    val serverDto = pushResult.getOrNull()
                    var finalId = item.id
                    var finalContent = item.content
                    var finalCustom = item.customFields

                    if (serverDto != null && serverDto.id.isNotBlank() && serverDto.id != item.id && shellKey != null) {
                        val serverId = serverDto.id
                        try {
                            val plainContent = cryptoEngine.decryptField(item.content, shellKey, ShellCryptionEngine.AadNamespace.secureNoteContent(item.id))
                            finalContent = cryptoEngine.encryptField(plainContent, shellKey, ShellCryptionEngine.AadNamespace.secureNoteContent(serverId))

                            if (item.customFields.isNotBlank() && item.customFields != "[]" && cryptoEngine.isEncryptedEnvelope(item.customFields)) {
                                val plainCustom = cryptoEngine.decryptField(item.customFields, shellKey, ShellCryptionEngine.AadNamespace.secureNoteCustomFields(item.id))
                                finalCustom = cryptoEngine.encryptField(plainCustom, shellKey, ShellCryptionEngine.AadNamespace.secureNoteCustomFields(serverId))
                            }

                            val reEncryptReq = request.copy(
                                id = serverId,
                                content = finalContent,
                                custom_fields = finalCustom.ifBlank { null }
                            )
                            client.updateNote(sessionToken, serverId, reEncryptReq)

                            database.secureNoteDao().deleteById(ownerUuid, item.id)
                            finalId = serverId
                        } catch (e: Exception) {
                            Log.e("SyncRepository", "Failed to re-key note to server ID $serverId", e)
                        }
                    }

                    database.secureNoteDao().upsert(
                        item.copy(
                            id = finalId,
                            content = finalContent,
                            customFields = finalCustom,
                            syncState = "SYNCED",
                            remoteUpdatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("SyncRepository", "Failed to push pending note ${item.id}", e)
            }
        }

        // 4. Drain pending syncs for SSH Keys
        val pendingSyncKeys = database.sshKeyDao().getPendingSyncItems(ownerUuid)
        for (item in pendingSyncKeys) {
            try {
                val request = CreateSshKeyRequest(
                    id = item.id,
                    title = item.title,
                    key_value = item.keyValue,
                    username = item.username.ifBlank { null },
                    category = item.category.ifBlank { null },
                    custom_fields = item.customFields.ifBlank { null },
                    tags = item.tags,
                    reprompt = item.reprompt
                )

                val pushResult = if (item.remoteUpdatedAt > 0L) {
                    client.updateSshKey(sessionToken, item.id, request)
                } else {
                    client.createSshKey(sessionToken, request)
                }

                if (pushResult.isSuccess) {
                    val serverDto = pushResult.getOrNull()
                    var finalId = item.id
                    var finalKey = item.keyValue
                    var finalCustom = item.customFields

                    if (serverDto != null && serverDto.id.isNotBlank() && serverDto.id != item.id && shellKey != null) {
                        val serverId = serverDto.id
                        try {
                            val plainKey = cryptoEngine.decryptField(item.keyValue, shellKey, ShellCryptionEngine.AadNamespace.sshKeyPrivate(item.id))
                            finalKey = cryptoEngine.encryptField(plainKey, shellKey, ShellCryptionEngine.AadNamespace.sshKeyPrivate(serverId))

                            if (item.customFields.isNotBlank() && item.customFields != "[]" && cryptoEngine.isEncryptedEnvelope(item.customFields)) {
                                val plainCustom = cryptoEngine.decryptField(item.customFields, shellKey, ShellCryptionEngine.AadNamespace.sshKeyCustomFields(item.id))
                                finalCustom = cryptoEngine.encryptField(plainCustom, shellKey, ShellCryptionEngine.AadNamespace.sshKeyCustomFields(serverId))
                            }

                            val reEncryptReq = request.copy(
                                id = serverId,
                                key_value = finalKey,
                                custom_fields = finalCustom.ifBlank { null }
                            )
                            client.updateSshKey(sessionToken, serverId, reEncryptReq)

                            database.sshKeyDao().deleteById(ownerUuid, item.id)
                            finalId = serverId
                        } catch (e: Exception) {
                            Log.e("SyncRepository", "Failed to re-key ssh key to server ID $serverId", e)
                        }
                    }

                    database.sshKeyDao().upsert(
                        item.copy(
                            id = finalId,
                            keyValue = finalKey,
                            customFields = finalCustom,
                            syncState = "SYNCED",
                            remoteUpdatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("SyncRepository", "Failed to push pending ssh key ${item.id}", e)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Multi-Domain Decrypted CRUD Operations (Task 05)
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun getPearlDetail(id: String): Result<PearlDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val ownerUuid = deviceVault.getOwnerUuid() ?: throw IllegalStateException("No owner UUID")
            val shellKey = deviceVault.getInMemoryShellKey() ?: throw IllegalStateException("Vault locked or shellKey missing")
            val entity = database.vaultPearlDao().getById(ownerUuid, id) ?: throw NoSuchElementException("Pearl not found: $id")

            val secretPlain = if (entity.secret.isNotBlank()) {
                cryptoEngine.decryptField(entity.secret, shellKey, ShellCryptionEngine.AadNamespace.pearlSecret(id))
            } else ""

            val totpPlain = if (entity.totpSecret.isNotBlank()) {
                cryptoEngine.decryptField(entity.totpSecret, shellKey, ShellCryptionEngine.AadNamespace.pearlTotp(id))
            } else ""

            val customFields = if (entity.customFields.isNotBlank() && entity.customFields != "[]") {
                if (cryptoEngine.isEncryptedEnvelope(entity.customFields)) {
                    val decryptedJson = cryptoEngine.decryptField(entity.customFields, shellKey, ShellCryptionEngine.AadNamespace.pearlCustomFields(id))
                    CustomFieldSerializer.deserializeFields(decryptedJson)
                } else {
                    CustomFieldSerializer.deserializeFields(entity.customFields)
                }
            } else emptyList()

            val history = if (entity.passwordHistory.isNotBlank() && entity.passwordHistory != "[]") {
                if (cryptoEngine.isEncryptedEnvelope(entity.passwordHistory)) {
                    val decryptedJson = cryptoEngine.decryptField(entity.passwordHistory, shellKey, ShellCryptionEngine.AadNamespace.pearlPasswordHistory(id))
                    CustomFieldSerializer.deserializeHistory(decryptedJson)
                } else {
                    CustomFieldSerializer.deserializeHistory(entity.passwordHistory)
                }
            } else emptyList()

            val tags = CustomFieldSerializer.deserializeTags(entity.tags)

            PearlDetail(
                id = entity.id,
                ownerUuid = entity.ownerUuid,
                title = entity.title,
                secret = secretPlain,
                username = entity.username,
                url = entity.url,
                type = entity.type,
                category = entity.category,
                notes = entity.notes,
                totpSecret = totpPlain,
                customFields = customFields,
                tags = tags,
                passwordHistory = history,
                reprompt = entity.reprompt,
                syncState = entity.syncState,
                createdAt = entity.createdAt,
                localUpdatedAt = entity.localUpdatedAt,
                remoteUpdatedAt = entity.remoteUpdatedAt
            )
        }
    }

    suspend fun savePearlDetail(pearl: PearlDetail): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val ownerUuid = deviceVault.getOwnerUuid() ?: pearl.ownerUuid
            val shellKey = deviceVault.getInMemoryShellKey() ?: throw IllegalStateException("Vault locked or shellKey missing")
            val id = if (pearl.id.isBlank() || pearl.id == "NEW") UUID.randomUUID().toString() else pearl.id

            val existing = database.vaultPearlDao().getById(ownerUuid, id)
            val updatedHistory = pearl.passwordHistory.toMutableList()
            if (existing != null && pearl.secret.isNotBlank()) {
                try {
                    val oldSecret = if (cryptoEngine.isEncryptedEnvelope(existing.secret)) {
                        cryptoEngine.decryptField(existing.secret, shellKey, ShellCryptionEngine.AadNamespace.pearlSecret(id))
                    } else existing.secret
                    if (oldSecret != pearl.secret && oldSecret.isNotBlank() && updatedHistory.none { it.password == oldSecret }) {
                        updatedHistory.add(0, PasswordHistoryEntry(password = oldSecret, timestamp = System.currentTimeMillis()))
                    }
                } catch (_: Exception) {}
            }

            val encryptedSecret = cryptoEngine.encryptField(pearl.secret, shellKey, ShellCryptionEngine.AadNamespace.pearlSecret(id))
            val encryptedTotp = if (pearl.totpSecret.isNotBlank()) {
                cryptoEngine.encryptField(pearl.totpSecret, shellKey, ShellCryptionEngine.AadNamespace.pearlTotp(id))
            } else ""

            val customFieldsJson = CustomFieldSerializer.serializeFields(pearl.customFields)
            val encryptedCustomFields = if (pearl.customFields.isNotEmpty()) {
                cryptoEngine.encryptField(customFieldsJson, shellKey, ShellCryptionEngine.AadNamespace.pearlCustomFields(id))
            } else ""

            val historyJson = CustomFieldSerializer.serializeHistory(updatedHistory)
            val encryptedHistory = if (updatedHistory.isNotEmpty()) {
                cryptoEngine.encryptField(historyJson, shellKey, ShellCryptionEngine.AadNamespace.pearlPasswordHistory(id))
            } else ""

            val tagsJson = CustomFieldSerializer.serializeTags(pearl.tags)

            var syncState = "PENDING_SYNC"
            var remoteUpdatedAt = pearl.remoteUpdatedAt
            var currentId = id
            var finalSecret = encryptedSecret
            var finalTotp = encryptedTotp
            var finalCustomFields = encryptedCustomFields
            var finalHistory = encryptedHistory

            val serverUrl = deviceVault.getServerUrl()
            val sessionToken = deviceVault.getSessionToken()
            if (connectivityMonitor.isOnline.value && !serverUrl.isNullOrBlank() && !sessionToken.isNullOrBlank()) {
                val client = clientProvider(serverUrl)
                val request = CreateVaultItemRequest(
                    id = currentId,
                    title = pearl.title,
                    username = pearl.username.ifBlank { null },
                    url = pearl.url.ifBlank { null },
                    category = pearl.category.ifBlank { null },
                    notes = pearl.notes.ifBlank { null },
                    secret = encryptedSecret,
                    totp_secret = encryptedTotp.ifBlank { null },
                    type = pearl.type,
                    custom_fields = encryptedCustomFields.ifBlank { null },
                    tags = tagsJson,
                    reprompt = pearl.reprompt
                )

                val pushResult = if (existing != null && pearl.remoteUpdatedAt > 0L) {
                    client.updateVaultItem(sessionToken, currentId, request)
                } else {
                    client.createVaultItem(sessionToken, request)
                }

                if (pushResult.isSuccess) {
                    syncState = "SYNCED"
                    remoteUpdatedAt = System.currentTimeMillis()
                    val serverDto = pushResult.getOrNull()
                    if (serverDto != null && serverDto.id.isNotBlank() && serverDto.id != currentId) {
                        val serverId = serverDto.id
                        finalSecret = cryptoEngine.encryptField(pearl.secret, shellKey, ShellCryptionEngine.AadNamespace.pearlSecret(serverId))
                        if (pearl.totpSecret.isNotBlank()) {
                            finalTotp = cryptoEngine.encryptField(pearl.totpSecret, shellKey, ShellCryptionEngine.AadNamespace.pearlTotp(serverId))
                        }
                        if (pearl.customFields.isNotEmpty()) {
                            finalCustomFields = cryptoEngine.encryptField(customFieldsJson, shellKey, ShellCryptionEngine.AadNamespace.pearlCustomFields(serverId))
                        }
                        if (updatedHistory.isNotEmpty()) {
                            finalHistory = cryptoEngine.encryptField(historyJson, shellKey, ShellCryptionEngine.AadNamespace.pearlPasswordHistory(serverId))
                        }

                        val reEncryptReq = request.copy(
                            id = serverId,
                            secret = finalSecret,
                            totp_secret = finalTotp.ifBlank { null },
                            custom_fields = finalCustomFields.ifBlank { null }
                        )
                        client.updateVaultItem(sessionToken, serverId, reEncryptReq)

                        database.vaultPearlDao().deleteById(ownerUuid, currentId)
                        currentId = serverId
                    }
                }
            }

            val entity = VaultPearlEntity(
                id = currentId,
                ownerUuid = ownerUuid,
                title = pearl.title,
                secret = finalSecret,
                username = pearl.username,
                url = pearl.url,
                type = pearl.type,
                category = pearl.category,
                notes = pearl.notes,
                totpSecret = finalTotp,
                attachments = "[]",
                customFields = finalCustomFields,
                tags = tagsJson,
                uris = "[]",
                passwordHistory = finalHistory,
                reprompt = pearl.reprompt,
                syncState = syncState,
                createdAt = pearl.createdAt.ifBlank { System.currentTimeMillis().toString() },
                localUpdatedAt = System.currentTimeMillis(),
                remoteUpdatedAt = remoteUpdatedAt
            )

            database.vaultPearlDao().upsert(entity)
        }
    }

    suspend fun getNoteDetail(id: String): Result<SecureNoteDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val ownerUuid = deviceVault.getOwnerUuid() ?: throw IllegalStateException("No owner UUID")
            val shellKey = deviceVault.getInMemoryShellKey() ?: throw IllegalStateException("Vault locked or shellKey missing")
            val entity = database.secureNoteDao().getById(ownerUuid, id) ?: throw NoSuchElementException("Note not found: $id")

            val contentPlain = if (entity.content.isNotBlank()) {
                cryptoEngine.decryptField(entity.content, shellKey, ShellCryptionEngine.AadNamespace.secureNoteContent(id))
            } else ""

            val customFields = if (entity.customFields.isNotBlank() && entity.customFields != "[]") {
                if (cryptoEngine.isEncryptedEnvelope(entity.customFields)) {
                    val decryptedJson = cryptoEngine.decryptField(entity.customFields, shellKey, ShellCryptionEngine.AadNamespace.secureNoteCustomFields(id))
                    CustomFieldSerializer.deserializeFields(decryptedJson)
                } else {
                    CustomFieldSerializer.deserializeFields(entity.customFields)
                }
            } else emptyList()

            val tags = CustomFieldSerializer.deserializeTags(entity.tags)

            SecureNoteDetail(
                id = entity.id,
                ownerUuid = entity.ownerUuid,
                title = entity.title,
                content = contentPlain,
                category = entity.category,
                customFields = customFields,
                tags = tags,
                reprompt = entity.reprompt,
                syncState = entity.syncState,
                createdAt = entity.createdAt,
                localUpdatedAt = entity.localUpdatedAt,
                remoteUpdatedAt = entity.remoteUpdatedAt
            )
        }
    }

    suspend fun saveNoteDetail(note: SecureNoteDetail): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val ownerUuid = deviceVault.getOwnerUuid() ?: note.ownerUuid
            val shellKey = deviceVault.getInMemoryShellKey() ?: throw IllegalStateException("Vault locked or shellKey missing")
            val id = if (note.id.isBlank() || note.id == "NEW") UUID.randomUUID().toString() else note.id

            val existing = database.secureNoteDao().getById(ownerUuid, id)
            val encryptedContent = cryptoEngine.encryptField(note.content, shellKey, ShellCryptionEngine.AadNamespace.secureNoteContent(id))

            val customFieldsJson = CustomFieldSerializer.serializeFields(note.customFields)
            val encryptedCustomFields = if (note.customFields.isNotEmpty()) {
                cryptoEngine.encryptField(customFieldsJson, shellKey, ShellCryptionEngine.AadNamespace.secureNoteCustomFields(id))
            } else ""

            val tagsJson = CustomFieldSerializer.serializeTags(note.tags)

            var syncState = "PENDING_SYNC"
            var remoteUpdatedAt = note.remoteUpdatedAt
            var currentId = id
            var finalContent = encryptedContent
            var finalCustomFields = encryptedCustomFields

            val serverUrl = deviceVault.getServerUrl()
            val sessionToken = deviceVault.getSessionToken()
            if (connectivityMonitor.isOnline.value && !serverUrl.isNullOrBlank() && !sessionToken.isNullOrBlank()) {
                val client = clientProvider(serverUrl)
                val request = CreateNoteRequest(
                    id = currentId,
                    title = note.title,
                    content = encryptedContent,
                    category = note.category.ifBlank { null },
                    custom_fields = encryptedCustomFields.ifBlank { null },
                    tags = tagsJson,
                    reprompt = note.reprompt
                )

                val pushResult = if (existing != null && note.remoteUpdatedAt > 0L) {
                    client.updateNote(sessionToken, currentId, request)
                } else {
                    client.createNote(sessionToken, request)
                }

                if (pushResult.isSuccess) {
                    syncState = "SYNCED"
                    remoteUpdatedAt = System.currentTimeMillis()
                    val serverDto = pushResult.getOrNull()
                    if (serverDto != null && serverDto.id.isNotBlank() && serverDto.id != currentId) {
                        val serverId = serverDto.id
                        finalContent = cryptoEngine.encryptField(note.content, shellKey, ShellCryptionEngine.AadNamespace.secureNoteContent(serverId))
                        if (note.customFields.isNotEmpty()) {
                            finalCustomFields = cryptoEngine.encryptField(customFieldsJson, shellKey, ShellCryptionEngine.AadNamespace.secureNoteCustomFields(serverId))
                        }

                        val reEncryptReq = request.copy(
                            id = serverId,
                            content = finalContent,
                            custom_fields = finalCustomFields.ifBlank { null }
                        )
                        client.updateNote(sessionToken, serverId, reEncryptReq)

                        database.secureNoteDao().deleteById(ownerUuid, currentId)
                        currentId = serverId
                    }
                }
            }

            val entity = SecureNoteEntity(
                id = currentId,
                ownerUuid = ownerUuid,
                title = note.title,
                content = finalContent,
                category = note.category,
                attachments = "[]",
                customFields = finalCustomFields,
                tags = tagsJson,
                reprompt = note.reprompt,
                syncState = syncState,
                createdAt = note.createdAt.ifBlank { System.currentTimeMillis().toString() },
                localUpdatedAt = System.currentTimeMillis(),
                remoteUpdatedAt = remoteUpdatedAt
            )

            database.secureNoteDao().upsert(entity)
        }
    }

    suspend fun getSshKeyDetail(id: String): Result<SshKeyDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val ownerUuid = deviceVault.getOwnerUuid() ?: throw IllegalStateException("No owner UUID")
            val shellKey = deviceVault.getInMemoryShellKey() ?: throw IllegalStateException("Vault locked or shellKey missing")
            val entity = database.sshKeyDao().getById(ownerUuid, id) ?: throw NoSuchElementException("SSH Key not found: $id")

            val keyPlain = if (entity.keyValue.isNotBlank()) {
                cryptoEngine.decryptField(entity.keyValue, shellKey, ShellCryptionEngine.AadNamespace.sshKeyPrivate(id))
            } else ""

            val customFields = if (entity.customFields.isNotBlank() && entity.customFields != "[]") {
                if (cryptoEngine.isEncryptedEnvelope(entity.customFields)) {
                    val decryptedJson = cryptoEngine.decryptField(entity.customFields, shellKey, ShellCryptionEngine.AadNamespace.sshKeyCustomFields(id))
                    CustomFieldSerializer.deserializeFields(decryptedJson)
                } else {
                    CustomFieldSerializer.deserializeFields(entity.customFields)
                }
            } else emptyList()

            val tags = CustomFieldSerializer.deserializeTags(entity.tags)

            SshKeyDetail(
                id = entity.id,
                ownerUuid = entity.ownerUuid,
                title = entity.title,
                keyValue = keyPlain,
                username = entity.username,
                category = entity.category,
                customFields = customFields,
                tags = tags,
                reprompt = entity.reprompt,
                syncState = entity.syncState,
                createdAt = entity.createdAt,
                localUpdatedAt = entity.localUpdatedAt,
                remoteUpdatedAt = entity.remoteUpdatedAt
            )
        }
    }

    suspend fun saveSshKeyDetail(key: SshKeyDetail): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val ownerUuid = deviceVault.getOwnerUuid() ?: key.ownerUuid
            val shellKey = deviceVault.getInMemoryShellKey() ?: throw IllegalStateException("Vault locked or shellKey missing")
            val id = if (key.id.isBlank() || key.id == "NEW") UUID.randomUUID().toString() else key.id

            val existing = database.sshKeyDao().getById(ownerUuid, id)
            val encryptedKey = cryptoEngine.encryptField(key.keyValue, shellKey, ShellCryptionEngine.AadNamespace.sshKeyPrivate(id))

            val customFieldsJson = CustomFieldSerializer.serializeFields(key.customFields)
            val encryptedCustomFields = if (key.customFields.isNotEmpty()) {
                cryptoEngine.encryptField(customFieldsJson, shellKey, ShellCryptionEngine.AadNamespace.sshKeyCustomFields(id))
            } else ""

            val tagsJson = CustomFieldSerializer.serializeTags(key.tags)

            var syncState = "PENDING_SYNC"
            var remoteUpdatedAt = key.remoteUpdatedAt
            var currentId = id
            var finalKey = encryptedKey
            var finalCustomFields = encryptedCustomFields

            val serverUrl = deviceVault.getServerUrl()
            val sessionToken = deviceVault.getSessionToken()
            if (connectivityMonitor.isOnline.value && !serverUrl.isNullOrBlank() && !sessionToken.isNullOrBlank()) {
                val client = clientProvider(serverUrl)
                val request = CreateSshKeyRequest(
                    id = currentId,
                    title = key.title,
                    key_value = encryptedKey,
                    username = key.username.ifBlank { null },
                    category = key.category.ifBlank { null },
                    custom_fields = encryptedCustomFields.ifBlank { null },
                    tags = tagsJson,
                    reprompt = key.reprompt
                )

                val pushResult = if (existing != null && key.remoteUpdatedAt > 0L) {
                    client.updateSshKey(sessionToken, currentId, request)
                } else {
                    client.createSshKey(sessionToken, request)
                }

                if (pushResult.isSuccess) {
                    syncState = "SYNCED"
                    remoteUpdatedAt = System.currentTimeMillis()
                    val serverDto = pushResult.getOrNull()
                    if (serverDto != null && serverDto.id.isNotBlank() && serverDto.id != currentId) {
                        val serverId = serverDto.id
                        finalKey = cryptoEngine.encryptField(key.keyValue, shellKey, ShellCryptionEngine.AadNamespace.sshKeyPrivate(serverId))
                        if (key.customFields.isNotEmpty()) {
                            finalCustomFields = cryptoEngine.encryptField(customFieldsJson, shellKey, ShellCryptionEngine.AadNamespace.sshKeyCustomFields(serverId))
                        }

                        val reEncryptReq = request.copy(
                            id = serverId,
                            key_value = finalKey,
                            custom_fields = finalCustomFields.ifBlank { null }
                        )
                        client.updateSshKey(sessionToken, serverId, reEncryptReq)

                        database.sshKeyDao().deleteById(ownerUuid, currentId)
                        currentId = serverId
                    }
                }
            }

            val entity = SshKeyEntity(
                id = currentId,
                ownerUuid = ownerUuid,
                title = key.title,
                keyValue = finalKey,
                username = key.username,
                category = key.category,
                customFields = finalCustomFields,
                tags = tagsJson,
                reprompt = key.reprompt,
                syncState = syncState,
                createdAt = key.createdAt.ifBlank { System.currentTimeMillis().toString() },
                localUpdatedAt = System.currentTimeMillis(),
                remoteUpdatedAt = remoteUpdatedAt
            )

            database.sshKeyDao().upsert(entity)
        }
    }

    suspend fun deleteItem(domain: VaultItemDomain, id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val ownerUuid = deviceVault.getOwnerUuid() ?: throw IllegalStateException("No owner UUID")
            val serverUrl = deviceVault.getServerUrl()
            val sessionToken = deviceVault.getSessionToken()
            val isOnline = connectivityMonitor.isOnline.value && !serverUrl.isNullOrBlank() && !sessionToken.isNullOrBlank()

            var remoteDeleteSucceeded = false
            if (isOnline) {
                val client = clientProvider(serverUrl!!)
                val result = when (domain) {
                    VaultItemDomain.PASSWORD -> client.deleteVaultItem(sessionToken!!, id)
                    VaultItemDomain.NOTE -> client.deleteNote(sessionToken!!, id)
                    VaultItemDomain.SSH_KEY -> client.deleteSshKey(sessionToken!!, id)
                }
                remoteDeleteSucceeded = result.isSuccess && (result.getOrNull() == true)
            }

            if (remoteDeleteSucceeded) {
                when (domain) {
                    VaultItemDomain.PASSWORD -> database.vaultPearlDao().deleteById(ownerUuid, id)
                    VaultItemDomain.NOTE -> database.secureNoteDao().deleteById(ownerUuid, id)
                    VaultItemDomain.SSH_KEY -> database.sshKeyDao().deleteById(ownerUuid, id)
                }
            } else {
                when (domain) {
                    VaultItemDomain.PASSWORD -> {
                        val existing = database.vaultPearlDao().getById(ownerUuid, id)
                        if (existing != null && existing.remoteUpdatedAt > 0L) {
                            database.vaultPearlDao().markForDeletion(ownerUuid, id)
                        } else {
                            database.vaultPearlDao().deleteById(ownerUuid, id)
                        }
                    }
                    VaultItemDomain.NOTE -> {
                        val existing = database.secureNoteDao().getById(ownerUuid, id)
                        if (existing != null && existing.remoteUpdatedAt > 0L) {
                            database.secureNoteDao().markForDeletion(ownerUuid, id)
                        } else {
                            database.secureNoteDao().deleteById(ownerUuid, id)
                        }
                    }
                    VaultItemDomain.SSH_KEY -> {
                        val existing = database.sshKeyDao().getById(ownerUuid, id)
                        if (existing != null && existing.remoteUpdatedAt > 0L) {
                            database.sshKeyDao().markForDeletion(ownerUuid, id)
                        } else {
                            database.sshKeyDao().deleteById(ownerUuid, id)
                        }
                    }
                }
            }
        }
    }
}
