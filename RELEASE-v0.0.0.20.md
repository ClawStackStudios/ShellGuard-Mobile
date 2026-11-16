# Release Manifest: v0.0.0.20 (Build 20) — 2026-11-15

> **Stage 2 Completion: Sync Bridge Resilience, SAF Import & Ergonomic Form Polish**: versionCode 20 (`versionName = "0.0.0.20"`). Hardens sync deserialization (`owner_uuid` DTO defaults + `ownerUuid` Room fallback), isolates per-domain downstream pulls inside Room transactions, fixes server-ID re-keying and `409 Conflict` mutation edge cases, and upgrades Room schema to `v2` (`MIGRATION_1_2`) with Bitwarden Item Integrity attachment parent linkage (`parent_id`, `parent_type`). Replaces raw JSON clipboard-paste vault import with Android Storage Access Framework (`OpenDocument`) file streaming. Enforces `>= 48.dp` accessible touch targets and tactile haptic feedback across Vault Dashboard, Item Detail, Item Form, TOTP Card, and Settings screens.

## *Stage 2: Mental Flow & Ergonomic Form Polish*

---

## 🚀 The Core Summary

1. **Sync Bridge Resiliency & Mutation Hardening (`ShellResponse.kt`, `SyncRepository.kt`, `VaultDashboardViewModel.kt`, `VaultDashboardScreen.kt`)**:
   - Added `= ""` defaults for `owner_uuid` on `PearlDto`, `SecureNoteDto`, and `SshKeyDto` with `dto.owner_uuid.ifBlank { ownerUuid }` fallback during Room entity mapping.
   - Isolated per-domain downstream pulls (`fetchVault`, `fetchNotes`, `fetchKeys`, `fetchAttachments`) inside `database.withTransaction { ... }`, serialized `deleteItem()` with `syncMutex`, hardened `pushPendingChanges()` server-ID re-keying and `409 Conflict` create recovery, and added `password_history` parity to `CreateVaultItemRequest`.
   - Sanitized `uiState.errorMessage` (CWE-532) and added a retryable/dismissible error banner to `VaultDashboardScreen`.
2. **Attachment Parent Linkage (`SecureAttachmentEntity.kt`, `ShellGuardDatabase.kt`, `ShellGuardClient.kt`)**:
   - Bumped Room database to `version = 2` with `MIGRATION_1_2` adding `parent_id` and `parent_type`, and wired multipart upload + staging support.
3. **Zero-Clipboard SAF Vault Import (`SettingsBackupScreen.kt`, `SettingsViewModel.kt`)**:
   - Removed the raw JSON paste field (`importInputText`) and replaced it with `ActivityResultContracts.OpenDocument` + bounded `InputStream` reading (`importVaultBackupFromStream`) so vault backups never hit the system clipboard.
4. **Ergonomics & Accessibility (`>= 48.dp` Touch Targets & Haptics)**:
   - Upgraded action `IconButton`s, `PodFilter` chips, secondary URL controls, TOTP copy button, and Settings back buttons to `>= 48.dp` touch targets and added `LocalHapticFeedback` on `copyToClipboard` and secret reveal.
5. **SAF Attachment Download-Only Flow (`ItemDetailScreen.kt`, `ItemDetailViewModel.kt`)**:
   - Removed the `View` button and `cacheDir/decrypted_attachments/` plaintext spill; replaced with `ActivityResultContracts.CreateDocument` **Download**-only streaming and non-destructive `Toast` error handling (#24).
