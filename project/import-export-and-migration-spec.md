# 📦 ShellGuard Mobile — Import, Export & Migration Engine Specification

> **Bitwarden Intake, `.sgtotp.bak` Bridge, `.sgvault.bak` Full Backup & Deduplication Engine**  
> *Targeted for Google AI Studio Android Application Generator.*

---

## 1. Migration Architecture & Supported Formats

ShellGuard Mobile supports seamless data ingestion from major password managers and authenticator apps, plus full-vault bidirectional portability with the ShellGuard ecosystem.

```mermaid
flowchart TD
    subgraph IntakeSurfaces["Input Formats (Intake Engine)"]
        BW["Bitwarden JSON\n(Logins, Notes, Cards, Custom Fields)"]
        SGTOTP[".sgtotp.bak\n(ShellGuard 2FA Backup)"]
        SGVAULT[".sgvault.bak\n(Full Mobile Encrypted Vault)"]
        ONEPASS["1Password CSV / 1PIF"]
        AEGIS["Aegis / 2FAS / Google Auth"]
    end

    subgraph Parser["MultiFormat Intake Engine"]
        SAF[Storage Access Framework URI] --> Detector{Format Detector}
        Detector --> BWParser[Bitwarden Parser]
        Detector --> SgTotpParser[sgtotp.bak HKDF Decryptor]
        Detector --> SgVaultParser[sgvault.bak Full Decryptor]
        Detector --> GenericParser[CSV / Generic Parser]
    end

    subgraph Deduplication["Pre-DAO Deduplication Engine"]
        BWParser --> Dedup[Fingerprint Calculator & Collision Filter]
        SgTotpParser --> Dedup
        SgVaultParser --> Dedup
        GenericParser --> Dedup
    end

    Dedup --> LocalStore[("Encrypted SQLCipher Room DB\n(syncState = PENDING_SYNC)")]

    classDef secure fill:#e2f0d9,stroke:#548235,stroke-width:2px;
    class Dedup,LocalStore secure;
```

---

## 2. Canonical Export Formats

### A. Full Encrypted Vault Backup (`.sgvault.bak`)
The primary backup format for ShellGuard Mobile. Encrypts all domains (Passwords, Notes, SSH Keys, Attachments metadata) into a single AES-GCM-256 envelope derived via HKDF from an export passphrase.

```json
{
  "version": "shellguard-vault-backup-v1",
  "created_at": "2026-09-24T23:00:00Z",
  "owner_uuid": "c983a542-...",
  "protection_mode": "PASSWORD",
  "checksum": "a1b2c3d4e5...",
  "cipher": {
    "v": 1,
    "alg": "AES-GCM-256",
    "iv": "3f9a...",
    "ct": "base64-payload...",
    "aad": "vault_backup:c983a542-..."
  }
}
```

### B. TOTP Companion Bridge (`.sgtotp.bak`)
100% compatible with the `ShellGuard-TOTP` Android app and the Web Server's `ImportExportView`:
- Envelope: `shellguard-totp-backup-v1`
- AAD: `totp_backup:{ownerUuid}`
- Exports login TOTP secrets + standalone verification codes.

---

## 3. Bitwarden JSON Ingestion Engine

Transforms standard Bitwarden vault exports (`items[]`) into ShellGuard domains:

| Bitwarden Field | ShellGuard Entity & Field | Transformation / Encryption |
|:---|:---|:---|
| `type == 1` (Login) | `VaultPearlEntity` | Maps `name` → `title`, `login.username` → `username`, `login.password` → ShellCrypted `secret`, `login.totp` → ShellCrypted `totpSecret`, `login.uris` → `uris`. |
| `type == 2` (Secure Note) | `SecureNoteEntity` | Maps `name` → `title`, `notes` → ShellCrypted `content`. |
| `fields[]` | `customFields` | JSON array of `{name, value, type}` serialized and encrypted into `customFields` blob. |
| `folderId` | `category` / Pod | Mapped from Bitwarden `folders[]` lookup table. |
| `favorite == true` | `tags` | Appends `"favorite"` tag to JSON array. |

---

## 4. Pre-DAO Fingerprint Deduplication

To prevent duplicate entries and UUID false-negatives when re-importing backups:

```kotlin
package com.clawstack.shellguard.data.backup

object DeduplicationEngine {

    /**
     * Computes a normalized invariant fingerprint for a vault pearl.
     */
    fun computePearlFingerprint(title: String, username: String, secret: String): String {
        val cleanTitle = title.trim().lowercase()
        val cleanUser = username.trim().lowercase()
        val cleanSecret = secret.trim().replace(" ", "").replace("-", "")
        return "$cleanTitle|$cleanUser|$cleanSecret"
    }

    /**
     * Filters incoming import items against existing fingerprints.
     */
    fun <T> filterDuplicates(
        incoming: List<T>,
        existingFingerprints: Set<String>,
        fingerprintSelector: (T) -> String
    ): List<T> {
        val seen = existingFingerprints.toMutableSet()
        val uniqueItems = ArrayList<T>()

        for (item in incoming) {
            val fp = fingerprintSelector(item)
            if (!seen.contains(fp)) {
                seen.add(fp)
                uniqueItems.add(item)
            }
        }
        return uniqueItems
    }
}
```

---

## 5. `BackupManager.kt` Implementation

```kotlin
package com.clawstack.shellguard.data.backup

import android.content.Context
import android.net.Uri
import com.clawstack.shellguard.crypto.ClawCrypto
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.data.local.ShellGuardDatabase
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val db: ShellGuardDatabase,
    private val cryptoEngine: ShellCryptionEngine
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /**
     * Exports full encrypted vault backup (.sgvault.bak) to destination URI.
     */
    suspend fun exportVaultBackup(
        context: Context,
        destinationUri: Uri,
        exportPassphrase: String,
        ownerUuid: String
    ): Result<Int> {
        return try {
            val pearls = db.vaultPearlDao().getAllItems(ownerUuid)
            val notes = db.secureNoteDao().getAllItems(ownerUuid)
            val keys = db.sshKeyDao().getAllItems(ownerUuid)

            val rawBackup = VaultBackupPayload(
                pearls = pearls,
                notes = notes,
                sshKeys = keys
            )
            val serializedJson = json.encodeToString(VaultBackupPayload.serializer(), rawBackup)
            val checksum = ClawCrypto.sha256Hex(serializedJson)

            val salt = ownerUuid.toByteArray()
            val exportKey = cryptoEngine.deriveExportKey(exportPassphrase, salt)
            val envelope = cryptoEngine.encryptBytes(
                serializedJson.toByteArray(Charsets.UTF_8),
                exportKey,
                aad = "vault_backup:$ownerUuid"
            )

            val container = VaultBackupContainer(
                version = "shellguard-vault-backup-v1",
                createdAt = java.time.Instant.now().toString(),
                ownerUuid = ownerUuid,
                checksum = checksum,
                cipher = envelope
            )

            context.contentResolver.openOutputStream(destinationUri)?.use { stream ->
                stream.write(json.encodeToString(VaultBackupContainer.serializer(), container).toByteArray(Charsets.UTF_8))
            }

            Result.success(pearls.size + notes.size + keys.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports and restores encrypted vault backup.
     */
    suspend fun importVaultBackup(
        context: Context,
        sourceUri: Uri,
        passphrase: String,
        ownerUuid: String
    ): Result<Int> {
        return try {
            val content = context.contentResolver.openInputStream(sourceUri)?.use {
                it.bufferedReader().readText()
            } ?: throw IllegalArgumentException("Could not read backup file")

            val container = json.decodeFromString(VaultBackupContainer.serializer(), content)
            val salt = container.ownerUuid.toByteArray()
            val exportKey = cryptoEngine.deriveExportKey(passphrase, salt)

            val decryptedBytes = cryptoEngine.decryptBytes(
                container.cipher,
                exportKey,
                aad = "vault_backup:${container.ownerUuid}"
            )
            val decryptedJson = String(decryptedBytes, Charsets.UTF_8)

            // Verify checksum
            val computedChecksum = ClawCrypto.sha256Hex(decryptedJson)
            if (!computedChecksum.equals(container.checksum, ignoreCase = true)) {
                throw SecurityException("Backup integrity checksum verification failed")
            }

            val payload = json.decodeFromString(VaultBackupPayload.serializer(), decryptedJson)

            // Deduplicate and insert
            val existing = db.vaultPearlDao().getAllItems(ownerUuid).map {
                DeduplicationEngine.computePearlFingerprint(it.title, it.username, it.secret)
            }.toSet()

            val uniquePearls = DeduplicationEngine.filterDuplicates(payload.pearls, existing) {
                DeduplicationEngine.computePearlFingerprint(it.title, it.username, it.secret)
            }.map { it.copy(ownerUuid = ownerUuid, syncState = "PENDING_SYNC") }

            db.vaultPearlDao().upsertItems(uniquePearls)
            Result.success(uniquePearls.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```
