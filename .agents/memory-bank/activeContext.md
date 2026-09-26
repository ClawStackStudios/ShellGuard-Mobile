# Active Context: ShellGuard Mobile

## Current Focus
Comprehensive audit and reorientation of all `.agents/` rules, workflows, and skills away from the standalone TOTP companion to the full **ShellGuard Mobile** secrets vault client (`com.clawstack.shellguard`), encoding multi-domain migration (Pearls, Notes, SSH, TOTP), Play Console deployment, and release lifecycle workflows.

## Recent Events (Sliding Window of 10)
1. **2026-09-24**: Hardened `crypto-and-keystore.md` with Biometric Recovery State Machine and CWE-359 sensitive clipboard masking.
2. **2026-09-24**: Authored `widgets-and-quick-tiles-spec.md` covering Quick Settings TileService and interactive Glance AppWidgets.
3. **2026-09-24**: Adopted Bitwarden-model Read-Only Offline Vault Caching with `ConnectivityMonitor` and mutation guards.
4. **2026-09-24**: Integrated `UriMatchMode` (5 algorithms), Claw Re-Prompt guardrail, and Android 11+ keyboard inline autofill.
5. **2026-09-25**: Hardened LAN & Tailscale transport policies (`base-config cleartextTrafficPermitted` and OkHttp `ConnectionSpec.CLEARTEXT`).
6. **2026-09-25**: Created root `DESIGN.md` establishing Reef Modernist Mobile design tokens, adaptive Master-Detail layout, and visual continuity across ShellGuard ecosystem.
7. **2026-09-25**: Reoriented `.agents/rules/android-development.md` and `AGENTS.md` to full mobile client architecture (bidirectional sync, Hybrid File-System attachments, R8 preservation).
8. **2026-09-25**: Reoriented `.agents/rules/zero-knowledge-migration.md`, `migration-and-ingest.md`, `play-console-release-workflow.md`, and CI signing skills to full multi-domain vault client.
9. **2026-09-25**: Executed forensic Bitwarden Android parity comparison; expanded `ui-ux-design-system.md` §10 with 6 settings sub-screens (Auto-Copy TOTP, Vault Timeout, Screen Capture toggle), confirming 98%+ MVP coverage.
10. **2026-09-25**: Scaffolded Stage 0 Android baseline (Gradle 9.3.1, SQLCipher 4.6.1+, AppContainer lazy DI, FLAG_SECURE window shielding, cleartext LAN config); verified testDebugUnitTest and assembleDebug 100% green.

## Next Steps
- Stage 0 complete and verified. Prompt user for Task Completion Gate (Commit check).
- Proceed to Phase 1: Cryptographic Engine & Room Database (Task 01 & Task 02).

