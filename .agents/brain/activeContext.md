# Active Context: ShellGuard Mobile

## Current Focus
Stage 3 (`v0.0.0.30` Build 30) on branch `feat/issue-15-lock-persistence-and-offline` (PR #34) is CI-green (152/152 tests passing, Cloud CI Run `44461930488`) and verified on physical Google Pixel (`sailfish`). Issues `#15` and `#16` are closed; `/walk-the-docs` alignment is complete with `RELEASE-v0.0.0.20.md` pruned and `RELEASE-v0.0.0.30.md` active.

## Recent Events (Sliding Window of 10)
12. **2026-10-19**: Integrated PR #4 & PR #5 on `feat/security-remediations-integration`, fixed `GatewayScreen.kt` Box overlap, and verified on physical Google Pixel over TLS ADB.
13. **2026-10-19**: Realigned Master Roadmap to Option 1 Decade Spacing (`v0.0.0.14` ➔ `v0.0.0.20` ➔ `v0.0.0.30` ➔ `v0.0.0.40` ➔ `v0.0.1.0`) and released `v0.0.0.14` (Build 14).
14. **2026-10-21**: Executed `/walk-the-docs` and authored 6 `/mobile/` VitePress chapters (`b925728`).
15. **2026-10-24**: Reviewed Jules PR #6 (`EncryptedDeviceVault` fail-closed fix) and dispatched Approach B (`interface DeviceVault`) guidance.
16. **2026-10-27**: Audited ShellGuard Server ↔ Mobile sync seams and formulated Task 04.A (`owner_uuid` default, `password_history` parity, `parent_id`/`parent_type` attachment linkage).
17. **2026-11-07**: Provisioned GitHub native control plane (5 Milestones, 10 labels, Issues #9–#21, Project #1).
18. **2026-11-15**: Executed Milestone 2 on `feat/stage-2-v0.0.0.20`: closed `#13`, `#22`, `#23`, `#24`; bumped `v0.0.0.20` (Build 20), 136/136 tests passing.
19. **2026-11-16**: Shipped 2-tab Reef navigation, Pull-to-Refresh, Recents-swipe lock (`VaultTaskCleanupService`), attachment Base64 fix (`PR #27`), off-main PBKDF2 (`PR #30`), cold-start lock gate (`PR #31`), and client caching (`PR #32`, `PR #33`).
20. **2026-11-18**: Closed `#16` (PIN keypad spring press/dot animations, `CircularDialPicker` polar math + 12 o'clock guard + 5s/15s haptics + 48dp targets, Panic Purge DataStore `.onStart` race + `Dispatchers.IO` `.commit()` cascade) and `#15` (`VaultLockManager` clock-rollback guard, offline CRUD cold-restart flush test, 10-domain AAD swap test); opened PR #34 and ran `/walk-the-docs` for `v0.0.0.30` (Build 30).

## Next Steps
- Merge PR #34 into `main` and cut `v0.0.0.30` release tag.
- Begin Stage 4 (`v0.0.0.40` Build 40): Issues `#17` (Browser/Native Autofill Gauntlet) and `#18` (16 KB Page Alignment & ProGuard R8 audit).

