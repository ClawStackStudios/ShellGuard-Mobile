# Changelog

All notable changes to the ShellGuard Mobile project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.0.0] - 2026-09-24

### Added
- Complete architectural blueprint and specification suite in `/project/`:
  - `architecture.md`: System role, boundaries, topology, and 10 core invariants.
  - `crypto-and-keystore.md`: ShellCryptionEngine, KeyStore biometrics, AAD registry, `KeyPermanentlyInvalidatedException` recovery state machine, sensitive clipboard masking (`EXTRA_IS_SENSITIVE`), and password history binding.
  - `room-storage-schema.md`: SQLCipher Room database, 7 entities, DAOs, Hybrid File-System Vault, and per-item `reprompt` flag for Claw Re-Prompt.
  - `routes-and-contracts.md`: Complete Ktor client contracts across 4 vault domains.
  - `ui-ux-design-system.md`: Reef Modernist theme tokens, Gateway, Master-Detail UI, Claw Re-Prompt UX, and URI Match Mode selectors.
  - `autofill-service-spec.md`: Android Autofill Framework, Credential Manager, 5 `UriMatchMode` algorithms, and Android 11+ `InlinePresentation` keyboard suggestion chips.
  - `totp-engine-spec.md`: RFC 6238 TOTP, Steam Guard, and CameraX ML Kit scanning.
  - `sync-and-offline-engine-spec.md`: Bitwarden-model Read-Only Offline Vault Caching, `ConnectivityMonitor` NetworkCallback reconnect, and mutation guards.
  - `import-export-and-migration-spec.md`: Bitwarden JSON intake, `.sgvault.bak`, and deduplication.
  - `widgets-and-quick-tiles-spec.md`: Android Quick Settings TileService and Jetpack Compose Glance widgets.
  - `verification-gates.md`: Android verification gates, Robolectric harness, and release grammar.
  - `16kb-page-size-alignment-guide.md`: Android 15/16 16 KB ELF alignment guide.
  - `app-icon-and-splash.md`: Adaptive launcher icon and Android 12+ SplashScreen.
  - `meta-prompt-ai-studio.md`: Master Google AI Studio execution prompt.
- Initialized root `ROADMAP.md` mapping 6 phases and 12 paired tasks for the MVP build.
- Initialized complete `.agents/brain/` suite (`projectBrief.md`, `productContext.md`, `activeContext.md`, `systemPatterns.md`, `techContext.md`, `progress.md`, `changelog.md`, `decision-log.md`).

## [0.0.0.1] - 2026-09-25

### Added
- Root `DESIGN.md` establishing Reef Modernist Mobile design system with flat 1dp Material 3 cards, 6 dynamic theme accents, and adaptive 3-pane master-detail layout achieving 1:1 visual continuity with ShellGuard Web and TOTP.
- Hardened `app/proguard-rules.pro` specification in `verification-gates.md` §7 protecting SQLCipher JNI, Kotlinx Serialization, and Room DAOs.
- Bitwarden parity expansion in `ui-ux-design-system.md` §10: 6 dedicated Settings sub-screens (Vault Timeout options, Timeout Action, Sensitive Clipboard timer, Screen Capture toggle, and Auto-Copy TOTP on Autofill).
- Auto-Copy TOTP on Autofill feature in `autofill-service-spec.md` §3.1.


### Changed
- Reoriented `.agents/rules/android-development.md` and `.agents/AGENTS.md` to full mobile vault client (bidirectional sync, Hybrid File-System vault, Bitwarden offline engine).
- Reoriented `.agents/rules/zero-knowledge-migration.md` and `.agents/workflows/migration-and-ingest.md` to multi-domain polymorphic entity mapping, reversing companion password purge invariant.
- Reoriented `.agents/workflows/play-console-release-workflow.md`, `development-release-cycle.md`, and CI signing skill (`android-headless-signing-ci`) to `com.clawstack.shellguard`.
- Hardened LAN & Tailscale network security configuration (`base-config cleartextTrafficPermitted`) across architecture and Ktor transport specifications.

## [0.0.0.2] - 2026-09-25

### Added
- Stage 0 Android application foundation scaffold:
  - Local git repository initialized on branch `chore/stage-0-initial-scaffold`.
  - Gradle 9.3.1 toolchain, AGP 9.1.1, Kotlin 2.2.10, and sanitized `libs.versions.toml` with zero third-party telemetry SDKs.
  - Android 15/16 16 KB page-size compliance via uncompressed packaging (`packaging.jniLibs.useLegacyPackaging = false`).
  - R8 preservation rules for SQLCipher JNI (`net.zetetic.**`) and Room DAOs.
  - Home lab and Tailscale CGNAT cleartext transport configuration (`res/xml/network_security_config.xml`).
  - OS cloud backup exclusion rules in `res/xml/data_extraction_rules.xml`.
  - `ShellGuardApp.kt` loading native SQLCipher binaries and exposing frameworkless `AppContainer` lazy DI.
  - `MainActivity.kt` with `FLAG_SECURE` window shielding and Reef Modernist Stage 0 baseline UI.
  - `crypto/ClawCrypto.kt` (SHA-256, format validation, HMAC) and `crypto/AndroidKeyStoreHelper.kt` (AES-256-GCM hardware key management).
  - Passing `ClawCryptoTest` unit test suite.

## [0.0.0.3] - 2026-09-26

### Added
- Phase 1: Cryptographic Engine & SQLCipher Room Architecture:
  - `ShellCryptionEngine.kt`: HKDF-SHA-256 key derivation with salt and `info = "clawchives-shellcryption-v1"`, AES-GCM-256 with 10 AAD domain namespaces, and `@Serializable ShellCryptionEnvelope`.
  - Room 2.7+ SQLCipher encrypted database (`ShellGuardDatabase.kt`) with 7 entities (`VaultPearl`, `SecureNote`, `SshKey`, `SecureAttachment`, `SyncMetadata`, `AuditLog`, `AgentKey`) and reactive DAOs.
  - Hybrid File-System Vault architecture protecting against CWE-400 2MB CursorWindow limits on attachments.
  - Authentic ClawStack Gateway UI & Theme Engine (`GatewayScreen.kt`, `GatewayViewModel.kt`) with 100% brand parity against ShellGuard Web & TOTP:
    - 🐚 gradient brand emblem, `"ShellGuard ©™"` title, and subtitle.
    - Unified 56dp segmented URL bar with interactive protocol selector (`http://` vs `https://`), borderless host input, vertical divider, and animated port input.
    - Dual mode toggles (`Upload File` vs `Paste ClawKey©™`).
    - 110dp `.json` identity file dropzone with tap-to-change and remove actions.
    - Amber warning box and Zero-Knowledge Authentication disclaimer card.
    - Password masking with toggleable eye visibility, paste from clipboard, and valid ClawKey format badge.
  - Passing test suites for `ClawCryptoTest`, `ShellCryptionEngineTest`, and `RoomDatabaseTest` (16/16 tests passing green).
  - Verified live deployment and UI rendering on physical Google Pixel (LineageOS ARM64, FLAG_SECURE active).
- Phase 2: Ktor API Client, Bidirectional Sync, Vault Dashboard & IME Hardening:
  - `ShellGuardClient.kt` & `KtorClientProvider.kt`: Direct network layer communicating with self-hosted API, supporting cleartext HTTP for home labs and Tailscale WireGuard mesh routes.
  - `SyncRepository.kt` & `ConnectivityMonitor.kt`: Bidirectional delta synchronization, timestamp conflict resolution, remote deletion pruning, and Bitwarden-model Read-Only offline caching.
  - `VaultDashboardScreen.kt` & `VaultDashboardViewModel.kt`: Interactive Master-Detail Dashboard with real-time search, Pod category filter chips (`[All]`, `[Passwords]`, `[Notes]`, `[SSH Keys]`), item cards, offline status badge, and mutation-guarded FAB.
  - Base62 Sovereign Identity Key Parity: Upgraded `CLAW_KEY_REGEX` to 67-character Base62 (`hu-[0-9a-zA-Z]{64}`) and added identity JSON UUID extraction.
  - Soft Keyboard IME Hardening: Scoped `FLAG_SECURE` to release builds to prevent Adreno GPU blackout over system IME; isolated root `Scaffold` window insets.
  - Robolectric Target SDK 34 Ceiling: Resolved CI test failure against targetSdk 36 via `robolectric.properties` and `@Config(sdk = [34])`.
  - GitHub Actions automated release pipeline (`.github/workflows/release.yml`) with Python 3 keystore decoding and dual artifact packaging (`.aab` and `.apk`).
  - Pre-flight test suite expanded to 23 tests, passing 100% green. Published live GitHub Release `v0.0.0.3`.

## [0.0.0.4] - 2026-09-26 (Build 4) — Phase 3: Vault Domains, Universal Item Editor & Zero-Knowledge Session Atomicity

### Added
- Phase 3: Vault Domains & Universal Item Editor (Tasks 05 & 06):
  - `CustomField.kt` & `CustomFieldSerializer`: Bitwarden-style custom fields (`TEXT`, `HIDDEN`, `BOOLEAN`, `LINKED`), `@Serializable CustomField`, `@Serializable PasswordHistoryEntry`, resilient JSON parsing.
  - `VaultDomainModels.kt`: Decrypted multi-domain models (`PearlDetail`, `SecureNoteDetail`, `SshKeyDetail`).
  - Remote CRUD APIs in `ShellGuardClient.kt` & `ShellResponse.kt` (`createNote`, `updateNote`, `deleteNote`, `createSshKey`, `updateSshKey`, `deleteSshKey`).
  - `SyncRepository.kt`: Multi-domain getters, encrypted savers with domain AAD namespaces (`vault_*_custom`, `vault_pearls_history`), automatic password history versioning on edits, and `deleteItem`.
  - `CustomFieldDisplayRow.kt` & `ClipboardToastPill.kt`: Reusable custom field rows, animated 30s auto-scrubbing toast pill, and sensitive clipboard masking (`EXTRA_IS_SENSITIVE = true`).
  - `ItemDetailScreen.kt` & `ItemDetailViewModel.kt`: Polymorphic detail views for Passwords, Notes, and SSH keys, collapsible password history, and Claw Re-Prompt gate.
  - `ItemFormScreen.kt` & `ItemFormViewModel.kt`: Universal form with pinned header/footer, domain selector, `.imePadding().verticalScroll()` IME protection, tags chip builder, and dynamic Custom Fields builder.
  - Robolectric test suites `CustomFieldTest` and `SyncRepositoryTest`.
- Zero-Knowledge Session Atomicity & Hardware KeyStore Persistence:
  - `EncryptedDeviceVault.kt`: Persisted derived 32-byte `shellKey` Base64 in Android KeyStore-backed `EncryptedSharedPreferences`, adding lazy key re-hydration across cold starts/process terminations.
  - Atomic session validation: `hasActiveSession()` strictly requires `getInMemoryShellKey() != null`, eliminating unauthenticated split-brain states.
  - `GatewayViewModel.kt`: Added `init` hook pre-filling server connection parameters (`protocol`, `host`, `port`) from stored server URL for frictionless re-entry.
  - `EncryptedDeviceVaultTest.kt`: Added 4 Robolectric unit tests for session key persistence, rehydration, and zeroization.
  - Pre-flight test suite expanded to 32 tests, passing 100% green. Verified on physical Google Pixel hardware with password unmasking and cold-restart survival.

## [Unreleased] — Phase 4: Algorithmic TOTP Engine, CameraX Scanner, Password Generator & Biometrics

### Added
- RFC 6238 TOTP Engine & Pure Kotlin RFC 4648 Base32 Decoder:
  - `Base32Decoder.kt`: RFC 4648 Base32 decoding with whitespace, hyphens, and padding sanitization.
  - `TotpEngine.kt`: RFC 6238 TOTP computation supporting HMAC-SHA1/256/512, 6/8 digits, dynamic truncation (RFC 4226 §5.4), and Steam Guard 5-character token generation.
  - `TotpTicker.kt`: Sub-second coroutine Flow emitting `TotpTick(remainingSeconds, progress)` for 60fps countdown animations.
  - `TotpUriParser.kt`: Parser for `otpauth://totp/...` URIs and raw Base32 secret keys.
- CameraX & ML Kit Barcode Scanning:
  - `QrCodeAnalyzer.kt`: ImageAnalysis analyzer binding ML Kit BarcodeScanning to CameraX image proxy with coordinate normalizers.
  - `QrScannerScreen.kt`: Viewfinder with animated pink laser bar, cyan reticle corners, torch toggle, and gallery image picker fallback.
- Password Generator Modal Sheet:
  - `PasswordGenerator.kt`: Cryptographically secure random password generator (sliders for length, character set toggles, ambiguous character exclusion) and Diceware passphrase generator with entropy scoring.
  - `PasswordGeneratorSheet.kt`: ModalBottomSheet with slider controls, regenerate action, and direct injection into `ItemFormScreen`.
- TOTP UI & Sensitive Clipboard Masking:
  - `TotpDisplayCard.kt`: Formatted code display with one-tap copy declaring `ClipDescription.EXTRA_IS_SENSITIVE = true`.
  - `TotpCountdownRing.kt`: Depleting Canvas arc with Cyan -> Amber -> Red color interpolation.
- Biometric Security Lifecycle & Auto-Lock:
  - `BiometricAuthManager.kt`: KeyStore hardware biometric authentication via `BiometricPrompt`.
  - `VaultLockManager.kt`: Background auto-lock timeout manager tracking user inactivity and lifecycle backgrounding.
  - `LockScreen.kt`: Biometric challenge with PIN/Password fallback.
- Agent Framework & Orchestration Fleet:
  - `.agents/agents/`: Converted Google Jules templates into four dedicated native Android engineering sub-agents (Bolt for Performance, Palette for UI/UX, Sentinel for Zero-Knowledge Security, Scribe for Documentation/Memory) mapped to project skills in `.agents/skills/`.
  - `.agents/ORCHESTRATION.md`: Codified dynamic multi-agent interaction topologies (Bundled, Chain, Staggered, Hybrid) with Mermaid diagrams, pre-collapsed delegation protocols, and conflict resolution hierarchies.
  - `.agents/TOOLS.md`: Documented complete Android development toolchain (Gradle wrapper, headless JBR exports, build-tools, physical Pixel wireless ADB, 16 KB ELF alignment verification).
  - `.agents/AGENTS.md`: Integrated Lucas's Core Being token lifespan principles and registered 4 specialized engineering sub-agents.
  - `.agents/rules/android-development.md`: Formally codified splash theme ActionBar suppression and Algorithmic TOTP/CameraX pipeline invariants.



- Agent Cognitive Architecture:
  - **Self vs Environment Split**: Refactored `.agents/brain/` into a root directory for internal agent cognition (Story, Decisions, Dream Logs, Archive) and a `project/` subdirectory for the external world model (Architecture, Tech Stack, Progress).
  - **Neurobiological Cognitive Sub-Processes**: Established `Dreamer` (offline REM hippocampal replay) and `Forgetter` (active molecular dissolution via Rac1/Cofilin) as autonomous sub-agents.
  - **Workflow Alignments**: Injected explicit cognitive mapping into `/memory` (Short-Term Encoding), `/story` (Prefrontal Waking Self), `/wake` (Neocortical Integration), and `/reflect` (Prediction Error Minimization).
  - **Strict Dissolution Boundaries**: Updated `/forget` and Forgetter to require a clean git worktree and a bespoke branch, ensuring that memory dissolution is a deliberate, diff-reviewable, and safe action.

- Brain Architecture & Cross-Repository Portability:
  - **Antigravity Brain Alignment**: Renamed `.agents/memory-bank/` to `.agents/brain/` across the entire codebase to match Google Antigravity harness patterns.
  - **Long-Term Memory Harmonization**: Renamed rule to `long-term-memory.md` and unified references to `decision-log.md` (episodic log) and Long-Term Memory throughout all workflows and agent prompts.
  - **Migration Automation**: Added `.agents/workflows/migrate-to-brain.md` providing scripted, safe cross-repository migration capabilities for other projects.

- Release v0.0.0.5 (Build 5) — Phase 4: TOTP Engine & Biometric Security Lifecycle:
  - **RFC 6238 TOTP Engine**: HMAC-SHA1/256/512 dynamic truncation, 6/8 digits, and Steam Guard support.
  - **Reactive Canvas Arc**: 60fps countdown Canvas timer with Cyan to Amber to Red dynamic interpolation.
  - **CameraX + ML Kit QR Scanner**: Barcode scanning with custom reticle and gallery picker fallback.
  - **Password Generator**: Cryptographic random generator supporting length, character sets, and passphrases.
  - **KeyStore Biometric Lifecycle**: Background auto-lock timeout and hardware biometric authentication challenge.
  - **Splash Theme Parity**: Pre-`super.onCreate()` splash install and ActionBar suppression.

## [0.0.0.6] - 2026-09-27 (Build 6)

### Added
- **System Autofill Framework & Architecture (Phase 5)**:
  - `ShellGuardAutofillService`: System-level AutofillService binding `android.permission.BIND_AUTOFILL_SERVICE` and configuration XML.
  - `AutofillStructureParser`: Resilient 4-tier ranked heuristic parsing of `AssistStructure` with 64-level recursion depth protection.
  - `DomainMatcher` & `UriMatchMode`: eTLD+1 multi-part ccTLD extraction, automatic IP/port home lab isolation, and `androidapp://` package support.
  - `AutofillAuthActivity`: Transparent biometric gate with `BiometricPrompt` and PIN fallback for locked vaults and Claw Re-Prompt items.
  - RemoteViews dropdown suggestions (`autofill_suggestion_item.xml`) and Android 11+ keyboard inline helper (`AutofillInlineHelper.kt`).
  - Bitwarden-parity TOTP auto-copy to sensitive clipboard with 30s background scrubbing timer.
  - Test suites: `DomainMatcherTest` and `AutofillStructureParserTest`.
- **Autofill System Settings Guidance & Smoothing (Phase 5.1)**:
  - `AutofillManagerHelper`: OS-level status queries (`hasEnabledAutofillServices()`) and one-tap deep-link intent generation (`Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE`).
  - `AutofillSettingsDialog`: Reef Modernist dialog with real-time provider status banner and lifecycle observer (`ON_RESUME`) for instant re-checks.
  - Integrated Autofill Settings trigger into `VaultDashboardScreen` top overflow menu.
- **Autofill Security Hardening & AutoSpill Defense (Phase 5.2)**:
  - AutoSpill Defense in `AutofillStructureParser`: Web domain context isolation preventing credential spill from embedded WebViews into hostile native host fields.
  - Multi-stage `CancellationSignal.isCanceled` evaluations across async coroutine boundaries in `ShellGuardAutofillService`.
  - Configured defensive `SaveInfo` generation (`SAVE_DATA_TYPE_PASSWORD`) enabling native form saving prompts on login/registration.
  - Expanded unit tests in `AutofillStructureParserTest` verifying WebView state tracking.

## [0.0.0.7] - 2026-09-27 (Build 7) — Hotfix 5.3: Bidirectional Sync Reconciliation & Dual Adversarial Hardening

### Added
- **Bidirectional Sync Reconciliation (`SyncRepository`)**: Complete push, pull, and reconciliation engine syncing local Room mutations with the central ShellGuard server.
- **Dual Adversarial Audit Pass**: Subjected sync and crypto engines to ruthless critic (`brutal_adversary`) and surgical hacker (`spectre_hacker`) audits, uncovering and hardening 7 critical edge cases.
- **Dedicated Adversarial Test Suite (`SyncReconciliationAdversarialTest`)**: 6 comprehensive unit tests asserting against concurrent sync dropping, health probe failure propagation, conflict-aware pull, tombstone retention, serverId re-key re-encryption, and fail-closed crypto retrieval.
- **Synthesized Meta-Rules (`meta-rules.md`)**: Codified 4 durable cross-session rules (`fail-closed-security-boundaries`, `two-phase-reconciliation-invariants`, `testable-platform-abstraction`, `bounded-sqlite-chunking`).
- **Self-Review Checklist v1 (`self-review-checklist.md`)**: 8-point pre-commit verification checklist.

### Fixed
- **Mutex Serialization**: Replaced `tryLock()` with `withLock` in `SyncRepository` to serialize requests and eliminate dropped syncs.
- **Zombie Item Resurrection**: Retained local tombstones until remote HTTP 200/204 confirmation.
- **Downstream Conflict Protection**: Excluded pending local IDs from downstream remote delta upserts.
- **SQLite 999 Parameter Evading**: Chunked batch pruning in 500-item chunks.
- **Fail-Closed Detail Retrieval**: Detail getters fail closed with `Result.failure`, blocking double-ciphertext corruption.
