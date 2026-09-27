# 🐚 ShellGuard Mobile — Release v0.0.0.5 (Build 5)

> **Phase 4: TOTP Engine, CameraX ML Kit QR Scanner, Password Generator & Biometric Security Lifecycle**: versionCode 5 (`versionName = "0.0.0.5"`). Delivers the RFC 6238 time-based one-time password (TOTP) algorithmic computation engine, Hardware KeyStore Biometric binding, background auto-lock lifecycle defense, customizable cryptographic Password Generator sheet, CameraX + ML Kit interactive barcode scanner with gallery picker, smooth Canvas countdown visualization with color interpolation, and splash theme ActionBar suppression.

## *Phase 4: TOTP Engine, CameraX Scanner & Biometric Lifecycle*

```text
███████╗██╗   ██╗███████╗██╗     ██╗     ██████╗ ██╗   ██╗ █████╗ ██████╗ ██████╗ 
██╔════╝██║   ██║██╔════╝██║     ██║     ██╔════╝ ██║   ██║██╔══██╗██╔══██╗██╔══██╗
███████╗███████║█████╗   ██║     ██║     ██║  ███╗██║   ██║███████║██████╔╝██║   ██║
╚════██║██╔══██║██╔══╝   ██║     ██║     ██║   ██║██║   ██║██╔══██║██╔══██╗██║   ██║
███████║██║   ██║███████╗███████╗███████╗╚██████╔╝╚██████╔╝██║   ██║██║  ██║██████╔╝
╚══════╝╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝ ╚═════╝  ╚═════╝ ╚═╝   ╚═╝╚═╝  ╚═╝╚═════╝ 
                                                  ~ **ClawStack Mobile Studios©™** ~
```

---

## 🚀 The Core Summary

Welcome to **v0.0.0.5** of **ShellGuard Mobile** — **Phase 4: TOTP Engine, CameraX Scanner & Biometric Security Lifecycle (Build 5)**. This release marks the completion of Phase 4 (Tasks 07 and 08) of the master roadmap, advancing ShellGuard Mobile to **67% completion**.

This release transforms ShellGuard Mobile into a full two-factor authenticator and zero-knowledge identity companion. Users can now generate RFC 6238 TOTP codes across SHA-1, SHA-256, and SHA-512 algorithms, scan QR codes directly with the device camera or pick from the gallery using CameraX and ML Kit Barcode Scanning, and produce cryptographically secure passwords and passphrases. 

Furthermore, this release hardens the application security lifecycle: the vault automatically locks when backgrounded, hardware biometrics (fingerprint / face unlock) gate sensitive item access and vault unlock, and splash theme ActionBar suppression prevents rogue system headers from disrupting the Edge-to-Edge Reef Modernist layout.

All **63 unit and Robolectric tests pass 100% green**, verified live on physical Google Pixel hardware.

---

## 💎 Key Themes & Highlights

### ⏱️ 1. Algorithmic TOTP Engine & Hardware Biometrics (Task 07)
* **RFC 6238 TOTP Engine (`TotpEngine`)**: Computes time-based authentication tokens with HMAC-SHA1/256/512, configurable 6 or 8 digits, dynamic truncation (RFC 4226 §5.4), and Steam Guard 5-character alphanumeric token derivation.
* **Base32 RFC 4648 Decoder (`Base32Decoder`)**: Robust decoding of secret seeds with whitespace/hyphen stripping and padding tolerance.
* **OTPAuth URI Parser (`TotpUriParser`)**: Parses standard `otpauth://totp/...` URIs, extracting secret, issuer, algorithm, digits, and period parameters with clean fallback defaults.
* **Hardware KeyStore Biometric Binding (`AndroidKeyStoreHelper`)**: Enforces hardware biometric gates via `androidx.biometric.BiometricPrompt` with automatic fallback to master password or PIN.
* **Vault Lock Lifecycle (`VaultLockManager`, `AppLifecycleObserver`)**: Configurable auto-lock timeouts (Immediate, 1m, 5m, 15m, 30m, 1h) monitoring application lifecycle to lock the vault upon backgrounding.

### 📷 2. Password Generator, Countdown Canvas & CameraX Scanner (Task 08)
* **Cryptographic Password Generator (`PasswordGeneratorSheet`, `PasswordGenerator`)**: Bottom sheet supporting configurable length (8–64), uppercase, lowercase, numbers, symbols, avoid-ambiguous mode, and multi-word diceware-style passphrases with entropy estimations.
* **Reactive Canvas Countdown Ring (`TotpCountdownRing`)**: Sub-second reactive ticker coroutine Flow driving 60fps smooth Canvas countdown arcs with dynamic color interpolation from Cyan Vent (`#00E5FF`) to Amber Flare (`#FFB300`) to Warning Red.
* **CameraX ML Kit Scanner (`QrScannerScreen`, `QrScannerViewModel`)**: Full-screen camera viewfinder with custom reticle styling, ML Kit Barcode Scanning analysis, flashlight toggle, and accessible gallery QR image picker fallback.
* **Splash Theme ActionBar Suppression**: `installSplashScreen()` invoked before `super.onCreate()` and `res/values/themes.xml` declared with `windowActionBar=false` and `windowNoTitle=true` to eliminate rogue platform headers.

---

## 🧪 Verification & Test Oracle Parity

* **100% Passing Test Oracle**: All **63 unit and Robolectric tests** pass cleanly across cryptographic, local database, remote sync, TOTP calculation, Base32 decoding, URI parsing, and generator modules:
  - `TotpEngineTest.kt` (RFC 6238 test vectors, SHA-1/256/512, 6/8-digit, Steam Guard)
  - `Base32DecoderTest.kt` (RFC 4648 vectors, invalid padding, lowercase normalization)
  - `TotpUriParserTest.kt` (URI parsing, issuer extraction, parameter overrides)
  - `PasswordGeneratorTest.kt` (length bounds, character class enforcement, passphrase count)
  - `VaultLockManagerTest.kt` (timeout durations, lock transitions)
  - `ShellCryptionEngineTest.kt` (HKDF-SHA256, AES-GCM-256 AAD namespaces)
  - `RoomDatabaseTest.kt` (Room DAO operations, cascade deletion)
  - `SyncRepositoryTest.kt` (delta reconciliation, conflict resolution)
* **Clean Pre-Flight Compilation**: Verified via `./gradlew testDebugUnitTest` in containerized headless JBR environment.
* **Physical Hardware Validation**: Tested live on physical Google Pixel (Android 14) with QR scanning, TOTP generation, and biometric authentication.

---

## 📦 Release Artifacts

- **Google Play App Bundle**: `shellguard-mobile-v0.0.0.5.aab` (`versionCode = 5`)
- **Direct Sideload APK**: `shellguard-mobile-v0.0.0.5.apk` (`versionName = "0.0.0.5"`)
- **Git Commit**: Main release commit with release tag `v0.0.0.5`.
