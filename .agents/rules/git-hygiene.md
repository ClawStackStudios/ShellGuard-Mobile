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
- Write clear, conventional commit messages (e.g. `feat:`, `fix:`, `refactor:`, `chore:`, `test:`).
- Prefer new commits over amending. Never amend or rebase a commit without explicit written approval in the task.
- Never skip hooks (`--no-verify`) or bypass commit signing unless explicitly asked.

## Android Secrets & Keystore Safety (CRITICAL)
- **NEVER Commit Keystores or Private Keys**:
  - `*.jks`, `*.keystore`, `*.p12`, `*.pem`, `my-upload-key.jks`, `debug.keystore`.
- **NEVER Commit Local Machine Configs & API Credentials**:
  - `local.properties` (contains local machine Android SDK paths).
  - `.env`, `.env.*`, `google-services.json`, `secrets/**`.
- **NEVER Commit Build Outputs & Binaries**:
  - `build/`, `**/build/`, `*.apk`, `*.aab`, `*.apks`, `*.obb`, `*.dex`, `*.class`.
- **NEVER Commit IDE Caches**:
  - `.idea/`, `.gradle/`, `.kotlin/`, `captures/`, `.externalNativeBuild/`, `.cxx/`.
- **NEVER Commit Internal Agent Scratchpads & Visual Design Assets**:
  - `.agents/internal/`, `.agents/scratch/`, `**/scratch/**`. Always ensure these directories are declared in `.gitignore` and excluded from git staging.

## Destructive Operations — NEVER Without Explicit Confirmation
- `git push --force` / `--force-with-lease`
- `git reset --hard`, `git checkout/restore` to an older commit
- Deleting branches, tags, or stashes
- `rm -rf` or any bulk file deletion
- If unsure whether a file belongs to another agent's in-flight work, stop and coordinate — don't delete to silence an error.

## Android Pre-Commit Verification Gate
- Run unit and UI tests before committing: `./gradlew testDebugUnitTest`.
- Ensure clean build verification: `./gradlew assembleDebug`.
- Never commit broken tests or unverified compilation states.

## Attribution & Commit Message Format
- Commit under the human's configured identity (`git config user.name` / `user.email`). No separate agent identity, no AI co-author line.
- Every commit message uses this two-layer format:

  ```
  <type>: <short summary>

  User: <the intention, system design, architecture decision, or glue that was provided>
  AI: <the concrete implementation, functions, refactors, or tests that were generated>
  ```

- The `User:` line is always the *why/what* — the intent, spec, or structural decision.
- The `AI:` line is always the *how* — the code, logic, or test coverage that fulfilled it.
- If the human did the implementation directly (rare), put it under `User:` and write `AI: (none)`.
- If the agent did purely exploratory work with no human direction in that commit, write `User: (autonomous)` — but this should be the exception, not the norm.
- No trailers, no co-author lines, no `AI-Model:` metadata. The two-layer message *is* the attribution.

## Rebase Hygiene
- When rebasing, avoid opening editors: set `GIT_EDITOR=:` and `GIT_SEQUENCE_EDITOR=:` (or pass `--no-edit`).
