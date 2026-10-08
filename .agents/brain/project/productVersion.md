# Product Version & Release State

## Active Version State
- **Current Version**: `0.0.0.10`
- **Monotonic Build Code**: `10` (`versionCode = 10` in `app/build.gradle.kts`)
- **Active Release Channel**: Production / GitHub Release & Google Play Track
- **Latest Git Tag**: `v0.0.0.10`
- **Release Status**: Published
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
| `0.0.0.7` | `7` | `v0.0.0.7` | 2026-10-03 | Hotfix 5.3: Bidirectional Sync Reconciliation & Dual Adversarial Hardening | Published |
| `0.0.0.8` | `8` | `v0.0.0.8` | 2026-10-03 | Hotfix 5.4: Web Interoperability & Secure Note Parity | Published |
| `0.0.0.9` | `9` | `v0.0.0.9` | 2026-10-04 | Phase 5: Context-Aware Autofill, Inline Chips & Add-Item Deep Linking | Published |
| `0.0.0.10` | `10` | `v0.0.0.10` | 2026-10-04 | Phase 6: Settings Hub, Security Controls & Web-Parity Backup Engine | Published |

## Pending Delta (Next Release Target)
- **Immediate Target**: Phase 7: Context-Aware Autofill Expansion & Heuristics (Tasks 13.1–13.3)
- **Target Version**: `0.0.0.11` (Build 11)
- **Anticipated Tier**: PATCH / MILESTONE (`X.Y.Z+1.0`)
- **Blast Radius Protection**: Do NOT update version numbers in `app/build.gradle.kts`, `README.md`, or `CHANGELOG.md` until implementation and verification pass 100% green.
