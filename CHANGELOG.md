# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - Phase 6: Settings Hub & Backup Engine
### Added
- **Web Parity Backup Engine (`VaultBackupEngine.kt`)**: Refactored the backup export schema to use a unified polymorphic `items` JSON array instead of isolated object lists (`pearls`, `notes`, `sshKeys`), enabling 100% data import/export parity with ShellGuard Web's `ImportExportView`.
- **Sovereign ClawKey Backup Authorization (`SettingsBackupScreen`)**: Connected the `hu-` Sovereign ClawKey UI input field for HKDF-SHA256 active-key backup protection, complete with strict Base62 length validation (`^hu-[0-9a-zA-Z]{64}$`) and a fallback toggle for isolated device-only session key exports.
- **Categorized Settings Hub (`SettingsScreen`)**: Integrated a comprehensive native settings hub powered by `androidx.datastore` spanning Security, Appearance, Sync, Autofill, and Backup operations.
- **Panic Purge Trigger (`PanicPurgeCountdownScreen`)**: Implemented a full-screen emergency vault destruction cascade with a 3-ring pulsing Canvas countdown and hardware back abort.

## [0.0.0.9] - 2026-10-04 (Build 9) — Phase 5: Context-Aware Autofill, Inline Chips & Add-Item Deep Linking
### Added
- **Context-Aware Locked Inline Suggestions (`ShellGuardAutofillService`)**: When the vault is locked and matching pearls exist for the active domain/subdomain, ShellGuard presents the clean domain string inline above the keyboard with a locked shell icon (🔒) and `"Unlock Vault"` subtitle, confirming site recognition without leaking sensitive plaintext titles or usernames.
- **Zero-Match "Add Item" Option Chip**: When 0 matching items exist for the active domain or package, ShellGuard exclusively offers an `"Add Item"` option chip inline.
- **Deep-Linked Item Form Pre-population (`MainActivity`)**: Registered intent-filter for `shellguard://app/form/` deep links; tapping `"Add Item"` launches `MainActivity` with `shellguard://app/form/NEW/PASSWORD/new?url=[encoded_domain]`, pre-filling the target URL and title in `ItemFormViewModel`.
- **Global LockScreen Overlay Architecture (`MainActivity`)**: Lifted `LockScreen` out of `NavHost` into a global top-level Compose overlay, preserving deep-linked backstacks, routes, and pre-populated state across biometric/PIN unlock.
- **Android 14+ Credential Provider Service Baseline (`ShellGuardCredentialProviderService`, `CredentialAuthActivity`)**: Established API 34+ Passkey and Credential Manager dual-stack foundation.

### Fixed
- **Null Boolean HTML Attributes Crash (`AutofillStructureParser`)**: Resolved crash when traversing WebView DOM nodes where boolean HTML attribute value pairs returned null, hardening parser stability on complex web login forms.
- **Autofill Fallback Flash Defect**: Eliminated translucent screen flash and activity drop by replacing generic authentication fallback with direct deep-linked `PendingIntent` execution.

### Changed
- **Test Oracle & Hardware Verification**: All 83 unit and Robolectric tests passing 100% green (`BUILD SUCCESSFUL in 2m 41s`), verified with `./gradlew assembleDebug` and live on-device testing on Google Pixel (`sailfish`) hardware.

## [0.0.0.8] - 2026-10-03 (Build 8) — Hotfix 5.4: Web Interoperability & Secure Note Parity
### Added
- **Structural Envelope Validation (`ShellCryptionEngine.isEncryptedEnvelope`)**: Structural verification of JSON envelopes prior to AES-GCM decryption, preventing crashes on unencrypted payloads.
- **Secure Note Masking & Eye-Beside-Copy Cluster (`ItemDetailScreen`)**: Notes load masked by default with monospace bullet glyphs (`••••••••••••••••••••••••••••••••`) and "Tap or click eye to reveal" prompt, equipped with adjacent Eye toggle and Copy action buttons.
- **Biometric Re-Prompt Gating for Notes**: Enforced `state.reprompt` challenges before revealing or copying sensitive note content.
- **Login Item Notes Quick-Copy**: Added one-tap clipboard copy button to login item notes sections.
- **Redline 8 Ratification (`testOracle.md`)**: Formally codified the Masked Secret & Re-prompt Gating Invariant.

### Fixed
- **Web UI Empty JSON Array Deserialization**: Resolved `Unexpected JSON token at offset 0: Expected start of the object '{'. but had '[' instead` exception when loading items minted in the Web UI where `password_history` or `custom_fields` were stored as unencrypted empty JSON arrays (`"[]"`).
- **Detail Getter & Pull Sanitization (`SyncRepository`)**: Gracefully normalizes raw JSON arrays into empty collections while preserving fail-closed cryptographic boundaries.

### Changed
- **Test Oracle & Hardware Verification**: Full suite of 83 unit and Robolectric tests passing 100% green (`BUILD SUCCESSFUL in 3m 6s`), clean `./gradlew assembleDebug` APK generation, and live verified on physical Google Pixel (`sailfish`) hardware.

## [0.0.0.7] - 2026-09-27 (Build 7) — Hotfix 5.3: Bidirectional Sync Reconciliation & Dual Adversarial Hardening
### Added
- **Bidirectional Sync Reconciliation (`SyncRepository`)**: Complete push, pull, and reconciliation engine syncing local Room mutations (creations, edits, deletions) with the central ShellGuard server.
- **Dual Adversarial Audit Pass**: Subjected sync and crypto engines to ruthless infrastructure critic (`brutal_adversary`) and surgical cryptologist hacker (`spectre_hacker`) audits, uncovering and hardening 7 critical edge cases.
- **Dedicated Adversarial Test Suite (`SyncReconciliationAdversarialTest`)**: 6 comprehensive unit tests asserting against concurrent sync dropping, health probe failure propagation, conflict-aware pull, tombstone retention, serverId re-key re-encryption, and fail-closed crypto retrieval.
- **Synthesized Meta-Rules (`meta-rules.md`)**: Codified 4 durable cross-session architectural rules (`fail-closed-security-boundaries`, `two-phase-reconciliation-invariants`, `testable-platform-abstraction`, `bounded-sqlite-chunking`).
- **Self-Review Checklist v1 (`self-review-checklist.md`)**: 8-point pre-commit verification checklist mapping to the 5 core error taxonomy categories.

### Fixed
- **Mutex Serialization**: Replaced `syncMutex.tryLock()` with `syncMutex.withLock` to serialize sync requests and eliminate silent dropped syncs caused by overlapping probes.
- **Zombie Item Resurrection**: Retained local `PENDING_DELETE` tombstones until remote HTTP 200/204 response is confirmed, preventing un-deleted server records from resurrecting on delta pulls.
- **Downstream Conflict Protection**: Excluded local `PENDING_SYNC` and `PENDING_DELETE` IDs from downstream remote delta upserts to prevent overwriting fresh local edits.
- **SQLite 999 Parameter Limit Evading**: Implemented batch pruning in 500-item chunks (`chunked(500)`) via local vs remote ID set differences in `VaultPearlDao`, `SecureNoteDao`, and `SshKeyDao`.
- **Server ID Re-Key Re-Encryption**: Automatically re-encrypts ciphertext under `{domain}:{serverId}` and pushes an update when the server assigns a new identifier.
- **Fail-Closed Detail Retrieval**: Detail getters (`getPearlDetail`, `getNoteDetail`, `getSshKeyDetail`) fail closed with `Result.failure` on decryption errors, preventing raw JSON ciphertext exposure and double-ciphertext database corruption.
- **Headless Network Mocking**: Added `initialOnlineOverride` and `setOnlineForTesting` in `ConnectivityMonitor` to prevent Robolectric's null network capabilities from falsely short-circuiting sync into offline mode.

### Changed
- **Test Oracle & Hardware Verification**: 18 remote & adversarial unit tests passing (`BUILD SUCCESSFUL`), and clean `./gradlew assembleDebug` APK generation.

## [0.0.0.6] - 2026-09-27 (Build 6) — Phase 5: Android Autofill Framework, AutoSpill Defense & Adversary Remediations
### Added
- **System-Level Autofill Service (`ShellGuardAutofillService`)**: System service extending Android `AutofillService` with `BIND_AUTOFILL_SERVICE` and metadata configuration.
- **Resilient Form Parser (`AutofillStructureParser`)**: 4-tier ranked heuristic detection (hints ➔ HTML attributes ➔ input types ➔ ID/content description heuristics) with 64-level tree recursion ceiling.
- **AutoSpill & WebView Isolation Defense**: Web domain hierarchy propagation in `AutofillStructureParser` ensuring credentials inside WebViews are strictly isolated from hostile native host fields (mitigating Black Hat 2023 AutoSpill CWE-200 / CWE-1021).
- **Domain Matcher & Home Lab Port Isolation (`DomainMatcher`, `UriMatchMode`)**: eTLD+1 multi-part ccTLD extraction (`co.uk`, `com.au`), automatic promotion of `BASE_DOMAIN` to `EXACT` host/port matching for IP addresses/localhost, and strict two-way `androidapp://` package matching.
- **Transparent Biometric Gate (`AutofillAuthActivity`)**: Lightweight `FragmentActivity` gate with `BiometricPrompt` and PIN fallback for locked vaults and Claw Re-Prompt items.
- **Bitwarden-Parity TOTP Auto-Copy**: Automatically copies TOTP verification codes to sensitive clipboard (`EXTRA_IS_SENSITIVE = true`) with a 30s background scrubbing timer.
- **Autofill System Settings Guidance (`AutofillManagerHelper`, `AutofillSettingsDialog`)**: Native provider status inspection (`hasEnabledAutofillServices()`), one-tap deep-link intent (`Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE`), and dialog with dynamic `ON_RESUME` refresh in `VaultDashboardScreen`.
- **Responsive Cancellation & Defensive SaveInfo**: Evaluates `cancellationSignal.isCanceled` across coroutine boundaries and builds `SaveInfo(SAVE_DATA_TYPE_PASSWORD)` for registration/login capture.
- **Adversary Sub-Agent (`.agents/agents/adversary/`)**: 30-year veteran cryptologist sub-agent persona codified for ruthless adversarial audits.

### Fixed
- **Fail-Closed Decryption Security**: Replaced dangerous fallback that emitted raw ciphertext strings (`pearl.secret`) on decryption exceptions with strict fail-closed termination.
- **PendingIntent Collision Immunity**: Bound `PendingIntent` creation to unique data URIs (`shellguard://autofill/pearl/${pearl.id}`) with `FLAG_UPDATE_CURRENT` to prevent 32-bit `hashCode()` collision hijacking.
- **Asymmetric Package Matching Bypass**: Strictly rejected cross-matching between native apps and web URLs in `DomainMatcher`.

### Changed
- **Test Oracle & Build Verification**: 100% green test execution across all suites (`DomainMatcherTest`, `AutofillStructureParserTest`, etc.) and clean APK generation verified via `./gradlew assembleDebug`.

## [0.0.0.5] - 2026-09-27 (Build 5) — Phase 4: TOTP Engine, CameraX Scanner & Biometrics
### Added
- **RFC 6238 TOTP Engine (`TotpEngine`)**: Computes time-based authentication tokens with HMAC-SHA1/256/512, configurable 6 or 8 digits, dynamic truncation, and Steam Guard support.
- **Base32 RFC 4648 Decoder (`Base32Decoder`)**: Robust secret decoding with whitespace and padding tolerance.
- **CameraX ML Kit QR Scanner (`QrScannerScreen`)**: Barcode scanning with custom reticle styling, flashlight toggle, and gallery picker fallback.
- **Password Generator (`PasswordGeneratorSheet`)**: Cryptographic random generator with sliders, character toggles, and passphrases.
- **Hardware KeyStore Biometric Lifecycle**: Background auto-lock timeout manager and `BiometricPrompt` challenge.
- **Reactive Canvas Countdown Ring**: 60fps smooth Canvas countdown arc with dynamic Cyan to Amber to Red color interpolation.

## [0.0.0.4] - 2026-09-26 (Build 4) — Phase 3: Vault Domains, Universal Item Editor & Zero-Knowledge Session Atomicity
### Added
- **Multi-Domain Vault Architecture (`VaultDomainModels`)**: Unified support across Passwords (Pearls), Secure Notes, and SSH Keys with polymorphic detail and editor projections.
- **Bitwarden-Style Custom Fields Engine (`CustomField`)**: Supports 4 field types (`TEXT`, `HIDDEN`, `BOOLEAN`, `LINKED`) with JSON serialization and domain-bound ShellCryption AAD namespaces (`vault_*_custom:{id}`).
- **Defensive Password History Tracking**: Automatic client-side versioning of password changes with ISO timestamps and encrypted history storage (`vault_pearls_history:{id}`).
- **Universal Item Editor (`ItemFormScreen`, `ItemFormViewModel`)**: Single unified create/edit screen with pinned header/footer, domain selector, tags chip builder, custom field creator dialog, and `.imePadding().verticalScroll()` IME protection.
- **Polymorphic Item Detail Views (`ItemDetailScreen`, `ItemDetailViewModel`)**: Rich polymorphic inspection views for passwords, notes, and SSH keys with sensitive clipboard auto-scrubbing (30s timer) and Claw Re-Prompt biometric gates.
- **Zero-Knowledge Session Atomicity & KeyStore Key Persistence (`EncryptedDeviceVault`)**: Persisted derived 32-byte `shellKey` Base64 in Android KeyStore-backed `EncryptedSharedPreferences` (AES-256-GCM), adding dynamic RAM re-hydration to survive Android process terminations and cold restarts. Hardened `hasActiveSession()` to strictly require `getInMemoryShellKey() != null`, eliminating unauthenticated split-brain states.
- **Frictionless Gateway Re-entry (`GatewayViewModel`)**: Pre-filled server parameters (`protocol`, `host`, `port`) from stored server URL when returning to Gateway upon lock/fallback.
- **Accessible Fail-Safe Navigation**: Added high-contrast `Back` and `Retry` actions on item detail error screens to prevent user entrapment.

### Changed
- **Test Oracle & Hardware Verification**: 32/32 tests passing 100% green (`CustomFieldTest`, `SyncRepositoryTest`, `EncryptedDeviceVaultTest`, `ClawCryptoTest`, `ShellCryptionEngineTest`, `RoomDatabaseTest`), and verified live on Google Pixel (`sailfish`, Android 14 LineageOS) with full item decryption, Toggle Visibility password unmasking, and cold-restart survival.

## [0.0.0.3] - 2026-09-26 (Build 3) — Phase 2: Ktor API Client, Bidirectional Sync, Vault Dashboard & IME Hardening
### Added
- **Ktor Network Layer (`ShellGuardClient`)**: High-performance HTTP client powered by the OkHttp engine, supporting `ConnectionSpec.CLEARTEXT` for local home lab servers (Unraid, TrueNAS, LAN IPs) and Tailscale/WireGuard mesh routes, alongside `COMPATIBLE_TLS` and `MODERN_TLS`.
- **Bidirectional Sync Engine (`SyncRepository`)**: Full downstream delta pull across Pearls (passwords), Secure Notes, and SSH Keys with timestamp reconciliation and remote deletion pruning.
- **Unified Reactive Stream**: Merges `vault_pearls`, `vault_secure_notes`, and `vault_ssh_keys` Room flows into a unified `UnifiedVaultItem` stream via `kotlinx.coroutines.flow.combine`.
- **Master-Detail Vault Dashboard (`VaultDashboardScreen`)**: Brand header with server connectivity status badge, instant debounced search bar, horizontal Pod filter chips (`All`, `Passwords`, `Notes`, `SSH Keys`) with dynamic item counters, domain-badged item cards, Bitwarden-model Read-Only offline status banner, and mutation-guarded Floating Action Button.
- **Session Persistence (`EncryptedDeviceVault`)**: Hardware-backed session storage in `EncryptedSharedPreferences` for session tokens, server URLs, owner UUIDs, usernames, and hashed keys with in-memory secret zeroization.
- **Connectivity Monitoring (`ConnectivityMonitor`)**: Active Android `NetworkCallback` listener triggering automated `GET /api/health` probes on network availability, automatically transitioning from `OfflineReadOnly` to `OnlineSynced`.
- **Navigation Flow (`MainActivity`)**: Upgraded to Compose `NavHost` connecting Gateway and Dashboard with clean session locking.

### Fixed
- **Sovereign Key Base62 Parity (Authentication Gate)**: Fixed issue where "Login with Identity File" remained disabled by upgrading `CLAW_KEY_REGEX` in `ClawCrypto` from strictly hexadecimal (`[0-9a-f]`) to Base62 (`[0-9a-zA-Z]`, 67 chars), matching keys generated by the web client and server. Added UUID extraction from uploaded identity files (`uploadedUuid`).
- **Soft Keyboard IME Surface Composition**: Scoped `FLAG_SECURE` in `MainActivity` strictly to release builds (`if (!BuildConfig.DEBUG)`), eliminating a hardware surface composer failure on Adreno 530 GPUs where insecure system IME overlays blacked out the window.
- **Root Scaffold Double Inset Isolation**: Set `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0))` on the root Activity Scaffold to prevent double-subtraction of keyboard height when child screens apply `.imePadding()`.
- **Gateway Focused Auto-Scroll**: Added `LaunchedEffect` auto-scroll in `GatewayScreen` to keep the segmented URL bar smoothly visible above the soft keyboard when focused.

### Changed
- **Test Oracle & Hardware Verification**: 23/23 tests passing 100% green (`ClawCryptoTest`, `ShellCryptionEngineTest`, `RoomDatabaseTest`, `SyncRepositoryTest`), and verified live on physical Google Pixel (LineageOS Android 14) with smooth keyboard typing, visible cursor positioning, active login button, and verified UI tree hierarchy.

## [0.0.0.2] - 2026-09-26 (Build 2) — Phase 1: Cryptographic Engine, Room Storage & Gateway UI
### Added
- **ShellCryption Android Engine (`ShellCryptionEngine`)**: HKDF-SHA-256 key derivation with AES-GCM-256 encryption/decryption across all 10 domain-bound AAD namespaces (`vault_pearls`, `vault_pearls_totp`, `vault_secure_notes`, `vault_ssh_keys`, `vault_secure_attachments`, `totp_backup`).
- **Room Database Bedrock (`ShellGuardDatabase`)**: Room 2.7+ database encrypted at rest via whole-database SQLCipher 4.6.1+ containing 7 entities: `VaultPearlEntity`, `SecureNoteEntity`, `SshKeyEntity`, `SecureAttachmentEntity`, `SyncMetadataEntity`, `AuditLogEntity`, and `AgentKeyEntity`.
- **Reef Modernist Mobile Design System (`ui/theme/`)**: 6 curated bioluminescent theme accents with `ThemeAccent.REEF_DEFAULT` (Reef Pink `#E4048A`), flat 1dp exoskeletal card borders, and spring press physics.
- **Remote Gateway Login (`GatewayScreen`)**: 56dp segmented URL container with protocol selector (`http://` vs `https://`), borderless host input, vertical divider, animated port input (68dp → 105dp), dual-mode pill switch (`Upload File` vs `Paste ClawKey©™`), `.json` identity file dropzone, amber zero-knowledge warning card, and client-side SHA-256 digest hashing.
- **Robolectric Headless Compatibility**: SQLite open helper factory fallback and JVM HMAC KeyStore fallback for fast CI testing.

### Changed
- **Test Oracle & Hardware Verification**: 16/16 unit and Robolectric tests passing green, and deployed `app-debug.apk` to Google Pixel, verifying SQLCipher native library loading, `FLAG_SECURE` window shielding, and Gateway form rendering.

## [0.0.0.1] - 2026-09-25 (Build 1) — Stage 0: Foundational Android Application Scaffold
### Added
- **Application Baseline**: Single-Activity, Single-Module Android project targeting Android 16 (API 36 preview) with min SDK 24.
- **Dependency Stack**: Kotlin 2.2+, Jetpack Compose, Material 3, SQLCipher 4.6.1+, Room 2.7+, Ktor Client, and Navigation Compose.
- **16 KB Memory Page Alignment**: Configured `jniLibs.useLegacyPackaging = false` in `app/build.gradle.kts` for 16 KB ELF segment alignment compliance on Android 15+.
- **AppContainer Lazy DI**: Lightweight, application-scoped thread-safe lazy dependency injection avoiding KSP annotation processor overhead.
- **Network Security Configuration**: Permitted cleartext traffic for local home lab servers in `res/xml/network_security_config.xml`.
- **ProGuard / R8 Hardening**: Codified R8 preservation rules for SQLCipher, Kotlinx Serialization, and Room in `proguard-rules.pro`.

---

<div align="center">
  <sub>Engineered with precision for the ClawStack / ShellGuard ecosystem.</sub>
</div>

[Unreleased]: https://github.com/ClawStackStudios/ShellGuard-Mobile/compare/v0.0.0.9...HEAD
[0.0.0.9]: https://github.com/ClawStackStudios/ShellGuard-Mobile/compare/v0.0.0.8...v0.0.0.9
[0.0.0.8]: https://github.com/ClawStackStudios/ShellGuard-Mobile/compare/v0.0.0.7...v0.0.0.8
[0.0.0.7]: https://github.com/ClawStackStudios/ShellGuard-Mobile/compare/v0.0.0.6...v0.0.0.7
[0.0.0.6]: https://github.com/ClawStackStudios/ShellGuard-Mobile/compare/v0.0.0.5...v0.0.0.6
[0.0.0.5]: https://github.com/ClawStackStudios/ShellGuard-Mobile/compare/v0.0.0.4...v0.0.0.5
[0.0.0.4]: https://github.com/ClawStackStudios/ShellGuard-Mobile/compare/v0.0.0.3...v0.0.0.4
[0.0.0.3]: https://github.com/ClawStackStudios/ShellGuard-Mobile/compare/v0.0.0.2...v0.0.0.3
[0.0.0.2]: https://github.com/ClawStackStudios/ShellGuard-Mobile/compare/v0.0.0.1...v0.0.0.2
[0.0.0.1]: https://github.com/ClawStackStudios/ShellGuard-Mobile/releases/tag/v0.0.0.1
