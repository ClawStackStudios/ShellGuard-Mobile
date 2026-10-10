# 📋 Milestone: Full Server Parity — Encrypted Attachments & Multi-URI Bridge

> **Status**: **PHASE 1 COMPLETE (Merged via PR #1)** | **PHASE 2 & 3 ACTIVE**
>
> The foundational Data, Network, and Synchronization layer has been delivered, verified 100% green in CI, and merged into the primary branch. The remaining work has been partitioned into two orthogonal, parallel-ready tasks.

---

## 🏁 Phase 1: Data, Network & Sync Layer (Completed ✅)

- **PR**: #1 (`feat/stage-1-server-parity-attachments-and-multi-uri`)
- **Key Deliverables Merged**:
  1. `AttachmentDto`, `CreateAttachmentResponse`, and attachment models in `ShellResponse.kt`.
  2. Ktor Client streaming endpoints (`fetchAttachments`, `downloadAttachmentFile`, `uploadAttachmentMultipart`, `deleteAttachment`) in `ShellGuardClient.kt`.
  3. Hybrid ciphertext manager (`AttachmentVaultManager.kt`) streaming encrypted files directly to `context.filesDir/vault_attachments/{id}.enc` (CWE-400 CursorWindow defense).
  4. Room metadata DAO (`SecureAttachmentDao.kt`) with reactive Flow queries and pending sync tracking.
  5. Bidirectional sync reconciliation in `SyncRepository.kt` with upstream multipart push, downstream fetch, local disk file cleanup, and attachment staging helpers.
  6. `DomainMatcher.matchesAnyUri` supporting fallback checking across primary and secondary URLs.

---

## 🚀 Active Task Pipeline

The remaining work is divided into two self-contained task files with zero file overlap:

### 1. [Task 1: Multi-URI & Attachment UI/UX + Autofill Hook](task-1-multi-uri-and-attachment-ui.md)
- **Domain**: Jetpack Compose UI & System Autofill
- **Files**:
  - `ItemFormScreen.kt` & `ItemFormViewModel.kt` (Dynamic Multi-URI builder + File picker)
  - `ItemDetailScreen.kt` & `ItemDetailViewModel.kt` (Multi-URI list + Decrypt & open attachment cards)
  - `VaultDashboardScreen.kt` & `VaultDashboardViewModel.kt` (`[Attachments]` category pod chip)
  - `ShellGuardAutofillService.kt` (Multi-URI matching via `DomainMatcher.matchesAnyUri`)

### 2. [Task 2: Core Storage Wiring, Settings Debt & Panic Purge Parity](task-2-settings-debt-and-storage-wiring.md)
- **Domain**: DI, Settings, Manifest & Storage Security
- **Files**:
  - `AppContainer.kt` (`attachmentVaultManager` DI binding)
  - `SettingsViewModel.kt` (Panic Purge physical disk wipe)
  - `SettingsRepository.kt` & `SettingsAutofillScreen.kt` (Default URI Match Mode preference)
  - `AndroidManifest.xml` & `attachment_file_paths.xml` (FileProvider for temporary decrypted cache previews)
  - `VaultBackupEngine.kt` (Preserve `attachments` in `BackupVaultItem`)
