# Active Context: ShellGuard Mobile

## Current Focus
Phase 6 Settings Hub (Stage 7): Completed Sub-Phase C (Appearance & Sync Settings). Created `SettingsAppearanceScreen.kt` (Theme Mode radio options, Material You dynamic colors toggle, website favicons toggle, compact view toggle, brand palette preview) and `SettingsSyncScreen.kt` (Reef endpoint connection card, manual delta sync trigger with progress and status banners, cellular sync toggle, pull-to-refresh toggle, and zero-knowledge offline guarantee disclosure). Wired into `MainActivity.kt` NavHost and added unit tests in `SettingsViewModelTest` (8/8 tests passing, 13/13 green total). Ready for Sub-Phase D (Security & Panic Purge Flow).

## Recent Events (Sliding Window of 10)
1. **2026-10-04**: Configured `ShellGuardAutofillService` to display matched domain strings inline when locked and direct "Add Item" chip when 0 matches exist.
2. **2026-10-04**: Compiled `app-debug.apk` and deployed to Pixel via wireless ADB; verified successful install and live inline behavior.
3. **2026-10-04**: Committed changes under two-layer attribution format (`feat(autofill)` `d2703e3`).
4. **2026-10-04**: Bumped version to `v0.0.0.9 (Build 9)` (`5c6e97d`), updated release notes/changelogs, and shifted Phase 6 release horizon to `0.0.0.10 (Build 10)`.
5. **2026-10-04**: Merged `feat/autofill-inline-chips` into `main`, tagged `v0.0.0.9`, pushed tag to remote, and actively monitored GitHub Actions cloud release build to completion.
6. **2026-10-04**: Audited Bitwarden Android Settings menu via ADB on physical Pixel device, authored `settings_taxonomy_proposal.md` and approved `settings_implementation_plan.md`.
7. **2026-10-04**: Formulated Stage 8 Autofill Expansion plan in `ROADMAP.md` and `meta-prompt-ai-studio.md`.
8. **2026-10-04**: Completed Sub-Phase A of Settings Hub: integrated `datastore-preferences`, implemented `SettingsRepository`, and verified with 5/5 unit tests green (`66ad757`).
9. **2026-10-04**: Completed Sub-Phase B of Settings Hub: navigation sealed hierarchy (`Screen.kt`), `SettingsViewModel`, `SettingsHubScreen`, dashboard overflow integration, committed (`9041a3d`).
10. **2026-10-04**: Completed Sub-Phase C of Settings Hub: `SettingsAppearanceScreen`, `SettingsSyncScreen`, NavHost routing, and 13/13 unit tests passing 100% green.

## Next Steps
- Prompt user for Sub-Phase C commit under two-layer attribution format.
- Execute Sub-Phase D: Security & Panic Purge Flow (`SettingsSecurityScreen.kt` with lock timeout, screen capture toggle, sensitive clipboard clear timeout, biometric re-prompt toggle, custom `CircularDialPicker` for panic wipe duration 5s-60s, and full-screen `PanicPurgeCountdownScreen.kt` with pulsing red Canvas rings).
- Write Robolectric tests verifying panic purge duration clamping and countdown flow.
