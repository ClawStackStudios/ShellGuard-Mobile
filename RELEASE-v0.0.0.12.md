# 🐚 ShellGuard Mobile — Release v0.0.0.12 (Build 12)

> **Stage 1 & 1.5: Server Parity (Encrypted Attachments & Multi-URI Bridge) & In-Place Re-Auth**: versionCode 12 (`versionName = "0.0.0.12"`). Delivers full bidirectional server parity for sensitive file attachments and multi-URI login matching, backed by a hybrid file-system vault that streams encrypted payloads off the SQLite main thread. Introduces in-place vault re-authentication via PBKDF2-SHA256 PIN unlock and sovereign Master Key modal, backed by decoupled session key persistence and attempt lockout counters. Hardened with deterministic decrypted cache purging across cold boot, lock, logout, and emergency wipe, biometric Claw Re-Prompt gating on attachment views, native `androidapp://` package launching, and systemic cleartext transport warning banners. Verified with all 121 unit and Robolectric tests passing 100% green and live hardware validation on physical Google Pixel (`sailfish`).

## *Stage 1: Full Server Parity & Stage 1.5: In-Place Vault Re-Authentication*

```text
███████╗██╗   ██╗███████╗██╗     ██╗     ██████╗ ██╗   ██╗ █████╗ ██████╗ ██████╗ 
██╔════╝██║   ██║██╔════╝██║     ██║     ██╔════╝ ██║   ██║██╔══██╗██╔══██╗██╔══██╗
███████╗███████║█████╗   ██║     ██║     ██║  ███╗██║   ██║███████║██████╔╝██║   ██║
╚════██║██╔══██║██╔══╝   ██║     ██║     ██║   ██║██║   ██║██╔══██║██╔══██╗██║   ██║
███████║██║   ██║███████╗███████╗███████╗╚██████╔╝╚██████╔╝██║   ██║██║  ██║██████╔╝
╚══════╝╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝ ╚═════╝  ╚═════╝ ╚═╝   ╚═╝╚═╝  ╚═╝╚═════╝ 
                                                  ~ **ClawStack Mobile Studios©™** ~
```

---

## 🚀 The Core Summary

Welcome to **v0.0.0.12** of **ShellGuard Mobile** — **Stage 1: Server Parity (Encrypted Attachments & Multi-URI Bridge) & Stage 1.5: In-Place Re-Authentication (Build 12)**. This release completes the cryptographic and data-layer bridge between ShellGuard Mobile and the ShellGuard Web Server, bringing full feature parity across all 4 core vault tables (`vault_pearls`, `vault_secure_notes`, `vault_ssh_keys`, and `vault_secure_attachments`):

1. **Hybrid File-System Vault (`AttachmentVaultManager.kt`)**: Implemented private internal storage streaming (`context.filesDir/vault_attachments/{id}.enc`) for sensitive file attachments. Encrypted payloads are streamed directly to disk, keeping multi-megabyte ciphertext out of SQLite rows and permanently eliminating Android 2MB `CursorWindowAllocationException` crashes (CWE-400 defense).
2. **Encrypted Multipart Streaming Network Engine (`ShellGuardClient.kt`)**: Added multipart/form-data chunked streaming upload (`POST /api/attachments`) with real-time byte progress callbacks, 1MB buffered streaming download (`GET /api/attachments/:id/file`), metadata querying (`GET /api/attachments`), and remote record deletion (`DELETE /api/attachments/:id`).
3. **Dynamic Multi-URI Dynamic Editor (`ItemFormScreen.kt`)**: Implemented a dynamic list builder supporting multiple login URLs per password entry (`vault_pearls.uris`), matching Bitwarden and ShellGuard Web. Supports launching in browser, one-tap clipboard copy, and deletion.
4. **Biometric-Gated Attachment Viewer & Share Engine (`ItemDetailScreen.kt`, `ItemDetailViewModel.kt`)**: Designed dedicated attachment cards rendering filename, humanized byte size, extension badges, and tap-to-view/share actions via Android's `FileProvider`. Enforces biometric Claw Re-Prompt challenges before granting access to sensitive attachments.
5. **Native Android App Launcher Scheme (`androidapp://`)**: Seamless package launcher resolving `androidapp://<package_name>` URIs to launch native installed Android applications via `PackageManager.getLaunchIntentForPackage`.
6. **Systemic Cleartext HTTP Transport Warnings (`GatewayScreen.kt`, `SettingsSyncScreen.kt`)**: Prominent amber warning banners informing users of plaintext transport risks when connecting to local home lab servers without TLS armor.
7. **Room Entity Persistence Hotfix (`SyncRepository.kt`)**: Corrected data-drop defect where saving edited Pearls and Notes hardcoded `"[]"` for `attachments` and `uris`, restoring full bidirectional serialization and deserialization across database and network boundaries.
8. **Deterministic Plaintext Cache Purge (`clearDecryptedCache`)**: Enforced deterministic deletion of temporary plaintext files in `context.cacheDir/decrypted_attachments/` across cold start (`ShellGuardApp.onCreate`), user logout (`VaultDashboardViewModel.logout`), and emergency wipe (`SettingsViewModel.executePanicPurge`).
9. **Filename Sanitization & Path Traversal Immunity**: Stripped directory traversal sequences (`..`, `/`, `\`) from attachment filenames before creating files on the Android filesystem.
10. **In-Place Vault Re-Authentication & PBKDF2-SHA256 PIN Unlock (`PinAuthManager.kt`, `LockScreen.kt`)**: Replaced blocking lock state with responsive in-place unlocking supporting Biometrics, 100k-iteration PBKDF2-SHA256 PIN with `PinPadView` (haptic tactile feedback, masked visualizer, 5-attempt rate-limiting lockout), and fallback Master Sovereign Key (`hu-`) intake modal without terminating network sessions, dropping form edits, or resetting navigation.

All **121 unit and Robolectric tests pass 100% green** (`BUILD SUCCESSFUL in 7m 41s`), backed by GitHub Actions cloud CI and verified live on physical Google Pixel (`sailfish`) hardware.

---

## 💎 Key Themes & Highlights

### 📦 1. Hybrid File-System Vault & CWE-400 Defense
* **Storage Offload**: Attachment binary ciphertext is stored in `context.filesDir/vault_attachments/{id}.enc` while Room's `SecureAttachmentDao` indexes lightweight metadata (`id`, `title`, `fileName`, `mimeType`, `sizeBytes`, `localFilePath`).
* **Chunked Streaming**: 64KB bounded buffer streams handle multi-megabyte payloads safely off the UI thread without spiking JVM heap allocation.
* **AAD Cryptographic Binding**: Payloads are sealed with HKDF-SHA256 derived keys bound to the `vault_secure_attachments:{id}` AAD namespace.

### 🌐 2. Multi-URI Matching & App Launcher Parity
* **Flexible Login Routing**: Allows attaching arbitrary secondary URIs to a single login record.
* **Autofill Multi-URI Evaluation**: `DomainMatcher` inspects all configured URIs during autofill matching, matching both primary and secondary web addresses.
* **Native App Deep Launch**: URLs formatted as `androidapp://com.example.app` launch native apps directly from the detail pane.

### 🛡️ 3. Decrypted Cache Scrubbing & Biometric Security
* **Lifecycle Purge Triggers**: Decrypted temporary files written for external viewing intents are actively wiped across 4 critical lifecycle hooks: app cold launch, vault lock, user logout, and emergency Panic Purge.
* **Biometric Interception**: Attachments belonging to items with Claw Re-Prompt enabled cannot be decrypted or shared without passing an interactive biometric or PIN challenge.
* **Emergency Wipe Integration**: Panic Purge triggers full file deletion across `context.filesDir/vault_attachments/` and `context.cacheDir/decrypted_attachments/`.

### 🔑 4. In-Place Re-Authentication & Decoupled Session Persistence
* **Decoupled Session Persistence**: On vault lock, `zeroizeMemory()` purges volatile memory shell keys while hardware KeyStore `EncryptedSharedPreferences` retains the sealed key, enabling instant re-authentication without dropping network credentials.
* **PBKDF2-SHA256 PIN Security**: 100,000 iterations PBKDF2-SHA256 with 16-byte random salt, constant-time `MessageDigest.isEqual` comparison, rate-limiting lockout counter, and custom tactile `PinPadView`.
* **Master Sovereign Key Modal**: When PIN is not set or lockout threshold is reached, users can enter their `hu-` sovereign key directly into an in-line modal to restore access in place.

---

## 🧪 Verification & Test Oracle Parity

* **Full Unit Test Suite**: 121/121 unit and Robolectric tests passing 100% green across all 19 test suites (including `PinAuthManagerTest`).
* **Cloud CI Verification**: GitHub Actions PR #1 Run `38150392350` passed 100% green in 4m 04s on merged commit `3bd4420`.
* **Hardware Verification**: Verified live on physical Google Pixel (`sailfish`) running Android 14:
  - File picker staging and attachment encryption.
  - Multi-URI list row display and launch actions.
  - Room edit persistence across cold restarts.
  - Plaintext decrypted cache cleanup.
  - PIN setup, wrong PIN lockout countdown warning (`"Incorrect PIN. 4 attempts remaining."`), instant in-place unlock (`123456`), and Master Sovereign Key modal in-place unlock.
* **Architecture Compliance**: Android 16 (API 36) `targetSdk`, 16 KB page-size uncompressed native JNI packaging (`useLegacyPackaging = false`), StrongBox AES-256 KeyStore isolation, and SQLCipher 4.6.1+ whole-database encryption.

---

## 📦 Binary Packaging Receipts

- **Application ID**: `com.clawstack.shellguard`
- **Target SDK**: 36 (Android 16 Baklava)
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Version Code**: 12
- **Version Name**: `0.0.0.12`
- **Page Alignment**: 16 KB ELF segment alignment verified (`jniLibs.useLegacyPackaging = false`)
- **Distribution Artifacts**: `app-release.aab` (Google Play Console) + `app-release.apk` (Direct GitHub Release)
