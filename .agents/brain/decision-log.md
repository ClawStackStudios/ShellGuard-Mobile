# Decision Log

## 2026-09-25 — design parity and blind side-by-side verification
Authored root DESIGN.md establishing Reef Modernist Mobile design tokens, flat 1dp Material 3 cards, 6 dynamic theme accents, and adaptive 3-pane master-detail layout achieving 1:1 visual continuity with ShellGuard Web and TOTP.

## 2026-09-25 — rejection of ai studio enterprise hallucinations
Rejected AI Studio recommendations for "native obfuscation via ProGuard" (technically impossible on ELF binaries), third-party logging/monitoring SDKs (violates zero-telemetry vault invariant), multi-module Gradle complexity (violates single-module invariant), and SaaS build flavors. Formulated verified R8 preservation rules instead (verification-gates.md §7).

## 2026-09-25 — governance and migration rule reorientation
Reoriented .agents/rules/android-development.md, zero-knowledge-migration.md, and deployment workflows from read-only TOTP companion constraints to full multi-domain vault client, reversing the volatile password purge into polymorphic Room entity ingestion with pre-DAO deduplication.

## 2026-09-25 — bitwarden parity audit and settings hub expansion
Audited ShellGuard Mobile straight up against Bitwarden Android; expanded ui-ux-design-system.md §10 with 6 dedicated Settings sub-screens (Vault Timeout, Timeout Action, Sensitive Clipboard timer, Screen Capture toggle, and Auto-Copy TOTP on Autofill), confirming 98%+ MVP specification coverage.

## 2026-09-25 — stage 0 scaffold and appcontainer lazy di
Scaffolded foundational Android application baseline in chore/stage-0-initial-scaffold; adopted frameworkless AppContainer lazy DI from ShellGuard ecosystem (avoiding KSP annotation processor churn with Kotlin 2.2+ and enabling deterministic RAM zeroization), passing testDebugUnitTest and assembleDebug 100% green.

## 2026-09-26 — kotlinx.serialization for cryptographic envelopes
Encountered Android framework mock limitation ('Method put in org.json.JSONObject not mocked') during host JVM test execution for ShellCryptionEngine. Switched envelope schema to pure Kotlin @Serializable data class ShellCryptionEnvelope, eliminating mock friction and achieving faster, portable serialization across both Android runtime and headless JVM tests.

## 2026-09-26 — gateway brand parity & animated segmented url bar
Forensically aligned Remote Login form with ShellGuard Web and TOTP companion UI patterns. Engineered a unified 56dp segmented URL container with animated port input, auto-parsing on paste, dual file/paste toggles, and JSON key extraction with zero-knowledge warning disclosure.

## 2026-09-26 — unified multi-domain reactive stream
Combined VaultPearl, SecureNote, and SshKey Room flows in SyncRepository using kotlinx.coroutines.flow.combine to project polymorphic records into UnifiedVaultItem for the VaultDashboard. Enabled instant sub-16ms search and Pod filtering while isolating domain-specific Room table schemas.

## 2026-09-26 — base62 sovereign key validation parity
Discovered ShellGuard master identity keys use Base62 alphanumeric encoding (`hu-[0-9a-zA-Z]{64}`), not strictly lowercase hexadecimal. Found by inspecting live Pixel device state where a valid identity file (`shellguard_identity_xxzioimibiexx.json`) was loaded but the login button remained disabled due to overly restrictive `[0-9a-f]` regex in `ClawCrypto`. Upgraded `CLAW_KEY_REGEX` and `GatewayViewModel` to Base62 and extracted UUID directly.

## 2026-09-26 — debug flag_secure scoping & ime inset resolution
Diagnosed black screen and cursor invisibility during soft keyboard IME input on physical Pixel. Traced to unconditional `FLAG_SECURE` triggering Adreno 530 compositor blackout over system IME window, combined with `Scaffold` double-subtracting IME padding. Scoped `FLAG_SECURE` to `!BuildConfig.DEBUG` (matching ShellGuard-TOTP pattern) and set `contentWindowInsets = WindowInsets(0, 0, 0, 0)` on root `Scaffold`, verifying smooth typing and visual cursor retention.

## 2026-09-26 — robolectric sdk 36 ceiling in headless ci
Encountered UnsupportedOperationException from DefaultSdkProvider during headless GitHub Actions CI test run due to targetSdk = 36. Capped Robolectric to Android 14 via app/src/test/resources/robolectric.properties with sdk=34 and @Config(sdk = [34]) on test classes, greening the test gate and enabling successful release signing.

## 2026-09-26 — polymorphic item editor & encrypted custom fields
Architected universal `ItemFormScreen` and polymorphic `ItemDetailScreen` supporting Passwords, Notes, and SSH Keys. Wrapped custom fields (`TEXT`, `HIDDEN`, `BOOLEAN`, `LINKED`) and password history into domain-specific HKDF AAD encryption (`vault_*_custom`, `vault_pearls_history`) with sensitive clipboard auto-scrubbing and offline mutation guards.

## 2026-09-26 — session key persistence & zero-knowledge active session invariant
Diagnosed "Error Loading Item: Vault locked or shellKey missing" on physical Pixel when opening vault items across app restarts. Traced to `inMemoryShellKey` living exclusively in volatile RAM while `hasActiveSession()` checked only persisted tokens, allowing the dashboard to open without the decryption key. Persisted the derived `shellKey` in hardware KeyStore-backed `EncryptedSharedPreferences` (see `android-development.md` §3 E), hardened `hasActiveSession()` to strictly require a valid shellKey, and verified seamless decryption and cold-restart persistence on hardware.

## 2026-09-27 — splash theme actionbar suppression & camera totp verification
Resolved rogue native ActionBar overlapping Compose TopAppBar by invoking `installSplashScreen()` in `MainActivity.onCreate()` and declaring `windowActionBar=false` and `windowNoTitle=true` in `themes.xml`. Verified live on physical Google Pixel hardware across CameraX QR scanner, item creation with TOTP secret, and 30s countdown Canvas ring with Cyan to Amber color interpolation.

## 2026-09-27 — specialized sub-agent fleet and opsec rebase scrub
Converted Google Jules templates to native Android specialists (Bolt, Palette, Sentinel, Scribe), codified dynamic orchestration topologies, exempted markdown docs from test runs (mandating builds for VitePress docs), and rebase-scrubbed local TOOLS.md to eliminate physical hardware serial leakage while preserving the file locally.

## 2026-09-27 — system autofill framework and home lab port isolation
Delivered system-level Android AutofillService and transparent AutofillAuthActivity biometric gate with Bitwarden-parity TOTP clipboard auto-copy. Addressed home lab port cross-talk by automatically promoting BASE_DOMAIN matching to EXACT host+port matching whenever the target is an IP or localhost (see `deep_plan.md`), and guarded AssistStructure traversal against deep DOM recursion with a 64-level depth ceiling.

## 2026-09-27 — dual adversarial audit and fail-closed sync reconciliation
Subjected bidirectional sync and cryptographic retrieval to a dual adversarial audit pass (ruthless critic and surgical hacker). Replaced `tryLock` with serialized `withLock` to eliminate dropped sync calls, retained local tombstones until remote HTTP 200/204 to prevent zombie resurrection, guarded downstream pull against overwriting local pending edits, and converted detail retrieval to fail-closed Result.failure to block double-ciphertext corruption.

## 2026-10-03 — connectivity transition guard & test coroutine lifecycle
Diagnosed ComparisonFailure on CI runner in `SyncReconciliationAdversarialTest.testZombieResurrectionPreventedWhenRemoteDeleteFails`. Traced to `SyncRepository.init` collecting `connectivityMonitor.isOnline` and immediately launching an unshielded `syncAll` before test mocks were armed. Constrained the collector to transition events (`isOnline && !wasOnline`), introduced `cancelScope()`, and enforced clean shared preference and scope teardown across test fixtures.

## 2026-10-03 — structural envelope validation & fail-closed crypto contract
Diagnosed deserialization exception on Web UI items storing unencrypted empty JSON arrays (`"[]"`). Added `ShellCryptionEngine.isEncryptedEnvelope` to gate decryption calls, normalized pull defaults in `SyncRepository`, and enforced strict `IllegalArgumentException` in `decryptField` to maintain fail-closed boundaries (see `testOracle.md` § Redline 7).

## 2026-10-03 — note masking parity and action cluster alignment
Resolved visual and cryptographic inconsistency where opening a Note in Android displayed cleartext unmasked content by default. Implemented masked default rendering with bullet characters, the Eye-beside-Copy header action cluster, and biometric re-prompt gating on reveal/copy (see systemPatterns.md § Vault Domains & Parity).

