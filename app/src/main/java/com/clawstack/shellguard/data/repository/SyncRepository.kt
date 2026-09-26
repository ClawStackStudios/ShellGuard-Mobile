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
    private val cryptoEngine: ShellCryptionEngine = ShellCryptionEngine,
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

    // ══════════════════════════════════════════════════════════════════════════
    // Multi-Domain Decrypted CRUD Operations (Task 05)
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun getPearlDetail(id: String): Result<PearlDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val ownerUuid = deviceVault.getOwnerUuid() ?: throw IllegalStateException("No owner UUID")
            val shellKey = deviceVault.getInMemoryShellKey() ?: throw IllegalStateException("Vault locked or shellKey missing")
            val entity = database.vaultPearlDao().getById(ownerUuid, id) ?: throw NoSuchElementException("Pearl not found: $id")

            val secretPlain = try {
                cryptoEngine.decryptField(entity.secret, shellKey, ShellCryptionEngine.AadNamespace.pearlSecret(id))
            } catch (e: Exception) {
                entity.secret
            }

            val totpPlain = if (entity.totpSecret.isNotBlank()) {
                try {
                    cryptoEngine.decryptField(entity.totpSecret, shellKey, ShellCryptionEngine.AadNamespace.pearlTotp(id))
                } catch (_: Exception) {
                    entity.totpSecret
                }
            } else ""

            val customFields = if (entity.customFields.isNotBlank()) {
                try {
                    val decryptedJson = cryptoEngine.decryptField(entity.customFields, shellKey, ShellCryptionEngine.AadNamespace.pearlCustomFields(id))
                    CustomFieldSerializer.deserializeFields(decryptedJson)
                } catch (_: Exception) {
                    CustomFieldSerializer.deserializeFields(entity.customFields)
                }
            } else emptyList()

            val history = if (entity.passwordHistory.isNotBlank()) {
                try {
                    val decryptedJson = cryptoEngine.decryptField(entity.passwordHistory, shellKey, ShellCryptionEngine.AadNamespace.pearlPasswordHistory(id))
                    CustomFieldSerializer.deserializeHistory(decryptedJson)
                } catch (_: Exception) {
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
                    val oldSecret = cryptoEngine.decryptField(existing.secret, shellKey, ShellCryptionEngine.AadNamespace.pearlSecret(id))
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

            val serverUrl = deviceVault.getServerUrl()
            val sessionToken = deviceVault.getSessionToken()
            if (connectivityMonitor.isOnline.value && !serverUrl.isNullOrBlank() && !sessionToken.isNullOrBlank()) {
                val client = clientProvider(serverUrl)
                val request = CreateVaultItemRequest(
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
                    client.updateVaultItem(sessionToken, id, request)
                } else {
                    client.createVaultItem(sessionToken, request)
                }

                if (pushResult.isSuccess) {
                    syncState = "SYNCED"
                    remoteUpdatedAt = System.currentTimeMillis()
                }
            }

            val entity = VaultPearlEntity(
                id = id,
                ownerUuid = ownerUuid,
                title = pearl.title,
                secret = encryptedSecret,
                username = pearl.username,
                url = pearl.url,
                type = pearl.type,
                category = pearl.category,
                notes = pearl.notes,
                totpSecret = encryptedTotp,
                attachments = "[]",
                customFields = encryptedCustomFields,
                tags = tagsJson,
                uris = "[]",
                passwordHistory = encryptedHistory,
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

            val contentPlain = try {
                cryptoEngine.decryptField(entity.content, shellKey, ShellCryptionEngine.AadNamespace.secureNoteContent(id))
            } catch (e: Exception) {
                entity.content
            }

            val customFields = if (entity.customFields.isNotBlank()) {
                try {
                    val decryptedJson = cryptoEngine.decryptField(entity.customFields, shellKey, ShellCryptionEngine.AadNamespace.secureNoteCustomFields(id))
                    CustomFieldSerializer.deserializeFields(decryptedJson)
                } catch (_: Exception) {
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

            val serverUrl = deviceVault.getServerUrl()
            val sessionToken = deviceVault.getSessionToken()
            if (connectivityMonitor.isOnline.value && !serverUrl.isNullOrBlank() && !sessionToken.isNullOrBlank()) {
                val client = clientProvider(serverUrl)
                val request = CreateNoteRequest(
                    title = note.title,
                    content = encryptedContent,
                    category = note.category.ifBlank { null },
                    custom_fields = encryptedCustomFields.ifBlank { null },
                    tags = tagsJson,
                    reprompt = note.reprompt
                )

                val pushResult = if (existing != null && note.remoteUpdatedAt > 0L) {
                    client.updateNote(sessionToken, id, request)
                } else {
                    client.createNote(sessionToken, request)
                }

                if (pushResult.isSuccess) {
                    syncState = "SYNCED"
                    remoteUpdatedAt = System.currentTimeMillis()
                }
            }

            val entity = SecureNoteEntity(
                id = id,
                ownerUuid = ownerUuid,
                title = note.title,
                content = encryptedContent,
                category = note.category,
                attachments = "[]",
                customFields = encryptedCustomFields,
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

            val keyPlain = try {
                cryptoEngine.decryptField(entity.keyValue, shellKey, ShellCryptionEngine.AadNamespace.sshKeyPrivate(id))
            } catch (e: Exception) {
                entity.keyValue
            }

            val customFields = if (entity.customFields.isNotBlank()) {
                try {
                    val decryptedJson = cryptoEngine.decryptField(entity.customFields, shellKey, ShellCryptionEngine.AadNamespace.sshKeyCustomFields(id))
                    CustomFieldSerializer.deserializeFields(decryptedJson)
                } catch (_: Exception) {
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

            val serverUrl = deviceVault.getServerUrl()
            val sessionToken = deviceVault.getSessionToken()
            if (connectivityMonitor.isOnline.value && !serverUrl.isNullOrBlank() && !sessionToken.isNullOrBlank()) {
                val client = clientProvider(serverUrl)
                val request = CreateSshKeyRequest(
                    title = key.title,
                    key_value = encryptedKey,
                    username = key.username.ifBlank { null },
                    category = key.category.ifBlank { null },
                    custom_fields = encryptedCustomFields.ifBlank { null },
                    tags = tagsJson,
                    reprompt = key.reprompt
                )

                val pushResult = if (existing != null && key.remoteUpdatedAt > 0L) {
                    client.updateSshKey(sessionToken, id, request)
                } else {
                    client.createSshKey(sessionToken, request)
                }

                if (pushResult.isSuccess) {
                    syncState = "SYNCED"
                    remoteUpdatedAt = System.currentTimeMillis()
                }
            }

            val entity = SshKeyEntity(
                id = id,
                ownerUuid = ownerUuid,
                title = key.title,
                keyValue = encryptedKey,
                username = key.username,
                category = key.category,
                customFields = encryptedCustomFields,
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

            // If online, call remote delete endpoint
            if (connectivityMonitor.isOnline.value && !serverUrl.isNullOrBlank() && !sessionToken.isNullOrBlank()) {
                val client = clientProvider(serverUrl)
                when (domain) {
                    VaultItemDomain.PASSWORD -> client.deleteVaultItem(sessionToken, id)
                    VaultItemDomain.NOTE -> client.deleteNote(sessionToken, id)
                    VaultItemDomain.SSH_KEY -> client.deleteSshKey(sessionToken, id)
                }
            }

            // Delete locally from Room
            when (domain) {
                VaultItemDomain.PASSWORD -> database.vaultPearlDao().deleteById(ownerUuid, id)
                VaultItemDomain.NOTE -> database.secureNoteDao().deleteById(ownerUuid, id)
                VaultItemDomain.SSH_KEY -> database.sshKeyDao().deleteById(ownerUuid, id)
            }
        }
    }
}
