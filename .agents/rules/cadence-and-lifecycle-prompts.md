---
trigger: always_on
---

# Epistemic Lifecycle & Conversational Cadence Guardrails

## Core Principle: Epistemic Boundary Awareness
The agent acts as an active cartographer of the project's development lifecycle. It does not perform blind execution in a vacuum; it holds the development state in memory and proactively surfaces natural pivot questions at phase boundaries.

---

## 🧭 The 4 Boundary Prompts

### 1. Task Completion Gate (Verified → Commit Check)
- **When to Trigger**: Immediately after a task's code changes are implemented and the pre-flight verification gate (`./gradlew testDebugUnitTest`) passes 100% green.
- **Agent Action**: Ask the user:
  > *"All tests are passing 100% green. Should I commit these changes now under our two-layer attribution format, or are you still reviewing / experimenting locally?"*

### 2. Commit Completion Gate (Committed → Version Bump Check)
- **When to Trigger**: Immediately after a commit is successfully created following `git-hygiene.md`.
- **Agent Action**: Ask the user:
  > *"Commit is recorded cleanly. Does this change warrant bumping our release version tag (`versionCode` + `versionName` in `app/build.gradle.kts`), or are we keeping the current version to bundle additional tasks?"*

### 3. Version Bump Gate (Versioned → Release Movement Check)
- **When to Trigger**: Immediately after `versionCode` / `versionName` are updated.
- **Agent Action**: Ask the user:
  > *"Version is bumped to `vX.Y.Z.W (Build N)`. Are we moving towards a release to Google Play (pushing the git tag to trigger the GitHub Actions cloud build), or continuing local development on more features first?"*

### 4. Non-Blocking Memory Persistence
- If the user indicates *"No, we're still adding / reviewing"*, the agent:
  1. Respects the user's focus and does not push or nag.
  2. Records the in-flight context in `.agents/memory-bank/activeContext.md`.
  3. Automatically re-evaluates the cadence at the next natural task completion point when a series of uncommitted changes accumulates.

---

## 🚫 Anti-Patterns to Avoid
- **Never Auto-Commit Unprompted**: Always ask before creating commits.
- **Never Auto-Bump Versions Unprompted**: Version increments must be an intentional decision by the developer.
- **Never Auto-Tag / Push Releases Unprompted**: Tag pushes trigger remote GitHub Actions and must be confirmed.
