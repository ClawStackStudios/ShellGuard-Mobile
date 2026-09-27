# Active Context: ShellGuard Mobile

## Current Focus
Comprehensive audit and reorientation of all `.agents/` rules, workflows, and skills away from the standalone TOTP companion to the full **ShellGuard Mobile** secrets vault client (`com.clawstack.shellguard`), encoding multi-domain migration (Pearls, Notes, SSH, TOTP), Play Console deployment, and release lifecycle workflows.

## Recent Events (Sliding Window of 10)
1. **2026-09-26**: Executed Phase 1 (Task 01 ShellCryption HKDF + Room SQLCipher, Task 02 Gateway UI & Theme Engine); verified 100% green (16/16 tests pass, `app-debug.apk` built).
2. **2026-09-26**: Forensically aligned Remote Login form with ShellGuard web/TOTP parity (segmented URL bar, upload dropzone, warning box); verified on physical Google Pixel (16/16 tests, APK installed, UI hierarchy verified).
3. **2026-09-26**: Executed Phase 2 (Task 03 Ktor API Client & Bidirectional Delta Sync, Task 04 Vault Dashboard & Pod Filters); verified 100% green (23/23 tests pass, APK built and deployed on Google Pixel).
4. **2026-09-26**: Resolved Base62 identity key validation and debug FLAG_SECURE IME blackout on physical Pixel; verified seamless typing, visual cursor retention, and button activation.
5. **2026-09-26**: Created comprehensive `README.md` and mirrored the `ShellGuard-TOTP` documentation suite (`CHANGELOG.md`, `CONTRIBUTING.md`, `RELEASE-PLAY.md`, `ARCHITECTURE.md`, `store-assets/`).
6. **2026-09-26**: Mirrored `ShellGuard-TOTP` GitHub Actions release pipeline (`release.yml`), configured Gradle `signingConfigs` in `app/build.gradle.kts`, and created `RELEASE-v0.0.0.3.md`.
7. **2026-09-26**: Merged Phase 2 into `main`, resolved Robolectric SDK 34 compatibility, and successfully executed GitHub Actions cloud release `v0.0.0.3 (Build 3)` with signed AAB & APK published.
8. **2026-09-26**: Executed Phase 3 (Task 05 Custom Fields & Password History, Task 06 Universal ItemFormScreen & Detail Views); verified 100% green across unit tests (CustomFieldTest, SyncRepositoryTest), build compilation, and live interactive UI flows on physical Google Pixel.
9. **2026-09-26**: Diagnosed and resolved "Vault locked or shellKey missing" on physical Google Pixel; persisted derived `shellKey` in `EncryptedSharedPreferences`, hardened `hasActiveSession()` check, and verified on-device decryption, unmasking, and cold-restart persistence.
10. **2026-09-26**: Tagged and executed automated GitHub Actions cloud release for `v0.0.0.4 (Build 4)` on `main`; signed `.aab` and `.apk` artifacts published to GitHub Releases.

## Next Steps
- Begin Phase 4: TOTP Engine & Biometric Security Lifecycle (Tasks 07 & 08 in `ROADMAP.md`).
- Branch onto `feat/phase-4-totp-and-biometrics` for RFC 6238 TOTP computation, CameraX ML Kit QR scanning, password generator, and biometric lock screen.


