---
description: Operational workflow for executing zero-knowledge multi-domain vault migrations, polymorphic schema mapping (Passwords, Notes, SSH, TOTP), pre-DAO deduplication, and persistence routing.
---

# 📥 Multi-Domain Vault Migration & Ingestion Workflow

> **Use When:** Implementing or testing third-party vault imports (Bitwarden Password Manager, Bitwarden Authenticator, Aegis, 2FAS, Google Authenticator) or proprietary backups (`.sgvault.bak`, `.sgtotp.bak`) in **ShellGuard Mobile**.  
> **Pair With:** `.agents/rules/zero-knowledge-migration.md` and `.agents/rules/lobsterized-philosophy.md`.

---

## 🔍 Step 1: Storage Access Framework (SAF) & Format Detection
1. Launch system file picker via `ActivityResultContracts.OpenDocument()`.
2. Inspect JSON root structure to identify format:
   - **Bitwarden Vault**: Contains `items` array with polymorphic item types (`type = 1` Login, `type = 2` Note, `type = 3` Card, `type = 4` Identity, `sshKey`) and `folders` array.
   - **ShellGuard Full Vault (`.sgvault.bak`)**: Canonical encrypted envelope containing all 4 domains (`pearls`, `notes`, `keys`, `attachments`).
   - **ShellGuard TOTP Companion (`.sgtotp.bak`)**: 2FA token bridge envelope.
   - **Aegis / 2FAS / Authenticator**: 2FA export archives (`otpauth://`, Base32, Steam Guard).
3. If encrypted (`encrypted: true`), prompt user for decryption password and derive key via PBKDF2/AES-256-CBC or HKDF in RAM.

---

## 🧹 Step 2: Polymorphic Entity Mapping & Memory Sanitization
1. **Logins & Passwords (`type = 1`)**:
   - Extract title, username, password, URIs (with `UriMatchMode`), TOTP seed, custom fields, and tags.
   - Map into `VaultPearlEntity`.
2. **Secure Notes (`type = 2`)**:
   - Extract title, note content, custom fields, and tags.
   - Map into `SecureNoteEntity`.
3. **SSH Keys**:
   - Extract private key, public key, and passphrase notes.
   - Map into `SshKeyEntity`.
4. **2FA Seeds (Standalone & Embedded)**:
   - Parse `otpauth://totp/...`, raw Base32, or `steam://` URIs (routed to `SteamTotpGenerator`).
   - If embedded in a login, store in `VaultPearlEntity.totpSecret`. If standalone, store as a TOTP Pearl.
5. **Unsupported Items (Cards, Identities)**:
   - Transform into encrypted `SecureNoteEntity` records labeled `[Converted]`, or discard in volatile memory per user preference.
6. Map `folderId` ➔ Pod category (defaulting uncategorized items to `"General"`).

---

## ⚔️ Step 3: Conflict & Pre-DAO Fingerprint Deduplication
1. Generate cryptographic fingerprint (`secret` + `title` + `username`) for each candidate record.
2. Cross-reference fingerprints against existing database entities via `VaultPearlDao`, `SecureNoteDao`, and `SshKeyDao`.
3. Present user with `ImportPreviewDialog.kt`:
   - Display breakdown: total items, count by domain (Pearls, Notes, SSH), and duplicate count.
   - User selects Conflict Policy: `[ Skip Duplicates ]` (default), `[ Overwrite Existing ]`, or `[ Keep Both ]`.

---

## 🔀 Step 4: Dual-Pathway Persistence Routing
1. User selects destination: `[ 📱 Save to Local Vault Only ]` vs `[ ☁️ Save & Sync with Remote Gateway ]`.
2. **Local Pathway**:
   - Batch insert mapped entities into Room SQLCipher with `syncState = "PENDING_PUSH"` (or `"LOCAL"` if disconnected).
3. **Remote Gateway Pathway**:
   - Encrypt each payload via `ShellCryptionEngine` using domain-bound AAD namespaces (`vault_pearls:{id}`, `vault_secure_notes:{id}`, `vault_ssh_keys:{id}`).
   - Push batch upstream via Ktor client `POST /api/vault/bulk-import` or individual entity routes.

---

## 📜 Step 5: Post-Commit Hooks & Verification
1. **Audit Log Hook**: Insert an immutable `AuditLogEntity` event into `security_audit_logs`:
   - `eventType: "IMPORT_SUCCESS"`, `description: "Imported X pearls, Y notes, Z ssh keys (W skipped) ➔ [Destination]"`
2. **Auto-Backup Hook**: If `Automatic Backups` is enabled in `SettingsBackupsScreen.kt`, trigger `BackupManager.triggerAutomaticBackupIfEnabled()`.
3. Verify zero leakage: assert no unencrypted secret strings or temporary files exist on device disk or in memory dumps.
