# Ratified System Patterns: ShellGuard Mobile

## pattern: hybrid-attachment-filesystem-vault
**weight**: 3 | **last validated**: 2026-09-24 | **first observed**: 2026-09-24

Never store multi-megabyte attachment blobs inside SQLite database rows. Android's SQLite CursorWindow limits query rows to 2MB, causing unavoidable `CursorWindowAllocationException` or `SQLiteBlobTooBigException` crashes on SELECT. Decouple metadata into Room and stream encrypted ciphertext directly to private internal disk files (`filesDir/vault_attachments/{id}.enc`).

**History:**
- 2026-09-24: Audited `SecureAttachmentEntity` against web server 500MB attachment specification; realized inline BLOBs would cause fatal app crashes under Android SQLite CursorWindow limitations; refactored to Hybrid File-System Vault.

**Shaped perspective:** Databases are indexes, not file systems. Mobile relational databases should store structure and searchable pointers; the OS filesystem should store the weight.

---

## pattern: bitwarden-model-readonly-offline-caching
**weight**: 4 | **last validated**: 2026-09-26 | **first observed**: 2026-09-24
**pinned**: false
**status**: hot

When a mobile client is disconnected from its authoritative server, vault operations must be strictly Read-Only. Allow 100% read, search, copy, autofill, and TOTP functionality from the local encrypted cache, but block creation, editing, and deletion in the UI. Automatically probe server health via `NetworkCallback` upon connection restoration to resume online synchronization seamlessly.

**History:**
- 2026-09-24: Evaluated optimistic offline write queues vs read-only offline caching. Validated against Bitwarden's client-server architecture; confirmed that blocking offline mutations eliminates split-brain conflicts and guarantees vault integrity.
- 2026-09-26: Re-validated during Phase 2 Ktor client and SyncRepository implementation; mutation guards and ConnectivityMonitor live-tested on physical Pixel.

**Shaped perspective:** This holds because zero-knowledge encryption prevents the client or server from inspecting plaintext to merge diverged offline diffs intelligently. It would break if client users demanded multi-device offline write capabilities without an active authoritative connection. What it costs to maintain is strict UI gating (disabled FABs, dimmed edit/delete icons) and an automated background health probe to detect network restoration.

---

## pattern: cwe-359-sensitive-clipboard-masking
**weight**: 3 | **last validated**: 2026-09-26 | **first observed**: 2026-09-24
**pinned**: false
**status**: hot

All password, key, and TOTP clipboard operations must declare `ClipDescription.EXTRA_IS_SENSITIVE = true` on Android 13+ (API 33+) to suppress floating system thumbnail previews, paired with an automated 30s/60s background coroutine timer to purge transient secrets from the system pasteboard.

**History:**
- 2026-09-24: Identified CWE-359 clipboard leakage on Android 13+; codified in `crypto-and-keystore.md`.
- 2026-09-25: Audited against Bitwarden Android implementation; added configurable 30s/60s/120s timer settings in `ui-ux-design-system.md` §10.
- 2026-09-26: Enforced across `VaultDashboardScreen` and `SyncRepository` copy actions during live device verification.

**Shaped perspective:** This holds because modern mobile operating systems render persistent visual thumbnail previews of clipboard data that expose sensitive credentials to screen recorders, recents caches, and shoulder surfers. It would break if OEM background process killing abruptly terminates the scrubbing coroutine before the timer completes. What it costs to maintain is managing background lifecycle coroutines and educating users why copied credentials disappear from the clipboard after 30 seconds.

---

## pattern: zero-knowledge-session-atomicity
**weight**: 3 | **last validated**: 2026-09-27 | **first observed**: 2026-09-24
**pinned**: false
**status**: hot

An active mobile vault session must atomically couple transport authorization (`sessionToken`) with cryptographic capability (`shellKey`). `hasActiveSession()` must strictly verify `getInMemoryShellKey() != null`. Derived 32-byte symmetric keys must be persisted at rest in hardware KeyStore-backed `EncryptedSharedPreferences` (AES-256-GCM) with dynamic RAM re-hydration to survive Android process death without user lockout. On lock or logout, both volatile RAM references and persisted KeyStore preferences must be actively zeroized.

**History:**
- 2026-09-24: Formulated in `architecture.md` §4 as a core zero-knowledge invariant.
- 2026-09-26: Diagnosed split-brain failure on physical Pixel where RAM key was lost across restarts while session token survived; persisted `shellKey` in KeyStore `EncryptedSharedPreferences` and hardened `hasActiveSession()`.
- 2026-09-27: Re-validated during Phase 4 biometric lock and background timeout testing on hardware.

**Shaped perspective:** Mobile zero-knowledge architecture cannot assume persistent memory. Because the mobile operating system aggressively reclaims background process memory, separating authentication from decryption capability creates split-brain states where the UI appears unlocked but cannot read data. Persisting the derived symmetric key inside the hardware KeyStore enclave preserves the zero-knowledge guarantee at rest while preventing user disruption across cold restarts.

---

## pattern: cwe-359-ime-protection-and-inset-isolation
**weight**: 3 | **last validated**: 2026-09-27 | **first observed**: 2026-09-24
**pinned**: false
**status**: hot

All cryptographic and secret input fields (passwords, PINs, seeds, keys) must apply `PasswordVisualTransformation()` and `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` to prevent predictive dictionary learning. Interactive form screens must apply `.imePadding().verticalScroll(rememberScrollState())` to prevent the soft keyboard from obscuring inputs, while the root `Scaffold` must configure `contentWindowInsets = WindowInsets(0, 0, 0, 0)` to prevent destructive double-subtraction of IME height. `FLAG_SECURE` window shielding must be scoped strictly to release builds (`!BuildConfig.DEBUG`) to prevent Adreno GPU compositor blackouts over system keyboard overlays.

**History:**
- 2026-09-24: Specified CWE-359 keyboard telemetry prevention in `architecture.md` and `ui-ux-design-system.md`.
- 2026-09-26: Diagnosed Adreno GPU blackout and double keyboard inset subtraction on physical Pixel; scoped `FLAG_SECURE` and isolated root Scaffold insets.
- 2026-09-27: Verified universal form keyboard handling and secret input masking during Phase 4 live testing on hardware.

**Shaped perspective:** IME input on Android is an inter-process IPC boundary subject to GPU compositor limitations, keyboard service logging, and system inset negotiation. Failing to isolate root insets causes keyboard crushing, while unconditional `FLAG_SECURE` on legacy hardware drivers blanks out the entire window during text entry. Defensive UI design treats the keyboard overlay as a distinct external surface that must be isolated at both the view and window levels.

---

## pattern: base62-sovereign-key-parity
**weight**: 3 | **last validated**: 2026-09-27 | **first observed**: 2026-09-26
**pinned**: false
**status**: hot

ShellGuard master identity keys (`hu-`) and agent keys (`lb-`) use 64 alphanumeric Base62 characters (`[0-9a-zA-Z]`, 67 total string length). Enforcing lowercase hexadecimal validation (`[0-9a-f]`) falsely rejects authentic web-generated credentials and locks mobile users out of their vaults. All client regex patterns, form validators, and key decoders must accept the full Base62 character space.

**History:**
- 2026-09-26: Diagnosed disabled login button on physical Pixel despite valid JSON identity file loaded; traced to `[0-9a-f]` regex in `ClawCrypto` rejecting uppercase letters in web-generated `hu-` keys. Upgraded regex to Base62.
- 2026-09-26: Held in accumulating register during first dream cycle (weight 2/3).
- 2026-09-27: Re-validated during Phase 4 CameraX QR scanning, TOTP secret parsing, and codified as a load-bearing redline in `testOracle.md`.

**Shaped perspective:** This holds because cryptographic identity formats are dictated by the sovereign web authority that mints them, not the downstream mobile consumer. It would break if the core ShellGuard cryptographic specification altered its key-generation entropy encoding away from Base62. What it costs to maintain is ensuring that any future input masks, validators, or QR parsers consistently test against mixed alphanumeric strings rather than assuming standard hex byte serialization.

---

## pattern: claw-re-prompt
**weight**: 3 | **last validated**: 2026-09-27 | **first observed**: 2026-09-24
**pinned**: false
**status**: hot

Designated high-security items (`reprompt == true`) must enforce an explicit biometric or PIN re-verification gate before unmasking secrets, copying credentials to clipboard, or filling inputs, even when the parent vault is already in an unlocked state.

**History:**
- 2026-09-24: Formulated claw re-prompt specification in `architecture.md` and `room-storage-schema.md` for high-risk corporate and financial credentials.
- 2026-09-26: Implemented in `ItemDetailScreen` and `ItemDetailViewModel`, gating secret visibility and clipboard export behind hardware biometric challenge.
- 2026-09-27: Extended into `AutofillAuthActivity` transparent gate, intercepting autofill requests for re-prompt items and demanding biometrics before emitting autofill datasets.

**Shaped perspective:** This holds because vault unlock is a coarse-grained perimeter defense, whereas credentials inside a vault possess heterogeneous threat levels. A compromised device left unlocked on a desk or handed to a colleague breaches all items unless individual high-value pearls require re-authentication. What it costs to maintain is an extra cryptographic/biometric challenge state machine in detail views and autofill flows, along with educating users on why certain items challenge them again.

---

## pattern: fail-closed-structural-envelope-validation
**weight**: 4 | **last validated**: 2026-10-04 | **first observed**: 2026-09-27
**pinned**: false
**status**: hot

All cryptographic retrieval, detail getters, and backup decoders must strictly fail closed on anomalies, returning explicit `Result.failure` or throwing fatal validation errors rather than silently falling back to raw ciphertext or partial payloads. Payloads originating from cross-platform web endpoints must structurally validate via `ShellCryptionEngine.isEncryptedEnvelope()` prior to AES-GCM decryption to safely handle unencrypted empty collections (`"[]"`) without crashing or triggering double-ciphertext corruption.

**History:**
- 2026-09-27: Subjected sync and detail getters to dual adversarial audit; eliminated dangerous fallback that exposed raw ciphertext strings on decryption failures, enforcing fail-closed termination.
- 2026-10-03: Diagnosed JSON deserialization crash on web items storing empty unencrypted arrays (`"[]"`); implemented `isEncryptedEnvelope` structural sniffing and fail-closed `decryptField` boundaries.
- 2026-10-04: Extended fail-closed envelope sniffing and checksum verification into `VaultBackupEngine`, rejecting corrupt or mismatched backups during import.

**Shaped perspective:** In zero-knowledge architecture, partial failure is a security breach. A system that falls back to raw data when decryption fails turns a cryptographic error into an information disclosure vulnerability. True security demands that every layer either decrypts completely with valid authentication or halts execution immediately.

---

## pattern: context-aware-autofill-and-blast-radius-gating
**weight**: 4 | **last validated**: 2026-10-08 | **first observed**: 2026-09-27
**pinned**: false
**status**: hot

Android Autofill and IME inline suggestion pipelines must treat view hierarchy traversal, cross-origin WebView boundaries, and Binder IPC payloads as hostile, high-blast-radius surfaces. Specifically: (1) entering a WebView (`webDomain != null`) immediately invalidates outer native host package matching and clears any fields captured outside the WebView to prevent AutoSpill credential cross-contamination; (2) only verified editable leaf nodes (`isEditableInputNode`) may bind `AutofillId` targets, with password classification mutually exclusive from username heuristics; (3) low-confidence heuristics (Rank 4/5) and `"Add Item"` creation fallbacks must be gated on password field co-presence (`passwordId != null`); and (4) inline keyboard `Slice` views must use zero-copy `Icon.createWithResource` references and partially masked username subtitles (`Work · lu***@company.com`) to prevent Binder `TransactionTooLargeException` crashes and shoulder-surfing leaks.

**History:**
- 2026-09-27: Engineered `AutofillStructureParser`, `ShellGuardAutofillService`, and `DomainMatcher` with strict AutoSpill WebView domain precedence (`webDomain` overrides `packageName`) and immutable `PendingIntent` codes.
- 2026-10-03: Resolved Gboard/Samsung Keyboard silent chip drops by migrating inline `Dataset` presentations to Jetpack `androidx.autofill.inline.v1.InlineSuggestionUi` slices with `Icon.createWithResource` zero-copy icons.
- 2026-10-04: Implemented context-aware locked vault inline chips (`"Unlock ShellGuard"` vs. `"Add Item"` fallback) and `EXTRA_AUTOFILL_SAVE_MODE` overlay routing so the user returns cleanly to the host app after credential creation.
- 2026-10-08: Hardened `AutofillStructureParser` and `ShellGuardAutofillService` with 5-tier confidence ranking, Strict Editable-Input Gate (preventing `<form>`/`<div>` container hijacking), Password/Username mutual exclusion (`login_password` fallthrough prevention), Co-Presence Gate (`passwordId != null`), simultaneous multi-field binding, and Option B masked username disambiguation.

**Shaped perspective:** In an OS-level autofill service, every false positive is visible on the user's keyboard across every app they own, and every structural misbinding either silently drops their tap or injects credentials into the wrong node. Security and UX reliability converge when heuristics are ranked by confidence and gated by blast radius: aggressive enough to recognize broken real-world login markup via proximity, yet strictly silence-defaulting whenever a password field is absent or a WebView boundary is crossed.

---

## pattern: deterministic-datastore-viewmodel-synchronization
**weight**: 3 | **last validated**: 2026-10-09 | **first observed**: 2026-10-03
**pinned**: false
**status**: hot

Reactive ViewModel state pipelines backed by AndroidX `DataStore` (`Dispatchers.IO`) and background connectivity flows must enforce four synchronization invariants for deterministic production and JVM/Robolectric execution: (1) seed `DataStore` flows inside `combine(...)` with `.onStart { emit(DefaultState()) }` so synchronous UI state mutations never stall waiting for initial disk I/O; (2) return the launched `Job` from all ViewModel mutation methods so tests can `.join()` completion deterministically; (3) reset singleton `context.dataStore` state in `@Before setUp()` via `runBlocking { repository.clearAll() }` without nesting `runTest` on a shared `TestDispatcher`; and (4) gate background connectivity collectors on state transitions (`isOnline && !wasOnline`) with explicit scope teardown.

**History:**
- 2026-10-03: Diagnosed CI `ComparisonFailure` in `SyncReconciliationAdversarialTest` caused by `SyncRepository.init` launching an unshielded `syncAll` on collector startup; constrained collector to transition events and enforced scope teardown.
- 2026-10-04: Resolved singleton `context.dataStore` cross-test pollution via explicit `clearAll()` in `@Before setUp()` and eliminated 60-second coroutine test timeouts by returning `Job` from ViewModel mutation functions.
- 2026-10-09: Diagnosed GitHub Actions CI `AssertionError` (`v0.0.0.11`, run `37897075874`) in `SettingsViewModelTest.testTriggerManualSyncWithNoActiveSession` where `combine(settingsRepo.settingsFlow, _extraState)` blocked in-memory error emissions until `DataStore` completed its initial disk read on `Dispatchers.IO`; fixed by seeding `.onStart { emit(AppSettings()) }` and using `runBlocking` in `setUp()`.

**Shaped perspective:** This holds because `DataStore` intentionally confines file I/O to background threads (`Dispatchers.IO`), whereas MVI ViewModels and `UnconfinedTestDispatcher` tests mutate UI state synchronously on the main/test thread. When `combine` bridges those two worlds without an immediate initial value, fast local CPUs mask the race while slower cloud CI runners expose it as missing state updates. Explicit `.onStart` seeding, `Job` return contracts, and `runBlocking` fixture resets make the boundary deterministic across every hardware speed.
