# Product Version & Release State

## Active Version State
- **Current Version**: `0.0.0.6`
- **Monotonic Build Code**: `6` (`versionCode = 6` in `app/build.gradle.kts`)
- **Active Release Channel**: Production / GitHub Release & Google Play Track
- **Latest Git Tag**: `v0.0.0.6`
- **Release Status**: Published & Cloud Verified (Run #36361045902)
- **Central Version Source**: `app/build.gradle.kts` (`versionCode`, `versionName`)

## SemVer Calculus & Increment Rules (4-Digit Convention: `MAJOR.MINOR.PATCH.REVISION`)
- **MAJOR (`X+1.0.0.0`)**: Fundamental structural overhauls, database breaking migrations, or full public production milestones.
- **MINOR (`X.Y+1.0.0`)**: Significant new subsystems or large cohesive multi-feature deliverables (e.g., entire Glance widget system).
- **PATCH (`X.Y.Z+1.0`)**: Discrete feature additions, completed 2-task phase deliverables, or targeted bug fixes (e.g. `0.0.0.5` ➔ `0.0.0.6`).
- **REVISION (`X.Y.Z.N+1`)**: Small patches, hotfixes, CI pipeline tweaks, minor UI polish, or iterative hardening passes.
- **Monotonic Build Invariant**: Every release bundle MUST strictly increment `versionCode` (`N + 1` integer) in `app/build.gradle.kts`, even when maintaining `versionName` display parity.

## Release History Chronicle
| Version | Build Code | Git Tag | Release Date | Scope & Key Milestones | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `0.0.0.1` | `1` | `v0.0.0.1` | 2026-09-25 | Stage 0 Android Scaffold & Foundation | Published |
| `0.0.0.2` | `2` | `v0.0.0.2` | 2026-09-26 | Phase 1: Cryptographic Engine & Local Cache | Published |
| `0.0.0.3` | `3` | `v0.0.0.3` | 2026-09-26 | Phase 2: Ktor API Client & Bidirectional Sync | Published |
| `0.0.0.4` | `4` | `v0.0.0.4` | 2026-09-26 | Phase 3: Vault Domains & Universal Editor | Published |
| `0.0.0.5` | `5` | `v0.0.0.5` | 2026-09-27 | Phase 4: TOTP Engine & Biometric Security Lifecycle | Published |
| `0.0.0.6` | `6` | `v0.0.0.6` | 2026-09-27 | Phase 5: Autofill Framework, Service Smoothing & AutoSpill Hardening | Published |

## Pending Delta (Next Release Target)
- **Immediate Target**: Hotfix 5.3: Bidirectional Sync Reconciliation & Room Pruning Fix
- **Target Version**: `0.0.0.7` (Build 7)
- **Anticipated Tier**: REVISION / HOTFIX (`X.Y.Z.N+1`)
- **Shifted Target**: Phase 6: Settings, Backup Bridge, Theme Customization & Production Polish (Tasks 11 & 12) ➔ `0.0.0.8` (Build 8)
- **Blast Radius Protection**: Do NOT update version numbers in `app/build.gradle.kts`, `README.md`, or `CHANGELOG.md` until implementation and verification pass 100% green.
