---
trigger: always_on
description: Comprehensive Android development invariants, MVI architecture, storage, security, 16 KB alignment, and headless JVM build standards for ShellGuard Mobile (Full Android Vault Client).
---

# 🤖 Android Development & Architecture Invariants — ShellGuard Mobile

> **Core Objective:** Establish immutable engineering guardrails for native Android development within the **ShellGuard Mobile** (Full Vault Client) codebase.

---

## 1. Platform Constraints & Headless Execution Environment

### Platform Boundaries
- **Client-Side Only**: No server runtime, no Firebase, no Google Workspace APIs. All network I/O is direct HTTP/HTTPS from the app process to the self-hosted ShellGuard instance.
- **Single-Activity, Single-Module**: Use Compose Navigation for all screens. Package: `com.clawstack.shellguard`. No Activity-per-screen patterns.
- **Dedicated System Services**:
  - `ShellGuardAutofillService` extending Android `AutofillService` (`BIND_AUTOFILL_SERVICE`).
  - `ShellGuardQuickTileService` extending Android `TileService` (`BIND_QUICK_SETTINGS_TILE`).
- **No NDK / C++**: Native code is strictly bundled via verified precompiled dependencies (SQLCipher 4.6.1+).

### Subshell & Headless Build Environment Invariants
When executing Gradle, ADB, or JVM test commands in subshells or headless CI containers, never rely on default environment variables. Always explicitly export the bundled Android Studio JBR and platform tools:

```bash
export JAVA_HOME="/config/Applications/android-studio/jbr"
export PATH="$JAVA_HOME/bin:/config/Android/Sdk/platform-tools:$PATH"
export GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"
```

- **JVM Flags**: `-XX:-UsePerfData` prevents memory crashes in containerized environments.
- **Temp Directory**: Isolating `java.io.tmpdir` to `app/build/tmp` prevents permission errors and file locks.

### Cloud CI Runner & SDK Invariants (GitHub Actions)
- **Avoid Legacy SDK Actions**: Never use third-party actions (such as `android-actions/setup-android@v3`) that invoke `sdkmanager tools`. Google has removed the legacy `tools` package from the remote SDK repository, causing exit code 1 build failures.
- **Runner-Native Android SDK**: GitHub Actions `ubuntu-latest` runners already pre-install the Android SDK at `/usr/local/lib/android/sdk` (`ANDROID_HOME`). Accept licenses directly via runner-native tooling:
  ```yaml
  - name: 🤖 Setup Android SDK
    if: steps.detect.outputs.tag != ''
    run: |
      yes | sdkmanager --licenses || true
  ```
- **CLI Release Observability**: When monitoring cloud releases or troubleshooting pipeline failures, use `gh` CLI commands:
  ```bash
  gh run list -L 5
  gh run view <run-id> --log-failed
  gh run view --job=<job-id>
  gh release view <tag>
  ```

---

## 2. Architecture & MVI Layer Boundaries

| Layer | Technology |
|---|---|
| **State** | MVI (Unidirectional Data Flow) |
| **DI** | Dagger Hilt |
| **Local DB** | Room 2.7+ (SQLCipher whole-database encryption via `ShellGuardDatabase`) |
| **Sensitive Secrets** | EncryptedSharedPreferences (Jetpack Security, Android Keystore) |
| **UI Framework** | Jetpack Compose + Material 3 (no custom external widget libraries) |
| **Domains** | Vault Pearls (Logins), Secure Notes, SSH Keys, Secure Attachments, TOTP |

### Layer Data Flow
```
UI (Compose) ──(UserIntent)──> ViewModel ──> UseCase ──> Repository ──> Local Room / Remote Ktor
     ▲                                                                                │
     └────────────────────────── StateFlow<State> ────────────────────────────────────┘
```

- **UI (Compose)**: Renders immutable `State`, emits `UserIntent` actions. Zero business logic.
- **ViewModel**: Exposes `StateFlow<State>`, handles `UserIntent`. No Android framework imports beyond Compose.
- **UseCase**: Single-responsibility domain operations. Pure Kotlin, no Android framework imports.
- **Repository**: Data abstraction layer. Manages local caching vs. remote bidirectional synchronization.
- **Data Source**: Concrete I/O (Room DAOs, Ktor HTTP client, KeyStore hardware vault).

---

## 3. Storage, Cryptographic Boundaries & Hybrid File-System Vault

### A. Room Database & SQLCipher
All structured user data (pearls, notes, keys, audit logs, caches) is stored in Room 2.7+ encrypted at rest via SQLCipher whole-database encryption.
- **Bounded SQLite Chunking (999 Parameter Invariant)**: Whenever executing Room queries, deletions, or batch lookups referencing dynamic collections (`IN (:ids)` or `NOT IN (:ids)`), the collection MUST be chunked into batches of ≤ 500 items (`collection.chunked(500)`). Passing collections exceeding 999 items triggers fatal `SQLiteException` (too many SQL variables), and passing empty collections to `NOT IN` creates SQLite syntax errors. Batch operations must use in-memory set differences partitioned across 500-item chunks.

### B. Hybrid File-System Vault (CWE-400 CursorWindow Defense)
- Android enforces a hard **2MB `CursorWindow` limit** on SQLite query rows.
- Storing multi-megabyte attachments directly as inline Room BLOBs is **strictly prohibited**.
- Room stores only lightweight metadata (`id`, `ownerUuid`, `fileName`, `sizeBytes`, `mimeType`, `localFilePath`).
- Ciphertext payload bytes stream directly to `context.filesDir/vault_attachments/{id}.enc` via `CipherInputStream` and `CipherOutputStream`, bounding memory allocations to `<= 64KB` buffers.

### C. 10 Domain-Specific AAD Namespaces
All cryptographic operations use HKDF-SHA-256 + AES-GCM-256 with domain-bound Additional Authenticated Data (AAD):
- `vault_pearls:{id}` — Pearl secret (password)
- `vault_pearls_totp:{id}` — Pearl TOTP secret
- `vault_pearls_custom:{id}` — Pearl custom fields JSON
- `vault_pearls_history:{id}` — Pearl password history JSON
- `vault_secure_notes:{id}` — Secure note content
- `vault_secure_notes_custom:{id}` — Secure note custom fields JSON
- `vault_ssh_keys:{id}` — SSH private key value
- `vault_ssh_keys_custom:{id}` — SSH key custom fields JSON
- `vault_secure_attachments:{id}` — Attachment file payload
- `totp_backup:{ownerUuid}` — Interoperable `sgtotp.bak` bridge

### D. Biometric Recovery State Machine
Hardware biometric keys configured with `setInvalidatedByBiometricEnrollment(true)` are permanently invalidated when the user adds or modifies fingerprints/face data in system settings. `Cipher.init()` throws `KeyPermanentlyInvalidatedException`. The app implements an automated recovery state machine routing to Master Password/PIN fallback, regenerating the hardware key, and re-sealing the master secret without user lockout or data loss.

### E. EncryptedSharedPreferences (Android KeyStore)
Master identity keys, session tokens, derived symmetric keys (`shellKey`), and PIN hashes are stored exclusively in `EncryptedSharedPreferences` (backed by Android KeyStore AES-256-GCM hardware encryption). Never store raw identity keys or unencrypted master secrets in Room.

### F. Zero-Knowledge Session Atomicity & Cold-Restart Key Persistence
- **The Atomic Session Invariant**: An active session requires BOTH transport authorization (`sessionToken`) and cryptographic capability (`shellKey`). `hasActiveSession()` must strictly verify `getInMemoryShellKey() != null` (either cached in memory or re-hydrated from `EncryptedSharedPreferences`). A session with a valid network token but a missing decryption key is an invalid split-brain state and is strictly prohibited.
- **Key Re-Hydration Across Lifecycles**: Derived symmetric keys (`shellKey`) must be stored at rest in `EncryptedSharedPreferences` so they survive Android process terminations and cold restarts without prompting the user on every app launch. `getInMemoryShellKey()` must dynamically re-hydrate the in-memory cache from encrypted preferences if the RAM reference was cleared.
- **Frictionless Gateway Fallback**: If an invalid session or missing key forces a redirect to the `GatewayScreen`, the client must preserve and pre-fill server connection parameters (`protocol`, `host`, `port`) so the user only needs to supply their key/file to restore access.
- **Session Zeroization**: On explicit user lock or logout, both volatile RAM references AND persisted KeyStore preferences (`KEY_SHELL_KEY`, `KEY_SESSION_TOKEN`) must be actively zeroized.
- *(Ratified Pattern: see [long-term/patterns.md § pattern: zero-knowledge-session-atomicity](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/.agents/brain/long-term/patterns.md))*.

### G. Fail-Closed Cryptography vs. UI Graceful Degradation
- **Fail-Closed Boundary**: Cryptographic operations, key derivations, and session authentications MUST strictly fail closed. If field decryption fails, the operation must return `Result.failure` or throw an explicit security exception. It must **NEVER** degrade gracefully by returning raw JSON ciphertext strings, empty dummy secrets, or unauthenticated session tokens. Returning raw ciphertext strings causes downstream edits to re-encrypt the ciphertext envelope, producing nested ciphertext and permanently destroying user data.
- **UI Exception Presentation**: While the cryptographic and domain layers strictly fail closed, the presentation layer may handle the resulting `Result.failure` gracefully by displaying a non-blocking error badge or retry card with an unconditional navigation exit route.

### H. Two-Phase Reconciliation Invariants
- **Confirmed Remote ACK Before Release**: Local `PENDING_DELETE` tombstones must NEVER be purged from Room until the remote server returns an explicit HTTP 200 or 204 response. Purging tombstones prematurely causes failed network requests to resurrect deleted records on subsequent delta pulls.
- **Mutex Queuing Over tryLock()**: Multi-domain sync pipelines must use coroutine mutex serialization (`syncMutex.withLock`) rather than non-blocking `tryLock()`. `tryLock()` silently drops concurrent user-initiated mutations or test executions with false-positive success.
- **Conflict-Aware Downstream Ingestion**: Incoming remote sync entities must be filtered against local `PENDING_SYNC` and `PENDING_DELETE` IDs to ensure remote delta pulls do not overwrite fresh local modifications.

### I. Testable Platform Abstractions
- **Constructor Test Hooks**: Any component wrapping Android OS system singletons (`ConnectivityManager`, `TelephonyManager`, `BiometricManager`) MUST provide explicit constructor test parameters (`initialOnlineOverride: Boolean? = null`) or test-hook mutators. Never assume Robolectric or headless JVM stubs reflect valid connected states.

---

## 4. UI, Theming, Motion & IME Hardening

- **Dynamic Theming**: Never hardcode static brand color tokens in screen composables; bind strictly to `MaterialTheme.colorScheme` and `LocalShellGuardColors`.
- **Default Theme Accent**: The canonical default theme accent is `ThemeAccent.REEF_DEFAULT` (Reef Pink `#E4048A`). 6 Curated Theme Accents supported via Settings.
- **Adaptive Master-Detail Ergonomics**:
  - Compact Phones: Fluid single-column navigation.
  - Tablets & Foldables (>= 840dp): 3-pane layout (`SidebarFolderTree` 240dp, `ItemListPane` 340dp, `ItemDetailPane` weight 1f) achieving **100% layout parity with the desktop web client**.
- **Claw Re-Prompt Gate**: Sensitive item protection (`reprompt == true`) intercepts password reveal and copy actions, enforcing a `BiometricPrompt` or PIN challenge before exposing plaintext.
- **Sensitive Clipboard Masking (CWE-359)**:
  Clipboard copies apply `ClipDescription.EXTRA_IS_SENSITIVE = true` to suppress visual cleartext previews in Android 13+ clipboard overlays, paired with an automated 30s/60s background scrubbing timer.
- **Soft Keyboard & Scrolling**:
  All interactive form/input screens must apply `.imePadding()` and `.verticalScroll(rememberScrollState())` to prevent the soft keyboard from obscuring inputs. Action menus must expand **upward** (dropup).
- **Root Scaffold Inset Isolation**:
  When child screen composables apply `.statusBarsPadding()` and `.imePadding()`, the root Activity `Scaffold` must configure `contentWindowInsets = WindowInsets(0, 0, 0, 0)`. Failing to zero the Scaffold insets causes double-subtraction of keyboard height (crushing the available scroll layout to zero height when the IME opens).
- **FLAG_SECURE Debug Scoping**:
  `WindowManager.LayoutParams.FLAG_SECURE` must be scoped strictly to release builds (`if (!BuildConfig.DEBUG)`). On legacy GPU drivers (e.g. Snapdragon 821 / Adreno 530), enforcing `FLAG_SECURE` in debug builds causes the hardware surface composer to black out the application window whenever the software keyboard overlay opens, while also blocking ADB screenshot and test inspection.
- **Sensitive Key Masking & IME Protection**:
  All cryptographic, seed, or secret input fields (passwords, PINs, seeds, keys) MUST apply:
  - `PasswordVisualTransformation()` (paired with an accessible toggleable eye icon).
  - `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` to prevent predictive dictionary learning and third-party keyboard telemetry caching.
- **Fail-Safe Navigation & Unconditional Exit Routes**:
  Every screen composable that renders an error, locked, or unauthenticated UI state MUST provide an explicit, accessible navigation exit route (`onBackClick` or close action) alongside any retry/re-auth action. Solitary "Retry" buttons that depend on pre-existing session state create fatal navigational traps when the underlying failure is terminal (e.g. cleared in-memory keys, expired session tokens). The return path to a safe parent surface (Dashboard or Gateway) must always remain visible and unobstructed.
- **Splash Theme ActionBar Suppression & Theme Inheritance**:
  `Theme.ShellGuard.Starting` uses `parent="Theme.SplashScreen"`. To ensure the window correctly transitions to `Theme.ShellGuard` (`NoActionBar`) without allocating a rogue platform ActionBar over Compose `TopAppBar` headers, `MainActivity.onCreate()` MUST invoke `installSplashScreen()` BEFORE `super.onCreate()`. In addition, `res/values/themes.xml` must explicitly declare `windowActionBar = false` and `windowNoTitle = true`.
- *(Ratified Pattern: see [long-term/patterns.md § pattern: cwe-359-ime-protection-and-inset-isolation](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/.agents/brain/long-term/patterns.md))*.

---

## 5. Network & Cleartext HTTP Specification

Cleartext HTTP is **intentional** for this project to support local Unraid/TrueNAS and home lab servers where TLS is not available.
- Do NOT flag, "fix", or remove cleartext HTTP.
- Configured via `res/xml/network_security_config.xml`:
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <network-security-config>
      <base-config cleartextTrafficPermitted="true">
          <trust-anchors>
              <certificates src="system" />
              <certificates src="user" />
          </trust-anchors>
      </base-config>
  </network-security-config>
  ```
- Referenced in `AndroidManifest.xml` via `android:networkSecurityConfig="@xml/network_security_config"` and `android:usesCleartextTraffic="true"`.
- **Ktor OkHttp Engine**: Configured with `ConnectionSpec.CLEARTEXT`, `COMPATIBLE_TLS`, and `MODERN_TLS`, routing seamlessly through active Android `VpnService` routes for transparent Tailscale and WireGuard mesh traversal.

---

## 6. 16 KB Memory Page-Size Alignment & Packaging

Android 15+ (API 35/36) mandates 16 KB page-aligned native binaries:
- Use SQLCipher `4.6.1+` compiled with 16 KB ELF segment alignment.
- Configure `jniLibs.useLegacyPackaging = false` in `app/build.gradle.kts` to store `.so` libraries uncompressed and page-aligned inside APKs/AABs.

---

## 7. Bidirectional Sync, Bitwarden Offline Engine & Migration Integrity

- **Bidirectional Delta Reconciliation**: When connected, the client initiates bidirectional sync (pushes local pending changes, pulls server deltas) with server-wins conflict resolution based on timestamp comparison.
- **Bitwarden-Model Read-Only Offline Caching**:
  - When disconnected from the server (`OfflineReadOnly`), the vault remains 100% accessible for viewing, searching, copying credentials, running TOTP tickers, and performing system Autofill.
  - **Offline Mutation Guards**: Creating, editing, or deleting items is strictly disabled in the UI (FAB disabled with tooltip, edit/delete actions dimmed) to eliminate split-brain synchronization divergence.
  - Top bar displays an amber `OfflineReadOnly` status banner.
  - Reconnection is handled automatically via Android `ConnectivityManager.NetworkCallback` with an automated health probe (`GET /api/health`), transitioning to `OnlineSynced`, clearing the banner, and executing a delta pull without user friction.
- **Multi-Format Backup & Migration**:
  - `BackupManager` exports/imports full vault backups (`.sgvault.bak`) in AES-256 ShellCrypted JSON envelopes with SHA-256 checksum verification.
  - Supports `.sgtotp.bak` for 1:1 interoperability with the ShellGuard-TOTP companion app.
  - Supports Bitwarden JSON intake with pre-DAO deduplication.

---

## 8. ClawKey Identity, Autofill & Deduplication

- **Sovereign Key Format**: The ShellGuard ClawKey format is strictly `hu-` (human master key) or `lb-` (agent key) followed by 64 Base62 alphanumeric characters (`[0-9a-zA-Z]`, total length: 67). Never restrict to lowercase hexadecimal, as web client generation and server identity JSON exports use Base62.
- **Single Source Validator**: All ClawKey input surfaces (Gateway login, Vault creation, Lock screen, Settings import) must use `ClawKeyValidator.isValid()`.
- **Pre-DAO Fingerprint Deduplication**: Backup import engines must deduplicate incoming records by normalized `secret` + `title` fingerprint prior to DAO insertion, preventing duplicate UUID false negatives.
- **Autofill `UriMatchMode` Invariants**: The autofill domain matcher supports 5 algorithms (`BASE_DOMAIN`, `HOST`, `EXACT`, `STARTS_WITH`, `NEVER`) enabling exact port matching for multi-tenant local home labs (`http://192.168.1.50:8080` vs `http://192.168.1.50:9000`).
- **Android 11+ Inline Presentation**: Supports keyboard suggestion chips above Gboard/SwiftKey alongside standard popup dropdowns.
- **Algorithmic TOTP Engine & CameraX Pipeline**:
  - RFC 6238 TOTP engine supporting HMAC-SHA1/256/512, 6/8 digits, dynamic truncation (RFC 4226 §5.4), and Steam Guard 5-character alphanumeric token derivation.
  - Sub-second reactive ticker coroutine Flow emitting progress for 60fps smooth Canvas countdown arcs with dynamic color interpolation (Cyan ➔ Amber ➔ Red).
  - CameraX viewfinder bound to ML Kit `BarcodeScanning` via `ImageAnalysis.Analyzer` with custom reticle styling, flashlight toggle, and accessible gallery picker fallback.

---

## 9. Robolectric, Headless KeyStore & ProGuard/R8 Invariants

- **KeyStore Headless JVM Fallback**: KeyStore wrapper classes (`AndroidKeyStoreHelper`, `EncryptedDeviceVault`) must provide a fallback mechanism to HMAC-derived `SecretKeySpec` for headless JVM unit tests when `AndroidKeyStore` is absent.
- **Robolectric Framework SQLite Open Helper**: When configuring Room databases (`ShellGuardDatabase`), always detect Robolectric via `Class.forName("org.robolectric.Robolectric")` and assign `FrameworkSQLiteOpenHelperFactory()` to prevent host `UnsatisfiedLinkError` crashes against native SQLCipher binaries.
- **Robolectric Target SDK Ceiling (`sdk=34`)**: When `targetSdk = 36`, Robolectric's `DefaultSdkProvider` fails with `UnsupportedOperationException` on host/CI runners if tests run against unsupported SDK 36. Always declare `sdk=34` in `app/src/test/resources/robolectric.properties` and annotate Robolectric test classes with `@Config(sdk = [34])`.
- **ProGuard / R8 Release Hardening (`app/proguard-rules.pro`)**:
  ProGuard rules in release builds strictly protect reflection, serialization, and JNI bridges from being stripped by R8:
  - `-keep class net.zetetic.** { *; }` (SQLCipher JNI preservation).
  - Kotlinx Serialization companion serializers (`@Serializable`).
  - Room `@Dao` interfaces and `@Entity` models.
  - Strip `Log.d` / `Log.v` debug logs in release (`-assumenosideeffects class android.util.Log`).
  - Zero attempt to "obfuscate native code" via ProGuard (native `.so` binaries are precompiled ELF machine code).
- **Test Oracle Audit on Refactor**: Whenever an architectural rule or UI layout changes, all existing test classes in `app/src/test` MUST be audited for obsolete assertions.

---

## 10. Monotonic Versioning & Release Verification

- **Dynamic Binding**: User-facing version labels must bind dynamically to `BuildConfig.VERSION_NAME`.
- **Google Play Monotonicity**: Every release bundle requires a strictly incremented monotonic `versionCode` (+1).
- **Pre-Release Gate**: Full test suite (`./gradlew testDebugUnitTest`) and build compilation (`./gradlew assembleDebug`) must pass green before committing or version bumping.
