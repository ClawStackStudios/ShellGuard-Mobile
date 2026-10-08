# Active Context: ShellGuard Mobile

## Current Focus
Phase 6 Settings Hub (Stage 7): Drafted Release `v0.0.0.10 (Build 10)`. Bumped `versionCode = 10` and `versionName = "0.0.0.10"` in `app/build.gradle.kts`. Created root release manifest `RELEASE-v0.0.0.10.md`, updated `RELEASE-PLAY.md` with localized Play Store notes under 500 characters, updated `CHANGELOG.md`, `README.md` badges, and `testOracle.md` to reflect 105/105 tests passing 100% green. Ready to commit release artifacts and ask user about release tagging / branch merge.

## Recent Events (Sliding Window of 10)
1. **2026-10-04**: Audited Bitwarden Android Settings menu via ADB on physical Pixel device, authored `settings_taxonomy_proposal.md` and approved `settings_implementation_plan.md`.
2. **2026-10-04**: Formulated Stage 8 Autofill Expansion plan in `ROADMAP.md` and `meta-prompt-ai-studio.md`.
3. **2026-10-04**: Completed Sub-Phase A of Settings Hub: integrated `datastore-preferences`, implemented `SettingsRepository`, and verified with 5/5 unit tests green (`66ad757`).
4. **2026-10-04**: Completed Sub-Phase B of Settings Hub: navigation sealed hierarchy (`Screen.kt`), `SettingsViewModel`, `SettingsHubScreen`, dashboard overflow integration, committed (`9041a3d`).
5. **2026-10-04**: Completed Sub-Phase C of Settings Hub: `SettingsAppearanceScreen`, `SettingsSyncScreen`, NavHost routing, and 13/13 unit tests passing 100% green (`1ef6b70`).
6. **2026-10-04**: Completed Sub-Phase D of Settings Hub: `CircularDialPicker`, `SettingsSecurityScreen`, `PanicPurgeCountdownScreen`, NavHost wiring, and 15/15 unit tests passing 100% green (`3e63cf7`).
7. **2026-10-04**: Completed Sub-Phase E of Settings Hub: `VaultBackupEngine`, `SettingsBackupScreen`, `SettingsAutofillScreen`, `SettingsAboutScreen`, verified with 95/95 unit tests passing 100% green, and committed (`59dbad1`).
8. **2026-10-04**: Implemented Web Parity Enhancements: Polymorphic `items` payload schema and `hu-` ClawKey UI validation integration, verified 105/105 tests green, committed (`ef9f4a8`).
9. **2026-10-04**: Executed `/walk-the-docs`, updating architecture and migration specs, roadmap, and test assertions.
10. **2026-10-04**: Drafted Release `v0.0.0.10 (Build 10)`: bumped `versionCode = 10`, authored `RELEASE-v0.0.0.10.md`, prepended `RELEASE-PLAY.md`, synchronized `CHANGELOG.md`, `README.md`, and `testOracle.md`.

## Next Steps
- Commit the drafted release metadata under two-layer attribution format.
- Merge `feat/settings-hub` into `main` and trigger Git Tag `v0.0.0.10` to deploy via GitHub Actions.
- Advance into Stage 8: Heuristics & Context-Aware Autofill.
