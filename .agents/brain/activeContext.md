# Active Context: ShellGuard Mobile

## Current Focus
Phase 6 Settings Hub (Stage 7): Completed Sub-Phase C (Web Parity Enhancements). Updated `VaultBackupEngine.kt` to use the unified polymorphic `items` JSON array schema, perfectly matching the Web App's `ImportExportView` expectations. Connected the `hu-` Sovereign ClawKey input field in `SettingsBackupScreen.kt` providing cross-platform HKDF-SHA256 compatibility. Updated `VaultBackupEngineTest.kt` assertions and ensured 100% green tests. Walking documentation to ensure alignment before committing.

## Recent Events (Sliding Window of 10)
1. **2026-10-04**: Bumped version to `v0.0.0.9 (Build 9)` (`5c6e97d`), updated release notes/changelogs, and shifted Phase 6 release horizon to `0.0.0.10 (Build 10)`.
2. **2026-10-04**: Merged `feat/autofill-inline-chips` into `main`, tagged `v0.0.0.9`, pushed tag to remote, and actively monitored GitHub Actions cloud release build to completion.
3. **2026-10-04**: Audited Bitwarden Android Settings menu via ADB on physical Pixel device, authored `settings_taxonomy_proposal.md` and approved `settings_implementation_plan.md`.
4. **2026-10-04**: Formulated Stage 8 Autofill Expansion plan in `ROADMAP.md` and `meta-prompt-ai-studio.md`.
5. **2026-10-04**: Completed Sub-Phase A of Settings Hub: integrated `datastore-preferences`, implemented `SettingsRepository`, and verified with 5/5 unit tests green (`66ad757`).
6. **2026-10-04**: Completed Sub-Phase B of Settings Hub: navigation sealed hierarchy (`Screen.kt`), `SettingsViewModel`, `SettingsHubScreen`, dashboard overflow integration, committed (`9041a3d`).
7. **2026-10-04**: Completed Sub-Phase C of Settings Hub: `SettingsAppearanceScreen`, `SettingsSyncScreen`, NavHost routing, and 13/13 unit tests passing 100% green (`1ef6b70`).
8. **2026-10-04**: Completed Sub-Phase D of Settings Hub: `CircularDialPicker`, `SettingsSecurityScreen`, `PanicPurgeCountdownScreen`, NavHost wiring, and 15/15 unit tests passing 100% green (`3e63cf7`).
9. **2026-10-04**: Completed Sub-Phase E of Settings Hub: `VaultBackupEngine`, `SettingsBackupScreen`, `SettingsAutofillScreen`, `SettingsAboutScreen`, verified with 95/95 unit tests passing 100% green, and committed (`59dbad1`).
10. **2026-10-04**: Implemented Web Parity Enhancements (Sub-Phase C of Settings fixes): Polymorphic `items` payload schema and `hu-` ClawKey UI validation integration to align with ShellGuard Web.

## Next Steps
- Commit the Web Parity Enhancements under the two-layer attribution format.
- Advance into Stage 8: Heuristics & Context-Aware Autofill.
