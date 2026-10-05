# Decision Log

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

## 2026-10-03 — jetpack inline slice protocol & credential manager dual-stack
Diagnosed silent keyboard inline autofill failure on Gboard/SwiftKey: traced to raw empty Slice.Builder rejecting IME layout inflation without androidx.autofill.inline.v1.InlineSuggestionUi. Built comprehensive dual-stack specification combining API 26-33 AutofillService inline slices with API 34+ CredentialProviderService bottom sheets, and reinforced locked dataset presentation binding.

## 2026-10-03 — splitting the autofill authentication stack
While researching 2026 Autofill implementation patterns, discovered we must aggressively unlearn legacy `AutofillNode` structures. Confirmed that standard forms and passkey authentication now exist on two entirely separate architectural planes (`Modifier.semantics` vs `Credential Manager`). We will implement both independently rather than forcing one into the other.

## 2026-10-04 — context-aware locked inline chips & add-item deep linking
Transformed the Autofill suggestion pipeline: when locked, rather than hiding accounts or leaking titles, we present matched domain strings with 'Unlock Vault' inline above the keyboard. When 0 matches exist, we only show 'Add Item', which deep links directly to MainActivity's item form with the active URI pre-populated, preserving context via a global LockScreen overlay.

## 2026-10-04 — datastore preferences isolation & test state reset
Encountered test isolation cross-talk in `SettingsRepositoryTest` when tests mutated the singleton Application `context.dataStore` without resetting state between runs. Added explicit `clearAll()` method to `SettingsRepository` and invoked it inside `@Before setUp()`, ensuring pure deterministic state across all test passes.

## 2026-10-04 — job-returning viewmodel mutations & unified test schedulers
Hit 60-second coroutine timeouts in `SettingsViewModelTest` when testing DataStore mutations asynchronously. Returned `Job` from all ViewModel mutation functions, allowing tests to `.join()` before asserting downstream flow state, and bound `runTest(testDispatcher)` directly to `Dispatchers.Main`'s scheduler, reducing test suite time from 80s to 21s green.

## 2026-10-04 — settings appearance & sync sub-screen projection
Designed and built dedicated `SettingsAppearanceScreen` and `SettingsSyncScreen` sub-screens in Reef Modernist styling with inline radio groups, dynamic Monet color toggle, and integrated manual delta sync actions. Bound all options reactively to `SettingsViewModel` and verified 13/13 unit tests 100% green in 4.3s.

## 2026-10-04 — circular dial countdown and fail-closed panic purge cascade
Engineered interactive `CircularDialPicker` with trigonometric angle mapping (5s–60s clamp) and full-screen `PanicPurgeCountdownScreen` with 3 pulsing concentric red Canvas rings. Implemented a 4-step fail-closed destruction cascade in `executePanicPurge` wiping Room tables, clearing EncryptedSharedPreferences session, wiping DataStore preferences, and unlocking vault state, verified green via Robolectric unit tests.

## 2026-10-04 — dual-mode backup parity and test database isolation
Engineered `VaultBackupEngine` supporting web-parity dual protection modes (`ACTIVE_KEY` via HKDF vs `CUSTOM_PASSPHRASE` with PBKDF2-SHA256 600,000 iterations), automatic format sniffing, and Bitwarden JSON ingestion. Resolved Robolectric `IllegalStateException` on main thread Room queries by initializing isolated in-memory test databases (`getInMemoryDatabase`) with explicit `.allowMainThreadQueries()`, passing 95/95 unit tests 100% green.




## 2026-10-04 — polymorphic payload schema and sovereign key alignment
While auditing the backup engine for Web Parity, I identified that the initial implementation exported isolated lists (`pearls`, `notes`, `sshKeys`), whereas the ShellGuard Web counterpart requires a unified polymorphic `items` JSON array. I refactored `VaultBackupPayload` to serialize and deserialize polymorphic `BackupVaultItem` objects using a `type` discriminator. I also replaced the active key string extraction with a dedicated `hu-` Sovereign ClawKey input field enforcing Base62 validation (`^hu-[0-9a-zA-Z]{64}$`), matching HKDF-SHA256 active-key backup protection perfectly with the Web client.
