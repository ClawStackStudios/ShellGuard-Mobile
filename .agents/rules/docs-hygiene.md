# Documentation Hygiene & Anti-Rot Rule

**Objective:** Prevent documentation rot by ensuring that architectural, state, and API changes are fundamentally tied to their documentation updates within the *same* branch and commit.

## 1. Zero-Deferred Documentation
- **Never defer documentation updates.** If you change the behavior of a module, component, or state model, the corresponding documentation MUST be updated in the same branch before merging to `main`.
- "I will update the docs later" is treated as an incomplete task.

## 2. Trigger Conditions
You MUST proactively update the corresponding `.agents/memory-bank/` files or `docs/` files when:
- **State Model Changes:** If you alter how data flows, where it is stored, or how contexts (like React Context or Zustand) are structured, you must update `attractorBeacon.md` and/or `systemPatterns.md`.
- **API/Endpoint Changes:** If a server route's payload or response shape changes, update the API documentation or relevant README.
- **Component Refactors:** If a large component is split or renamed, update the overarching UI documentation and `activeContext.md`.
- **Dependency Changes:** If a new core dependency is added (e.g., swapping a crypto library), update `techContext.md`.

## 3. Inline Documentation
- Maintain JSDoc/TSDoc integrity. If you change a function signature, you must update its `@param` and `@returns` docstrings immediately.
- Preserve existing comments that explain *why* code exists, unless the *why* has fundamentally changed.

## 4. The "Same Commit" Mandate
Documentation updates should not be isolated to a separate "chore: update docs" commit if they belong to a feature. They should be bundled into the specific `AI:` layer of the commit that introduced the feature/fix, proving that the code and its explanation evolved together.

## 5. Release Documentation & Template Protocol
When preparing a version increment, phase completion, or release, you MUST execute the `.agents/workflows/walk-the-docs.md` workflow to systematically verify that the documentation bows to the code with structural precision.

- **Use Official Release Template:** Draft `RELEASE-vX.Y.Z.N.md` in the repository root adhering strictly to `.agents/doc-templates/release-template.md`.
- **Google Play Store Notes:** Prepend concise (<500-char) `<en-US>` release notes to `RELEASE-PLAY.md` for direct mobile store deployment.
- **Synchronize Central Anchors (8-Point Checklist):** In the same release preparation commit, synchronize all of the following:
  1. `app/build.gradle.kts`: Increment monotonic `versionCode` and update `versionName`.
  2. `ROADMAP.md`: Check off completed tasks (`[x]`), update `current_position`, recalculate `features_completed: XX%`, and **re-align downstream monotonic build projections** (`Phase X ➔ Build Y`).
  3. `project/meta-prompt-ai-studio.md`: Re-index build codes and stage headers for upcoming phases.
  4. `CHANGELOG.md` (Repository Root): Add `## [X.Y.Z.N] - YYYY-MM-DD (Build N)` entry following Keep a Changelog format.
  5. `RELEASE-vX.Y.Z.N.md`: Ensure root release note exists for `.github/workflows/release.yml`.
  6. `RELEASE-PLAY.md`: Ensure `<en-US>` block is strictly under 500 characters.
  7. `README.md`: Update version badge and verified test suite count.
  8. Domain & Storage Specs: Update `project/room-storage-schema.md`, `project/crypto-and-keystore.md`, and `ARCHITECTURE.md` if schema, crypto, or security flags were introduced.
  9. Cross-Repository Compatibility: Synchronize partner repository `compatibility_layer.md` if payload schemas or sync endpoints were modified.
- **Automated Publication Trigger:** Pushing with `--release vX.Y.Z.N` in the commit message or pushing tag `vX.Y.Z.N` automatically executes `.github/workflows/release.yml` to build, sign, and publish the release with the markdown notes and branded binaries.


