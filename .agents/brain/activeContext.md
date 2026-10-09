# Active Context: ShellGuard Mobile

## Current Focus
Phase 7 (Stage 8): Context-Aware Autofill Expansion, 2-Step Login Support & Settings Version/License Alignment completed and verified live on Google Pixel (`sailfish`) and JVM (`114/114` tests green) on branch `feat/phase-7-autofill-heuristics`.

## Recent Events (Sliding Window of 10)
1. **2026-10-04**: Completed Sub-Phase E of Settings Hub: `VaultBackupEngine`, `SettingsBackupScreen`, `SettingsAutofillScreen`, `SettingsAboutScreen`, verified with 95/95 unit tests passing 100% green, and committed (`59dbad1`).
2. **2026-10-04**: Implemented Web Parity Enhancements: Polymorphic `items` payload schema and `hu-` ClawKey UI validation integration, verified 105/105 tests green, committed (`ef9f4a8`).
3. **2026-10-04**: Executed `/walk-the-docs`, updating architecture and migration specs, roadmap, and test assertions.
4. **2026-10-04**: Drafted Release `v0.0.0.10 (Build 10)`: bumped `versionCode = 10`, authored `RELEASE-v0.0.0.10.md`, prepended `RELEASE-PLAY.md`, synchronized `CHANGELOG.md`, `README.md`, and `testOracle.md` (`feab553`).
5. **2026-10-08**: Redacted test sovereign key in `myStory.md` to `hu-[REDACTED_SOVEREIGN_CLAWKEY]` (`b36b4ce`).
6. **2026-10-08**: Merged `feat/settings-hub` into `main` (`a08cb5c`), tagged `v0.0.0.10`, and pushed `main` + `v0.0.0.10` to `origin` (GitHub Actions cloud build verified 100% green).
7. **2026-10-08**: Executed `/memory` sync and promoted `pattern: fail-closed-structural-envelope-validation` to `.agents/brain/long-term/patterns.md` (`ae1c3ac`), followed by `/story` narrative update (`4ec192a`).
8. **2026-10-08**: Completed Phase 7 Stroke 1 (`AutofillStructureParser.kt` + `AutofillStructureParserTest.kt`): 5-tier confidence ranking, editable-input gating, password/username mutual exclusion, HTML/InputType email detection, proximity fallback, and Co-Presence Gate.
9. **2026-10-08**: Completed Phase 7 Strokes 2 & 3 (`ShellGuardAutofillService.kt`, `AutofillAuthActivity.kt`, `AutofillInlineHelper.kt`): simultaneous multi-field dataset binding, non-blank username guards, Option B masked username + category/tag disambiguation, and Binder-safe resource iconography.
10. **2026-10-08**: Resolved dynamic `BuildConfig` version display in `SettingsHubScreen.kt`, GNU AGPL v3.0 license attribution in `SettingsAboutScreen.kt` + `README.md`, and 2-step email-first login `"Add Item"` chip rendering (`accounts.google.com`) on physical Google Pixel (`114/114` unit tests green).

## Next Steps
- Await user confirmation at the Task Completion Gate to commit `feat/phase-7-autofill-heuristics` under the two-layer attribution format.


