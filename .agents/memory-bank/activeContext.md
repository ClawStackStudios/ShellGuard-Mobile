# Active Context: ShellGuard Mobile

## Current Focus
Comprehensive audit and reorientation of all `.agents/` rules, workflows, and skills away from the standalone TOTP companion to the full **ShellGuard Mobile** secrets vault client (`com.clawstack.shellguard`), encoding multi-domain migration (Pearls, Notes, SSH, TOTP), Play Console deployment, and release lifecycle workflows.

## Recent Events (Sliding Window of 10)
1. **2026-09-24**: Adopted Bitwarden-model Read-Only Offline Vault Caching with `ConnectivityMonitor` and mutation guards.
2. **2026-09-24**: Integrated `UriMatchMode` (5 algorithms), Claw Re-Prompt guardrail, and Android 11+ keyboard inline autofill.
3. **2026-09-25**: Hardened LAN & Tailscale transport policies (`base-config cleartextTrafficPermitted` and OkHttp `ConnectionSpec.CLEARTEXT`).
4. **2026-09-25**: Created root `DESIGN.md` establishing Reef Modernist Mobile design tokens, adaptive Master-Detail layout, and visual continuity across ShellGuard ecosystem.
5. **2026-09-25**: Reoriented `.agents/rules/android-development.md` and `AGENTS.md` to full mobile client architecture (bidirectional sync, Hybrid File-System attachments, R8 preservation).
6. **2026-09-25**: Reoriented `.agents/rules/zero-knowledge-migration.md`, `migration-and-ingest.md`, `play-console-release-workflow.md`, and CI signing skills to full multi-domain vault client.
7. **2026-09-25**: Executed forensic Bitwarden Android parity comparison; expanded `ui-ux-design-system.md` §10 with 6 settings sub-screens, confirming 98%+ MVP coverage.
8. **2026-09-25**: Scaffolded Stage 0 Android baseline (Gradle 9.3.1, SQLCipher 4.6.1+, AppContainer lazy DI, FLAG_SECURE, cleartext LAN config); verified green.
9. **2026-09-26**: Executed Phase 1 (Task 01 ShellCryption HKDF + Room SQLCipher, Task 02 Gateway UI & Theme Engine); verified 100% green (16/16 tests pass, `app-debug.apk` built).
10. **2026-09-26**: Forensically aligned Remote Login form with ShellGuard web/TOTP parity (segmented URL bar, upload dropzone, warning box); verified on physical Google Pixel (16/16 tests, APK installed, UI hierarchy verified).

## Next Steps
- Phase 1 complete and verified green across test suite, build, and hardware deployment. Trigger Task Completion Gate (Commit check).
- Upon commit, evaluate version bump (`v0.0.0.3 (Build 3)` per `ROADMAP.md`).
- Transition into Phase 2: Ktor API Client & Bidirectional Sync (Task 03 & Task 04) on `feat/phase-2-ktor-sync-and-dashboard`.

