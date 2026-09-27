---
roadmap_version: 1.0.0
last_updated: 2026-09-26
current_position: "Phase 4 Complete (Tasks 07 & 08) — Algorithmic TOTP Engine, CameraX Scanner, Password Generator & Biometrics verified green; ready for Phase 5 execution"
statistics:
  description: "Deterministic build roadmap for ShellGuard Mobile (Full Vault Android Client). Engineered strictly in synergistic 2-task phases where Task A delivers core functionality and Task B delivers the corresponding UI/UX component."
  features_completed: "███████░░░ 67%"
  features_in_progress: "░░░░░░░░░░ 0%"
---

# Master Project Roadmap — ShellGuard Mobile (Full Client)

### 🏷️ Work-Driven Versioning Policy: `MAJOR.MINOR.PATCH.REVISION` (`X.Y.Z.N`)
- **Baseline**: `v0.0.0.1 (Build 1)` (Genesis Scaffold: Phases 1–6).
- **Versioning Invariant**: Versions evolve strictly from verified work, never arbitrary targets.
  - **`MAJOR` (`X.0.0.0`)**: 1.0.0 production public release with full vault & autofill.
  - **`MINOR` (`X.Y.0.0`)**: Substantial new subsystems (e.g. Autofill, Biometrics, Multi-Account).
  - **`PATCH` (`X.Y.Z.0`)**: Self-contained 2-task phase deliverables.
  - **`REVISION` (`X.Y.Z.N`)**: Minor patches, hotfixes, or CI adjustments.
  - **`versionCode` (`Build N+1`)**: Monotonically increments (+1) on every Google Play release upload.

---

## 🛡️ Systemic Design Rule
"Build features around security, not security around features." Each Phase contains strictly 2 paired tasks: **Task A (Core Functionality / Security Engine)** followed immediately by **Task B (Corresponding UI Component / Interactive State)**.

---

## Phase 1: Cryptographic Engine & Local Cache [Baseline: v0.0.0.1 (Build 1)]

> Phase Feature Set Overview:
> Delivers mathematical parity with ShellGuard's ShellCryption (HKDF-SHA256 + AES-GCM-256 with all 10 AAD namespaces) and the SQLCipher-encrypted Room database, paired immediately with the Reef Modernist theme engine and the ClawStack Gateway Login screen.

- [x] **Task 01: [Functionality] ShellCryption HKDF Engine & SQLCipher Room Architecture**
  - Implement `ShellCryptionEngine` providing HKDF-SHA256 key derivation (`info = "clawchives-shellcryption-v1"`) and AES-GCM-256 encryption/decryption across all AAD namespaces.
  - Set up AndroidX Room with SQLCipher (`net.zetetic:sqlcipher-android:4.6.1`) initialized in the Application class.
  - Define local Room entities: `VaultPearlEntity`, `SecureNoteEntity`, `SshKeyEntity`, `SecureAttachmentEntity`, `SyncMetadataEntity`, `AuditLogEntity`, and `AgentKeyEntity`.
  - Implement DAOs providing reactive `Flow` observation and delta pruning.
  - Add unit test suite (`ShellCryptionEngineTest.kt` & `RoomDatabaseTest.kt`) proving 100% cryptographic parity and zero plaintext leaks.
  - *Success Criteria*: Unit tests pass 100% green; Room opens with KeyStore passphrase; ShellCryption decrypts sample web client payloads without error.

- [x] **Task 02: [UI Component] Standardized Gateway Login & Dynamic Theme Engine**
  - Implement `ui/theme/Color.kt`, `Theme.kt`, and `Type.kt` matching the **Reef Modernist** design system.
  - Implement `ThemeAccent` enum with 6 curated palettes (`REEF_DEFAULT`, `CYAN_VENT`, `PURPLE_SHELL`, `EMERALD_TRENCH`, `AMBER_FLARE`, `MONOCHROME`) and `LocalShellGuardColors`.
  - Implement `GatewayScreen.kt` & `GatewayViewModel.kt`: faithful 1:1 port of the ClawStack Gateway with protocol/host/port segment bar, key paste / upload file dual mode, and warning card.
  - *Success Criteria*: Gateway renders smoothly with theme switching; validates `hu-` and `api-` keys; stores active session in `EncryptedDeviceVault`.

---

## Phase 2: Ktor API Client & Bidirectional Sync [Baseline: v0.0.0.2 (Build 2)]

> Phase Feature Set Overview:
> Establishes the network layer using Ktor Client with OkHttp engine and cleartext LAN support, paired immediately with the Master-Detail Vault Dashboard, Pod filter chips, and search engine.

- [x] **Task 03: [Functionality] Ktor API Client & Bidirectional Delta Sync Engine**
  - Implement `ShellGuardClient.kt` supporting `/api/auth/token`, `/api/vault`, `/api/notes`, `/api/keys`, and `/api/attachments`.
  - Implement `SyncRepository.kt` managing two-way synchronization: upstream push of `PENDING_SYNC` items, downstream pull of server records, conflict resolution, and pruning.
  - Schedule periodic background sync via `VaultSyncWorker` (`AndroidX WorkManager`).
  - Configure `network_security_config.xml` permitting cleartext HTTP on local LAN and VPN subnets.
  - *Success Criteria*: Client synchronizes items bidirectionally with a running Express 5 server; offline modifications queue and push automatically on reconnection.

- [x] **Task 04: [UI Component] Master-Detail Vault Dashboard & Pod Filters**
  - Implement `VaultDashboardScreen.kt` with master-detail layout (phone collapsible sheet / tablet dual-pane).
  - Search bar with instant unified search across Pearls, Notes, and SSH Keys.
  - Horizontal Pod category filter chips (`[All]`, `[Passwords]`, `[Notes]`, `[SSH Keys]`, dynamic pods).
  - Floating action button (FAB) speed dial for creating items or scanning QR codes.
  - *Success Criteria*: Dashboard displays cached items reactively from Room DB; search filters in sub-16ms frames; category chips group items accurately.

---

## Phase 3: Vault Domains & Universal Item Editor [Baseline: v0.0.0.3 (Build 3)]

> Phase Feature Set Overview:
> Delivers the multi-domain data layer with support for custom fields, tags, and password history, paired immediately with the universal ItemFormScreen and polymorphic detail view renderers.

- [x] **Task 05: [Functionality] Multi-Domain Data Layer & Bitwarden-Style Custom Fields**
  - Implement domain models for `CustomField` (`Text`, `Hidden`, `Checkbox`, `Linked`).
  - Implement JSON serialization and ShellCryption encryption for custom fields with per-item AAD namespaces.
  - Implement password history tracking and tag system.
  - *Success Criteria*: Custom fields roundtrip through encryption without schema pollution; linked fields resolve dynamically.

- [x] **Task 06: [UI Component] Universal ItemFormScreen & Polymorphic Detail Views**
  - Implement `ItemFormScreen.kt` supporting create/edit across Passwords, Secure Notes, and SSH Keys.
  - Dynamic Custom Fields editor allowing users to add, reorder, toggle visibility, and delete fields.
  - Implement `ItemDetailScreen.kt` rendering masked passwords with tap-to-copy, haptic feedback, eye visibility toggle, and note markdown preview.
  - *Success Criteria*: Form validates inputs cleanly; detail screen renders custom fields according to type; passwords mask/unmask securely.

---

## Phase 4: TOTP Engine & Biometric Security Lifecycle [Baseline: v0.0.0.4 (Build 4)]

> Phase Feature Set Overview:
> Delivers the RFC 6238 TOTP computation engine and hardware-backed Android KeyStore biometrics, paired immediately with the password generator, CameraX QR scanner, and Biometric LockScreen.

- [x] **Task 07: [Functionality] Algorithmic TOTP Engine & Hardware KeyStore Biometrics**
  - Implement `TotpEngine.kt` (RFC 6238, HMAC-SHA1/256/512, 6/8 digits, Steam Guard 5-char alphanumeric).
  - Implement `AndroidKeyStoreHelper.kt` managing hardware-backed AES-256-GCM secret keys with `setUserAuthenticationRequired(true)`.
  - Implement `AppLifecycleObserver.kt` enforcing auto-lock timeouts when the app is backgrounded.
  - Apply `FLAG_SECURE` to prevent screen captures and recents thumbnails.
  - *Success Criteria*: TOTP codes match Google Authenticator / ShellGuard web client; biometric prompt unlocks hardware cipher; cold boot locks the vault.

- [x] **Task 08: [UI Component] Password Generator, Countdown Display & CameraX Scanner**
  - Implement `PasswordGeneratorSheet.kt` with length slider, character set toggles, and passphrase mode.
  - Implement `TotpCard.kt` and `TotpCountdownRing.kt` Canvas component with smooth color transitions.
  - Implement `QrScannerScreen.kt` using CameraX and ML Kit Barcode Scanning with gallery QR picker.
  - *Success Criteria*: Generator produces cryptographically random passwords; scanner detects `otpauth://` URIs instantly; countdown ring depletes smoothly.

---

## Phase 5: Android Autofill & Credential Provider [Baseline: v0.0.0.5 (Build 5)]

> Phase Feature Set Overview:
> Delivers system-level Android Autofill Framework and Android 14+ Credential Manager integration, paired immediately with the biometric authorization gate and inline autofill suggestion UI.

- [ ] **Task 09: [Functionality] Autofill Service Architecture & Domain Matcher**
  - Implement `ShellGuardAutofillService` (`AutofillService`) and `ShellGuardCredentialProviderService`.
  - Implement `AutofillStructureParser` traversing view hierarchies to locate username/password fields and web domains.
  - Implement `DomainMatcher` with eTLD+1 extraction for browser and native app matching.
  - *Success Criteria*: Service responds to OS fill requests; accurately matches URLs to vault items; ignores non-login views.

- [ ] **Task 10: [UI Component] Autofill Presentation Views & Biometric Authorization Gate**
  - Implement dropdown and inline suggestion presentation views (`autofill_suggestion_item`).
  - Implement `AutofillAuthActivity`: prompts biometric/PIN unlock before releasing credentials to the calling app when vault is locked.
  - *Success Criteria*: Suggestions display in third-party apps and browsers; tapping a suggestion prompts biometrics; credentials fill securely.

---

## Phase 6: Settings, Backup Bridge & Release Hardening [Baseline: v0.0.1.0 (Build 6) — Milestone 1]

> Phase Feature Set Overview:
> Delivers the categorized settings hub, `.sgvault.bak` full encrypted backup engine, and `.sgtotp.bak` companion bridge, paired immediately with adaptive launcher icons, Android 12+ splash screen, and 16 KB page alignment.

- [ ] **Task 11: [Functionality] Settings Persistence & Multi-Format Backup Engine**
  - Implement `BackupManager.kt` exporting/importing full encrypted backups (`.sgvault.bak`) and TOTP bridges (`.sgtotp.bak`).
  - Implement `DeduplicationEngine` with normalized fingerprinting.
  - Implement `PanicTriggerReceiver` for emergency instant vault purge.
  - *Success Criteria*: Full backups export and restore with SHA-256 checksum verification; duplicate items are skipped; panic wipe purges all databases and keys.

- [ ] **Task 12: [Configuration] Adaptive App Icon, Splash Screen & 16 KB Alignment**
  - Create adaptive launcher icon (`ic_launcher_foreground.xml` with Reef Shield + Pearl Emblem).
  - Configure Android 12+ `SplashScreen` API with static vector drawable.
  - Configure `jniLibs.useLegacyPackaging = false` in `app/build.gradle.kts` for 16 KB page-size alignment.
  - Verify release build gates (`./gradlew testDebugUnitTest assembleDebug`).
  - *Success Criteria*: App icon displays correctly on home screens; splash screen launches cleanly without API <31 inflation crashes; APK passes 16 KB ELF alignment verification.
