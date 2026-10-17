---
trigger: always_on
---

# Git & Android Workspace Hygiene

## Isolation & Branching
- Never work directly on the default branch (`main`/`master`). Start every task on a fresh branch or worktree: `git checkout -b <type>/<short-desc>`.
- One task = one branch. Don't mix unrelated architectural changes into the same branch or working tree.
- Before starting, snapshot state: `git status` and `git diff --stat`. If the tree is dirty with work you didn't author, stop and ask.
- **Automated Bot & Security PR Forensic Verification**: Never trust commit messages, PR descriptions, or bot summaries claiming vulnerabilities are fixed. Always inspect the physical tree diff (`git show --stat <commit>` or `git diff <base>...<head>`) to confirm actual code modifications before accepting or branching from automated security fixes.


## Commits & Conventions
- Keep changes small and self-contained; one logical change per commit. No mega-commits, no unrelated refactors bundled in.
- Write clear, conventional commit messages adhering to the Conventional Commits specification:
  `<type>[optional scope][optional !]: <description>`
- **Allowed Types & SemVer Mapping**:
  - `feat`: New user-facing feature or domain capability (maps to Changelog `Added`).
  - `fix`: Bug fix, error resolution, or regression patch (maps to Changelog `Fixed`).
  - `refactor`: Code restructure without behavioral or public API changes (maps to Changelog `Changed`).
  - `perf`: Performance optimization, memory reduction, or query acceleration (maps to Changelog `Changed`).
  - `security`: Cryptographic hardening, CVE defense, or security boundaries (maps to Changelog `Security`).
  - `revert`: Reverting a previous commit (maps to Changelog `Removed`).
  - `docs`: Documentation only changes.
  - `test`: Adding or correcting tests; no production code change.
  - `build`: Changes affecting build system, Gradle scripts, or external dependencies.
  - `ci`: Changes to CI/CD workflows and release automation.
  - `chore`: Maintenance tasks, brain synchronization, or housekeeping.
- **Scopes**: Use consistent, lowercase domain scopes where applicable: `(auth)`, `(sync)`, `(crypto)`, `(vault)`, `(totp)`, `(autofill)`, `(keystore)`, `(ui)`, `(release)`, `(brain)`.
- **Breaking Changes**: Any breaking architectural, database, or cryptographic change MUST be marked with an exclamation mark `!` before the colon (e.g. `feat(crypto)!: migrate HKDF salt derivation`) and include a `BREAKING CHANGE:` explanation in the footer.
- Prefer new commits over amending. Never amend or rebase a commit without explicit written approval in the task.
- Never skip hooks (`--no-verify`) or bypass commit signing unless explicitly asked.

## Android Secrets & Keystore Safety (CRITICAL)
- **NEVER Commit Keystores or Private Keys**:
  - `*.jks`, `*.keystore`, `*.p12`, `*.pem`, `my-upload-key.jks`, `debug.keystore`.
- **NEVER Commit Local Machine Configs & API Credentials**:
  - `local.properties` (contains local machine Android SDK paths).
  - `.env`, `.env.*`, `google-services.json`, `secrets/**`.
  - `.agents/TOOLS.md`, `.agents/*tools*`, `*.local.*` (contains machine filesystem paths, local IPs, and hardware serials).
- **NEVER Commit Physical Hardware Identifiers**:
  - Device serial numbers from `adb devices` (e.g. `FA6A40302394`), MAC addresses, or local network mesh IP mappings.
- **NEVER Commit Build Outputs & Binaries**:
  - `build/`, `**/build/`, `*.apk`, `*.aab`, `*.apks`, `*.obb`, `*.dex`, `*.class`.
- **NEVER Commit IDE Caches**:
  - `.idea/`, `.gradle/`, `.kotlin/`, `captures/`, `.externalNativeBuild/`, `.cxx/`.
- **NEVER Commit Internal Agent Scratchpads & Visual Design Assets**:
  - `.agents/internal/`, `.agents/scratch/`, `**/scratch/**`. Always ensure these directories are declared in `.gitignore` and excluded from git staging.

### Unpushed Rebase Scrub Protocol
If local machine configurations, hardware serial numbers, or environment secrets are accidentally committed on a local branch:
1. **Forensic Check**: Verify the commit has NEVER been pushed to remote (`git log origin/<branch>..HEAD`).
2. **No Lazy Fixes**: Never use a trailing `git rm --cached` in a subsequent commit, which permanently preserves the sensitive identifier in the git log history.
3. **Execute Rebase Scrub**:
   - Back up the local file to a safe scratch directory outside git.
   - Re-anchor the commit that introduced the leak, removing the file from git staging (`git rm -f <file>`) and staging its `.gitignore` rule.
   - Replay/cherry-pick downstream commits cleanly.
   - Restore the physical file so local tooling/device testing remains active without disruption.
   - Verify with `git log origin/<branch>..HEAD -- <file>` returning empty.


## Destructive Operations — NEVER Without Explicit Confirmation
- `git push --force` / `--force-with-lease`
- `git reset --hard`, `git checkout/restore` to an older commit
- Deleting branches, tags, or stashes
- `rm -rf` or any bulk file deletion
- If unsure whether a file belongs to another agent's in-flight work, stop and coordinate — don't delete to silence an error.

### Subagent Feature-Branch Push Pre-Clearance
- Non-destructive pushes to an assigned task/feature branch (`git push origin HEAD` or `git push origin <branch>`) are explicitly **pre-authorized and expected** once local tests and verification pass.
- Subagents (e.g. Jules) must never halt or block awaiting permission to push regular commits to their designated feature branch.
- The prohibition strictly applies to pushing to the default branch (`main`) and force-pushing (`--force`).

## Cloud CI Offload & Pre-Commit Verification Gate
- **Zero Local Strain Principle**: You do **NOT** need to trigger heavy `./gradlew testDebugUnitTest` or `./gradlew assembleDebug` builds locally on the development machine. Full build compilation, 121+ Robolectric/unit tests, and debug APK artifact packaging are offloaded to GitHub Actions Cloud CI (`.github/workflows/ci.yml`).
- **Fast Local Pre-Commit Gate**:
  - Run the lightweight documentation drift check: `python3 scripts/audit_docs_drift.py` (executes in <1 second with zero CPU strain).
  - **Documentation Exemption**: Markdown doc files are excluded from requiring local Gradle builds.
- **Push & Tail Cloud Build Protocol**:
  - Commit changes adhering to the two-layer attribution format.
  - Push branch to remote: `git push origin <branch>`.
  - Tail and monitor the cloud build via GitHub CLI:
    ```bash
    # View recent CI runs
    gh run list -L 3
    # Watch live progress and logs
    gh run watch
    # Or view failure logs if a run fails
    gh run view --log-failed
    ```
- **Local Gradle Exception**: Running local `./gradlew testDebugUnitTest` or `./gradlew assembleDebug` is strictly optional and reserved for offline development or localized single-test debugging (`--tests "com.clawstack.shellguard.*SpecificTest*"`).

## Attribution & Commit Message Format
- Commit under the human's configured identity (`git config user.name` / `user.email`). No separate agent identity, no AI co-author line.
- Every commit message uses this two-layer format:

  ```
  <type>[optional scope][optional !]: <short imperative summary>

  User: <the intention, system design, architecture decision, or glue that was provided>
  AI: <the concrete implementation, functions, refactors, or tests that were generated>

  [optional BREAKING CHANGE: <explanation and migration requirements>]
  [optional Closes #<issue> / Fixes #<issue>]
  ```

- The `User:` line is always the *why/what* — the intent, spec, or structural decision.
- The `AI:` line is always the *how* — the code, logic, or test coverage that fulfilled it.
- If the human did the implementation directly (rare), put it under `User:` and write `AI: (none)`.
- If the agent did purely exploratory work with no human direction in that commit, write `User: (autonomous)` — but this should be the exception, not the norm.
- No trailers, no co-author lines, no `AI-Model:` metadata. The two-layer message *is* the attribution.

## Rebase Hygiene
- When rebasing, avoid opening editors: set `GIT_EDITOR=:` and `GIT_SEQUENCE_EDITOR=:` (or pass `--no-edit`).

### 1. Pre-Rebase Safety Anchor Protocol
Before executing any interactive rebase, history surgery, or multi-commit squash on a feature branch:
1. **Create Safety Anchor**: Create a temporary backup branch pointing to current HEAD:
   ```bash
   git branch backup/$(git branch --show-current)-pre-rebase
   ```
2. **Perform Rebase**: Execute rebase cleanly.
3. **Verify Integrity**: Push to remote feature branch (`git push --force-with-lease`) and monitor cloud CI verification (`gh run watch`), or optionally run `./gradlew testDebugUnitTest` if working strictly offline.
4. **Prune Anchor**: Once verified and merged/pushed, delete the temporary safety branch (`git branch -D backup/...`).

### 2. Autosquash & Clean Commit Curation
- During task development, fixups can be committed using `git commit --fixup <commit-hash>`.
- Before opening a PR or merging to `main`, curate commits into atomic units:
  ```bash
  git rebase -i --autosquash $(git merge-base HEAD main)
  ```
- All final squashed commits MUST maintain the two-layer attribution format (`User:` / `AI:`).

### 3. Remote Push Guardrails: Never Raw Force
- `git push --force` is **STRICTLY FORBIDDEN**.
- If updating an un-merged local feature branch whose history was rebased, use strictly:
  ```bash
  git push --force-with-lease origin <branch-name>
  ```
- `--force-with-lease` guarantees the push will fail if the remote branch was modified by someone else or a CI bot.

### 4. Automated Regression Hunting (Git Bisect + Gradle)
When hunting down a regression introduced across recent commits, do NOT manually check out intermediate commits. Use automated test-driven bisect:
```bash
git bisect start
git bisect bad HEAD
git bisect good <known-good-tag-or-hash>
# Run headless Gradle test runner automatically
git bisect run ./gradlew testDebugUnitTest --tests "com.clawstack.shellguard.*RegressionTest*"
git bisect reset
```
*Rule*: Working tree MUST be clean (`git status`) before starting `git bisect`.

### 5. Worktrees for Parallel Subagents & Hotfixes
To inspect code, run long background builds, or handle urgent hotfixes without stashing or disrupting an in-flight working tree:
```bash
# Create clean worktree outside the primary tree
git worktree add ../ShellGuard-Hotfix -b fix/urgent-patch main
# Work, test, commit in hotfix directory
# Clean up upon merge
git worktree remove ../ShellGuard-Hotfix
git worktree prune
```

### 6. Reflog Disaster Recovery Playbook
If an accidental `git reset`, dropped stash, or bad rebase occurs:
1. **Do NOT panic or run blind destructive commands.**
2. Inspect reflog: `git reflog -n 20`.
3. Locate the commit hash immediately preceding the error (e.g. `HEAD@{1}`).
4. Restore state cleanly to a recovery branch:
   ```bash
   git branch recovery-$(date +%Y%m%d%H%M) <hash>
   ```
5. Inspect the recovery branch diff (`git diff HEAD..<recovery-hash>`) before applying.

## Subagent PR & Dual-Branch Merge Rollout Protocol
When merging upstream subagent PRs (e.g. Google Labs Jules PRs) that target `main` alongside downstream lead feature branches:
1. **Reconcile on Feature Branch**: Apply all doc reconciliations, manifest prunings, and tests on the active feature branch.
2. **Audit Drift**: Execute `python3 scripts/audit_docs_drift.py` locally (<1s).
3. **Merge Subagent PR**: Merge the subagent PR into `main` via `gh pr merge <PR_ID> --merge`.
4. **Synchronize & Fast-Forward**: Checkout `main`, pull latest, merge the feature branch (`git merge <branch>`), and push to `origin/main`.
5. **Tag & Trigger Cloud Release**: Tag the release (`git tag -a vX.Y.Z.N`) and push tag to origin to trigger cloud release packaging.

