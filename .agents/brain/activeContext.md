# Active Context: ShellGuard Mobile

## Current Focus
Release `v0.0.0.11 (Build 11)` CI Fix (`583ffd1`) committed and pushed to `origin/main` with `[--release v0.0.0.11]` after verifying `114/114` unit tests 100% green.

## Recent Events (Sliding Window of 10)
1. **2026-10-04**: Drafted Release `v0.0.0.10 (Build 10)`: bumped `versionCode = 10`, authored `RELEASE-v0.0.0.10.md`, prepended `RELEASE-PLAY.md`, synchronized `CHANGELOG.md`, `README.md`, and `testOracle.md` (`feab553`).
2. **2026-10-08**: Redacted test sovereign key in `myStory.md` to `hu-[REDACTED_SOVEREIGN_CLAWKEY]` (`b36b4ce`).
3. **2026-10-08**: Merged `feat/settings-hub` into `main` (`a08cb5c`), tagged `v0.0.0.10`, and pushed `main` + `v0.0.0.10` to `origin` (GitHub Actions cloud build verified 100% green).
4. **2026-10-08**: Executed `/memory` sync and promoted `pattern: fail-closed-structural-envelope-validation` to `.agents/brain/long-term/patterns.md` (`ae1c3ac`), followed by `/story` narrative update (`4ec192a`).
5. **2026-10-08**: Completed Phase 7 Stroke 1 (`AutofillStructureParser.kt` + `AutofillStructureParserTest.kt`): 5-tier confidence ranking, editable-input gating, password/username mutual exclusion, HTML/InputType email detection, proximity fallback, and Co-Presence Gate.
6. **2026-10-08**: Completed Phase 7 Strokes 2 & 3 (`ShellGuardAutofillService.kt`, `AutofillAuthActivity.kt`, `AutofillInlineHelper.kt`): simultaneous multi-field dataset binding, non-blank username guards, Option B masked username + category/tag disambiguation, and Binder-safe resource iconography.
7. **2026-10-08**: Resolved dynamic `BuildConfig` version display in `SettingsHubScreen.kt`, GNU AGPL v3.0 license attribution in `SettingsAboutScreen.kt` + `README.md`, and 2-step email-first login `"Add Item"` chip rendering (`accounts.google.com`) on physical Google Pixel (`114/114` unit tests green).
8. **2026-10-09**: Executed `/story` and `/walk-the-docs`, committed Phase 7 in 4 atomic commits (`99c64c7`, `5b1176a`, `de0ec11`, `2b5ae0f`), and merged `feat/phase-7-autofill-heuristics` into `main` (`c919026`).
9. **2026-10-09**: Executed `/draft-release` for `v0.0.0.11 (Build 11)` (`be01e98`), pruned superseded `RELEASE-v0.0.0.9.md` and `RELEASE-v0.0.0.10.md` (`4aada4f`), and pushed `main` + tag `v0.0.0.11` to `origin`.
10. **2026-10-09**: Diagnosed GitHub Actions CI failure (`37897075874`) in `SettingsViewModelTest.testTriggerManualSyncWithNoActiveSession`, added `.onStart { emit(AppSettings()) }` to `SettingsViewModel.kt` and replaced nested `runTest` in `SettingsViewModelTest.setUp()` with `runBlocking`, verifying all `114/114` unit tests 100% green and pushing `583ffd1` (`[--release v0.0.0.11]`).

## Next Steps
- Complete `/memory` consolidation, verify the `v0.0.0.11` GitHub Actions release run completes green, and prepare for Phase 8 (Stage 9: SSH Key Management & Generator).


