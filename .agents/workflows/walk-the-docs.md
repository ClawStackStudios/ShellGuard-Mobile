---
description: Deterministic workflow to walk the documentation, verify all claims against the physical codebase, and ensure docs bow to code with systemic accuracy.
---

# 🚶‍♂️ Walk the Docs Workflow

> **Philosophy:** The codebase is the physical reality and sole source of structural truth. The documentation must bow to the code with systemic precision.
> **Pair With:** `.agents/rules/docs-hygiene.md`, `.agents/rules/git-hygiene.md`, and `.agents/rules/cadence-and-lifecycle-prompts.md`.

---

## 🧭 Overview & Execution Trigger
Execute this workflow:
1. Immediately upon completing all implementation and passing unit/Robolectric tests for a phase or task.
2. Whenever asked: *"have we ensured any docs that might need updating from touching this phase got updated with the phase?"* or when auditing repo state.
3. Before triggering a release tag or commit.

---

## 🔍 Phase 1: Physical Codebase Inventory
Inspect the physical tree diff to establish what actually changed before reading or modifying documentation:

```bash
git status
git diff --stat
```

Identify the exact domains touched:
- **Core Entities & DB**: Did entities, DAOs, or migrations change?
- **Cryptography / Keystore / Security**: Were keys, ciphers, receivers, or preferences altered?
- **UI & Navigation**: Were routes, screens, viewmodels, or themes added/modified?
- **Sync & Networking**: Were payloads, endpoints, or serialization models modified?
- **Versioning**: Were `versionCode` or `versionName` in `app/build.gradle.kts` touched?

---

## 📋 Phase 2: The 8-Point Documentation Walk

Walk each anchor document sequentially and enforce bidirectional consistency with the code:

### 1. Versioning & Build Manifest (`app/build.gradle.kts`)
- [ ] Confirm monotonic `versionCode` (+1 for release builds).
- [ ] Confirm semantic `versionName` matches the target milestone.

### 2. Strategic Roadmap (`ROADMAP.md`)
- [ ] Mark completed tasks with `[x]`.
- [ ] Update `current_position` to reflect the completed phase and name the next phase.
- [ ] Recalculate `features_completed: XX%`.
- [ ] **Monotonic Downstream Build Alignment**: Trace all future phases and verify that build projections (`Phase X ➔ Build Y`) account for any hotfixes or inserted phases, preserving monotonic build code ordering throughout the document.

### 3. Stage & Prompt Alignment (`project/meta-prompt-ai-studio.md`)
- [ ] Re-index upcoming stage headers (e.g. `### Stage 14 (Phase 13): ... (Build 17)`).
- [ ] Verify prompts, version tags, and task descriptions align with the updated roadmap.

### 4. Public Changelog (`CHANGELOG.md` at Repository Root)
- [ ] Add `## [X.Y.Z.N] - YYYY-MM-DD (Build N)` header.
- [ ] Itemize Added / Changed / Fixed / Verified entries under Keep a Changelog conventions.
- [ ] Check against actual git diff to ensure every significant feature or fix is represented.

### 5. Root Release Manifest (`RELEASE-vX.Y.Z.N.md`)
- [ ] Ensure a dedicated release note file exists at root (`RELEASE-v<versionName>.md`).
- [ ] Verify that `.github/workflows/release.yml` will find and parse this file upon tag push.

### 6. Mobile Store Distribution Notes (`RELEASE-PLAY.md`)
- [ ] Prepend `<en-US>` release notes block for the build.
- [ ] **Strict Constraint**: Ensure the block length is **strictly under 500 characters** to satisfy Google Play Console limits.

### 7. Repository Gateway (`README.md`)
- [ ] Bump release version badge (e.g., `vX.Y.Z.N (Build N)`).
- [ ] Update passing test suite count badge/prose to match current test oracle total.
- [ ] Highlight new user-facing features or security suites in the feature list.

### 8. Architectural & Storage Specs (`project/*.md`, `ARCHITECTURE.md`)
- [ ] **Room DB Schema** (`project/room-storage-schema.md`): If database entities changed, update schema version, entity listings, and DAO signatures.
- [ ] **Cryptography & Keystore** (`project/crypto-and-keystore.md`): If key management, broadcast receivers, or security controls changed, document methods and security guarantees.
- [ ] **Architecture Blueprint** (`ARCHITECTURE.md`): Update component interaction diagrams, threat models, and operational modes.
- [ ] **UI/UX Design System** (`project/ui-ux-design-system.md`): If theme tokens, typography, or component patterns changed, update design specs.

### 9. Cross-Repository Compatibility (`compatibility_layer.md`)
- [ ] If network sync payloads, item formats, or REST endpoints were updated, open and synchronize `compatibility_layer.md` in the partner repository (e.g. ShellGuard Web Server) before closing the task.

---

## 🧠 Phase 3: Memory Bank & Companion Log Attestation

Walk and synchronize the agent semantic and episodic memory:
- [ ] `.agents/memory-bank/activeContext.md`: Slide 10-event window, record current phase status.
- [ ] `.agents/memory-bank/progress.md`: Mark phase done, update test metrics, update what's next.
- [ ] `.agents/memory-bank/changelog.md`: Append version entry.
- [ ] `.agents/memory-bank/raw_reflection_log.md`: Record learnings and difficulties.
- [ ] `.agents/memory-bank/decision-log.md`: Record felt friction and architectural calibrations.

---

## ✅ Phase 4: Verification & Pre-Commit Oracle Gate

Never attest to documentation accuracy without running the test suite:

```bash
export JAVA_HOME="/config/Applications/android-studio/jbr"
export PATH="$JAVA_HOME/bin:/config/Android/Sdk/platform-tools:$PATH"
export GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"

# 1. Oracle Verification
./gradlew testDebugUnitTest --no-configuration-cache --no-daemon

# 2. Build Verification
./gradlew assembleDebug --no-daemon
```

Confirm that test counts in `README.md`, `CHANGELOG.md`, and memory bank match the actual test run output (`Total: N, Failures: 0, Errors: 0`).
