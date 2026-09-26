# Active Context: ShellGuard Mobile

## Current Focus
Comprehensive audit and reorientation of all `.agents/` rules, workflows, and skills away from the standalone TOTP companion to the full **ShellGuard Mobile** secrets vault client (`com.clawstack.shellguard`), encoding multi-domain migration (Pearls, Notes, SSH, TOTP), Play Console deployment, and release lifecycle workflows.

## Recent Events (Sliding Window of 10)
1. **2026-09-25**: Reoriented `.agents/rules/zero-knowledge-migration.md`, `migration-and-ingest.md`, `play-console-release-workflow.md`, and CI signing skills to full multi-domain vault client.
2. **2026-09-25**: Executed forensic Bitwarden Android parity comparison; expanded `ui-ux-design-system.md` §10 with 6 settings sub-screens, confirming 98%+ MVP coverage.
3. **2026-09-25**: Scaffolded Stage 0 Android baseline (Gradle 9.3.1, SQLCipher 4.6.1+, AppContainer lazy DI, FLAG_SECURE, cleartext LAN config); verified green.
4. **2026-09-26**: Executed Phase 1 (Task 01 ShellCryption HKDF + Room SQLCipher, Task 02 Gateway UI & Theme Engine); verified 100% green (16/16 tests pass, `app-debug.apk` built).
5. **2026-09-26**: Forensically aligned Remote Login form with ShellGuard web/TOTP parity (segmented URL bar, upload dropzone, warning box); verified on physical Google Pixel (16/16 tests, APK installed, UI hierarchy verified).
6. **2026-09-26**: Executed Phase 2 (Task 03 Ktor API Client & Bidirectional Delta Sync, Task 04 Vault Dashboard & Pod Filters); verified 100% green (23/23 tests pass, APK built and deployed on Google Pixel).
7. **2026-09-26**: Resolved Base62 identity key validation and debug FLAG_SECURE IME blackout on physical Pixel; verified seamless typing, visual cursor retention, and button activation.
8. **2026-09-26**: Created comprehensive `README.md` and mirrored the `ShellGuard-TOTP` documentation suite (`CHANGELOG.md`, `CONTRIBUTING.md`, `RELEASE-PLAY.md`, `ARCHITECTURE.md`, `store-assets/`).
9. **2026-09-26**: Mirrored `ShellGuard-TOTP` GitHub Actions release pipeline (`release.yml`), configured Gradle `signingConfigs` in `app/build.gradle.kts`, and created `RELEASE-v0.0.0.3.md`.
10. **2026-09-26**: Merged Phase 2 into `main`, resolved Robolectric SDK 34 compatibility, and successfully executed GitHub Actions cloud release `v0.0.0.3 (Build 3)` with signed AAB & APK published.

## Next Steps
- Evaluate version bump to `v0.0.0.4 (Build 4)` in `app/build.gradle.kts` for Phase 3 baseline.
- Branch into `feat/phase-3-vault-domains-and-editor` to begin Task 05 (Custom Fields & Tags) and Task 06 (Universal ItemFormScreen & Detail Views).

