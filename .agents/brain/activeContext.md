# Active Context: ShellGuard Mobile

## Current Focus
Phase 6 Settings Hub (Stage 7): Completed Sub-Phase B (Navigation & Base UI Hub). Created `Screen.kt` navigation sealed hierarchy, `SettingsViewModel` with reactive state combining DataStore and device vault credentials, `SettingsHubScreen` with Reef Modernist category cards, and wired into `VaultDashboardScreen` overflow and `MainActivity.kt` NavHost with dynamic `FLAG_SECURE` toggling. All 10 Settings unit tests passing 100% green. Ready for Sub-Phase C (Appearance & Sync Settings).

## Recent Events (Sliding Window of 10)
1. **2026-10-04**: Refactored `MainActivity` with global `LockScreen` overlay and `shellguard://app/form/` deep link support with pre-filled URI.
2. **2026-10-04**: Configured `ShellGuardAutofillService` to display matched domain strings inline when locked and direct "Add Item" chip when 0 matches exist.
3. **2026-10-04**: Compiled `app-debug.apk` and deployed to Pixel via wireless ADB; verified successful install and live inline behavior.
4. **2026-10-04**: Committed changes under two-layer attribution format (`feat(autofill)` `d2703e3`).
5. **2026-10-04**: Bumped version to `v0.0.0.9 (Build 9)` (`5c6e97d`), updated release notes/changelogs, and shifted Phase 6 release horizon to `0.0.0.10 (Build 10)`.
6. **2026-10-04**: Merged `feat/autofill-inline-chips` into `main`, tagged `v0.0.0.9`, pushed tag to remote, and actively monitored GitHub Actions cloud release build to completion.
7. **2026-10-04**: Audited Bitwarden Android Settings menu via ADB on physical Pixel device, authored `settings_taxonomy_proposal.md` and approved `settings_implementation_plan.md`.
8. **2026-10-04**: Formulated Stage 8 Autofill Expansion plan in `ROADMAP.md` and `meta-prompt-ai-studio.md`.
9. **2026-10-04**: Completed Sub-Phase A of Settings Hub: integrated `datastore-preferences`, implemented `SettingsRepository`, and verified with 5/5 unit tests green (`66ad757`).
10. **2026-10-04**: Completed Sub-Phase B of Settings Hub: navigation sealed hierarchy (`Screen.kt`), `SettingsViewModel`, `SettingsHubScreen`, dashboard overflow integration, and 100% green unit tests (10/10 green).

## Next Steps
- Prompt user for Sub-Phase B commit under two-layer attribution format.
- Execute Sub-Phase C: Build `SettingsAppearanceScreen` (Theme mode, dynamic colors, accents, favicons, compact view) and `SettingsSyncScreen` (server URL, manual sync trigger, sync over cellular, pull-to-refresh).
- Wire navigation routes in `MainActivity.kt` and write Robolectric unit tests for Sub-Phase C.
