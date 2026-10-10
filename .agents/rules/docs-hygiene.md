# Documentation Hygiene & Anti-Rot Rule

**Objective:** Prevent documentation rot by ensuring that architectural, state, and API changes are fundamentally tied to their documentation updates within the *same* branch and commit.

## 1. Zero-Deferred Documentation
- **Never defer documentation updates.** If you change the behavior of a module, component, or state model, the corresponding documentation MUST be updated in the same branch before merging to `main`.
- "I will update the docs later" is treated as an incomplete task.

## 2. Trigger Conditions
You MUST proactively update the corresponding `.agents/brain/` files or `project/` specs when:
- **State Model Changes:** If you alter how data flows, where it is stored, or how MVI state models (ViewModel `StateFlow`, Room DAOs, or KeyStore vaults) are structured, you must update `systemPatterns.md` and/or `project/room-storage-schema.md`.
- **API/Endpoint Changes:** If a server route's payload or response shape changes, update `routes-and-contracts.md` and `techContext.md`.
- **Component Refactors:** If a large component or screen is split or renamed, update the overarching UI documentation and `activeContext.md`.
- **Dependency Changes:** If a new core dependency is added (e.g., swapping a crypto or serialization library), update `techContext.md`.

## 3. Inline Documentation
- Maintain KotlinDoc (`/** ... */`) integrity. If you change a public method, ViewModel intent, or repository signature, update its `@param` and `@return` documentation.
- Preserve existing comments that explain *why* code exists, unless the *why* has fundamentally changed.

## 4. The "Same Commit" Mandate
- **Testing & Build Exemption**: Markdown doc files are excluded from requiring a build or test run (**NO TESTING** is required). Test **ONLY** when editing application files, or after stages/strokes of work.

## 5. Release Documentation & Template Protocol
When preparing a version increment, phase completion, or release, you MUST execute the `.agents/workflows/walk-the-docs.md` workflow to systematically verify that the documentation bows to the code with structural precision.

### Keep a Changelog 1.1.0 Strict Standards
Every version entry in `CHANGELOG.md` and `.agents/brain/project/changelog.md` MUST adhere strictly to [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/):
- **Allowed Subsections (Alphabetical & Exhaustive)**:
  - `### Added`: For new user-facing features, endpoints, or UI capabilities.
  - `### Changed`: For changes in existing functionality, performance improvements, or refactors.
  - `### Deprecated`: For soon-to-be removed features or obsolete APIs.
  - `### Removed`: For now removed features, eliminated routes, or reverted changes.
  - `### Fixed`: For any bug fixes, crash resolutions, or logic corrections.
  - `### Security`: In case of vulnerability patches, cryptographic mitigations, or AAD re-binding.
- **Prohibited Headers**: Never invent custom headings (e.g. `### Bug Fixes`, `### Features`, `### Improvements`, `### Tasks`). Use ONLY the 6 canonical categories above.
- **Comparison Footnote Links**: Every version entry MUST maintain comparison links at the end of the changelog:
  ```markdown
  [Unreleased]: https://github.com/ClawStackStudios/ShellGuard-Mobile/compare/vX.Y.Z.N...HEAD
  [X.Y.Z.N]: https://github.com/ClawStackStudios/ShellGuard-Mobile/compare/vPrevious...vX.Y.Z.N
  ```

### Dual-Tier Release Notes Standard
- **Tier 1: Public GitHub Release Notes (`RELEASE_NOTES.md` / GitHub Release)**:
  - Uses categorized icons for public legibility:
    - 🚀 **Features** (derived from `Added`)
    - 🐛 **Bug Fixes** (derived from `Fixed`)
    - 🛡️ **Security** (derived from `Security`)
    - ⚡ **Performance & Refactoring** (derived from `Changed`)
    - 📚 **Documentation & Maintenance** (derived from `docs` / `chore`)
  - Ends with `**Full Changelog**: https://github.com/ClawStackStudios/ShellGuard-Mobile/compare/vPrev...vCurr`.
- **Tier 2: Internal Engineering Release Manifest (`RELEASE-vX.Y.Z.N.md`)**:
  - Adheres strictly to `.agents/doc-templates/release-template.md`:
    - Executive Summary & Roadmap Milestone
    - Core Architectural Highlights
    - Breaking Changes & Migration Steps
    - Known Issues & Scheduled Remediations
    - Dependency Updates Table (`Package | From | To | Reason`)
    - Cryptographic & 16 KB Page-Alignment Verification Receipts

- **Use Official Release Template:** Draft `RELEASE-vX.Y.Z.N.md` in the repository root adhering strictly to `.agents/doc-templates/release-template.md`.
- **Google Play Store Notes:** Prepend concise (<500-char) `<en-US>` release notes to `RELEASE-PLAY.md` for direct mobile store deployment.
- **Synchronize Central Anchors (10-Point Checklist):** In the same release preparation commit, synchronize all of the following:
  1. `app/build.gradle.kts`: Increment monotonic `versionCode` and update `versionName`.
  2. `ROADMAP.md`: Check off completed tasks (`[x]`), update `current_position`, recalculate `features_completed: XX%`, and **re-align downstream monotonic build projections** (`Phase X ➔ Build Y`).
  3. `project/meta-prompt-ai-studio.md`: Re-index build codes and stage headers for upcoming phases.
  4. `README.md`: Update version badge and verified test suite count.
  5. `SECURITY.md`: Update supported versions table, cryptographic invariants, and disclosure channels.
  6. `CHANGELOG.md` (Repository Root): Add `## [X.Y.Z.N] - YYYY-MM-DD (Build N)` entry following Keep a Changelog format with comparison links.
  7. `RELEASE-vX.Y.Z.N.md`: Ensure root release note exists for `.github/workflows/release.yml`, and git rm any superseded prior `RELEASE-v*.md` files from the repository root so only the active release manifest and `RELEASE-PLAY.md` remain.
  8. `RELEASE-PLAY.md`: Ensure `<en-US>` block is strictly under 500 characters.
  9. Domain & Storage Specs: Update `project/room-storage-schema.md`, `project/crypto-and-keystore.md`, and `ARCHITECTURE.md` if schema, crypto, or security flags were introduced.
  10. Cross-Repository Compatibility: Synchronize partner repository `compatibility_layer.md` if payload schemas or sync endpoints were modified.
- **Automated Publication Trigger:** Pushing with `--release vX.Y.Z.N` in the commit message or pushing tag `vX.Y.Z.N` automatically executes `.github/workflows/release.yml` to build, sign, and publish the release with the markdown notes and branded binaries.


