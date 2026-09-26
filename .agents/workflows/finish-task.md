---
description: A workflow defining the strict closing protocol for verifying, documenting, and merging a completed task.
---

# 🏁 Task Finalization Workflow

> **Use When:** You have finished coding a feature, bugfix, or refactor and are ready to merge.
> **Pair With:** `.agents/rules/docs-hygiene.md`, `.agents/rules/continuous-improvement.md`, and `.agents/rules/git-hygiene.md`.

---

## 🔒 Step 1: The Verification Gate
You must absolutely verify the integrity of the codebase before declaring a task complete.
Run the following in the terminal:
```bash
# 1. Oracle Verification (All unit & UI test suites must pass)
./gradlew testDebugUnitTest

# 2. Artifact & Build Compilation Integrity
./gradlew assembleDebug
```
*If any of these fail, you MUST loop back to execution to resolve the errors before proceeding.*

## 📚 Step 2: Documentation & Anti-Rot Sync
Adhere to the `docs-hygiene.md` rule.
1. Update `attractorBeacon.md`, `systemPatterns.md`, or component READMEs if the architecture or state model changed.
2. Update `CHANGELOG.md` under the `## [Unreleased]` header with a descriptive summary of your changes.

## 🧠 Step 3: Memory Bank Consolidation
Adhere to the `continuous-improvement.md` rule.
1. Open `.agents/memory-bank/activeContext.md` and slide the "Recent Changes" window to include a summary of this task, keeping only the 10 most recent entries.
2. Log any significant new learnings, patterns, or resolved roadblocks into `.agents/memory-bank/raw_reflection_log.md`.

## 📦 Step 4: Git Hygiene & Merge
Adhere to the `git-hygiene.md` rule.
1. Stage your changes: `git add .`
2. Commit your work using the strict two-layer attribution format:
   ```bash
   git commit -m "<type>: <short summary>

   User: <the intent, spec, or structural decision provided>
   AI: <the concrete implementation, files modified, or tests added>"
   ```
3. Checkout `main` and merge your branch using `--no-ff` (no fast-forward) to preserve the feature branch history:
   ```bash
   git checkout main
   git merge <your-branch-name> --no-ff -m "merge: <short summary>"
   ```
4. Delete the local feature branch:
   ```bash
   git branch -d <your-branch-name>
   ```

## 🚀 Step 5: Handoff
Notify the user that the task is fully complete, verified, documented, and merged. Provide them with instructions to push to remote (e.g. `git push origin main`).
