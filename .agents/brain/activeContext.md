# Active Context: ShellGuard Mobile

## Current Focus
Phase 6 Settings Hub (Stage 7): Completed Sub-Phase E (Backup, Restore & Autofill Prep). Implemented `VaultBackupEngine.kt` providing full-vault dual-mode backup protection (`ACTIVE_KEY` via HKDF-SHA256 vs `CUSTOM_PASSPHRASE` via PBKDF2-SHA256 600,000 iterations matching Web client parity), plaintext JSON export, format sniffing, and Bitwarden unencrypted JSON import. Created `SettingsBackupScreen.kt`, `SettingsAutofillScreen.kt` (system service status, inline suggestion toggle, Stage 8 heuristics teaser), and `SettingsAboutScreen.kt` (system diagnostics, 16 KB page-size alignment, KeyStore AES-256-GCM, SQLCipher, GPL 3.0). Wired into `MainActivity.kt` NavHost. Verified with 95/95 unit tests passing 100% green. Ready to commit Sub-Phase E and prepare version bump to `v0.0.0.10 (Build 10)`.

## Recent Events (Sliding Window of 10)
1. **2026-10-04**: Committed changes under two-layer attribution format (`feat(autofill)` `d2703e3`).
2. **2026-10-04**: Bumped version to `v0.0.0.9 (Build 9)` (`5c6e97d`), updated release notes/changelogs, and shifted Phase 6 release horizon to `0.0.0.10 (Build 10)`.
3. **2026-10-04**: Merged `feat/autofill-inline-chips` into `main`, tagged `v0.0.0.9`, pushed tag to remote, and actively monitored GitHub Actions cloud release build to completion.
4. **2026-10-04**: Audited Bitwarden Android Settings menu via ADB on physical Pixel device, authored `settings_taxonomy_proposal.md` and approved `settings_implementation_plan.md`.
5. **2026-10-04**: Formulated Stage 8 Autofill Expansion plan in `ROADMAP.md` and `meta-prompt-ai-studio.md`.
6. **2026-10-04**: Completed Sub-Phase A of Settings Hub: integrated `datastore-preferences`, implemented `SettingsRepository`, and verified with 5/5 unit tests green (`66ad757`).
7. **2026-10-04**: Completed Sub-Phase B of Settings Hub: navigation sealed hierarchy (`Screen.kt`), `SettingsViewModel`, `SettingsHubScreen`, dashboard overflow integration, committed (`9041a3d`).
8. **2026-10-04**: Completed Sub-Phase C of Settings Hub: `SettingsAppearanceScreen`, `SettingsSyncScreen`, NavHost routing, and 13/13 unit tests passing 100% green (`1ef6b70`).
9. **2026-10-04**: Completed Sub-Phase D of Settings Hub: `CircularDialPicker`, `SettingsSecurityScreen`, `PanicPurgeCountdownScreen`, NavHost wiring, and 15/15 unit tests passing 100% green (`3e63cf7`).
10. **2026-10-04**: Completed Sub-Phase E of Settings Hub: `VaultBackupEngine`, `SettingsBackupScreen`, `SettingsAutofillScreen`, `SettingsAboutScreen`, in-memory test database isolation, and 95/95 unit tests passing 100% green.

## Next Steps
- Prompt user for Sub-Phase E commit under two-layer attribution format.
- Evaluate Version Bump Gate for Phase 6 closure (`v0.0.0.10 (Build 10)` in `app/build.gradle.kts`, `productVersion.md`, and changelogs).
- Advance into Stage 8: Heuristics & Context-Aware Autofill.
