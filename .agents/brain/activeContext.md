# Active Context: ShellGuard Mobile

## Current Focus
Planning Horizon & Phase 6 Specification Refinement: Incorporated Vault Unlock Methods (Biometrics & PIN code alternatives to ClawKey login), Cold-Start / Process Kill Lock Persistence, and Settings Security Hub into `project/meta-prompt-ai-studio.md`, `ROADMAP.md`, and project specs.

## Recent Events (Sliding Window of 10)
1. **2026-10-03**: Published release `v0.0.0.7 (Build 7)` via GitHub Actions cloud pipeline (Run ID `37153067907`).
2. **2026-10-03**: Diagnosed Web UI item crash caused by unencrypted JSON arrays (`"[]"`) in `password_history` and `custom_fields`.
3. **2026-10-03**: Added `ShellCryptionEngine.isEncryptedEnvelope` and hardened `SyncRepository` detail getters, savers, and reconciliation passes.
4. **2026-10-03**: Expanded test suite to 83 tests passing 100% green; built and installed `app-debug.apk` onto connected Google Pixel over ADB.
5. **2026-10-03**: Aligned `VaultItemDomain.NOTE` in `ItemDetailScreen.kt` with Web Client: masked default display, Eye-beside-Copy cluster, and biometric re-prompt gating verified on Pixel.
6. **2026-10-03**: Created branch `fix/web-interop-and-note-masking`, committed fixes (`19fb25b`), and bumped `versionCode = 8` / `versionName = "0.0.0.8"` (`877a983`).
7. **2026-10-03**: Fast-forward merged `fix/web-interop-and-note-masking` into `main` and tagged `v0.0.0.8`.
8. **2026-10-03**: Executed full `docs-hygiene.md` 10-point checklist: aligned `ROADMAP.md`, `README.md`, `SECURITY.md`, `CHANGELOG.md` (Keep a Changelog 1.1.0 with comparison links), `crypto-and-keystore.md`, and brain files.
9. **2026-10-03**: Published Release `v0.0.0.8 (Build 8)` via GitHub Actions cloud pipeline (Run ID `37173690713`), verified `.aab` and `.apk` assets.
10. **2026-10-03**: Diagnosed cold-start lock bypass; incorporated Vault Unlock Methods (Biometrics & PIN) and Cold-Start Lock Persistence into `meta-prompt-ai-studio.md` Stage 7 horizon and `ROADMAP.md` Task 11.

## Next Steps
- Commit and push planning horizon updates to `main`.
- Initiate Phase 6 implementation (Stage 7) targeting `v0.0.0.9 (Build 9)`: Settings Hub, Biometric & PIN Unlock, Cold-Start Lock, and Backup Engine.





