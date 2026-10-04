# Active Context: ShellGuard Mobile

## Current Focus
Phase 5 Autofill Refinement: Delivered context-aware inline suggestion chips for domain matching when locked, streamlined the zero-match fallback to an "Add Item" chip with deep-link navigation to pre-populate the current URI, and layered the biometric LockScreen as a global overlay in MainActivity.

## Recent Events (Sliding Window of 10)
1. **2026-10-03**: Published Release `v0.0.0.8 (Build 8)` via GitHub Actions cloud pipeline (Run ID `37173690713`), verified `.aab` and `.apk` assets.
2. **2026-10-03**: Diagnosed cold-start lock bypass; incorporated Vault Unlock Methods and Cold-Start Lock Persistence into roadmap.
3. **2026-10-03**: Investigated missing IME inline autofill chips; authored `credential-provider-spec.md` and overhauled `autofill-service-spec.md`.
4. **2026-10-03**: Completed comprehensive educational research into 2026 Jetpack Compose Autofill patterns.
5. **2026-10-04**: Diagnosed and resolved crash in `AutofillStructureParser.kt` caused by null boolean HTML attributes in WebViews.
6. **2026-10-04**: Verified keyboard inline suggestion chips displaying properly on physical Pixel device above Gboard.
7. **2026-10-04**: Drafted and executed Deep Plan for context-aware URI matching when locked and "Add Item" fallback.
8. **2026-10-04**: Refactored `MainActivity` with global `LockScreen` overlay and `shellguard://app/form/` deep link support with pre-filled URI.
9. **2026-10-04**: Configured `ShellGuardAutofillService` to display matched domain strings inline when locked and direct "Add Item" chip when 0 matches exist.
10. **2026-10-04**: Compiled `app-debug.apk` and deployed to Pixel via wireless ADB; verified successful install.

## Next Steps
- Verify live behavior on Pixel: test tapping "Add Item" when 0 matches exist, and test locked vs unlocked matching chips on active login sites.
- Commit clean changes under two-layer attribution format once confirmed.
