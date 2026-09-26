---
description: A workflow for updating ShellGuard application and documentation versioning with semantic versioning hygiene.
---

# 🏷️ Version Update Protocol Workflow

> **Use When:** Updating project versioning, preparing a new release candidate, or executing deployment hotfix increments.
> **Pair With:** `.agents/rules/semantic-versioning.md` and `.agents/rules/git-hygiene.md`.

---

## 🎯 Step 1: Determine the Target Version

Follow work-driven 4-digit Semantic Versioning (`MAJOR.MINOR.PATCH.REVISION` / `X.Y.Z.N`):
1. **User Explicit Input:** Highest priority (e.g. user specifies `v0.0.0.4`, `v0.0.1.0`, `v0.1.0.0`).
2. **Inference from Implemented Work & `CHANGELOG.md`:**
   - **Breaking Change / Overhaul:** `MAJOR` increment (`X+1.0.0.0`)
   - **Substantial New Subsystem (`### Added`):** `MINOR` increment (`X.Y+1.0.0`)
   - **Phase Deliverable / Discrete Feature (`### Fixed` / `### Added`):** `PATCH` increment (`X.Y.Z+1.0`, e.g. `0.0.0.1` ➔ `0.0.1.0`)
   - **Iterative Tweak / Hotfix / Task Increment:** `REVISION` increment (`X.Y.Z.N+1`, e.g. `0.0.0.1` ➔ `0.0.0.2`)
   - **Google Play Invariant:** Every release bundle upload MUST increment `versionCode` (`N + 1` monotonic integer).

---

## 🛡️ Step 2: Pre-Update Verification Gate

Before modifying version files, verify system integrity:

```bash
# 1. Test Oracle Verification (100% passing required)
./gradlew testDebugUnitTest

# 2. Production Build Validation
./gradlew assembleDebug
```

---

## 📝 Step 3: Synchronize Central Version Files

Update all version anchors across the codebase:

1. **`app/build.gradle.kts`**: Update `versionCode = N` (+1 monotonic) and `versionName = "X.Y.Z.N"`.
2. **`README.md`**: Update version badge linking to `CHANGELOG.md`:
   ```markdown
   [![Version](https://img.shields.io/badge/Version-vX.Y.Z.N-blue?style=for-the-badge)](CHANGELOG.md)
   ```
3. **`CHANGELOG.md`**: Create `## [X.Y.Z.N] - YYYY-MM-DD` section and move relevant items from `[Unreleased]`.
4. **`RELEASE-vX.Y.Z.N.md`** *(if cutting a release)*: Generate release document adhering to `.agents/templates/release-template.md`.
5. **`RELEASE-PLAY.md`** *(if cutting a release)*: Prepend punchy `<en-US>` release notes (<500 characters) for Google Play Console testers.

---

## 🧠 Step 4: Memory Bank & Active Context Sync

1. Update `.agents/memory-bank/activeContext.md`:
   - Slide recent changes window (maintain top 10).
2. Update `.agents/memory-bank/raw_reflection_log.md`:
   - Record version update details and verification results per continuous improvement protocol.

---

## 🚀 Step 5: Git Hygiene & Tagging

1. **Stage Modified Files:**
   ```bash
   git add app/build.gradle.kts README.md .agents/memory-bank/changelog.md RELEASE-PLAY.md [RELEASE-vX.Y.Z.N.md]
   ```

2. **Commit using Two-Layer Attribution:**
   ```bash
   git commit -m "chore: bump version to X.Y.Z for [reason]

   User: <intent/direction provided by human>
   AI: <files updated, tests verified, and documentation synchronized>"
   ```

3. **Create Annotated Git Tag:**
   ```bash
   git tag -a vX.Y.Z -m "Release vX.Y.Z: [Short Descriptive Title]"
   ```

4. **Push Instructions for User:**
   ```bash
   git push origin <current-branch>
   git push origin vX.Y.Z
   ```
