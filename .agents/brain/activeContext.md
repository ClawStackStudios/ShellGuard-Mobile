# Active Context: ShellGuard Mobile

## Current Focus
Stage 2 (`v0.0.0.20` Build 20) on branch `feat/stage-2-v0.0.0.20` is CI-green (136/136 tests passing, Cloud CI Run `43771352373`). Issues `#13`, `#22`, `#23`, and `#24` are closed; `/walk-the-docs` documentation alignment is complete with `RELEASE-v0.0.0.14.md` pruned and `RELEASE-v0.0.0.20.md` active.

## Recent Events (Sliding Window of 10)
10. **2026-10-17**: Seeded 11 domain-scoped proactive `// TODO:` anchors across the codebase and pushed to `main` (`89d9928`).
11. **2026-10-18**: Paired with Jules on `KtorClientProvider.kt` security fixes (CWE-532 log redaction & CWE-295/297 self-signed TLS consent).
12. **2026-10-19**: Integrated PR #4 & PR #5 on `feat/security-remediations-integration`, fixed `GatewayScreen.kt` Box overlap, and verified on physical Google Pixel over TLS ADB.
13. **2026-10-19**: Realigned Master Roadmap to Option 1 Decade Spacing (`v0.0.0.14` ➔ `v0.0.0.20` ➔ `v0.0.0.30` ➔ `v0.0.0.40` ➔ `v0.0.1.0`) and released `v0.0.0.14` (Build 14).
14. **2026-10-21**: Executed `/walk-the-docs` and authored 6 `/mobile/` VitePress chapters (`b925728`).
15. **2026-10-24**: Reviewed Jules PR #6 (`EncryptedDeviceVault` fail-closed fix) and dispatched Approach B (`interface DeviceVault`) guidance.
16. **2026-10-27**: Audited ShellGuard Server ↔ Mobile sync seams and formulated Task 04.A (`owner_uuid` default, `password_history` parity, `parent_id`/`parent_type` attachment linkage).
17. **2026-11-07**: Provisioned GitHub native control plane (5 Milestones, 10 labels, Issues #9–#21, Project #1).
18. **2026-11-15**: Executed Milestone 2 on `feat/stage-2-v0.0.0.20`: closed `#13`, `#22` (sync bridge & mutation hardening + CWE-532 error banner), and `#23` (SAF zero-clipboard vault import); bumped `versionCode = 20`, `versionName = "0.0.0.20"`, and passed Cloud CI (`43759259562`).
19. **2026-11-15**: Closed `#24` (removed attachment `View`/`ACTION_VIEW`/`cacheDir` plaintext write in favor of SAF `CreateDocument` Download-only flow + `ItemDetailViewModelTest.kt`, Cloud CI `43771352373`), closed `#13`, `#22`, `#23`, and ran `/walk-the-docs` (pruned `RELEASE-v0.0.0.14.md`, updated `room-storage-schema.md` to `v2`, `CHANGELOG.md`, `README.md`).

## Next Steps
- Complete remaining checks/polish in Issue `#14` (soft keyboard auto-scroll in Universal Item Form & Gateway, Secure Notes markdown rendering, PIN/password-gen haptics).
- Open PR from `feat/stage-2-v0.0.0.20` to `main`, close `#14`, and cut `v0.0.0.20` release tag.

