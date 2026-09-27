# Active Context: ShellGuard Mobile

## Current Focus
Established specialized native Android engineering sub-agent fleet (Bolt, Palette, Sentinel, Scribe), dynamic orchestration topologies, and toolchain specifications on `chore/agents-and-rules-tightening`. Ready to merge into `main` and transition to Phase 5 (Android Autofill Framework & Credential Provider).

## Recent Events (Sliding Window of 10)
1. **2026-09-26**: Executed Phase 2 (Task 03 Ktor API Client & Bidirectional Delta Sync, Task 04 Vault Dashboard & Pod Filters); verified 100% green (23/23 tests pass, APK built and deployed on Google Pixel).
2. **2026-09-26**: Resolved Base62 identity key validation and debug FLAG_SECURE IME blackout on physical Pixel; verified seamless typing, visual cursor retention, and button activation.
3. **2026-09-26**: Created comprehensive `README.md` and mirrored the `ShellGuard-TOTP` documentation suite (`CHANGELOG.md`, `CONTRIBUTING.md`, `RELEASE-PLAY.md`, `ARCHITECTURE.md`, `store-assets/`).
4. **2026-09-26**: Mirrored `ShellGuard-TOTP` GitHub Actions release pipeline (`release.yml`), configured Gradle `signingConfigs` in `app/build.gradle.kts`, and created `RELEASE-v0.0.0.3.md`.
5. **2026-09-26**: Merged Phase 2 into `main`, resolved Robolectric SDK 34 compatibility, and successfully executed GitHub Actions cloud release `v0.0.0.3 (Build 3)` with signed AAB & APK published.
6. **2026-09-26**: Executed Phase 3 (Task 05 Custom Fields & Password History, Task 06 Universal ItemFormScreen & Detail Views); verified 100% green across unit tests (CustomFieldTest, SyncRepositoryTest), build compilation, and live interactive UI flows on physical Google Pixel.
7. **2026-09-26**: Diagnosed and resolved "Vault locked or shellKey missing" on physical Google Pixel; persisted derived `shellKey` in `EncryptedSharedPreferences`, hardened `hasActiveSession()` check, and verified on-device decryption, unmasking, and cold-restart persistence.
8. **2026-09-26**: Tagged and executed automated GitHub Actions cloud release for `v0.0.0.4 (Build 4)` on `main`; signed `.aab` and `.apk` artifacts published to GitHub Releases.
9. **2026-09-27**: Executed and verified Phase 4 (Tasks 07 & 08: RFC 6238 TOTP Engine, CameraX ML Kit QR Scanner, Password Generator Sheet, and Biometric Vault Lifecycle); verified 100% green (63/63 tests pass, `app-debug.apk` built), resolved splash theme rogue ActionBar overlap, and verified live on physical Google Pixel hardware (CameraX overlay, live TOTP code generation, Canvas countdown ring, and sensitive clipboard masking).
10. **2026-09-27**: Converted Google Jules templates into 4 specialized native Android engineering sub-agents (Bolt, Palette, Sentinel, Scribe) with project skills; codified dynamic orchestration topologies in `ORCHESTRATION.md`, documented toolchain in `TOOLS.md`, integrated Core Being token lifespan principles in `AGENTS.md`, and tightened `android-development.md`.

## Next Steps
- Merge `chore/agents-and-rules-tightening` into `main` and push to remote (no tag, no release).
- Create branch `feat/phase-5-autofill-and-credential-provider` for Phase 5 (Tasks 09 & 10 in `ROADMAP.md`).
- Implement `ShellGuardAutofillService`, domain matching engine, and biometric presentation gate.



