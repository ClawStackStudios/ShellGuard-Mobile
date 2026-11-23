# Active Context: ShellGuard Mobile

## Current Focus
Issue `#35` (First-Class Identity Vault Items, `CustomField` Wire Parity & Bitwarden/ShellGuard Backup Import Fixes) on branch `feat/issue-35-identity-contract-and-imports` (`58371fc`) is CI-green (Cloud CI Run `45097694988`, all unit/Robolectric tests passing and debug APK compiled).

## Recent Events (Sliding Window of 10)
13. **2026-10-19**: Realigned Master Roadmap to Option 1 Decade Spacing (`v0.0.0.14` ➔ `v0.0.0.20` ➔ `v0.0.0.30` ➔ `v0.0.0.40` ➔ `v0.0.1.0`) and released `v0.0.0.14` (Build 14).
14. **2026-10-21**: Executed `/walk-the-docs` and authored 6 `/mobile/` VitePress chapters (`b925728`).
15. **2026-10-24**: Reviewed Jules PR #6 (`EncryptedDeviceVault` fail-closed fix) and dispatched Approach B (`interface DeviceVault`) guidance.
16. **2026-10-27**: Audited ShellGuard Server ↔ Mobile sync seams and formulated Task 04.A (`owner_uuid` default, `password_history` parity, `parent_id`/`parent_type` attachment linkage).
17. **2026-11-07**: Provisioned GitHub native control plane (5 Milestones, 10 labels, Issues #9–#21, Project #1).
18. **2026-11-15**: Executed Milestone 2 on `feat/stage-2-v0.0.0.20`: closed `#13`, `#22`, `#23`, `#24`; bumped `v0.0.0.20` (Build 20), 136/136 tests passing.
19. **2026-11-16**: Shipped 2-tab Reef navigation, Pull-to-Refresh, Recents-swipe lock (`VaultTaskCleanupService`), attachment Base64 fix (`PR #27`), off-main PBKDF2 (`PR #30`), cold-start lock gate (`PR #31`), and client caching (`PR #32`, `PR #33`).
20. **2026-11-18**: Closed `#16` and `#15`; opened PR #34 and ran `/walk-the-docs` for `v0.0.0.30` (Build 30).
21. **2026-11-22**: Mapped cross-repo Identity & Backup/Bitwarden import seams across `ShellGuard` and `ShellGuard-Mobile`, opened Issue `#35` under `v0.0.0.40`, implemented `CustomFieldWireSerializer`, `IdentityDetail` + `IdentityFieldMapper`, `VaultBackupEngine` format sniffer & Bitwarden types `1..5`, and modular `IdentityFormSection` / `IdentityDetailSection` UI (`58371fc`, Cloud CI Run `45097694988` green).

## Next Steps
- Open PR for `feat/issue-35-identity-contract-and-imports` (Issue `#35`) and verify on physical Google Pixel (`sailfish`).
- Open companion interoperability issue on `ClawStackStudios/ShellGuard` (`v0.0.3.1`).
- Proceed with Stage 4 (`v0.0.0.40` Build 40): Issues `#17` (Browser/Native Autofill Gauntlet) and `#18` (16 KB Page Alignment & ProGuard R8 audit).
