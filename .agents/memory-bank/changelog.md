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
- Initialized complete `.agents/memory-bank/` suite (`projectBrief.md`, `productContext.md`, `activeContext.md`, `systemPatterns.md`, `techContext.md`, `progress.md`, `changelog.md`, `decision-log.md`).

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

