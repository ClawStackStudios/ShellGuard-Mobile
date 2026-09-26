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


