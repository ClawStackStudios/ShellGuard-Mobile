# 🐚 ShellGuard Mobile `v0.0.0.30` (Build 30) — Stage 3: Security Lifecycle, Biometrics & Offline Resilience

**Release Date**: 2026-11-18  
**Build Code**: `30` (`versionCode = 30`, `versionName = "0.0.0.30"`)  
**Verification**: **152/152 unit & Robolectric tests passing green** on Cloud CI + verified on physical Google Pixel (`sailfish`).

---

## 🎯 Release Highlights

1. **PIN Keypad & Circular Dial Polish (`#16`)**:
   - Added spring press scale + `ReefPink` border feedback, animated dot fills, empty-input haptic guard, and stable-height `Verifying PIN...` state in `PinPadView.kt` and `LockScreen.kt`.
   - Extracted pure `polarOffsetToSeconds()` in `CircularDialPicker.kt` with 12 o'clock drag boundary clamping (`60s ↔ 5s` teleport guard), `5s`/`15s` haptic snap ticks, `48.dp` stepper targets, and clickable `Default: 15s` reset.
2. **Emergency Panic Purge Cascade Hardening (`#16`)**:
   - Fixed DataStore `.onStart` `15s` countdown race in `PanicPurgeCountdownScreen.kt` so user-configured durations (`5s–60s`) are respected.
   - Executed the 4-step destruction cascade on `Dispatchers.IO` with synchronous `.commit()` in `EncryptedDeviceVault.clearSession()`, full attachment cleanup, and `AppContainer.clearCachedClient()`.
3. **Process-Death Lock Persistence & Offline CRUD Resilience (`#15`)**:
   - Hardened `VaultLockManager.onAppForegrounded()` against clock rollback (`elapsed < 0L || elapsed >= timeout.millis`).
   - Added Airplane Mode offline CRUD + cold-restart + reconnect flush verification in `SyncReconciliationAdversarialTest.kt` and pairwise 10-domain (90-pair) cross-namespace AAD swap tests in `ShellCryptionEngineTest.kt`.
4. **2-Tab Reef Navigation & Performance (`PR #27–#33`)**:
   - Shipped route-gated 2-tab `NavigationBar` (`Vault` | `Settings`), 3-domain `+` Speed-Dial FAB, Pull-to-Refresh, Recents-swipe lock (`VaultTaskCleanupService`), off-main-thread PBKDF2 PIN setup/verify, and cached `ShellGuardClient` pooling.

---

## 📦 Distribution Artifacts
- **Android App Bundle (`.aab`)** & **Universal Signed APK (`.apk`)** built and signed via Cloud CI (`release.yml`).
