# Active Context: ShellGuard Mobile

## Current Focus
Autofill & Credential Provider Dual-Stack Research: Completed extensive 2026 API research, cementing the split between standard Compose Autofill (`Modifier.semantics`) and Passkey integration (`androidx.credentials:credentials`). Consolidated learnings into brain files.

## Recent Events (Sliding Window of 10)
1. **2026-10-03**: Added `ShellCryptionEngine.isEncryptedEnvelope` and hardened `SyncRepository` detail getters, savers, and reconciliation passes.
2. **2026-10-03**: Expanded test suite to 83 tests passing 100% green; built and installed `app-debug.apk` onto connected Google Pixel over ADB.
3. **2026-10-03**: Aligned `VaultItemDomain.NOTE` in `ItemDetailScreen.kt` with Web Client: masked default display, Eye-beside-Copy cluster, and biometric re-prompt gating verified on Pixel.
4. **2026-10-03**: Created branch `fix/web-interop-and-note-masking`, committed fixes (`19fb25b`), and bumped `versionCode = 8` / `versionName = "0.0.0.8"` (`877a983`).
5. **2026-10-03**: Fast-forward merged `fix/web-interop-and-note-masking` into `main` and tagged `v0.0.0.8`.
6. **2026-10-03**: Executed full `docs-hygiene.md` 10-point checklist: aligned `ROADMAP.md`, `README.md`, `SECURITY.md`, `CHANGELOG.md` (Keep a Changelog 1.1.0 with comparison links), `crypto-and-keystore.md`, and brain files.
7. **2026-10-03**: Published Release `v0.0.0.8 (Build 8)` via GitHub Actions cloud pipeline (Run ID `37173690713`), verified `.aab` and `.apk` assets.
8. **2026-10-03**: Diagnosed cold-start lock bypass; incorporated Vault Unlock Methods (Biometrics & PIN) and Cold-Start Lock Persistence into `meta-prompt-ai-studio.md` Stage 7 horizon and `ROADMAP.md` Task 11 (committed `1cb0351`, pushed to `origin/main`).
9. **2026-10-03**: Investigated missing IME inline autofill chips; identified empty raw Slice defect and missing `InlineSuggestionUi` in `AutofillInlineHelper`; created branch `research/autofill-and-credential-provider`, authored `credential-provider-spec.md`, overhauled `autofill-service-spec.md`, and bolstered Stage 6 references in `meta-prompt-ai-studio.md` and `ROADMAP.md`.
10. **2026-10-03**: Completed comprehensive educational research into 2026 Jetpack Compose Autofill patterns. Clarified the architectural split between `Modifier.semantics` and `androidx.credentials`. Authored checklist artifact and synchronized brain memory.

## Next Steps
- Review research findings with user and confirm commit under two-layer attribution on `research/autofill-and-credential-provider`.
- Merge research branch into main.
- Move into execution for Phase 5 (Autofill integration) or Phase 7 (Cold-Start lock).
