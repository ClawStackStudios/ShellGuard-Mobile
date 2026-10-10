# Decision Log

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
While auditing the backup engine for Web Parity, I identified that the initial implementation exported isolated lists (`pearls`, `notes`, `sshKeys`), whereas the ShellGuard Web counterpart requires a unified polymorphic `items` JSON array. I refactored `VaultBackupPayload` to serialize and deserialize polymorphic `BackupVaultItem` objects using a `type` discriminator, and enforced Base62 `hu-` Sovereign ClawKey validation.

## 2026-10-08 — autofill container hijacking defense and blast-radius co-presence gate
Diagnosed missing username inline chips and single-field password autofill on device: pre-order traversal in `AutofillStructureParser` evaluated Rank 4 substring heuristics on non-input parent containers (`<form id="login-form">`) and password nodes (`id="login_password"`) before child `<input>` elements, stealing `usernameId`. Enforced editable-input gating (including `AutoCompleteTextView` browser URL bar exclusion), mutual exclusion between password and username nodes, a Co-Presence Gate suppressing weak Rank 4/5 heuristics when `passwordId == null` while permitting Case A `"Add Item"` chips on explicit Rank 1–3 two-step email-first login pages (`accounts.google.com`), non-blank username overwrite guards, and Option B masked username + category/tag disambiguation (`testOracle.md` § Redline 10).

## 2026-10-09 — datastore combine initial emission & ci test determinism
Diagnosed `AssertionError` in `SettingsViewModelTest.testTriggerManualSyncWithNoActiveSession` during GitHub Actions CI run `37897075874` (`v0.0.0.11`). Traced to `combine(settingsRepo.settingsFlow, _extraState)` in `SettingsViewModel` blocking `_extraState` emissions until AndroidX `DataStore` finished its asynchronous initial disk read on `Dispatchers.IO`. Seeded `settingsRepo.settingsFlow.onStart { emit(AppSettings()) }` and replaced nested `runTest` in `@Before setUp()` with `runBlocking`, verifying `114/114` unit tests 100% green.

## 2026-10-10 — dual feedback medium & jules pr comment orchestration
Codified dual-medium iteration protocol into global jules-cli skill: Jules natively ingests feedback through either private Web UI chat prompts or public GitHub PR review threads (reacting with 👀 and pushing commits). Standardized an agent triage question ("Do you want a prompt to give to Jules in the Web UI, or should we make a comment on the pull request?"), and established the critical invariant to NEVER tag @jules on GitHub (as that handle pings an unrelated human account, not the bot).

