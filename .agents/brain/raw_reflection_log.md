# Raw Reflection Log

<!-- Fresh entries from ongoing tasks will be recorded here and subsequently consolidated into consolidated_learnings.md -->

---
Date: 2026-09-27
TaskRef: "Bidirectional Sync Reconciliation & Dual Adversarial Hardening (Hotfix 5.3)"

Learnings:
- In Robolectric unit tests without simulated network capabilities, `ConnectivityManager.activeNetwork` is null, causing `ConnectivityMonitor` to default to offline and short-circuit `SyncRepository.syncAll()`. Adding `initialOnlineOverride: Boolean? = null` allows tests to evaluate online sync paths deterministically.
- Using `syncMutex.tryLock()` causes concurrent sync calls (e.g. from background triggers or tests) to silently drop and return `Result.success(Unit)` without performing work. `syncMutex.withLock` guarantees queueing and serialization.
- Server ID re-keying requires re-encrypting ciphertext on the server: if the server assigns an ID different from the client's temporary ID, the server holds ciphertext encrypted with AAD `{domain}:{tempId}`. An immediate `client.update*` with the newly assigned server ID reconciles the AAD binding.
- SQL queries with `IN (:collection)` fail when the collection exceeds SQLite's 999 parameter ceiling. Batch deletion in chunks of 500 (`chunked(500)`) safely evades this limit.

Difficulties:
- Silent fallback in detail getters (`getPearlDetail`, etc.) returned raw JSON ciphertext as cleartext on decryption failure. If edited, this resulted in double-ciphertext corruption. Resolved by failing closed with `Result.failure`.
- Tombstone resurrection: deleting local `PENDING_DELETE` records prior to verifying remote delete responses caused subsequent downstream pulls to re-download the remote item. Resolved by retaining tombstones until remote deletion is confirmed.

Successes:
- Subjecting sync and crypto code to a dual adversarial audit pass (ruthless critic + surgical underground hacker) exposed 7 critical vectors before production deployment.
- Created `SyncReconciliationAdversarialTest.kt` covering all 6 core attack vectors with 100% green verification.

Improvements_Identified_For_Consolidation:
- Fail-closed cryptographic detail retrieval pattern.
- Mutex serialization over non-blocking tryLock for mission-critical sync pipelines.
- Pruning via set difference of local synced IDs vs remote IDs in chunks of 500.
---

---
Date: 2026-10-03
TaskRef: "Jetpack Compose Autofill 2026 API Research"

Learnings:
- Android Compose 1.8.0 fundamentally shifted Autofill implementations, deprecating `AutofillNode` and `LocalAutofillTree` entirely.
- The 2026 modern pattern relies purely on standard semantics: `Modifier.semantics { contentType = ContentType.Username + ContentType.Password }`.
- `LocalAutofillManager.current?.commit()` is required to trigger explicit OS saves on form submission.
- The unified Android `Credential Manager` (via `androidx.credentials:credentials:1.7.0+`) is required for modern Passkey and System Bottom-Sheet support, operating independently but alongside the inline keyboard autofill.

Difficulties:
- Researching API shifts revealed widespread proliferation of legacy tutorials online (using deprecated node-based autofill). Had to cross-reference multiple 2025/2026 blogs to verify standard practice.
- Discovered agent-skill hubs like `chrisbanes/skills` and `awesome-android-agent-skills` lack updated codification of these specific Compose 1.8.0 APIs, meaning we will need to originate the architectural pattern internally for ShellGuard.

Successes:
- Synthesized and cataloged a robust reading list and Checklist artifact for the actual integration phase.
- Correctly parsed the difference between standard OS autofill and the Credential Manager APIs.

Improvements_Identified_For_Consolidation:
- General pattern: Treat standard Compose form autofill (via Semantics) and passkey-enabled Authentication (via Credential Manager) as two distinct architectural stacks in 2026.
---

---
Date: 2026-10-04
TaskRef: "Autofill Inline UX: Context-Aware Locked Suggestions & Add-Item Deep Linking (Hotfix 5.5 / v0.0.0.9)"

Learnings:
- Translucent activity window management: Launching a translucent activity (`AutofillAuthActivity`) from Autofill dataset and finishing with `RESULT_CANCELED` causes the Android OS window manager to bring the underlying browser back to foreground, burying `MainActivity`. Direct `PendingIntent.getActivity` with a deep-link URI reliably brings the app forward.
- Decoupled Compose Lock Layer: Placing authentication routes inside `NavHost` (`navController.navigate("lock")`) destroys in-flight deep-link navigation arguments. Extracting `LockScreen` into a top-level Compose overlay conditional on `isUnlocked` lets `NavHost` retain deep-link routes underneath while shielding content until authentication succeeds.
- Privacy-preserving inline chips: Displaying the recognized site domain with an "Unlock Vault" subtitle in the keyboard inline strip acknowledges vault matching without leaking usernames or account titles while locked.

Difficulties:
- Backstack loss across biometric unlock when deep-linking. Resolved with global overlay architecture.

Successes:
- Seamless deep linking with URI pre-fill tested live on Pixel.
- Clean version bump to `0.0.0.9 (Build 9)` and roadmap synchronization.

Improvements_Identified_For_Consolidation:
- Pattern: Global Compose lock overlay for deep-link preservation across biometric auth.
- Pattern: Context-aware locked inline suggestion presentation.
---
Date: 2026-10-04
TaskRef: "Settings Hub Sub-Phase B: Navigation & Base UI Hub"

Learnings:
- In ViewModel unit testing with coroutines and DataStore, `viewModelScope.launch` jobs dispatch disk I/O onto `Dispatchers.IO` while `runTest` advances virtual time on its own scheduler. If mutation functions return `Unit`, tests spinning on flow filters can trigger 60-second timeouts (`UncompletedCoroutinesError`).
- Returning `Job` from ViewModel mutation functions (`fun updateX(): Job = viewModelScope.launch { ... }`) provides a deterministic handle for tests to `.join()`, ensuring data writes complete before downstream assertions, while Compose UI callers simply ignore the return value.
- Unifying `runTest(testDispatcher)` across all test cases guarantees that `Dispatchers.Main`, `viewModelScope`, and test scopes share the exact same `TestCoroutineScheduler`.
- Dynamic window security: Binding `FLAG_SECURE` reactively to `settings.allowScreenCapture` in `MainActivity` ensures screenshot blocking dynamically toggles without requiring app restarts.

Difficulties:
- 60-second test timeout in `testUpdatePanicWipeCountdownClamped` caused by test virtual time racing ahead of `Dispatchers.IO` DataStore disk writes. Resolved cleanly by returning `Job` and joining the operation.

Successes:
- Designed and verified `SettingsHubScreen` with Reef Modernist cards across 6 categories.
- 10/10 Settings unit tests passing 100% green in 21s.
- Zero state leaks across tests.

Improvements_Identified_For_Consolidation:
- Pattern: Return `Job` from ViewModel coroutine launches to enable deterministic `.join()` in test suites.
- Pattern: Dynamic `FLAG_SECURE` window binding in Compose activities.
---
Date: 2026-10-04
TaskRef: "Settings Hub Sub-Phase C: Appearance & Sync Settings"

Learnings:
- In Jetpack Compose theme architecture, computing `isDarkTheme` from a reactive `AppSettings` flow at the root Activity level and passing it to `ShellGuardTheme` enables whole-app theme mode transitions (System Default, Dark, Light) with zero Activity recreations or flashes.
- Material You Monet dynamic color palette (`dynamicDarkColorScheme` / `dynamicLightColorScheme`) requires gating on `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` (Android 12+); exposing this constraint directly in UI helper text sets clear user expectations.
- Manual synchronization triggers in Compose work best when paired with an immediate loading state (`isSyncing`) on the button itself and dismissible status cards for informational and error outcomes.

Difficulties:
- None encountered; the Job-returning ViewModel architecture established in Sub-Phase B enabled seamless `.join()` operations for all new test cases.

Successes:
- Built `SettingsAppearanceScreen.kt` and `SettingsSyncScreen.kt` with full Reef Modernist polish and 100% reactive state bindings.
- All 13 Settings unit tests (`SettingsViewModelTest` and `SettingsRepositoryTest`) passing 100% green in 4.3 seconds.

Improvements_Identified_For_Consolidation:
- Pattern: Whole-app theme switching via root Compose theme flow observation without Activity recreation.
- Pattern: Clear separation of UI feedback banners (info vs error) with dismissible close buttons.
---
Date: 2026-10-04
TaskRef: "Settings Hub Sub-Phase D: Security & Panic Purge Flow"

Learnings:
- Polar angle conversion in Jetpack Compose: using `atan2(dy, dx)` with `+ 90.0` offset and normalizing negative angles maps touch/drag coordinates cleanly to a 12 o'clock origin clock face. Dividing by 360 degrees and multiplying by 60 seconds yields an intuitive, tactile duration selector for emergency countdowns.
- Emergency UX architecture: high-stakes destructive operations require unambiguous visual hierarchy (pulsing concentric Canvas circles, 68sp monospace typography) paired with explicit abort mechanics (prominent cancel button and BackHandler intercepting physical back navigation).
- Fail-closed purge cascade: when executing irreversible emergency wipes, clearing in-memory keys and session tokens in `EncryptedSharedPreferences` must be guaranteed in a `try-finally` or `catch` block even if SQLite table wipes throw.

Difficulties:
- Build compilation error: `CircularDialPicker.kt` was missing `androidx.compose.foundation.layout.width` import for horizontal button spacers. Detected immediately on test compilation and rectified.

Successes:
- Designed and built `CircularDialPicker.kt`, `SettingsSecurityScreen.kt`, and `PanicPurgeCountdownScreen.kt`.
- Wired destinations to `MainActivity.kt` NavHost.
- Expanded `SettingsViewModelTest` to cover screen capture toggling, clipboard clearing duration, panic countdown clamping, and fail-closed wipe execution.
- All 15 unit tests passing 100% green.

Improvements_Identified_For_Consolidation:
- Pattern: Canvas circular dial picker with polar coordinate touch tracking and range clamping.
---
Date: 2026-10-04
TaskRef: "Settings Hub Sub-Phase E: Backup, Restore & Autofill Prep"

Learnings:
- Dual-mode vault backup protection: to achieve feature and cryptographic parity with the ShellGuard web client, backups must support both `ACTIVE_KEY` derivation (HKDF-SHA256 from sovereign `hu-` master identity key) and `CUSTOM_PASSPHRASE` derivation (PBKDF2WithHmacSHA256 with 600,000 iterations matching web client parameters).
- Format sniffing: inspecting serialized payloads for structural markers (`format`, `version`, `encrypted`) allows clean disambiguation between ShellGuard encrypted v1, ShellGuard plain v1, Bitwarden encrypted JSON, and Bitwarden plain JSON before attempting decryption or ingestion.
- Room Robolectric test threading: calling `clearAllTables()` on a disk-backed singleton database (`getInstance()`) inside JUnit fixtures triggers `IllegalStateException: Cannot access database on the main thread`. Using `ShellGuardDatabase.getInMemoryDatabase(context)` (which configures `.allowMainThreadQueries()`) and `database.close()` provides clean isolation for test suites without main thread query exceptions.

Difficulties:
- Encountered `IllegalStateException` on `clearAllTables()` in `VaultBackupEngineTest.setUp()` / `tearDown()`. Diagnosed via `--stacktrace` and resolved by adopting `getInMemoryDatabase()` with `.allowMainThreadQueries()`.

Successes:
- Built `VaultBackupEngine.kt` supporting dual-mode encryption, plaintext export, format sniffing, SHA-256 payload integrity checksums, and Bitwarden JSON ingestion.
- Built `SettingsBackupScreen.kt`, `SettingsAutofillScreen.kt`, and `SettingsAboutScreen.kt` with full Reef Modernist styling and system integration.
- Wired all destinations into `MainActivity.kt` NavHost.
- All 95 unit tests across the entire repository passing 100% green.

Improvements_Identified_For_Consolidation:
- Pattern: Multi-format backup sniffing and web-parity dual protection key derivation (HKDF vs PBKDF2 600k).
- Pattern: Room in-memory test database fixture configuration to prevent main-thread assertion failures.
---





