# Active Context: ShellGuard Mobile

## Current Focus
Comprehensive audit and reorientation of all `.agents/` rules, workflows, and skills away from the standalone TOTP companion to the full **ShellGuard Mobile** secrets vault client (`com.clawstack.shellguard`), encoding multi-domain migration (Pearls, Notes, SSH, TOTP), Play Console deployment, and release lifecycle workflows.

## Recent Events (Sliding Window of 10)
1. **2026-09-25**: Reoriented `.agents/rules/android-development.md` and `AGENTS.md` to full mobile client architecture (bidirectional sync, Hybrid File-System attachments, R8 preservation).
2. **2026-09-25**: Reoriented `.agents/rules/zero-knowledge-migration.md`, `migration-and-ingest.md`, `play-console-release-workflow.md`, and CI signing skills to full multi-domain vault client.
3. **2026-09-25**: Executed forensic Bitwarden Android parity comparison; expanded `ui-ux-design-system.md` §10 with 6 settings sub-screens, confirming 98%+ MVP coverage.
4. **2026-09-25**: Scaffolded Stage 0 Android baseline (Gradle 9.3.1, SQLCipher 4.6.1+, AppContainer lazy DI, FLAG_SECURE, cleartext LAN config); verified green.
5. **2026-09-26**: Executed Phase 1 (Task 01 ShellCryption HKDF + Room SQLCipher, Task 02 Gateway UI & Theme Engine); verified 100% green (16/16 tests pass, `app-debug.apk` built).
6. **2026-09-26**: Forensically aligned Remote Login form with ShellGuard web/TOTP parity (segmented URL bar, upload dropzone, warning box); verified on physical Google Pixel (16/16 tests, APK installed, UI hierarchy verified).
7. **2026-09-26**: Executed Phase 2 (Task 03 Ktor API Client & Bidirectional Delta Sync, Task 04 Vault Dashboard & Pod Filters); verified 100% green (23/23 tests pass, APK built and deployed on Google Pixel).
8. **2026-09-26**: Resolved Base62 identity key validation and debug FLAG_SECURE IME blackout on physical Pixel; verified seamless typing, visual cursor retention, and button activation.
9. **2026-09-26**: Created comprehensive `README.md` and mirrored the `ShellGuard-TOTP` documentation suite (`CHANGELOG.md`, `CONTRIBUTING.md`, `RELEASE-PLAY.md`, `ARCHITECTURE.md`, `store-assets/`).
10. **2026-09-26**: Mirrored `ShellGuard-TOTP` GitHub Actions release pipeline (`release.yml`), configured Gradle `signingConfigs` in `app/build.gradle.kts`, and created `RELEASE-v0.0.0.3.md`; verified green (23/23 tests, assembleDebug).

## Next Steps
- Commit the CI release pipeline and signing configuration under two-layer attribution format.
- Evaluate version bump (`v0.0.0.4 (Build 4)` per `ROADMAP.md`).
- Merge `feat/phase-2-ktor-sync-and-dashboard` into `main` and branch into `feat/phase-3-vault-domains-and-editor`.

