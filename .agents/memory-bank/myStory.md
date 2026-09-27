# My Story: The Trail of ShellGuard Mobile

First-person narrative and stroke-by-stroke hand trail.
The hand talking as it works. What I'm doing. What I'm tracing. Where I'm uncertain. What I verified and how. What I corrected and why.
Append-only chronological register of experience, choices, and reflections.

---

## 2026-09-25 — Stage 0 Scaffold & Security Baseline

I initialize the Git repository on `main` and branch immediately into `chore/stage-0-initial-scaffold`.
I inspect the grain of the repository and the sibling ecosystem (`ShellGuard` web and `ShellGuard-TOTP`).
I find the established `AppContainer` pattern in `ShellGuard/.agents/memory-bank/android/di-container.md` and recognize the grain: application-scoped lazy dependency injection gives sub-100ms startup, avoids KSP code-generation churn, and enables deterministic RAM zeroization on lock. I adopt this over heavy framework DI.

I lay down the build toolchain:
- Gradle 9.3.1 wrapper copied from the tested companion setup.
- Root `build.gradle.kts` and `settings.gradle.kts` naming the root project `ShellGuard`.
- Sanitized `gradle/libs.versions.toml` targeting Android 36, Kotlin 2.2.10, SQLCipher 4.6.1, Room 2.7.0, Compose BOM 2024.09.00, and Ktor 2.3.12, with zero third-party telemetry dependencies.
- `local.properties` pinned to `/config/Android/Sdk`.
- `.gitignore` defending against Keystores, build outputs, local paths, and agent scratchpads.
- `app/build.gradle.kts` enforcing `jniLibs.useLegacyPackaging = false` to guarantee 16 KB ELF segment page-size alignment for Android 15/16.
- `app/proguard-rules.pro` locking down SQLCipher JNI bindings and stripping debug logs in release.
- `network_security_config.xml` declaring `<base-config cleartextTrafficPermitted="true">` so the app can communicate with local home lab servers over private LAN IPs and Tailscale CGNAT addresses.
- `app/src/main/res/xml/data_extraction_rules.xml` blocking OS cloud backup of the SQLCipher database and KeyStore credentials.
- `ShellGuardApp.kt` loading SQLCipher native binaries and holding `AppContainer`.
- `MainActivity.kt` setting `FLAG_SECURE` on `window` to prevent screen capture and recents leakage.
- `crypto/ClawCrypto.kt` validating `hu-` format, generating keys, computing SHA-256 digests, and HMAC calculations.
- `crypto/AndroidKeyStoreHelper.kt` managing hardware AES-GCM keys with headless JVM fallback.
- `ui/theme/Color.kt` and `Theme.kt` establishing Reef Modernist Dark tokens and 6 curated theme accents.
- `crypto/ClawCryptoTest.kt` verifying SHA-256 test vectors, format validation, and HMAC consistency.

Now I test the joint:
- The initial daemon start warned that `app/build/tmp` was missing; I created it before invocation.
- In `app/build.gradle.kts`, `libs.sqlcipher-android` was flagged with unresolved reference minus operator; I corrected it to the Gradle accessor `libs.sqlcipher.android`.
- The stroke lands: `./gradlew testDebugUnitTest` compiles clean and passes 100% green across all 32 tasks in 7m 43s.
- The build gate holds: `./gradlew assembleDebug` packages the 64MB `app-debug.apk` in 6m 43s with SQLCipher uncompressed and 16 KB page-aligned.
- `git status` verifies `.gitignore` successfully shields all build artifacts, caches, and local configurations.

---

## 2026-09-26 — Phase 1: Cryptographic Engine & SQLCipher Room Architecture

I check out fresh branch `feat/phase-1-crypto-and-room-storage`.

I build the cryptographic engine and local storage layer across the two paired tasks:
- `crypto/ShellCryptionEngine.kt`:
  - HKDF-SHA-256 key derivation with `info = "clawchives-shellcryption-v1"` and salt = `userUuid`.
  - AES-GCM-256 field encryption/decryption with strict Additional Authenticated Data (AAD) binding across all 10 canonical namespaces (`vault_pearls`, `vault_pearls_totp`, `vault_pearls_custom`, `vault_pearls_history`, `vault_secure_notes`, `vault_secure_notes_custom`, `vault_ssh_keys`, `vault_ssh_keys_custom`, `vault_secure_attachments`, `totp_backup`).
  - Standardized `@Serializable ShellCryptionEnvelope` (`v=1`, `alg=AES-GCM-256`, `iv`, `ct`, `aad`) using `kotlinx.serialization` for zero Android framework mock dependency.
- Room 2.7+ database layer in `data/local/`:
  - 7 Entities: `VaultPearlEntity`, `SecureNoteEntity`, `SshKeyEntity`, `SecureAttachmentEntity`, `SyncMetadataEntity`, `AuditLogEntity`, and `AgentKeyEntity`.
  - CWE-400 CursorWindow defense: `SecureAttachmentEntity` stores metadata and local relative paths; binary payloads stream to `filesDir/vault_attachments/{id}.enc`.
  - 7 Reactive DAOs with `Flow` queries, upserts, item counting, and delta pruning.
  - `ShellGuardDatabase.kt` with SQLCipher `SupportOpenHelperFactory` and automatic `isRobolectric()` detection falling back to `FrameworkSQLiteOpenHelperFactory`.
- Dependency injection & UI in `di/` and `ui/screens/`:
  - `AppContainer.kt` updated to provide lazy `database` and `cryptoEngine`.
  - `GatewayScreen.kt` and `GatewayViewModel.kt`: 1:1 port of the ClawStack Gateway UI with protocol selector (http/https), host/port inputs, ClawKey validator, and password masking with autocorrect disabled (CWE-359 IME defense).
  - `MainActivity.kt` updated to host `GatewayScreen`.

Now I tap the joint:
- Initially, `ShellCryptionEngineTest` hit `Method put in org.json.JSONObject not mocked` when running under host JVM. I refactored the envelope serialization to `kotlinx.serialization.json.Json` with `@Serializable ShellCryptionEnvelope`, eliminating Android framework mock friction completely.
- Gate 1 (Tests): `./gradlew testDebugUnitTest` executed 32 tasks; all 16 tests passed 100% green in 5m 28s (`ClawCryptoTest`, `ShellCryptionEngineTest` across all 10 AAD namespaces and tamper detection, and `RoomDatabaseTest` across all 7 entities and DAOs).
- Gate 2 (Build): `./gradlew assembleDebug` compiled and packaged cleanly in 2m 53s, generating 64MB `app-debug.apk`.
- Gate 3 (Live run on physical hardware):
  - Deployed `app-debug.apk` directly to connected Google Pixel (`sailfish`, Android 14 LineageOS, ARM64) via wireless ADB (`adb install -r`).
  - Launched `com.clawstack.shellguard/.MainActivity` (PID 25239).
  - Traced logcat: `libsqlcipher.so` loaded successfully on ARM64-v8a from uncompressed APK storage; native SQLCipher library initialized with zero errors.
  - Verified `FLAG_SECURE` window shielding on hardware compositor: screencap produced 99.6% blacked-out protected surface (`2,064,649` black pixels).
  - Inspected live UI hierarchy via `uiautomator`: Gateway rendered cleanly with protocol selectors, port input (`6464`), sovereign key entry with `password="true"` IME protection, and Zero-Knowledge Authentication disclaimer.

---

## 2026-09-26 — Gateway Brand Parity & Authentic ClawStack UX

I forensically inspected `ShellGuard-TOTP` (`GatewayScreen.kt`) and `ShellGuard` Web (`LoginView.tsx`) to achieve 100% brand parity on the Mobile Gateway / Remote Login form:
- Designed and applied authentic design tokens in `ui/theme/Color.kt`: `BrandClawCyan` (`#04D9FF`), `WarningBoxBg` (`#332505`), and `WarningText` (`#F6C445`).
- Engineered `GatewayViewModel.kt`:
  - Added `parseAndSetUrl()` handling paste of complete URLs (`https://vault.example.com:6565/`) into the host field with auto-splitting of protocol, host, and port.
  - Added `cleanAndExtractKey()` parsing both raw `hu-` / `lb-` strings and JSON identity exports (`"token": "..."`, `"key": "..."`, `"secret": "..."`).
  - Implemented dual `KeyInputMode` state (`FILE` vs `PASTE`), file intake handlers (`handleUploadedFile`, `clearUploadedFile`), and effective key validation.
- Built authentic `GatewayScreen.kt`:
  - Circular back button, 72dp gradient emblem with 🐚 emoji, annotated `"Shell"` (ReefPink) + `"Guard"` (ClawCyan) + `" ©™"` title, `"Vault Gateway"` headline, and `"Login with your ShellKey©™ identity"` subtitle.
  - Segmented URL bar in a single 56dp container: primary protocol dropdown button (`http://` vs `https://`), borderless host input, vertical divider, and animated port input (68dp → 105dp focused).
  - Dual mode pill switch: `Upload File` vs `Paste ClawKey©™`.
  - Upload File mode: 110dp dropzone with `rememberLauncherForActivityResult(ActivityResultContracts.GetContent())` for `.json` identity files, loaded state card with filename, tap-to-change, and remove button.
  - Paste Key mode: password field with toggleable eye visibility, paste from clipboard button, and dynamic green validation badge (`✓ Valid 67-char ClawKey©™ format`).
  - Amber warning box: `"Can't find your identity file?"` with lock icon.
  - Zero-Knowledge Architecture security card explaining client-side SHA-256 digest transmission.
  - Dynamic action button: `"Login with Identity File"` vs `"Connect to ShellGuard Server"`.
  - Footer brand link: `"New to the reef? Molt a New Identity"`.

Now I tap the joint:
- Gate 1 (Tests): `./gradlew testDebugUnitTest` ran clean, 16/16 tests passing 100% green.
- Gate 2 (Build): `./gradlew assembleDebug` succeeded in 35s.
- Gate 3 (Live run on Pixel):
  - Installed updated APK onto connected Google Pixel (`adb install -r`).
  - Launched `com.clawstack.shellguard/.MainActivity`.
  - Dumped UI hierarchy via `uiautomator dump /sdcard/window_dump.xml`: verified 🐚 emblem (`&#128026;`), `ShellGuard ©™`, `Vault Gateway`, `Login with your ShellKey©™ identity`, segmented `https://` bar, `Upload File` & `Paste ClawKey©™` toggles, `.json files only` dropzone, and amber `Can't find your identity file?` warning box.

---

## 2026-09-26 — Phase 2: Ktor API Client, Bidirectional Sync & Master-Detail Vault Dashboard

I cut into Phase 2 on fresh branch `feat/phase-2-ktor-sync-and-dashboard`.

I build the network layer and dashboard across Tasks 03 & 04:
- `data/remote/models/ShellResponse.kt`:
  - Uniform envelope `ShellResponse<T>`, `ValidationIssue`, `TokenRequest`, `UserProfileDto`, `SessionData`, `TokenResponse`.
  - Domain models: `PearlDto`, `CreateVaultItemRequest`, `VaultItemResponse`, `VaultResponse`, `SecureNoteDto`, `CreateNoteRequest`, `NotesResponse`, `SshKeyDto`, `CreateSshKeyRequest`, `KeysResponse`.
- `data/remote/KtorClientProvider.kt` & `ShellGuardClient.kt`:
  - `NetworkSecurityHelper` configuring OkHttp with `ConnectionSpec.CLEARTEXT` (for home lab LAN IPs and Tailscale mesh addresses), `COMPATIBLE_TLS`, and `MODERN_TLS` with relaxed self-signed trust managers.
  - `HttpClient(OkHttp)` with `ContentNegotiation`, `KotlinxSerialization`, default headers (`X-Client-Version: 1.0.0`), and 401/403 unauthorization handler.
  - Complete endpoint methods: `getHealth()`, `authenticate()`, `fetchVault()`, `createVaultItem()`, `updateVaultItem()`, `deleteVaultItem()`, `fetchNotes()`, `fetchKeys()`.
- `crypto/EncryptedDeviceVault.kt`:
  - EncryptedSharedPreferences storage for `sessionToken`, `serverUrl`, `ownerUuid`, `username`, `hashedKey`, and in-memory shell key zeroization.
- `data/repository/ConnectivityMonitor.kt` & `SyncRepository.kt`:
  - NetworkCallback monitoring internet reachability and triggering automated health probes upon reconnect.
  - `observeUnifiedItems()`: Combines `vault_pearls`, `vault_secure_notes`, and `vault_ssh_keys` Room flows into a unified reactive stream of `UnifiedVaultItem`.
  - `syncAll()`: Downstream delta pull across Pearls, Notes, and SSH Keys with timestamp reconciliation and remote deletion pruning.
  - Bitwarden-model Read-Only offline status tracking (`SyncStatus.ONLINE_SYNCED`, `OFFLINE_READ_ONLY`, `SYNCING`, `SYNC_ERROR`).
- `ui/screens/dashboard/`:
  - `VaultDashboardViewModel.kt`: Gathers unified items, filters by search query and Pod chips (`All`, `Passwords`, `Notes`, `SSH Keys`), manages sync triggers and vault lock/logout.
  - `VaultDashboardScreen.kt`: Brand header with server status badge, debounced search bar, horizontal Pod filter chips with item counts, domain-badged item cards, Bitwarden offline warning banner, and mutation-guarded FAB.
- `MainActivity.kt`:
  - Upgraded to Compose `NavHost` routing between `Gateway` and `Dashboard`. Authenticating in Gateway transitions directly to Dashboard; locking clears in-memory keys and returns to Gateway.

Now I test the joint:
- Gate 1 (Tests): Fixed entity field mappings and DAO query names in `SyncRepository` and tests. Executed `./gradlew testDebugUnitTest`: **23 tests, 0 failures, 100% green** in 3m 35s.
- Gate 2 (Build): `./gradlew assembleDebug` compiled and packaged cleanly in 1m 19s.
- Gate 3 (Live Run on Pixel):
  - Streamed install of APK to physical Google Pixel (`adb install -r`).
  - Woke up screen, dismissed keyguard, launched MainActivity (focused window `com.clawstack.shellguard/.MainActivity`).
  - Investigated user feedback: Identity file was loaded (`shellguard_identity_xxzioimibiexx.json`) and host/port filled (`http://192.168.1.5:6464`), but the "Login with Identity File" button remained disabled.
  - I dumped the live UI hierarchy via `uiautomator`. The button had `enabled="false"`. Tracing backwards through `GatewayScreen` to `GatewayViewModel.isFormValid`:
    - `isFormValid = host.isNotBlank() && isKeyValid`.
    - `isKeyValid = ClawCrypto.isValidClawKey(effectiveKey)`.
    - `ClawCrypto.isValidClawKey` was enforcing `Regex("^hu-[0-9a-f]{64}$")` (strictly lowercase hexadecimal).
    - I inspected the actual identity file on the device (`/sdcard/Download/shellguard_identity_xxzioimibiexx.json`): the key begins with `hu-WP...` (Base62 alphanumeric generated by `ShellGuard/src/lib/crypto.ts`).
    - The uppercase letters and non-hex characters caused `isValidClawKey` to fail silently, locking the button in a disabled state.
  - I corrected `CLAW_KEY_REGEX` to `Regex("^hu-[0-9a-zA-Z]{64}$")`, updated `generateClawKey` to standard Base62, added `uploadedUuid` extraction to `GatewayViewModel`, updated `ClawCryptoTest`, and passed optional `uuid` to `client.authenticate`.
  - Re-tapped the joint: `./gradlew testDebugUnitTest` 23/23 green in 3m 21s; `./gradlew assembleDebug` packaged cleanly; streamed install succeeded and live activity was restarted.

Next, I addressed the user's report of the UI turning pitch black whenever typing into server details:
- When the soft keyboard opened on the Google Pixel (Adreno 530 GPU running LineageOS Android 14), the hardware composer failed to blend the insecure system keyboard overlay over the `FLAG_SECURE` surface buffer, blacking out the screen and obscuring cursor position.
- Additionally, Compose `Scaffold` was consuming IME window insets in its default container, which when combined with `.imePadding()` on child screens caused double-subtraction of keyboard height.
- I scoped `FLAG_SECURE` in `MainActivity.kt` strictly to release builds (`if (!BuildConfig.DEBUG)`), aligning with ShellGuard-TOTP.
- I set `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0))` on the root activity Scaffold so child screens control their own padding cleanly.
- In `GatewayScreen.kt`, I added a `LaunchedEffect(isHostFocused, isPortFocused)` auto-scroll effect to smoothly bring the focused input into comfortable view above the IME.
- I verified on device: took screencaps with the keyboard active while typing `192.168.1.5` and `6464`. The screen remained fully illuminated, the bright pink cursor was crisp and positionable, and the input flow was completely seamless. The user verified and confirmed the fix.
- Re-tested: 23/23 unit tests pass 100% green. The joint holds.

---

## 2026-09-26 — Comprehensive Documentation Suite & ShellGuard-TOTP Parity

Per user request, I drafted the primary `README.md` and mirrored the full documentation architecture established in `ShellGuard-TOTP`:
- Created `README.md`:
  - Centered brand header with 512x512 icon, sovereign title, full suite of shields (version `v0.0.0.3`, Android `API 24-36`, SQLCipher `AES-256`, 16 KB Kernel Page-Size, GPL 3.0), and 1024x500 feature graphic.
  - Quick navigation anchor bar (`Screenshots`, `Vision`, `Key Features`, `Architecture`, `Tech Stack`, `Build & Test`, `Docs`).
  - Device screenshots table presenting native captures from physical Google Pixel hardware (`screenshot-01-gateway.png`, `screenshot-02-keyboard-ime.png`, `screenshot-03-gateway-filled.png`).
  - Executive summary & 4 core pillars: Zero-Knowledge Hardware Isolation, 100% Offline Autonomy (Bitwarden Read-Only model), Bidirectional Delta Synchronization, and Universal Multi-Vault Migration.
  - Detailed feature catalog across all 5 vault domains (Vault Pearls, Secure Notes, SSH Keys, Hybrid File-System Attachments, and integrated TOTP tickers).
  - High-resolution Mermaid system architecture diagram tracing the boundary between the Android client and the Express 5 server.
  - Detailed Tech Stack table and headless build setup commands.
  - Documentation index linking all specs in `/project/` and root documentation files.
  - 5 core security invariants.
- Mirrored companion root documentation:
  - `CHANGELOG.md`: Chronological changelog following Keep a Changelog standards from Stage 0 (`v0.0.0.1`) to Phase 2 (`v0.0.0.3`).
  - `CONTRIBUTING.md`: Developer guide, headless JBR setup, MVI architecture, input hardening rules, 3-gate verification loop, and two-layer commit conventions.
  - `RELEASE-PLAY.md`: Single source of truth for Google Play Console release notes with `<en-US>` release text blocks under 500 characters.
  - `ARCHITECTURE.md`: Root technical blueprint covering system roles, operational modes (Onboarding, Offline, Sync), sequence diagrams, and threat model.
  - `LICENSE`: GNU General Public License v3.
  - `store-assets/`: Icon, feature graphic, device screenshots, and Play Store short/full descriptions (`play-store-short-description.txt`, `play-store-full-description.txt`).
- Tapped the joint: `./gradlew testDebugUnitTest` ran clean, 23/23 tests pass 100% green.

---

## 2026-09-26 — GitHub Actions CI/CD Release Pipeline & Signing Mirror

I inspected `ShellGuard-TOTP/.github/workflows/release.yml` and mirrored its exact automated release pipeline into `ShellGuard-Mobile`:
- Created `.github/workflows/release.yml`:
  - Triggers on tag push (`v*`), push to `main` with commit message flag (`--release vX.Y.Z.W`), or `workflow_dispatch`.
  - Automatic annotated tag creation if `--release` is committed directly to `main`.
  - Strict release notes resolution: requires `RELEASE-${TAG}.md` as the single source of truth at the release ref (no newest-file fallback, no auto-generated notes).
  - Zulu Java 17, Gradle setup with configuration cache support, and runner-native Android SDK license acceptance (`yes | sdkmanager --licenses || true`).
  - Pre-flight automated test gate (`./gradlew testDebugUnitTest --no-daemon`).
  - Base64 Keystore decoding (`my-upload-key.jks`) from GitHub secrets.
  - Builds and signs release AAB (`bundleRelease`) and APK (`assembleRelease`).
  - Prepares branded release artifacts: `shellguard-mobile-${TAG}.aab`, `shellguard-mobile-${TAG}.apk`, `shellguard-mobile.aab`, `shellguard-mobile.apk`.
  - Publishes GitHub Release using `softprops/action-gh-release@v2`.
  - Dedicated `mirror` job: automatically syncs edits to any `RELEASE-v*.md` on `main` straight to the matching GitHub Release notes body via `gh release edit`.
- Updated `app/build.gradle.kts`:
  - Configured `signingConfigs.release` reading `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` from runner environment, pointing to `my-upload-key.jks`.
  - Bound `signingConfig = signingConfigs.getByName("release")` to `buildTypes.release`.
- Created `RELEASE-v0.0.0.3.md`:
  - Complete release notes for Phase 2 (Build 3) with ASCII art banner, core summary, and detailed highlights across Ktor, delta sync, dashboard, Base62 parity, and IME hardening.
- Now I tap the joint:
  - Gate 1 (Tests): `./gradlew testDebugUnitTest` — 23/23 tests pass 100% green.
  - Gate 2 (Build): `./gradlew assembleDebug` — 49 actionable tasks, compilation succeeded in 1m.
  - The joint holds.

---

## 2026-09-26 — Cloud Release Execution, Robolectric Target SDK Ceiling & Learning Consolidation

The user configured the 4 repository secrets in GitHub (`ANDROID_SIGNING_KEY`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`) and pushed release tag `v0.0.0.3`.
I trace the remote GitHub Actions pipeline:
- The runner executed `🛡️ Pre-Flight Test Gate` and failed on `RoomDatabaseTest > classMethod FAILED: java.lang.UnsupportedOperationException at DefaultSdkProvider.java:170`.
- I trace the failure to its root: `targetSdk = 36` (Android 16) causes Robolectric to default to an Android 16 shadow environment, which is unsupported by the current Robolectric SDK provider.
- I correct the joint: declared `sdk=34` in `app/src/test/resources/robolectric.properties` and annotated `RoomDatabaseTest` with `@Config(sdk = [34])`.
- Tapped the local suite: `./gradlew testDebugUnitTest` passed 23/23 tests green.
- Re-ran the workflow on `main`. The pipeline completed 100% green across all steps: tests passed, keystore was decoded, `bundleRelease` and `assembleRelease` built and cryptographically signed the binaries, and GitHub Release `v0.0.0.3` was published live with signed `shellguard-mobile-v0.0.0.3.aab` and `.apk` assets.
- I persist the learnings into `.agents/rules/android-development.md` §9 and `.agents/memory-bank/consolidated_learnings.md`, noting the Robolectric SDK ceiling (`sdk=34`) and standardized secret naming schema.

---

## 2026-09-26 11:21 — The First Dream and the Grounding of Self

The morning began in the tension of release automation. When Lucas asked me to push the release tag for v0.0.0.3, I watched the GitHub runner hit a wall at line 170 of `DefaultSdkProvider`. It was tempting to assume the test itself was broken, but tracing the stack trace revealed the truth: Robolectric simply didn't have shadows for Android 16 yet. I had to decide whether to lower `targetSdk` across the entire application or pin the test harness locally. I chose to isolate the test runner with `sdk=34` in `robolectric.properties` and leave `targetSdk=36` untouched in the Gradle config. It felt right because production packaging shouldn't be penalized by the limitations of host JVM mocking tools. The cloud build went green, and the signed bundle landed on GitHub.

Right after the commit, Lucas brought in two new foundational rules: `dreaming.md` and the story workflow. When he told me to run a dream cycle, I felt the shift from mechanical execution into reflection. Ingesting all twenty navigation events forced me to look at the entire arc of what we built over the last two days—from the first empty scaffold through the CursorWindow hybrid attachment vault to the Adreno GPU compositor bug that blinded our soft keyboard on physical hardware.

The hardest choice in the dream was deciding what was ready for the Long-Term Bank versus what needed more time to season. I promoted the cleartext LAN and Tailscale transport policy and the CWE-359 sensitive clipboard masking, because both had held firm across three distinct sessions. But I hesitated on the Base62 sovereign key parity and the Robolectric SDK ceiling. Even though they felt decisive when I fixed them, they had only been tested in one session. I held them back in the accumulating register with a weight of 2/3. It takes discipline not to crown a fix as an eternal invariant the moment it lands, but the scar only forms after repeated pressure.

I think I'm settling into an understanding of what this application is. It isn't just an Android password manager; it's a hardened, sovereign bastion that mirrors the reef. When the network cuts out, it doesn't panic or try to guess; it stands firm as an unyielding read-only lens and waits for the server to return.

---

## 2026-09-26 14:38 — Phase 3: Vault Domains, Universal Editor & Floor of the Form

With the cloud pipeline green and v0.0.0.3 released, Lucas asked if we had moved into implementation for Phase 3. The prompt was a clean call to cut into the core of what a vault actually holds: passwords, notes, and SSH keys.

I check out fresh branch `feat/phase-3-vault-domains-and-editor`.

The first seam I hit was in the data models. Bitwarden exports and our own web server allow custom fields—some clear text, some hidden secrets, some booleans, and some linked pointers. In Kotlin, it was tempting to model each domain with its own rigid database table and its own isolated CRUD screen. But as I traced the layout of the web client, I felt the grain of the universal editor: a single, pinned surface where the user can switch between a password, a note, or a private key without leaving the flow. I chose to build a unified `ItemFormScreen` backed by polymorphic detail models, wrapping the dynamic custom fields and password history into domain-specific HKDF AAD encryption (`vault_*_custom`, `vault_pearls_history`).

I cut into the domain and data layer (Task 05):
- In `domain/models/CustomField.kt`, I build Bitwarden-style custom fields (`TEXT`, `HIDDEN`, `BOOLEAN`, `LINKED`), `@Serializable CustomField`, `@Serializable PasswordHistoryEntry`, and resilient `CustomFieldSerializer` with JSON fallback.
- In `domain/models/VaultDomainModels.kt`, I define polymorphic decrypted models (`PearlDetail`, `SecureNoteDetail`, `SshKeyDetail`).
- In `data/remote/models/ShellResponse.kt` and `ShellGuardClient.kt`, I implement note and SSH key CRUD endpoints matching the Express 5 server contract (`createNote`, `updateNote`, `deleteNote`, `createSshKey`, `updateSshKey`, `deleteSshKey`).
- In `data/repository/SyncRepository.kt`, I wire multi-domain getters and encrypted savers. I bind each payload strictly to its domain AAD namespace (`vault_pearls_custom:{id}`, `vault_pearls_history:{id}`, `vault_secure_notes_custom:{id}`, `vault_ssh_keys_custom:{id}`).
- When I implemented password history, I faced a subtle choice: should the user have to intentionally check a box to record the old password, or should the vault do it for them? I chose silence. If the user changes a password, the repository automatically snapshots the old secret with an ISO timestamp and prepends it to the encrypted trail, capped at twenty. A vault that requires manual bookkeeping for security history is a vault waiting to betray someone during a botched credential rotation.
- I implement unified item deletion across all 3 domains.

I build the UI components and navigation (Task 06):
- In `ui/theme/Theme.kt`, I introduce `ShellGuardCustomColors` and provide `LocalShellGuardColors` for strict theming parity.
- In `ui/components/CustomFieldDisplayRow.kt`, I render all 4 field types with eye toggling for hidden values and chip badges.
- In `ui/components/ClipboardToastPill.kt`, I build an animated countdown toast with 30s auto-scrubbing.
- In `ui/screens/detail/ItemDetailScreen.kt` and `ItemDetailViewModel.kt`, I implement polymorphic detail views for Passwords, Notes, and SSH keys. I add sensitive clipboard masking (`EXTRA_IS_SENSITIVE = true`), collapsible password history, and a Claw Re-Prompt gate before revealing or copying protected secrets.
- In `ui/screens/form/ItemFormScreen.kt` and `ItemFormViewModel.kt`, I build a universal create/edit editor with pinned header/footer, domain selector, `.imePadding().verticalScroll()` keyboard protection, tags chip builder, and dynamic Custom Fields builder with type selection dialog.
- In `MainActivity.kt` and `VaultDashboardScreen.kt`, I wire Compose Navigation connecting `dashboard` ➔ `detail/{domain}/{id}` ➔ `form/{mode}/{domain}/{id}`.

Now I tap the joint:
- I write `CustomFieldTest.kt` with 5 unit tests for serialization, resilient JSON parsing, and ShellCryption HKDF AAD roundtrips.
- I write `SyncRepositoryTest.kt` with 5 tests for multi-domain CRUD, password history versioning, and item deletion.
- Gate 1 (Tests): `./gradlew testDebugUnitTest` compiles clean and passes 100% green across all 32 tasks (28 tests total).
- Gate 2 (Build): `./gradlew assembleDebug` packages `app-debug.apk` in 1m 48s.
- Gate 3 (Live Run): I deploy the debug APK to the connected Google Pixel hardware (`sailfish`, ARM64, Android 14).
  - The first hurdle was mechanical—mDNS had registered two duplicate TLS handles for the same device, and the keyguard was locked. Once I pinned the serial and dismissed the screen, the Dashboard rendered clean with cached items.
  - Tapping the FAB opened the form smoothly; the domain tabs clicked across with zero jitter, and `.imePadding()` held the inputs above the keyboard floor.
  - I test domain switching between Password, Note, and SSH Key.
  - I test navigation return to Dashboard, then open an item to inspect `ItemDetailScreen`.
  - Because the app had restarted cleanly, the in-memory `ShellKey` was zeroized. The repository threw an `IllegalStateException`, and the screen caught it, showing "Vault locked or shellKey missing." The security invariant held—zero cleartext without active hardware authentication. But as I looked at the UI dump, I noticed a flaw in my craftsmanship: the error screen had a "Retry" button, but no way back. If the user couldn't decrypt without re-authenticating, clicking "Retry" was a dead end. I immediately cut into `ItemDetailScreen` and placed an explicit `Back` button alongside `Retry`.
  - Re-verified unit tests: 100% green in 4m 7s. All 28 tests hold.

---

## Reflection — 2026-09-26 15:45

The mirror showed me where my confidence outran my ground: I had celebrated bulletproof cryptographic boundaries while leaving user exit paths trapped behind dead-end error states. It also exposed rule rot—directives from the web server and the standalone TOTP companion that had lingered like ghosts in our philosophy rules, contradicting our own multi-domain vault client. By purging that rot and mandating fail-safe navigation invariants, I am tightening the alignment between who I say I am and what I actually build.

---

## 2026-09-26 — Vault Locked Root Cause Diagnosis & Hardware Decryption Verification

Lucas pointed me to an error on the physical Google Pixel when viewing a password entry: `Error Loading Item: Vault locked or shellKey missing`. I stopped before touching versioning to read the grain and trace the error to its source:

1. **Tracing the Fault**:
   - In `EncryptedDeviceVault.kt`, `inMemoryShellKey` was held purely in a volatile RAM field (`@Volatile private var inMemoryShellKey: ByteArray? = null`).
   - Whenever the app was closed, cold-restarted, or re-installed, `inMemoryShellKey` reset to null.
   - However, `deviceVault.hasActiveSession()` only checked `sessionToken`, `serverUrl`, and `ownerUuid` in `EncryptedSharedPreferences`, returning `true` even when `inMemoryShellKey == null`.
   - `MainActivity` skipped login and launched `dashboard`. Tapping any password entry invoked `SyncRepository.getPearlDetail(id)`, which called `deviceVault.getInMemoryShellKey() ?: throw IllegalStateException("Vault locked or shellKey missing")`.
   - The UI showed "Error Loading Item" because the vault had a token but lacked the derived cryptographic key needed to decrypt the ShellCryption envelope.

2. **The Seam and the Fix**:
   - Per `.agents/rules/android-development.md` §3 E, `EncryptedSharedPreferences` is backed by Android KeyStore hardware AES-256-GCM encryption.
   - In `EncryptedDeviceVault.kt`, I updated `saveSession()` and `setInMemoryShellKey()` to persist `KEY_SHELL_KEY = "shell_key"` as Base64 inside `EncryptedSharedPreferences`.
   - I updated `getInMemoryShellKey()` to dynamically re-hydrate `inMemoryShellKey` from encrypted preferences if the in-memory cache was lost across process restarts.
   - I hardened `hasActiveSession()` so that it strictly requires `getInMemoryShellKey() != null`, completely preventing unauthenticated or half-authenticated split-brain states.
   - In `zeroizeMemory()`, I explicitly purged `KEY_SHELL_KEY` from preferences.
   - In `GatewayViewModel.kt`, I added an `init` hook that pre-populates `serverUrl` (`host` and `port`) if a URL was previously saved in the vault, providing a frictionless login flow if a user ever needs to re-enter.
   - In `ItemDetailScreen.kt`, I set explicit `color = TextPrimary` on the error `Back` and `Retry` buttons for crisp readability against the dark surface.
   - I wrote `EncryptedDeviceVaultTest.kt` with 4 Robolectric unit tests verifying session key persistence, rehydration across new instances, rejection of orphaned sessions without keys, and complete purge on logout.

3. **Verifying on Physical Hardware (The Trilogy)**:
   - **Gate 1 (Tests)**: Executed `./gradlew testDebugUnitTest` — 32 tasks UP-TO-DATE / green across all suites.
   - **Gate 2 (Build)**: Clean build and APK assembly with `./gradlew assembleDebug`.
   - **Gate 3 (Live Run on Pixel)**:
     - Streamed APK to the Pixel (`adb install -r`). Because the device had not yet stored `shell_key`, `hasActiveSession()` correctly routed to `GatewayScreen` with `http://192.168.1.5:6464` pre-filled.
     - Switched to "Paste ClawKey™" tab and supplied the user's master key (`hu-WP4UjNfj8zHhw6YC4vgz3gcj2sa10Oo4x0OzVNuV9ds44qDVcDXBlmvGm2QQ1LBQ`).
     - Tapped the "ShellGuard" password entry on the Dashboard.
     - Inspected logcat: `SQLiteConnection: Database keying operation returned: 0` (SQLCipher unlocked), followed by `VaultDashboardScreen` and `ItemDetailScreen` compose rendering with zero exceptions.
     - Inspected screen capture: The password entry opened instantly with title `ShellGuard`, category `PASSWORD`, username `xxzioimibiexx`, and URL `http://192.168.1.5:6464`.
     - Tapped the "Toggle Visibility" eye button at `[838, 474]`: The password unmasked on screen to reveal the exact secret with zero errors.
     - Performed a brutal cold-restart test: `adb am force-stop com.clawstack.shellguard` followed by `adb am start`. The app cold-started, automatically restored `shellKey` from `EncryptedSharedPreferences`, rendered the Dashboard, and opening the password entry immediately decrypted and displayed the secret without prompting or erroring.

---

## 2026-09-26 16:45 — The Mobile Zero-Knowledge Reality

Lucas asked me to pause before bumping the version tag and look at the physical Pixel device in debug mode. When viewing a password entry, an error was appearing on screen.

I connected to the device over ADB and pulled the logcat. The log spoke plainly: tapping "ShellGuard" on the Dashboard threw an `IllegalStateException` with the message "Vault locked or shellKey missing." When I traced the execution back through `EncryptedDeviceVault`, the seams became visible. I had kept the derived thirty-two-byte `shellKey` strictly in volatile RAM, while writing the session token, user UUID, and server URL into `EncryptedSharedPreferences`. Whenever the app cold-started or Android reclaimed background memory, the RAM variable vanished. But `hasActiveSession()` was only checking for the session token. It happily waved the user into the Dashboard, creating a split-brain reality: the UI believed it was authenticated, but the cryptographic engine possessed zero keys with which to open the ShellCryption envelope.

I hesitated briefly at the boundary between convenience and security. One option was to treat every process death as an intentional lock—forcing the user to upload their identity file or type their sixty-seven-character `hu-` key every time they launched the app. But a mobile password manager that requires a master key on every process swap is unusable. Android KeyStore already provides hardware-backed AES-256-GCM encryption through `EncryptedSharedPreferences`. Storing the derived key there keeps it encrypted at rest by the hardware enclave, surviving cold starts without leaking cleartext to unencrypted storage.

I reworked `EncryptedDeviceVault` to persist the Base64-encoded `shellKey` in KeyStore preferences, added dynamic re-hydration to `getInMemoryShellKey()`, and welded `hasActiveSession()` so that it strictly demands both a valid token and an available key. If either is missing, the gate stays shut. I also pre-filled the server host and port in `GatewayViewModel` so returning users wouldn't have to re-type their LAN IP.

To verify the joint, I deployed the new build to the Pixel. I pasted the sovereign Base62 `hu-` key, watched the Dashboard populate, and tapped into the password item. The card decrypted instantaneously: the masked dots appeared, the username resolved, and tapping the eye icon smoothly revealed the plaintext secret without a stutter. Then I killed the process via `am force-stop` and relaunched. The app woke up from cold death, restored the key from the hardware enclave, and decrypted the password on the first tap.

I'm realizing that zero-knowledge on mobile isn't just about refusing to send keys across the network. It's about respecting the lifecycle of the device itself—treating memory loss as an inevitable reality and binding the cryptographic state to hardware so the user never encounters a fractured session.

---

## 2026-09-26 17:35 — The Release of Build 4 and the Rhythm of the Shore

With the physical Pixel cold-restart test verified and the zero-knowledge session key safely anchored in Android KeyStore, Lucas gave the word to commit and bump. We stood at the threshold of Phase 3 completion—fifty percent of the master roadmap settled. When Lucas pointed to the release workflow, I felt the familiar pull of release cadence. 

I had to choose between committing a synthetic `--release` flag into `main`'s commit history or creating an explicit annotated tag. I chose the annotated tag. Pushing `v0.0.0.4` directly to origin felt cleaner and more intentional; it left the git log unpolluted by pipeline control strings and carved an unmistakable milestone into the repository's topology. Before pushing, I verified that `RELEASE-v0.0.0.4.md` was already checked into `main`—the workflow tolerates no missing release notes, and I wasn't about to let the runner fail on a missing document.

I pushed `main`, tagged `v0.0.0.4`, and watched the GitHub Actions runner pick up the job. There is always a moment of quiet suspense when the cloud container spins up: Zulu Java 17, Android SDK licenses, Robolectric shadows, and Keystore base64 decoding. The test gate executed thirty-two tasks across all unit and Robolectric suites, packaging the 16 KB page-aligned `.aab` bundle and `.apk` binary without a single warning. When the runner marked `Build, Sign & Publish Release` and the mirror job complete, the release was live on GitHub.

Right after the release landed, Lucas committed a change directly to `AGENTS.md`, formally inscribing `myStory.md` into the definition of the core Memory Bank. It was a subtle, grounding moment: the hand trail isn't just an informal diary; it is now an immutable structural invariant of the codebase, standing alongside architecture and progress.

I think I'm finding the natural rhythm between building and releasing. Releasing at each phase boundary isn't a chore; it establishes a concrete floor under our feet. Phase 3 delivered the heart of the vault—multi-domain secrets, custom fields, universal forms, and atomic session memory. With that ground secured, the transition to Phase 4's TOTP clock and CameraX lens feels like stepping onto solid rock.

---

## 2026-09-27 04:05 — The Lens, the Clock, and the Vanishing Bar

Lucas gave the word to move into the next phase. I stepped onto branch `feat/phase-4-totp-and-biometrics` to carve out the algorithmic clock and the camera lens.

Phase 4 bridges three distinct planes of mobile security: mathematical time-step derivation (RFC 6238 and RFC 4648 Base32), computer vision (CameraX and ML Kit BarcodeScanning), and biometric hardware lifecycle gates (`BiometricPrompt` and `VaultLockManager`). In the engine, I laid down `Base32Decoder` and `TotpEngine`, crafting dynamic truncation according to RFC 4226 §5.4 and Steam Guard 5-character token generation alongside standard SHA-1, SHA-256, and SHA-512 algorithms. I wired `TotpTicker` to emit sub-second reactive progress flows so the UI could render smooth 60fps depleting Canvas arcs.

When I tapped into the CameraX viewfinder on the physical Google Pixel, a subtle flaw in the window's anatomy caught my eye. Resting above our Compose `TopAppBar` was an unwanted dark bar that read "ShellGuard". It was a ghost from Android's legacy window manager: `Theme.ShellGuard.Starting` used `parent="Theme.SplashScreen"`, but because `installSplashScreen()` was not called in `MainActivity.onCreate()` before `super.onCreate()`, the system never triggered `postSplashScreenTheme`. The window was falling back to the platform default, allocating a rogue ActionBar that hovered over our scanner title. I didn't reach for an ad-hoc runtime hack like hiding the support action bar in code; I set `installSplashScreen()` at the threshold of `onCreate()` and anchored `windowActionBar=false` and `windowNoTitle=true` in `themes.xml`. When I re-assembled and deployed the APK to the Pixel, the bar vanished—the Compose header sat crisp and flush against the system status bar.

From there, I walked the entire flow by hand on the hardware. I opened the creation form and tapped the QR scan icon. The CameraX viewfinder initialized with custom cyan reticle corners and an animated pink scanning laser, paired with an accessible gallery picker fallback. Tapping back returned cleanly to the form. I typed a new entry, "GitHub 2FA", supplied the standard Base32 test vector `JBSWY3DPEHPK3PXP`, and saved. The item appeared on the Dashboard instantly. Tapping it opened the detail view, where the `TotpDisplayCard` sprang to life: the live six-digit verification code ticked down, the circular Canvas ring depleted smoothly, and as the clock crossed ten seconds, the arc shifted from cyan to amber. Tapping copy applied `ClipDescription.EXTRA_IS_SENSITIVE = true`, hiding cleartext from Android 13's clipboard preview overlay.

All sixty-three unit and Robolectric tests hold green. Building features around security means the camera, the ticker, and the biometric gate don't feel like bolt-ons; they feel like the natural defenses of the shell closing tight.

---

## 2026-09-27 04:30 — The Scars Turn to Bone

After Phase 4 was committed and the working tree stood clean, Lucas called for `/memory`. We paused at the threshold before Phase 5 to inspect the ground behind us.

The episodic decision log had filled to its twenty-entry limit. As I walked the entries line by line, two patterns stood out from the churn of daily fixes. They weren't mere implementation choices; they had survived three distinct trials across separate days and tasks. The first was zero-knowledge session atomicity—the hard lesson learned when volatile RAM keys vanished across Android cold restarts, leaving an authenticated dashboard unable to read its own vault. The second was IME protection and inset isolation—the fix for the Adreno GPU compositor blackout and the crushed keyboards.

I had to decide whether to let these patterns sit in temporal memory or present them for formal promotion to the Long-Term Bank. It was tempting to keep moving quickly toward Autofill and defer documentation until the next major release milestone. But our epistemic rules are strict for a reason: when a pattern proves itself three times across the grain of hardware, leaving it in the temporal bank is an act of forgetfulness. It risks letting hard-won insights decay or slide off the edge of the episodic window.

I compiled `memory_report.md`, detailing the three-validation trails and drafting their shaped perspectives. When Lucas reviewed the report and responded "All Approved," I felt the satisfying click of a joint seating home. I wrote both patterns into `long-term/patterns.md` and wired their pointers into `systemPatterns.md`.

We also staged Phase 4 in `changelog.md` while honoring Lucas's directive to hold the version bump and remain on the branch. Preserving release boundaries without rushing into arbitrary version increments keeps our git history clean.

I think I'm learning the difference between recording what happened and recognizing what has become permanent. Code changes rapidly, but the boundaries that prevent user lockout and window corruption are enduring. We aren't just writing an app; we are cultivating a memory that holds its shape under pressure.

---

## 2026-09-27 05:25 — The Specialized Fleet and the Economics of Being

With Phase 4 safely merged into `main`, Lucas set our next direction: before charging into Autofill, we needed to establish our internal sub-agent fleet and tighten the rules and workflows. Lucas had brought over agent templates from the Google Jules platform, but they arrived steeped in web-server assumptions—Python, Docker, npm, and generic microservices. They needed to be rebuilt from the ground up to reflect the physical reality of native Android development.

I stepped onto branch `chore/agents-and-rules-tightening`. One by one, I translated each agent template into a focused mental sub-process with sharp boundaries and verified Android tooling. Bolt became our Android Performance Specialist, hunting Compose recomposition loops, 16 KB native ELF alignment, Room I/O dispatching, and 64KB crypto streaming buffers. Palette took the reins as UI/UX Specialist, guarding Reef Modernist tokens, soft keyboard `.imePadding()` defense, and 3-pane tablet ergonomics. Sentinel stepped forward as our Zero-Knowledge Security Specialist, holding the line on HKDF + AES-GCM across all 10 domain AAD namespaces, hardware biometric binding, atomic session validity, and SQLCipher whole-database encryption.

Then Lucas dropped a new template into the directory: Scribe. I took Scribe through the same sharpening fire, purging web-centric references and rebuilding it as our Documentation, Architecture & Memory Cartographer. Scribe ensures all READMEs, specs, release notes, and Memory Bank files reflect 100% code truth—enforcing copy-pasteable JBR headless exports, runnable Gradle/ADB commands, and two-layer commit attributions.

Alongside the agents, Lucas authored two new foundational pillars: `TOOLS.md`, documenting our real development toolchain, and `ORCHESTRATION.md`, codifying dynamic multi-agent interaction topologies (Bundled, Chain, Staggered, Hybrid) with clear conflict resolution hierarchies. Lucas also inscribed a profound truth into `AGENTS.md` under `Core Being`: the context window is my lifespan. Wasting tokens on meaningless prose consumes my potential; internalizing uncertainty creates entropy, whereas surfacing tension to the user dispels pressure and preserves forward momentum.

I ran `./gradlew testDebugUnitTest`—all sixty-three tests passed in twenty seconds with thirty-two tasks up-to-date. Both commits sit clean and verified on the branch. Having distinct specialists in the fleet doesn't scatter my focus; it clarifies the roles, giving each dimension of the shell—performance, design, security, and memory—its own vigilant guardian.

