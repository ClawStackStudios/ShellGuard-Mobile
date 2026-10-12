package com.clawstack.shellguard.data.backup

import com.clawstack.shellguard.crypto.EncryptedDeviceVault
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.data.local.ShellGuardDatabase
import com.clawstack.shellguard.data.local.entities.SecureNoteEntity
import com.clawstack.shellguard.data.local.entities.SshKeyEntity
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class BackupProtectionMode {
    ACTIVE_KEY,
    CUSTOM_PASSPHRASE,
    PLAINTEXT
}

enum class BackupFormatType {
    SHELLGUARD_ENCRYPTED,
    SHELLGUARD_PLAIN,
    BITWARDEN_JSON,
    BITWARDEN_ENCRYPTED,
    UNKNOWN
}

@Serializable
data class ShellGuardBackupEnvelope(
    val v: Int = 1,
    val version: Int = 1,
    val format: String = "shellguard-vault-backup-v1",
    val kind: String = "json",
    val kdf: String = "hkdf", // "hkdf" or "pbkdf2"
    val kdfIterations: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val salt: String, // Base64
    val iv: String, // Base64
    val payload: String, // Base64 AES-GCM ciphertext + tag
    val checksumSha256: String // Hex
)

@Serializable
data class BackupVaultItem(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "password", // "password", "note", "key", "card", "identity"
    val title: String = "",
    val secret: String = "", // Decrypted secret, note content, or SSH private key
    val username: String = "",
    val url: String = "",
    val uris: String = "[]",
    val category: String = "",
    val notes: String = "",
    val attachments: String = "[]",
    // Serialized with web naming convention (snake_case), with camelCase fallback
    val totp_secret: String? = null,
    val totpSecret: String? = null,
    val custom_fields: String? = null,
    val customFields: String? = null,
    val tags: String = "[]",
    val reprompt: Boolean = false,
    val created_at: String? = null,
    val createdAt: String? = null
) {
    val resolvedTotp: String get() = totp_secret?.takeIf { it.isNotBlank() } ?: totpSecret.orEmpty()
    val resolvedCustomFields: String get() = custom_fields?.takeIf { it.isNotBlank() } ?: customFields.orEmpty()
    val resolvedCreatedAt: String get() = created_at?.takeIf { it.isNotBlank() } ?: createdAt ?: java.time.Instant.now().toString()
}

@Serializable
data class BackupPearlItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val secret: String, // Plaintext secret
    val username: String = "",
    val url: String = "",
    val type: String = "password",
    val category: String = "",
    val notes: String = "",
    val totpSecret: String = "",
    val customFields: String = "",
    val tags: String = "[]",
    val uris: String = "[]",
    val reprompt: Boolean = false,
    val createdAt: String = java.time.Instant.now().toString()
)

@Serializable
data class BackupNoteItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String, // Plaintext content
    val category: String = "",
    val customFields: String = "",
    val tags: String = "[]",
    val reprompt: Boolean = false,
    val createdAt: String = java.time.Instant.now().toString()
)

@Serializable
data class BackupSshKeyItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val keyValue: String, // Plaintext private key
    val username: String = "",
    val category: String = "",
    val customFields: String = "",
    val tags: String = "[]",
    val reprompt: Boolean = false,
    val createdAt: String = java.time.Instant.now().toString()
)

@Serializable
data class VaultBackupPayload(
    val app: String = "ShellGuard Vault Backup",
    val version: String = "1.0",
    val exportedAt: String = java.time.Instant.now().toString(),
    val ownerUuid: String,
    val itemCount: Int = 0,
    val items: List<BackupVaultItem> = emptyList(),
    val pearls: List<BackupPearlItem>? = null,
    val notes: List<BackupNoteItem>? = null,
    val sshKeys: List<BackupSshKeyItem>? = null
) {
    fun allItems(): List<BackupVaultItem> {
        if (items.isNotEmpty()) return items
        // Fallback to legacy structure if present
        val list = mutableListOf<BackupVaultItem>()
        pearls?.forEach { p ->
            list.add(BackupVaultItem(
                id = p.id, type = p.type.ifBlank { "password" }, title = p.title, secret = p.secret,
                username = p.username, url = p.url, uris = p.uris, category = p.category, notes = p.notes,
                totp_secret = p.totpSecret, custom_fields = p.customFields, tags = p.tags, reprompt = p.reprompt,
                created_at = p.createdAt
            ))
        }
        notes?.forEach { n ->
            list.add(BackupVaultItem(
                id = n.id, type = "note", title = n.title, secret = n.content, notes = n.content,
                category = n.category, custom_fields = n.customFields, tags = n.tags, reprompt = n.reprompt,
                created_at = n.createdAt
            ))
        }
        sshKeys?.forEach { k ->
            list.add(BackupVaultItem(
                id = k.id, type = "key", title = k.title, secret = k.keyValue, username = k.username,
                category = k.category, custom_fields = k.customFields, tags = k.tags, reprompt = k.reprompt,
                created_at = k.createdAt
            ))
        }
        return list
    }
}

data class ExportResult(
    val jsonString: String,
    val isEncrypted: Boolean,
    val protectionMode: BackupProtectionMode,
    val pearlsCount: Int,
    val notesCount: Int,
    val sshKeysCount: Int,
    val checksumSha256: String
)

data class ImportResult(
    val pearlsCount: Int,
    val notesCount: Int,
    val sshKeysCount: Int
)

// Bitwarden Ingestion DTOs
@Serializable
data class BitwardenExport(
    val encrypted: Boolean = false,
    val folders: List<BitwardenFolder> = emptyList(),
    val items: List<BitwardenItem> = emptyList()
)

@Serializable
data class BitwardenFolder(
    val id: String? = null,
    val name: String = ""
)

@Serializable
data class BitwardenLogin(
    val username: String? = null,
    val password: String? = null,
    val totp: String? = null,
    val uris: List<BitwardenUri> = emptyList()
)

@Serializable
data class BitwardenUri(
    val uri: String? = null,
    val match: Int? = null
)

@Serializable
data class BitwardenCustomField(
    val name: String = "",
    val value: String? = null,
    val type: Int? = null
)

@Serializable
data class BitwardenItem(
    val id: String? = null,
    val type: Int = 1, // 1: Login, 2: Note
    val name: String = "",
    val notes: String? = null,
    val favorite: Boolean = false,
    val folderId: String? = null,
    val login: BitwardenLogin? = null,
    val fields: List<BitwardenCustomField> = emptyList()
)

class VaultBackupEngine(
    private val database: ShellGuardDatabase,
    private val deviceVault: EncryptedDeviceVault
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    companion object {
        const val SHELLGUARD_BACKUP_FORMAT = "shellguard-vault-backup-v1"
        const val DEFAULT_PBKDF2_ITERATIONS = 600_000
        private const val BACKUP_AAD = "shellguard_backup:json"
    }

    /**
     * Inspects raw backup content and identifies the format.
     */
    fun detectBackupFormat(raw: String): BackupFormatType {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return BackupFormatType.UNKNOWN
        }

        if (trimmed.contains("\"$SHELLGUARD_BACKUP_FORMAT\"")) {
            return BackupFormatType.SHELLGUARD_ENCRYPTED
        }

        if (trimmed.contains("\"pearls\"") && trimmed.contains("\"ownerUuid\"")) {
            return BackupFormatType.SHELLGUARD_PLAIN
        }

        if (trimmed.contains("\"items\"")) {
            if (trimmed.contains("\"encrypted\": true") || trimmed.contains("\"encrypted\":true")) {
                return BackupFormatType.BITWARDEN_ENCRYPTED
            }
            return BackupFormatType.BITWARDEN_JSON
        }

        return BackupFormatType.UNKNOWN
    }

    /**
     * Exports the user's active vault into an encrypted envelope or plaintext JSON.
     */
    suspend fun exportVault(
        ownerUuid: String,
        protectionMode: BackupProtectionMode,
        customPassphrase: String? = null,
        activeClawKey: String? = null
    ): Result<ExportResult> = runCatching {
        val shellKey = deviceVault.getInMemoryShellKey()
            ?: throw IllegalStateException("Active session shell key not found in memory.")

        // 1. Gather all active local items
        val activePearls = database.vaultPearlDao().getAllActivePearls(ownerUuid)
        val activeNotes = database.secureNoteDao().getAllActiveNotes(ownerUuid)
        val activeKeys = database.sshKeyDao().getAllActiveKeys(ownerUuid)

        // 2. Decrypt items into sovereign backup items
        val backupItems = mutableListOf<BackupVaultItem>()

        activePearls.forEach { pearl ->
            val decryptedSecret = try {
                ShellCryptionEngine.decryptField(
                    pearl.secret,
                    shellKey,
                    ShellCryptionEngine.AadNamespace.pearlSecret(pearl.id)
                )
            } catch (t: Throwable) {
                ""
            }

            val decryptedTotp = if (pearl.totpSecret.isNotBlank() && ShellCryptionEngine.isEncryptedEnvelope(pearl.totpSecret)) {
                try {
                    ShellCryptionEngine.decryptField(
                        pearl.totpSecret,
                        shellKey,
                        ShellCryptionEngine.AadNamespace.pearlTotp(pearl.id)
                    )
                } catch (t: Throwable) {
                    ""
                }
            } else {
                pearl.totpSecret
            }

            val decryptedCustom = if (pearl.customFields.isNotBlank() && ShellCryptionEngine.isEncryptedEnvelope(pearl.customFields)) {
                try {
                    ShellCryptionEngine.decryptField(
                        pearl.customFields,
                        shellKey,
                        ShellCryptionEngine.AadNamespace.pearlCustomFields(pearl.id)
                    )
                } catch (t: Throwable) {
                    pearl.customFields
                }
            } else {
                pearl.customFields
            }

            backupItems.add(BackupVaultItem(
                id = pearl.id,
                type = pearl.type.ifBlank { "password" },
                title = pearl.title,
                secret = decryptedSecret,
                username = pearl.username,
                url = pearl.url,
                category = pearl.category,
                notes = pearl.notes,
                totp_secret = decryptedTotp,
                custom_fields = decryptedCustom,
                tags = pearl.tags,
                uris = pearl.uris,
                attachments = pearl.attachments,
                reprompt = pearl.reprompt,
                created_at = pearl.createdAt
            ))
        }

        activeNotes.forEach { note ->
            val decryptedContent = try {
                ShellCryptionEngine.decryptField(
                    note.content,
                    shellKey,
                    ShellCryptionEngine.AadNamespace.secureNoteContent(note.id)
                )
            } catch (t: Throwable) {
                ""
            }

            val decryptedCustom = if (note.customFields.isNotBlank() && ShellCryptionEngine.isEncryptedEnvelope(note.customFields)) {
                try {
                    ShellCryptionEngine.decryptField(
                        note.customFields,
                        shellKey,
                        ShellCryptionEngine.AadNamespace.secureNoteCustomFields(note.id)
                    )
                } catch (t: Throwable) {
                    note.customFields
                }
            } else {
                note.customFields
            }

            backupItems.add(BackupVaultItem(
                id = note.id,
                type = "note",
                title = note.title,
                secret = decryptedContent,
                notes = decryptedContent,
                category = note.category,
                custom_fields = decryptedCustom,
                tags = note.tags,
                attachments = note.attachments,
                reprompt = note.reprompt,
                created_at = note.createdAt
            ))
        }

        activeKeys.forEach { key ->
            val decryptedPrivate = try {
                ShellCryptionEngine.decryptField(
                    key.keyValue,
                    shellKey,
                    ShellCryptionEngine.AadNamespace.sshKeyPrivate(key.id)
                )
            } catch (t: Throwable) {
                ""
            }

            val decryptedCustom = if (key.customFields.isNotBlank() && ShellCryptionEngine.isEncryptedEnvelope(key.customFields)) {
                try {
                    ShellCryptionEngine.decryptField(
                        key.customFields,
                        shellKey,
                        ShellCryptionEngine.AadNamespace.sshKeyCustomFields(key.id)
                    )
                } catch (t: Throwable) {
                    key.customFields
                }
            } else {
                key.customFields
            }

            backupItems.add(BackupVaultItem(
                id = key.id,
                type = "key",
                title = key.title,
                secret = decryptedPrivate,
                username = key.username,
                category = key.category,
                custom_fields = decryptedCustom,
                tags = key.tags,
                reprompt = key.reprompt,
                created_at = key.createdAt
            ))
        }

        val payload = VaultBackupPayload(
            app = "ShellGuard Vault Backup",
            version = "1.0",
            exportedAt = java.time.Instant.now().toString(),
            ownerUuid = ownerUuid,
            itemCount = backupItems.size,
            items = backupItems
        )

        val plaintextJson = json.encodeToString(payload)
        val checksumSha256 = sha256Hex(plaintextJson.toByteArray(StandardCharsets.UTF_8))

        if (protectionMode == BackupProtectionMode.PLAINTEXT) {
            return@runCatching ExportResult(
                jsonString = plaintextJson,
                isEncrypted = false,
                protectionMode = BackupProtectionMode.PLAINTEXT,
                pearlsCount = activePearls.size,
                notesCount = activeNotes.size,
                sshKeysCount = activeKeys.size,
                checksumSha256 = checksumSha256
            )
        }

        // Derive key and encrypt envelope
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val aad = BACKUP_AAD.toByteArray(StandardCharsets.UTF_8)

        val (derivedKey, kdfName, kdfIterations) = when (protectionMode) {
            BackupProtectionMode.ACTIVE_KEY -> {
                val keyMaterial = if (!activeClawKey.isNullOrBlank()) {
                    activeClawKey.trim().toByteArray(StandardCharsets.UTF_8)
                } else {
                    shellKey
                }
                val info = SHELLGUARD_BACKUP_FORMAT.toByteArray(StandardCharsets.UTF_8)
                val key = ShellCryptionEngine.hkdf(
                    ikm = keyMaterial,
                    salt = salt,
                    info = info,
                    length = 32
                )
                Triple(key, "hkdf", null)
            }
            BackupProtectionMode.CUSTOM_PASSPHRASE -> {
                require(!customPassphrase.isNullOrBlank()) { "Passphrase must not be blank for CUSTOM_PASSPHRASE mode." }
                val key = pbkdf2Sha256(customPassphrase.trim(), salt, DEFAULT_PBKDF2_ITERATIONS, 32)
                Triple(key, "pbkdf2", DEFAULT_PBKDF2_ITERATIONS)
            }
            BackupProtectionMode.PLAINTEXT -> error("Handled above")
        }

        val ciphertext = ShellCryptionEngine.aesGcmEncryptRaw(
            key = derivedKey,
            iv = iv,
            plaintext = plaintextJson.toByteArray(StandardCharsets.UTF_8),
            aad = aad
        )

        val envelope = ShellGuardBackupEnvelope(
            v = 1,
            version = 1,
            format = SHELLGUARD_BACKUP_FORMAT,
            kind = "json",
            kdf = kdfName,
            kdfIterations = kdfIterations,
            createdAt = System.currentTimeMillis(),
            salt = ShellCryptionEngine.base64Encode(salt),
            iv = ShellCryptionEngine.base64Encode(iv),
            payload = ShellCryptionEngine.base64Encode(ciphertext),
            checksumSha256 = checksumSha256
        )

        ExportResult(
            jsonString = json.encodeToString(envelope),
            isEncrypted = true,
            protectionMode = protectionMode,
            pearlsCount = activePearls.size,
            notesCount = activeNotes.size,
            sshKeysCount = activeKeys.size,
            checksumSha256 = checksumSha256
        )
    }

    /**
     * Decrypts a ShellGuard encrypted backup envelope into a VaultBackupPayload.
     */
    fun decryptBackupEnvelope(envelopeJson: String, secretKey: String): Result<VaultBackupPayload> = runCatching {
        val envelope = json.decodeFromString<ShellGuardBackupEnvelope>(envelopeJson)
        require(envelope.format == SHELLGUARD_BACKUP_FORMAT) { "Unrecognized backup format: ${envelope.format}" }

        val salt = ShellCryptionEngine.base64Decode(envelope.salt)
        val iv = ShellCryptionEngine.base64Decode(envelope.iv)
        val ciphertext = ShellCryptionEngine.base64Decode(envelope.payload)
        val aad = BACKUP_AAD.toByteArray(StandardCharsets.UTF_8)

        val derivedKey = if (envelope.kdf == "hkdf" || (envelope.kdf.isBlank() && secretKey.trim().startsWith("hu-"))) {
            val keyMaterial = if (secretKey.isNotBlank()) {
                secretKey.trim().toByteArray(StandardCharsets.UTF_8)
            } else {
                deviceVault.getInMemoryShellKey()
                    ?: throw IllegalArgumentException("No secret key provided and no active session key found.")
            }
            val info = SHELLGUARD_BACKUP_FORMAT.toByteArray(StandardCharsets.UTF_8)
            ShellCryptionEngine.hkdf(
                ikm = keyMaterial,
                salt = salt,
                info = info,
                length = 32
            )
        } else {
            val iterations = envelope.kdfIterations ?: DEFAULT_PBKDF2_ITERATIONS
            pbkdf2Sha256(secretKey.trim(), salt, iterations, 32)
        }

        val decryptedBytes = try {
            ShellCryptionEngine.aesGcmDecryptRaw(derivedKey, iv, ciphertext, aad)
        } catch (t: Throwable) {
            throw SecurityException("Decryption failed: Incorrect key or passphrase.")
        }

        // Verify SHA-256 Checksum
        if (envelope.checksumSha256.isNotBlank()) {
            val calculatedSha = sha256Hex(decryptedBytes)
            if (!calculatedSha.equals(envelope.checksumSha256, ignoreCase = true)) {
                throw SecurityException("Backup integrity error: Checksum mismatch.")
            }
        }

        val plaintextJson = String(decryptedBytes, StandardCharsets.UTF_8)
        json.decodeFromString<VaultBackupPayload>(plaintextJson)
    }

    /**
     * Ingests a Decrypted VaultBackupPayload into the local Room database,
     * encrypting every field with the active session's ShellKey.
     */
    suspend fun importPayload(payload: VaultBackupPayload, targetOwnerUuid: String): Result<ImportResult> = runCatching {
        val shellKey = deviceVault.getInMemoryShellKey()
            ?: throw IllegalStateException("Active session shell key not found in memory.")

        var importedPearls = 0
        var importedNotes = 0
        var importedKeys = 0

        val pearlsToUpsert = mutableListOf<VaultPearlEntity>()
        val notesToUpsert = mutableListOf<SecureNoteEntity>()
        val keysToUpsert = mutableListOf<SshKeyEntity>()

        payload.allItems().forEach { item ->
            val itemId = UUID.randomUUID().toString()
            when (item.type) {
                "note" -> {
                    val encContent = ShellCryptionEngine.encryptField(
                        item.secret,
                        shellKey,
                        ShellCryptionEngine.AadNamespace.secureNoteContent(itemId)
                    )
                    val encCustom = if (item.resolvedCustomFields.isNotBlank()) {
                        ShellCryptionEngine.encryptField(
                            item.resolvedCustomFields,
                            shellKey,
                            ShellCryptionEngine.AadNamespace.secureNoteCustomFields(itemId)
                        )
                    } else ""

                    importedNotes++
                    notesToUpsert.add(SecureNoteEntity(
                        id = itemId,
                        ownerUuid = targetOwnerUuid,
                        title = item.title.ifBlank { "Untitled Note" },
                        content = encContent,
                        category = item.category,
                        customFields = encCustom,
                        attachments = item.attachments,
                        tags = item.tags,
                        reprompt = item.reprompt,
                        syncState = "PENDING_SYNC",
                        createdAt = item.resolvedCreatedAt,
                        localUpdatedAt = System.currentTimeMillis()
                    ))
                }
                "key" -> {
                    val encPrivate = ShellCryptionEngine.encryptField(
                        item.secret,
                        shellKey,
                        ShellCryptionEngine.AadNamespace.sshKeyPrivate(itemId)
                    )
                    val encCustom = if (item.resolvedCustomFields.isNotBlank()) {
                        ShellCryptionEngine.encryptField(
                            item.resolvedCustomFields,
                            shellKey,
                            ShellCryptionEngine.AadNamespace.sshKeyCustomFields(itemId)
                        )
                    } else ""

                    importedKeys++
                    keysToUpsert.add(SshKeyEntity(
                        id = itemId,
                        ownerUuid = targetOwnerUuid,
                        title = item.title.ifBlank { "Untitled Key" },
                        keyValue = encPrivate,
                        username = item.username,
                        category = item.category,
                        customFields = encCustom,
                        tags = item.tags,
                        reprompt = item.reprompt,
                        syncState = "PENDING_SYNC",
                        createdAt = item.resolvedCreatedAt,
                        localUpdatedAt = System.currentTimeMillis()
                    ))
                }
                else -> {
                    val encSecret = ShellCryptionEngine.encryptField(
                        item.secret,
                        shellKey,
                        ShellCryptionEngine.AadNamespace.pearlSecret(itemId)
                    )
                    val encTotp = if (item.resolvedTotp.isNotBlank()) {
                        ShellCryptionEngine.encryptField(
                            item.resolvedTotp,
                            shellKey,
                            ShellCryptionEngine.AadNamespace.pearlTotp(itemId)
                        )
                    } else ""

                    val encCustom = if (item.resolvedCustomFields.isNotBlank()) {
                        ShellCryptionEngine.encryptField(
                            item.resolvedCustomFields,
                            shellKey,
                            ShellCryptionEngine.AadNamespace.pearlCustomFields(itemId)
                        )
                    } else ""

                    importedPearls++
                    pearlsToUpsert.add(VaultPearlEntity(
                        id = itemId,
                        ownerUuid = targetOwnerUuid,
                        title = item.title.ifBlank { "Untitled Login" },
                        secret = encSecret,
                        username = item.username,
                        url = item.url,
                        type = item.type.ifBlank { "password" },
                        category = item.category,
                        notes = item.notes,
                        totpSecret = encTotp,
                        attachments = item.attachments,
                        customFields = encCustom,
                        tags = item.tags,
                        uris = item.uris,
                        reprompt = item.reprompt,
                        syncState = "PENDING_SYNC",
                        createdAt = item.resolvedCreatedAt,
                        localUpdatedAt = System.currentTimeMillis()
                    ))
                }
            }
        }

        database.vaultPearlDao().upsertAll(pearlsToUpsert)
        database.secureNoteDao().upsertAll(notesToUpsert)
        database.sshKeyDao().upsertAll(keysToUpsert)

        ImportResult(importedPearls, importedNotes, importedKeys)
    }

    /**
     * Parses an unencrypted Bitwarden JSON export and ingests items into Room.
     */
    suspend fun importBitwardenJson(jsonString: String, targetOwnerUuid: String): Result<ImportResult> = runCatching {
        val bwExport = json.decodeFromString<BitwardenExport>(jsonString)
        if (bwExport.encrypted) {
            throw IllegalArgumentException("Encrypted Bitwarden backups are not supported. Please export an unencrypted JSON from Bitwarden.")
        }

        val folderMap = bwExport.folders.associate { (it.id ?: "") to it.name }
        val pearls = mutableListOf<BackupPearlItem>()
        val notes = mutableListOf<BackupNoteItem>()

        for (item in bwExport.items) {
            val category = folderMap[item.folderId.orEmpty()].orEmpty()
            val tags = if (item.favorite) "[\"favorite\"]" else "[]"
            val customFieldsJson = if (item.fields.isNotEmpty()) {
                json.encodeToString(item.fields.map { mapOf("name" to it.name, "value" to (it.value ?: "")) })
            } else ""

            when (item.type) {
                1 -> { // Login
                    val login = item.login
                    val urisJson = if (login != null && login.uris.isNotEmpty()) {
                        json.encodeToString(login.uris.map { mapOf("uri" to (it.uri ?: "")) })
                    } else "[]"

                    pearls.add(
                        BackupPearlItem(
                            id = UUID.randomUUID().toString(),
                            title = item.name.ifBlank { "Bitwarden Login" },
                            secret = login?.password.orEmpty(),
                            username = login?.username.orEmpty(),
                            url = login?.uris?.firstOrNull()?.uri.orEmpty(),
                            category = category,
                            notes = item.notes.orEmpty(),
                            totpSecret = login?.totp.orEmpty(),
                            customFields = customFieldsJson,
                            tags = tags,
                            uris = urisJson
                        )
                    )
                }
                2 -> { // Secure Note
                    notes.add(
                        BackupNoteItem(
                            id = UUID.randomUUID().toString(),
                            title = item.name.ifBlank { "Bitwarden Note" },
                            content = item.notes.orEmpty(),
                            category = category,
                            customFields = customFieldsJson,
                            tags = tags
                        )
                    )
                }
            }
        }

        val payload = VaultBackupPayload(
            ownerUuid = targetOwnerUuid,
            pearls = pearls,
            notes = notes
        )

        importPayload(payload, targetOwnerUuid).getOrThrow()
    }

    private fun pbkdf2Sha256(password: String, salt: ByteArray, iterations: Int, keyLengthBytes: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLengthBytes * 8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
