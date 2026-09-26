# 🛡️ Contributing to ShellGuard Mobile

Thank you for contributing to **ShellGuard Mobile**! This guide outlines the development environment, architectural principles, verification workflows, and git standards required to maintain the stability, performance, and security of the full secrets vault client.

---

## 🧭 Core Philosophy & Invariants

ShellGuard Mobile is a **sovereign, zero-knowledge, offline-first secrets vault native Android client** designed to pair with self-hosted ShellGuard servers or operate entirely standalone.

1. **Security Precedes Features**: We build features around security, not security around features.
2. **Zero Plaintext at Rest**: All database tables, indices, and data blocks are encrypted at rest with whole-database SQLCipher (AES-256). Sensitive identity keys and master secrets are managed strictly through the hardware-backed Android KeyStore (TEE / StrongBox) and `EncryptedSharedPreferences`.
3. **Zero External Telemetry**: We never embed third-party crash reporting, analytics, or tracking SDKs (no Firebase, Sentry, Datadog). System security events are logged on-device to an encrypted Room audit log (`AuditLogDao`) under the user's sovereign control.
4. **Bitwarden-Model Read-Only Offline Caching**: When disconnected from the self-hosted server, the vault remains 100% accessible for viewing, searching, copying credentials, running TOTP tickers, and performing system Autofill. Mutation actions (create/edit/delete) are strictly guarded in the UI to prevent split-brain synchronization divergence.
5. **Hybrid File-System Vault (CWE-400 CursorWindow Defense)**: Attachments never enter Room database rows directly. Metadata is stored in Room while encrypted byte streams flow to `filesDir/vault_attachments/{id}.enc` bounded to 64KB heap buffers.
6. **16 KB Page-Size Kernel Alignment**: All native dependencies (SQLCipher 4.6.1+) and packaging configurations (`jniLibs.useLegacyPackaging = false`) are compiled for 16 KB page-size compatibility on Android 15+ (API 35/36).

---

## 💻 Development Prerequisites

- **Language**: Kotlin 2.2+
- **JDK**: Java 21 (Bundled Android Studio JetBrains Runtime `jbr` recommended)
- **Target SDK**: Android 16 (API 36 preview) / Android 15 (API 35)
- **Minimum SDK**: Android 7.0 (API 24)

### Headless & Subshell Environment Setup
When executing Gradle or ADB commands in terminal subshells or containerized CI environments, always explicitly export the bundled JBR and container-safe JVM options:

```bash
export JAVA_HOME="/config/Applications/android-studio/jbr"
export PATH="$JAVA_HOME/bin:/config/Android/Sdk/platform-tools:$PATH"
export GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"
```
* `-XX:-UsePerfData` prevents JVM memory mapping crashes in containerized environments.
* `-Djava.io.tmpdir` confines temporary build artifacts within the project tree to prevent permission locks.

---

## 🏛️ Architecture Overview

The codebase is built on **Single-Activity, Single-Module** architecture using Jetpack Compose and Unidirectional Data Flow (MVI):

```
UI (Compose) ──(UserIntent)──> ViewModel ──> Repository ──> Data Source (Room / KeyStore / Ktor)
     ▲                                                                                │
     └────────────────────────── StateFlow<State> ────────────────────────────────────┘
```

- **UI Layer (`ui/`)**: Declarative Jetpack Compose Material 3 screens. Strictly renders immutable state and emits actions. Zero direct business logic.
- **ViewModels (`ui/screens/`)**: Exposes state via reactive `StateFlow` and handles incoming user intents.
- **Dependency Injection (`di/`)**: **Application-Scoped Lazy DI**. Singletons (`database`, `shellGuardClient`, `encryptedDeviceVault`, `syncRepository`, `connectivityMonitor`) are lazily initialized in `AppContainer.kt`. We intentionally avoid Dagger/Hilt to maintain fast builds and zero annotation processor overhead.
- **Cryptographic Engine (`crypto/`)**: Pure cryptographic HKDF-SHA256 and AES-GCM-256 calculations across all 10 domain AAD namespaces, ClawKey Base62 format validation, and SHA-256 identity hashing.
- **Storage Layer (`data/`)**: Room 2.7+ encrypted via SQLCipher open helper factory, backed by hardware-derived AES keys in `AndroidKeyStoreHelper` and `EncryptedDeviceVault`.
- **Remote Layer (`data/remote/`)**: Ktor Client with OkHttp engine supporting cleartext LAN IPs and Tailscale mesh addresses.

---

## 🛡️ Input Hardening & CWE-359 Invariants

All screens accepting cryptographic secrets (passwords, PINs, ClawKeys, SSH private keys) MUST apply:
1. `PasswordVisualTransformation()` paired with an accessible toggleable eye visibility icon.
2. `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` to prevent predictive dictionary learning and third-party keyboard telemetry caching.
3. `.imePadding()` and `.verticalScroll(rememberScrollState())` to prevent the soft keyboard from obscuring input fields.
4. `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0))` on the root activity container to prevent double-subtraction of keyboard insets.

---

## 🔄 The 3-Gate Verification Loop

We stack three verification gates before any code is committed, merged, or released:

### Gate 1: Tests (100% Green Required)
Run the full unit and Robolectric headless test suite:
```bash
./gradlew testDebugUnitTest
```
* Note: Headless Robolectric tests automatically use `FrameworkSQLiteOpenHelperFactory` and headless HMAC KeyStore fallbacks to ensure native binaries do not crash host JVMs.

### Gate 2: Build Compilation
Verify clean build artifact generation:
```bash
./gradlew assembleDebug
```

### Gate 3: Live Verification
Verify interactive UX, the soft keyboard, and edge-to-edge layouts on a physical device or emulator over ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.clawstack.shellguard/.MainActivity
```

---

## 🌿 Git Hygiene & Branching

- **Branch Isolation**: Never work directly on `main`. Create a fresh branch for every task:
  ```bash
  git checkout -b feat/<short-desc>   # For new features
  git checkout -b fix/<short-desc>    # For bug fixes
  git checkout -b chore/<short-desc>  # For infrastructure/scaffolding
  git checkout -b docs/<short-desc>   # For documentation updates
  ```
- **Android Secrets Safety (CRITICAL)**:
  - **NEVER** commit keystores or private keys: `*.jks`, `*.keystore`, `*.p12`, `*.pem`, `debug.keystore`.
  - **NEVER** commit local machine configs: `local.properties`, `.env`, `.env.*`.
  - **NEVER** commit build outputs: `build/`, `**/build/`, `*.apk`, `*.aab`.
  - **NEVER** commit internal agent scratchpads: `.agents/internal/`, `**/scratch/**`.

### Two-Layer Attribution Commit Format
Every commit message must follow this two-layer attribution format:

```git
<type>: <short summary>

User: <the intention, system design, architecture decision, or glue that was provided>
AI: <the concrete implementation, functions, refactors, or tests that were generated>
```

---

<div align="center">
  <sub>Engineered with precision for the ClawStack / ShellGuard ecosystem.</sub>
</div>
