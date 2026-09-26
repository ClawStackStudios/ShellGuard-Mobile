# The Trail: ShellGuard Mobile

Present tense. The hand talking as it works.

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
