---
name: zero-knowledge-migration
description: Strict security and architectural invariants for third-party vault ingestion (Bitwarden, Aegis, 2FAS, Google Authenticator), multi-domain mapping (Passwords, Notes, SSH, TOTP), memory isolation, fingerprint deduplication, and backup format integrity (.sgvault.bak, .sgtotp.bak).
---

# 🛡️ Rule: Zero-Knowledge Vault Migration & Multi-Domain Ingestion

## Core Mandate
ShellGuard Mobile is a sovereign, zero-knowledge secrets vault client. When ingesting external archives (Bitwarden Password Manager JSON, Bitwarden Authenticator, Aegis, 2FAS, Google Authenticator, `.sgvault.bak`, `.sgtotp.bak`), the application must enforce strict memory isolation, polymorphic multi-domain mapping, deterministic pre-DAO duplicate handling, and dual-pathway persistence routing.

---

## 🔒 1. Zero-Knowledge Memory Isolation & Domain Mapping

Unlike standalone 2FA companions, ShellGuard Mobile is the **full secrets vault**. Ingested records must be polymorphically mapped into their native Room encrypted domain entities:

- **Logins & Credentials**:
  - Ingested as `VaultPearlEntity`.
  - Maps `login.username`, `login.password` (encrypted with AAD `vault_pearls:{id}`), `login.uris` (with configurable `UriMatchMode`), `login.totp` (encrypted with AAD `vault_pearls_totp:{id}`), custom fields, and tags.
- **Secure Notes**:
  - Ingested as `SecureNoteEntity`.
  - Content encrypted with AAD `vault_secure_notes:{id}`, preserving custom fields and tags.
- **SSH Keys**:
  - Ingested as `SshKeyEntity`.
  - Private key encrypted with AAD `vault_ssh_keys:{id}`, extracting username and public key fingerprints.
- **Standalone 2FA Archives (Aegis, 2FAS, Google Authenticator)**:
  - Ingested as `VaultPearlEntity` with type `"totp"`, storing the seed in `totpSecret`.
- **Unsupported Entities (Credit Cards, Identities)**:
  - Safely transformed into encrypted `SecureNoteEntity` records with a `[Converted Card/Identity]` tag, or purged in volatile memory per explicit user import preference.
  - Non-ingested credentials MUST NEVER leak to unencrypted disk caches, temporary files, or logcat outputs.
- **Immediate Stream Release**: Files read via Android Storage Access Framework (SAF) `ContentResolver` must be closed and their in-memory buffers zeroized immediately after entity extraction.

---

## 🎮 2. Steam Guard 2FA Support
- **Alphanumeric Alphabet Parity**: Steam Guard 2FA utilizes a custom 26-character Base32 alphanumeric translation table (`23456789BCDFGHJKMNPQRTVWXY`).
- **Format Routing**: The URI parser (`TotpUriParser`) must detect `steam://` URIs and route them to `SteamTotpGenerator` to generate valid 5-character alphanumeric codes rather than failing numeric modulo validation.

---

## ⚔️ 3. Pre-DAO Fingerprint Deduplication & Conflict Policy
Every batch import flow must calculate a normalized fingerprint (`secret` + `title` + `username`) and apply an explicit conflict resolution policy before database commit:
1. `SKIP_DUPLICATES` *(Default)*: Skips imported items if an exact matching cryptographic fingerprint already exists in the vault, preventing duplicate false negatives.
2. `OVERWRITE_EXISTING`: Updates the existing entity's metadata (category/title/username/notes) while preserving primary keys and sync timestamps.
3. `KEEP_BOTH`: Assigns a fresh UUID and appends a discriminator (e.g. `GitHub (Imported)`).

---

## 🔀 4. Dual-Pathway Persistence Routing
Every import operation must explicitly route sanitized items to the destination selected by the user:
- **Local Vault Pathway**: Batch inserted directly into Room SQLCipher with `syncState = "PENDING_PUSH"` (or `"LOCAL"` if disconnected).
- **Remote Gateway Pathway**: Encrypted with `ShellCryptionEngine` using domain-specific AAD namespaces and pushed upstream to the self-hosted server gateway via `POST /api/vault/bulk-import` or individual entity routes.

---

## 📜 5. Post-Commit Security Hooks
Upon successful completion of any batch migration:
1. **Audit Log Emission**: Immediately append an immutable record to `security_audit_logs` via `AuditLogDao`:
   `[IMPORT_SUCCESS]`: `"Imported X pearls, Y notes, Z ssh keys (W skipped) ➔ Destination: [Local | Remote]"`
2. **Automated Backup Trigger**: If `Automatic Backups` is enabled in settings, invoke `BackupManager` to create an encrypted JSON backup snapshot.

---

## 📦 6. Proprietary Backup Formats (`.sgvault.bak` & `.sgtotp.bak`)
- **Canonical Full Vault Format**: The primary backup envelope for ShellGuard Mobile is `.sgvault.bak` (`format = "sgvault.bak"`), containing AES-256-GCM ShellCrypted payloads for all 4 domains plus SHA-256 integrity checksums.
- **Companion Bridge Format**: Full 1:1 interoperability with the standalone `.sgtotp.bak` (`format = "sgtotp.bak"`) format for seamless bidirectional migration between ShellGuard Mobile and ShellGuard-TOTP.
- **Bitwarden Intake**: Full support for Bitwarden standard unencrypted JSON and encrypted JSON exports.
- **Embedded Metadata**: Backup envelopes must serialize `protectionMode = "PIN" | "PASSWORD"`, `pinLength: Int?`, and `isBiometricEnabled: Boolean` to allow downstream clients to pre-configure appropriate unlock forms.
- **SAF Registration**: Document pickers and SAF launchers must register broad MIME filters: `arrayOf("*/*", "application/octet-stream", "application/json")`.

---

## 🔄 7. Sovereign Key Rotation at Import Time
- **Key Decoupling**: Decrypting an encrypted backup archive unlocks data using the historical backup key ($K_{backup}$).
- **Explicit User Choice**: The UI must present two explicit pathways upon decryption:
  1. `REUSE_SECRET` *(1-Tap Fast Track)*: Uses verified $K_{backup}$ directly to seal the local Android KeyStore vault.
  2. `ROTATE_KEY` *(Key Rotation)*: Generates a new master key ($K_{new}$) from a user-selected PIN or Master Password, encrypting all items under $K_{new}$ and immediately discarding $K_{backup}$ from memory.

---

## 🛡️ 8. BoringSSL / OpenSSL Cipher Error Interception
- **Exception Sanitization**: Low-level C++ BoringSSL or Java AEAD exceptions (`OPENSSL_INTERNAL:BAD_DECRYPT`, `AEADBadTagException`) MUST NEVER be surfaced directly to the user.
- **Multi-Salt Fallback**: Derivation logic must attempt all compatible salts (`ownerUuid`, `"local"`, default) before declaring failure.
- **Actionable User Feedback**: On unrecoverable tag failure, present clear UI feedback: *"Incorrect PIN or Master Password. Please check your secret and try again."*

---

## 📱 9. Dynamic Input Presentation & Physical Storage Advisory
- **Adaptive Keypad Presentation**: When PIN mode is detected in backup metadata, forms must automatically present `KeyboardType.NumberPassword` (numeric numpad) while maintaining a segmented toggle `[ 🔢 PIN Code ] [ 🔑 Password ]`.
- **Zero-Knowledge Physical Safe Storage Notice**: First-launch welcome and vault setup screens MUST prominently display a security notice instructing users to write down and physically secure their PIN or Master Password, explicitly warning that lost zero-knowledge secrets seal the vault forever without recovery backdoors.
