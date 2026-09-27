# Progress: ShellGuard Mobile

## Current Status: Release v0.0.0.5 (Build 5) Published & Cloud Verified — Ready for Phase 5
All foundational specifications, data schemas, API contracts, sync engines, UI/UX designs, and meta-prompts are written. Stage 0 Android scaffold, Phase 1 (ShellCryption HKDF + Room SQLCipher, Gateway UI & Theme Engine), Phase 2 (Ktor API Client, Bidirectional Delta Sync, Master-Detail Dashboard, Base62 Identity Parity, IME Hardening), Phase 3 (Multi-Domain Vault, Polymorphic Universal Editor, Custom Fields, Password History, and Zero-Knowledge Session Atomicity), and Phase 4 (RFC 6238 TOTP Engine, CameraX ML Kit Scanner, Password Generator Sheet, and Biometric Vault Lifecycle) have been executed, compiled, and verified green with 100% test pass rate (63/63 tests) and released to GitHub with signed AAB & APK assets.


## What Works (Documented, Designed & Scaffolded)
- [x] Complete ecosystem mapping of Web Server and TOTP Companion.
- [x] Full architecture specification with threat model and invariants (`architecture.md`).
- [x] Cryptography & KeyStore specification with 10 AAD namespaces (`crypto-and-keystore.md`).
- [x] Complete Room 2.7+ SQLCipher schema with all 7 entities and DAOs (`room-storage-schema.md`).
- [x] Hybrid File-System Vault architecture preventing 2MB `CursorWindow` crashes on attachments.
- [x] Full Ktor API client and DTOs for all 4 vault domains (`routes-and-contracts.md`).
- [x] Reef Modernist design system, theme tokens, and master-detail layout (`ui-ux-design-system.md`).
- [x] Android Autofill Framework & Credential Manager specification (`autofill-service-spec.md`).
- [x] Configurable URI match detection (`UriMatchMode`: Base Domain, Host, Exact, Starts With, Never).
- [x] Android 11+ Keyboard Inline Suggestion chips (`InlinePresentation`).
- [x] Claw Re-Prompt security guardrail requiring biometric confirmation for designated items.
- [x] Algorithmic TOTP generator & CameraX scanner pipeline (`totp-engine-spec.md`).
- [x] Bitwarden-model Read-Only Offline Vault Caching with `ConnectivityMonitor` and mutation guards (`sync-and-offline-engine-spec.md`).
- [x] Bitwarden intake, `.sgvault.bak`, and deduplication engine (`import-export-and-migration-spec.md`).
- [x] Android verification gates & release protocol (`verification-gates.md`).
- [x] Android 15/16 16 KB memory page size alignment guide (`16kb-page-size-alignment-guide.md`).
- [x] Adaptive icon & Android 12+ splash screen specification (`app-icon-and-splash.md`).
- [x] Quick Settings TileService & Glance AppWidgets specification (`widgets-and-quick-tiles-spec.md`).
- [x] Biometric invalidation recovery state machine & sensitive clipboard masking (`EXTRA_IS_SENSITIVE`).
- [x] Master Project Roadmap (`ROADMAP.md`).
- [x] Multi-stage Google AI Studio meta-prompt (`meta-prompt-ai-studio.md`).
- [x] Definitive root `DESIGN.md` establishing 1:1 visual and interactive continuity against `ShellGuard` and `ShellGuard-TOTP`.
- [x] Project governance & Android development rules (`.agents/rules/android-development.md`) reoriented to full mobile vault client.
- [x] Migration rules & deployment workflows (`zero-knowledge-migration.md`, `migration-and-ingest.md`, `play-console-release-workflow.md`, `development-release-cycle.md`) reoriented to multi-domain vault client.
- [x] **Stage 0 Android Foundation Scaffold**:
  - Local git repository initialized on branch `chore/stage-0-initial-scaffold` with strict `.gitignore`.
  - Gradle 9.3.1 wrapper, AGP 9.1.1, Kotlin 2.2.10, and sanitized `libs.versions.toml`.
  - Android 15/16 16 KB page-size uncompressed native packaging (`useLegacyPackaging = false`).
  - ProGuard/R8 rules preserving SQLCipher JNI bindings and stripping debug logs.
  - Home lab `<base-config cleartextTrafficPermitted="true">` in `network_security_config.xml`.
  - Cloud backup exclusion rules in `data_extraction_rules.xml`.
  - `ShellGuardApp.kt` with SQLCipher native loader and `DefaultAppContainer` lazy DI.
  - `MainActivity.kt` with `FLAG_SECURE` window memory shielding and Stage 0 baseline UI.
  - `ClawCrypto.kt` & `AndroidKeyStoreHelper.kt` with SHA-256, ClawKey format validation, and HMAC tests.
  - Verification gates passing 100%: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.
- [x] **Phase 1: Cryptographic Engine & Local Cache (Tasks 01 & 02)**:
  - `ShellCryptionEngine.kt`: HKDF-SHA-256 key derivation (`info = "clawchives-shellcryption-v1"`), AES-GCM-256 with 10 AAD namespaces, and `@Serializable ShellCryptionEnvelope`.
  - Room 2.7+ SQLCipher Database (`ShellGuardDatabase.kt`) with all 7 entities (`VaultPearl`, `SecureNote`, `SshKey`, `SecureAttachment`, `SyncMetadata`, `AuditLog`, `AgentKey`) and reactive DAOs.
  - Hybrid File-System Vault architecture protecting against CWE-400 CursorWindow limits.
  - `GatewayScreen.kt` & `GatewayViewModel.kt`: Full 1:1 brand parity port with signature segmented URL bar, animated port input, JSON identity file dropzone (`Upload File` vs `Paste ClawKey©™` toggles), amber warning box, Zero-Knowledge security card, and CWE-359 IME hardening.
  - 100% green verification: 16/16 unit tests passing, `app-debug.apk` cleanly generated, and live verified on physical Google Pixel (ARM64 LineageOS, FLAG_SECURE active, UI hierarchy confirmed).
- [x] **Phase 2: Ktor API Client & Bidirectional Sync (Tasks 03 & 04)**:
  - `ShellGuardClient.kt` & `KtorClientProvider.kt`: Network layer with cleartext HTTP (LAN / Tailscale) and self-signed TLS support. Complete API methods for `/api/auth/token`, `/api/vault`, `/api/notes`, `/api/keys`, and `/api/health`.
  - `EncryptedDeviceVault.kt`: Hardware-backed EncryptedSharedPreferences storage for tokens, server URL, owner UUID, and in-memory shell key zeroization.
  - `SyncRepository.kt` & `ConnectivityMonitor.kt`: Automated health probes on reconnect, timestamp conflict reconciliation, remote deletion pruning, and Bitwarden-model Read-Only offline caching.
  - `VaultDashboardScreen.kt` & `VaultDashboardViewModel.kt`: Interactive Master-Detail Dashboard with real-time search, Pod category filter chips (`[All]`, `[Passwords]`, `[Notes]`, `[SSH Keys]`), item cards, offline status badge, and mutation-guarded FAB.
  - `MainActivity.kt`: Upgraded with Compose Navigation routing between Gateway and Dashboard with session persistence.
  - 100% green verification: 23/23 unit tests passing, `app-debug.apk` cleanly generated, and verified live on Google Pixel hardware.
- [x] **Phase 3: Vault Domains & Universal Item Editor (Tasks 05 & 06)**:
  - `CustomField.kt` & `CustomFieldSerializer`: Bitwarden-style custom fields (`TEXT`, `HIDDEN`, `BOOLEAN`, `LINKED`), `@Serializable CustomField`, `@Serializable PasswordHistoryEntry`, resilient JSON parsing.
  - `VaultDomainModels.kt`: Decrypted multi-domain models (`PearlDetail`, `SecureNoteDetail`, `SshKeyDetail`).
  - Remote CRUD APIs in `ShellGuardClient.kt` & `ShellResponse.kt` (`createNote`, `updateNote`, `deleteNote`, `createSshKey`, `updateSshKey`, `deleteSshKey`).
  - `SyncRepository.kt`: Multi-domain getters, encrypted savers with domain AAD namespaces (`vault_*_custom`, `vault_pearls_history`), automatic password history versioning on edits, and `deleteItem`.
  - `CustomFieldDisplayRow.kt` & `ClipboardToastPill.kt`: Reusable custom field rows, animated 30s auto-scrubbing toast pill, and sensitive clipboard masking (`EXTRA_IS_SENSITIVE = true`).
  - `ItemDetailScreen.kt` & `ItemDetailViewModel.kt`: Polymorphic detail views for Passwords, Notes, and SSH keys, collapsible password history, and Claw Re-Prompt gate.
  - `ItemFormScreen.kt` & `ItemFormViewModel.kt`: Universal form with pinned header/footer, domain selector, `.imePadding().verticalScroll()` IME protection, tags chip builder, and dynamic Custom Fields builder.
  - `EncryptedDeviceVault.kt`: Zero-Knowledge Session Atomicity & KeyStore Key Persistence. Persisted derived 32-byte `shellKey` Base64 in Android KeyStore-backed `EncryptedSharedPreferences`, adding lazy RAM re-hydration across cold starts.
  - Atomic session validation: `hasActiveSession()` strictly requires `getInMemoryShellKey() != null`, eliminating unauthenticated split-brain states.
  - `GatewayViewModel.kt`: Pre-filled server parameters (`protocol`, `host`, `port`) from stored URL for frictionless re-entry.
  - Compose Navigation wired in `MainActivity.kt` and `VaultDashboardScreen.kt` connecting Dashboard ➔ Detail ➔ Form.
  - 100% green verification: 32 unit tests passing across all suites (`CustomFieldTest`, `SyncRepositoryTest`, `EncryptedDeviceVaultTest`), `app-debug.apk` cleanly compiled, and live interactive UI flows verified on physical Google Pixel hardware (password decryption, unmasking, cold-restart survival).
- [x] **Phase 4: TOTP Engine & Biometric Security Lifecycle (Tasks 07 & 08)**:
  - `Base32Decoder.kt`: Pure Kotlin RFC 4648 Base32 decoder with sanitization.
  - `TotpEngine.kt`: RFC 6238 TOTP computation (HMAC-SHA1/256/512, 6/8 digits, dynamic truncation RFC 4226 §5.4, Steam Guard 5-char token).
  - `TotpTicker.kt`: Sub-second coroutine Flow emitting `TotpTick(remainingSeconds, progress)` for 60fps animation.
  - `TotpUriParser.kt`: Standard `otpauth://totp/...` URI parser and raw Base32 secret key extractor.
  - `PasswordGenerator.kt` & `PasswordGeneratorSheet.kt`: Cryptographic `SecureRandom` character generator (sliders, character toggles, ambiguity exclusion) and diceware passphrase generator with entropy scoring.
  - `TotpCountdownRing.kt` & `TotpDisplayCard.kt`: Depleting Canvas arc with Cyan -> Amber -> Red color interpolation, live formatted code (`123 456`), and sensitive clipboard copy (`EXTRA_IS_SENSITIVE = true`).
  - `QrCodeAnalyzer.kt` & `QrScannerScreen.kt`: CameraX viewfinder, animated pink scanning laser, cyan reticle corners, torch toggle, and ML Kit gallery picker fallback.
  - `BiometricAuthManager.kt`, `VaultLockManager.kt`, & `LockScreen.kt`: Android KeyStore biometrics (`BiometricPrompt`), background auto-lock timeout manager, and Reef Modernist lock screen.
  - Resolved `MainActivity` splash theme rogue ActionBar overlap with `installSplashScreen()` and `themes.xml` window title suppression.
  - 100% green verification: 63 unit tests passing across all suites (`TotpEngineTest`, `Base32DecoderTest`, `TotpUriParserTest`, `PasswordGeneratorTest`, `VaultLockManagerTest`), `app-debug.apk` compiled in 56s, and verified live on physical Google Pixel hardware (camera scanner overlay, TOTP item creation, live countdown ticker, and color transitions).
- [x] **Agent Framework & Development Guardrails Tightening**:
  - Rebuilt Google Jules templates into four native Android mental sub-processes (Bolt for Performance, Palette for UI/UX, Sentinel for Zero-Knowledge Security, Scribe for Documentation/Memory) mapped to project skills in `.agents/skills/`.
  - Codified dynamic multi-agent orchestration topologies (Bundled, Chain, Staggered, Hybrid) and the Rule of 6 in `ORCHESTRATION.md`.
  - Gitignored local `TOOLS.md` under `.gitignore` with zero git leak, keeping local device hardware configuration intact on disk.
  - Integrated Core Being token lifespan principles in `AGENTS.md`.
  - Codified markdown documentation testing exemption across `development-release-cycle.md`, `cadence-and-lifecycle-prompts.md`, `git-hygiene.md`, and `docs-hygiene.md`.
- [x] **Release v0.0.0.5 (Build 5) — Phase 4 Shipped**:
  - Published signed `shellguard-mobile-v0.0.0.5.aab` and `.apk` via automated GitHub Actions cloud release pipeline.
  - Authored `RELEASE-v0.0.0.5.md` and prepended `<en-US>` release notes to `RELEASE-PLAY.md`.
- [x] **Agent Cognitive Architecture & Remote Release Observability**:
  - Decoupled Self (`brain/`) from Environment (`brain/project/`).
  - Added `/migrate-to-brain` workflow for cross-repository memory architecture portability.
  - Added `/follow-the-build` workflow for non-blocking GitHub Actions release monitoring and asset verification.



- [x] **Phase 5: Android Autofill Framework & Credential Provider**:
  - Implemented `UriMatchMode` and `DomainMatcher` with multi-part ccTLD extraction, automatic IP/port home lab isolation, and `androidapp://` package matching.
  - Implemented `AutofillStructureParser` with 4-tier ranked heuristic detection (hints ➔ HTML attributes ➔ input types ➔ ID/content description heuristics) and 64-level recursion ceiling.
  - Created `autofill_suggestion_item.xml` layout styled with Reef Modernist tokens and `AutofillInlineHelper` for Android 11+ keyboard chips.
  - Implemented `ShellGuardAutofillService` extending `AutofillService` with `BIND_AUTOFILL_SERVICE` and configuration XML.
  - Implemented `AutofillAuthActivity` transparent biometric gate with `BiometricPrompt` and PIN fallback.
  - Implemented Bitwarden-parity TOTP auto-copy to sensitive clipboard with 30s background scrubbing timer.
  - **Phase 5.1 Smoothing**: `AutofillManagerHelper` (OS support & enabled state checks, direct `Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE` deep-link) and `AutofillSettingsDialog` with lifecycle `ON_RESUME` status refresh, accessible via dashboard overflow menu.
  - **Phase 5.2 Hardening & AutoSpill Defense**: Web domain hierarchy propagation in `AutofillStructureParser` isolating WebView forms from hostile native wrappers; responsive `CancellationSignal` checks preventing ANRs/battery waste; defensive `SaveInfo` generation for credential capture.
  - Full test suite passing 100% green (`DomainMatcherTest`, `AutofillStructureParserTest`) and `assembleDebug` APK compilation verified.

## What's Left to Build (Phase 6)
- [ ] Phase 6: Settings, Backup Bridge & Release Hardening (Tasks 11 & 12).
  - Settings Screen & Theme/Security preferences.
  - Multi-format backup engine (`.sgvault.bak`, `.sgtotp.bak`, Bitwarden deduplication).
  - Adaptive launcher icon, Android 12+ SplashScreen, and 16 KB page-size release packaging.



