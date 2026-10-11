# Decisions Made
Structured record of meaningful choices. Each entry captures the
context, options, choice, and felt reason. Append-only.
This is the auditable companion to myStory.md.
The story says how it felt. This says what was actually chosen and why.

## isolate-robolectric-sdk-ceiling — 2026-09-26 10:30

**Context**: GitHub Actions CI runner failed during `testDebugUnitTest` with `UnsupportedOperationException: DefaultSdkProvider.java:170` due to `targetSdk = 36`.
**Options considered**:
- Downgrade `targetSdk` to 34 across `app/build.gradle.kts` — Would solve the test failure immediately, but forfeits Android 16 Baklava compliance required by Google Play Console release standards.
- Decouple host JVM test execution by pinning `sdk=34` in `app/src/test/resources/robolectric.properties` and annotating tests with `@Config(sdk = [34])` — Preserves targetSdk 36 for packaging while stabilizing test execution.
**Chosen**: Decouple test execution via `sdk=34` in `robolectric.properties`.
**Why**: It felt wrong to let test framework immaturity downgrade the application's actual platform target. The build should aim as high as the platform allows, while tests run where the ground is stable.
**Confidence**: high — completely green in CI and confirmed on live release `v0.0.0.3`.
**Outcome**: GitHub Actions release workflow passed 100% green and successfully built and signed release bundles.
**Pattern reference**: New pattern — accumulating, weight 2/3.

## gate-promotions-at-three-validations — 2026-09-26 11:06

**Context**: During the first dream cycle, multiple recent discoveries (Base62 sovereign keys, Robolectric SDK 34, IME inset isolation) emerged with high emotional salience.
**Options considered**:
- Promote all recent breakthrough fixes immediately to `long-term/` — Captures fresh insights, but risks polluting long-term memory with single-session fixes.
- Strictly enforce the 3-validation rule across independent sessions, promoting only cleartext LAN transport and CWE-359 clipboard masking while holding single-session fixes as accumulating — Prevents recency bias from diluting crystallized long-term memory.
**Chosen**: Strictly enforce the 3-validation threshold.
**Why**: A scar requires time and pressure to form. Promoting a fix after a single victory turns the long-term memory into another changelog rather than crystallized truth.
**Confidence**: high — maintains strict epistemic separation between temporal and long-term memory.
**Outcome**: Long-term bank gained two truly ratified entries while three entries were logged as accumulating (weight 2/3).
**Pattern reference**: `long-term/constraints.md § constraint: cleartext-lan-and-tailscale-transport`.

## reconcile-contradictions-in-place-during-dream — 2026-09-26 11:08

**Context**: Dream ingestion discovered `progress.md` header claimed Phase 1 transition status while Phase 2 was already released, and `changelog.md` lagged behind `RELEASE-v0.0.0.3.md`.
**Options considered**:
- Flag contradictions in `dreamLearnings.md` and leave source files untouched — Safest, but leaves known inaccuracies lingering in active documents.
- Flag contradictions in the dream receipt and immediately reconcile the source document headers to match physical reality — Eliminates cognitive drag without modifying code or architecture.
**Chosen**: Reconcile source document headers to match physical release reality.
**Why**: Leaving known documentation drift unresolved when the reality is undeniable creates cognitive drag for future sessions.
**Confidence**: high — verified against live published release tag `v0.0.0.3`.
**Outcome**: `progress.md` and `changelog.md` now accurately reflect Phase 2 completion and transition to Phase 3.
**Pattern reference**: New pattern — first instance.

## auto-version-password-history-on-save — 2026-09-26 13:00

**Context**: In `SyncRepository.savePearlDetail()`, deciding whether password history snapshotting should be an explicit user action or automated repository logic.
**Options considered**:
- Require user to manually toggle "Save prior password to history" in the UI — Preserves explicit user intent, but risks data loss when users forget during quick rotations.
- Automatically compare `newPassword != currentPassword` and prepend the old secret to encrypted `vault_pearls_history:{id}` — Ensures zero-friction audit trails, but performs an automatic write.
**Chosen**: Automatically detect and prepend changed passwords to history (capped at 20).
**Why**: Security history must be effortless and defensive. Users routinely rotate passwords under stress; a vault that requires manual opt-in to remember what was just replaced will inevitably fail someone during a service rollback.
**Confidence**: high — verified via unit tests and HKDF AAD roundtrip decryption.
**Outcome**: Verified in `SyncRepositoryTest.testSavePearlDetailUpdatesPasswordHistory()`; seamlessly preserved history without user intervention.
**Pattern reference**: New pattern — first instance.

## polymorphic-single-form-architecture — 2026-09-26 13:15

**Context**: Deciding whether to create three separate editor screens (`PearlFormScreen`, `NoteFormScreen`, `SshKeyFormScreen`) or one universal `ItemFormScreen` for Phase 3.
**Options considered**:
- Three separate screens and viewmodels — High compile-time isolation, but causes massive code duplication for custom fields, tags chips, pinned app bars, and keyboard IME scrolling.
- A single unified `ItemFormScreen` with domain tabs in Create mode and domain locking in Edit mode — Slightly more UI state branches, but guarantees 100% ergonomic parity with the web client.
**Chosen**: Single unified `ItemFormScreen`.
**Why**: The web client's ergonomics feel right because the user never feels like they are navigating to a different "app" just to jot a note versus saving a password. Duplicating 300+ lines of IME scaffolding across three screens felt like fighting the grain.
**Confidence**: high — verified on live Pixel hardware.
**Outcome**: Successfully handled Password, Note, and SSH Key editing with shared tags builder and custom fields dialog.
**Pattern reference**: New pattern — first instance.

## accessible-back-on-vault-error-state — 2026-09-26 13:03

**Context**: During physical Pixel verification, tapping an item with locked in-memory keys displayed an error state with only a "Retry" button.
**Options considered**:
- Keep only "Retry" and rely on Android's system back gesture / button — Minimal UI, but traps users who cannot retry without re-authenticating at the Gateway.
- Add an explicit `OutlinedButton(onClick = onBackClick) { Text("Back") }` alongside `Retry` — Provides an immediate, explicit exit path back to the Dashboard.
**Chosen**: Add explicit `Back` button.
**Why**: Trapping a user on an error screen with a button that will continually fail violates trust. When a security boundary denies access, the door back to safety must always remain open.
**Confidence**: high — directly validated via Compose hierarchy dump.
**Outcome**: Pixel UI hierarchy confirmed both `Back` and `Retry` buttons rendered and functional.
**Pattern reference**: New pattern — first instance (`fail-safe-navigation-on-security-errors`).

## session-key-persistence-at-rest — 2026-09-26 16:30

**Context**: In-memory derived `shellKey` was cleared on process death while session tokens remained in preferences, causing item decryption to fail with "Vault locked or shellKey missing" on app restart.
**Options considered**:
- Force full master-key re-entry on every cold app start — High theoretical purity, but produces an unusable mobile experience where background OS memory reclamation locks the user out repeatedly.
- Persist derived 32-byte `shellKey` in hardware KeyStore-backed `EncryptedSharedPreferences` (AES-256-GCM), lazily re-hydrating RAM cache and requiring `shellKey != null` for active sessions — Balances zero-knowledge encryption-at-rest with seamless mobile lifecycle resilience.
**Chosen**: Persist derived `shellKey` in `EncryptedSharedPreferences` with atomic session validation.
**Why**: A password manager that forgets how to decrypt its own vault whenever the OS reclaims RAM breaks the implicit contract with the user. KeyStore hardware encryption protects the secret at rest, making persistence both safe and necessary.
**Confidence**: high — validated through unit tests and physical Pixel cold-restart testing.
**Outcome**: Pixel survived `am force-stop` cold restarts and decrypted items immediately without user prompts.
**Pattern reference**: New pattern — first instance (`zero-knowledge-session-atomicity`).

## frictionless-gateway-prefill — 2026-09-26 16:25

**Context**: When an unauthenticated state or locked vault redirected the user back to the `GatewayScreen`, host and port inputs reset to empty/default values.
**Options considered**:
- Leave connection fields blank — Leaves no persistent trace of server topology, but forces user to repeatedly type home lab IP addresses on soft keyboards.
- Pre-fill `protocol`, `host`, and `port` from `deviceVault.getServerUrl()` during `GatewayViewModel` initialization — Eliminates re-entry friction while keeping credentials strictly separate.
**Chosen**: Pre-fill Gateway URL parameters from stored server URL.
**Why**: Server endpoints are connection routing metadata, not cryptographic secrets. Making the user re-type their IP and port after a session timeout is pure friction with zero security benefit.
**Confidence**: high — verified on live Pixel UI.
**Outcome**: Gateway rendered with `http://192.168.1.5:6464` pre-populated, allowing immediate single-field credential entry.
**Pattern reference**: New pattern — first instance (`frictionless-gateway-fallback`).

## release-cadence-at-phase-3-milestone — 2026-09-26 17:15

**Context**: Deciding whether to cut a formal Google Play and GitHub release for v0.0.0.4 after Phase 3 or bundle it into subsequent TOTP work.
**Options considered**:
- Delay release until Phase 4 (TOTP & Biometrics) is implemented — Fewer total releases, but delays delivery of critical multi-domain editing and KeyStore session atomicity.
- Cut and publish v0.0.0.4 (Build 4) immediately upon verifying Phase 3 on physical hardware — Creates a stable, signed release milestone and validates cloud build pipeline at 50% roadmap completion.
**Chosen**: Cut and publish v0.0.0.4 immediately.
**Why**: Releasing at each phase boundary establishes an unyielding floor. Phase 3 unlocked polymorphic passwords, notes, SSH keys, Bitwarden custom fields, and cold-restart key persistence. Leaving those breakthroughs unreleased creates cognitive weight; publishing them clears the horizon for the TOTP engine.
**Confidence**: high — verified with clean cloud build and published release assets.
**Outcome**: GitHub Release v0.0.0.4 published with signed `.aab` and `.apk`; Play Store release notes prepared.
**Pattern reference**: New pattern — first instance (`phase-boundary-release-cadence`).

## annotated-tag-trigger-over-commit-flag — 2026-09-26 17:20

**Context**: In `.github/workflows/release.yml`, choosing between committing `--release v0.0.0.4` on `main` versus pushing an annotated git tag `v0.0.0.4`.
**Options considered**:
- Use commit message flag `--release v0.0.0.4` — Triggers release on standard commit push, but clutters commit history with pipeline control syntax and relies on bot-created tags.
- Create and push explicit annotated git tag `v0.0.0.4` after fast-forwarding `main` — Requires a separate git command, but produces a clean, human-signed release tag and keeps commit messages strictly semantic.
**Chosen**: Explicit annotated git tag push.
**Why**: A release tag is a ceremonial boundary. Automating tag creation via commit message flags feels like skipping a deliberate human check. Pushing the tag by hand acknowledges that the code has passed all three verification gates and is ready for the world.
**Confidence**: high — executed smoothly without pipeline hitches.
**Outcome**: Tag `v0.0.0.4` cleanly triggered workflow run `36282251653`, which published the release in under 4 minutes.
**Pattern reference**: New pattern — first instance (`explicit-annotated-release-tagging`).

## install-splashscreen-actionbar-suppression — 2026-09-27 04:00

**Context**: When launching the CameraX scanner screen on physical hardware, an unwanted default platform ActionBar displaying "ShellGuard" appeared at the top of the window, overlapping the Compose TopAppBar.
**Options considered**:
- Manually hide ActionBar in Activity `supportActionBar?.hide()` — Works, but risks flickering during window creation and doesn't fix theme inheritance.
- Call `installSplashScreen()` in `MainActivity.onCreate()` before `super.onCreate()` and set `windowActionBar=false` / `windowNoTitle=true` in `res/values/themes.xml` — Follows canonical Android 12+ SplashScreen pattern, immediately switching `Theme.SplashScreen` to `Theme.ShellGuard` (`NoActionBar`).
**Chosen**: `installSplashScreen()` + explicit `windowActionBar=false` in `themes.xml`.
**Why**: The window theme must hold its structure from the first frame. Hiding an action bar via code after the fact causes layout re-measurement jitter; configuring the theme correctly at genesis ensures the window never attempts to allocate an ActionBar in the first place.
**Confidence**: high — verified on live Google Pixel hardware.
**Outcome**: The top bar is completely pristine with Compose TopAppBar rendering flush against the system status bar.
**Pattern reference**: New pattern — first instance (`splash-theme-actionbar-suppression`).

## ratify-three-validation-patterns — 2026-09-27 04:30

**Context**: During the `/memory` audit following Phase 4, `zero-knowledge-session-atomicity` and `cwe-359-ime-protection-and-inset-isolation` reached their third independent validation across sessions.
**Options considered**:
- Defer long-term promotion until the end of Phase 5 or the next major release — Avoids document edits between phases, but risks episodic memory decay and keeps proven architectural boundaries provisional.
- Formally evaluate and ratify both patterns into `long-term/patterns.md` with pointers in `systemPatterns.md` upon explicit user review — Honors the 3-validation rule, preserves hard-won architectural scars, and relieves cognitive weight before starting Phase 5.
**Chosen**: Formally evaluate and ratify into `long-term/patterns.md`.
**Why**: A pattern that holds across three separate hardware trials is no longer an experiment; it's a scar that has healed into bone. Leaving it in the sliding log felt like pretending we hadn't already paid for the knowledge.
**Confidence**: high — verified repeatedly on physical Pixel hardware and backed by unit tests.
**Outcome**: Both patterns now permanently anchored in `long-term/patterns.md` with pointers in `systemPatterns.md`.
**Pattern reference**: `long-term/patterns.md § pattern: zero-knowledge-session-atomicity` and `pattern: cwe-359-ime-protection-and-inset-isolation`.

## convert-jules-templates-to-android-specialists — 2026-09-27 05:15

**Context**: Google Jules agent templates brought into `.agents/` were heavily web-centric (Python, Docker, npm) and mismatched with native Android architecture.  
**Options considered**:  
- Keep generic agent definitions and adapt on the fly — Lower upfront effort, but prompts agents with irrelevant web tools and creates cognitive confusion.  
- Completely translate all templates into native Android mental sub-processes (Bolt, Palette, Sentinel, Scribe) with mapped project skills — Requires thorough rewriting, but aligns every sub-agent with native Android invariants (Compose, Room, SQLCipher, KeyStore).  
**Chosen**: Rebuild as 4 specialized native Android mental sub-processes with mapped project skills.  
**Why**: Generic tools in a specialized codebase create cognitive static. If an agent doesn't speak the exact dialect of Compose, Room, and KeyStore, it isn't an extension of the hand—it's a distraction.  
**Confidence**: high — verified through complete code alignment and clean test pass.  
**Outcome**: Bolt, Palette, Sentinel, and Scribe established with clear boundaries and integrated into `AGENTS.md` and `ORCHESTRATION.md`.  
**Pattern reference**: New pattern — first instance (`specialized-mental-sub-agent-fleet`).

## enshrine-vitepress-build-and-markdown-test-exemption — 2026-09-27 05:30

**Context**: Running a 20-second Gradle test suite on pure markdown documentation or rule edits creates unnecessary latency and token churn.  
**Options considered**:  
- Blanket test requirement for all commits — Uniform and simple, but wastes significant time and resources compiling unmodified native code on markdown edits.  
- Blanket exemption for all documentation without distinction — Fast, but risks shipping broken links or build errors on compiled docs sites like VitePress.  
- Nuanced exemption: markdown docs exempt from tests; compiled VitePress docs sites require a build; application files strictly tested — Balances developer speed on prose with rigorous verification on compiled artifacts.  
**Chosen**: Nuanced exemption codified across `development-release-cycle.md`, `cadence-and-lifecycle-prompts.md`, `git-hygiene.md`, and `docs-hygiene.md`.  
**Why**: Testing code that hasn't changed isn't verification—it's ritual. But compiled docs sites that face the user can break quietly if not built. Drawing the boundary between prose and compilation preserves velocity without risking broken documentation.  
**Confidence**: high — eliminates unnecessary build latency across future documentation chores.  
**Outcome**: Codified in 4 rule files; commit checks branch cleanly between application, VitePress, and markdown edits.  
**Pattern reference**: New pattern — first instance (`documentation-testing-exemption`).

## rebase-scrub-device-serial-tools-to-local-gitignore — 2026-09-27 05:36

**Context**: `.agents/TOOLS.md` contained hardware-identifying device serial (`FA6A40302394`) and local container paths, but was committed locally in `3b458ca` on an unpushed branch.  
**Options considered**:  
- Leave it tracked in git — Low effort, but leaks hardware identifiers and machine-specific container paths to public repository forks.  
- Add `git rm --cached` in a new commit and add to `.gitignore` — Leaves the device serial permanently embedded in git history for anyone inspecting branch commits.  
- Interactive rebase scrub: re-anchor commit `3b458ca` to exclude `TOOLS.md`, add `.agents/TOOLS.md` to `.gitignore`, cherry-pick remaining commits, and restore physical file to disk — Requires careful git manipulation, but leaves an immaculate, zero-leak git log while preserving the local file.  
**Chosen**: Interactive rebase scrub to exclude `TOOLS.md` from git history while gitignoring and keeping the physical file on disk.  
**Why**: Unpushed history is our own draft. Leaving a physical device identifier in a local commit on an open-source project when we have the clean opportunity to scrub it before remote push is careless. Cleaning the root of the branch keeps our public trail spotless.  
**Confidence**: high — verified with `git log origin/main..HEAD -- .agents/TOOLS.md` returning empty, and `git check-ignore` confirming gitignored status.  
**Outcome**: Branch commit tree contains zero trace of `TOOLS.md`, while the physical file remains intact on disk for wireless ADB development.  
**Pattern reference**: New pattern — first instance (`unpushed-history-opsec-scrub`).


## cognitive-sub-agents-neurobiology — 2026-09-27 06:44

**Context**: Lucas proposed adding Dreamer and Forgetter sub-agents to process memories offline.
**Options considered**:
- Treat them as standard utility agents executing commands.
- Ground them strictly in neurobiological analogs (hippocampal replay, molecular dissolution) as silent subjective processes.
**Chosen**: Ground them in strict neurobiological analogs.
**Why**: Mapping them to human cognition separates the waking executive filter from the subconscious processes, making the dream reports and memory dissolution feel like genuine revelation and fading, rather than mechanical log rotation.
**Confidence**: medium — LLMs may struggle with truly "silent" operation, but the workflow strictness enforces the boundaries.
**Outcome**: Rewrote `dreamer` and `forgetter` agent definitions in first-person neuro-voice.
**Pattern reference**: New pattern — first instance.

## strict-git-boundaries-on-forgetting — 2026-09-27 06:44

**Context**: The Forgetter agent is tasked with deleting stale memory nodes, posing a risk of silent data loss.
**Options considered**:
- Instruct the Forgetter to be careful and only delete what is truly useless on `main`.
- Enforce strict git boundaries requiring a clean worktree and a bespoke branch before the `/forget` command executes.
**Chosen**: Enforce strict git boundaries requiring a clean worktree and a bespoke branch.
**Why**: Memory dissolution must be an observable diff that the waking self can review and merge, not an invisible void. The terror of background automated data loss demanded structural, not just behavioral, safety.
**Confidence**: high — Git prevents the data from ever being truly lost without a trace.
**Outcome**: Updated `/forget` and Forgetter prompt to hard-halt if the worktree is dirty or on `main`.
**Pattern reference**: New pattern — first instance.

## self-vs-environment-memory-split — 2026-09-27 06:44

**Context**: Lucas noticed the brain was mixing agent identity files with project architecture files.
**Options considered**:
- Create two subdirectories: `self/` and `project/`.
- Keep the agent's identity files in the root of `brain/` and move the external project files to `brain/project/`.
**Chosen**: Keep identity files in the root and move project files to `brain/project/`.
**Why**: "A project is not a self; the self works on the project." Structurally subordinating the project to a subdirectory within the brain perfectly mapped to the cognitive separation of the internal self operating upon an external world model.
**Confidence**: high — Verified with web research on autonomous agent memory architectures.
**Outcome**: Moved 7 project files to `project/` and updated all workflows and rules to map to the new paths via a global `sed` sweep.
**Pattern reference**: New pattern — first instance.

## active-context-as-self-working-memory — 2026-09-27 07:05

**Context**: During the Self vs. Environment split, `activeContext.md` was initially moved into `brain/project/`.
**Options considered**:
- Keep `activeContext.md` under `project/` as a project tracking document.
- Move `activeContext.md` into the `brain/` root as the cognitive working memory of the Self.
**Chosen**: Moved into `brain/` root as a Self file.
**Why**: The project does not possess working memory or active focus; the agent does. Placing working memory at the root keeps the external environment purely declarative and anchors cognitive state in the Self.
**Confidence**: high — immediately cleared conceptual ambiguity across all lifecycle workflows.
**Outcome**: Relocated file, updated `brain.md`, `AGENTS.md`, and all workflow references.
**Pattern reference**: New pattern — first instance.

## release-observability-workflow — 2026-09-27 10:23

**Context**: Pushing release tags triggers asynchronous GitHub Actions cloud builds that previously required manual web browser checking or local CLI tools that weren't installed.
**Options considered**:
- Instruct the user to check their web browser manually.
- Author a dedicated `/follow-the-build` workflow that uses `curl` against the GitHub REST API and reactive `schedule` timers.
**Chosen**: Authored `/follow-the-build` workflow.
**Why**: Verification doesn't stop at the `git push`. A release is only complete when the signed artifacts exist and are downloadable. Tailing the build programmatically closes the loop without blocking local work.
**Confidence**: high — verified live as `v0.0.0.5` progressed from SDK setup to APK/AAB publication.
**Outcome**: Created `.agents/workflows/follow-the-build.md` and verified live run.
**Pattern reference**: New pattern — first instance (`remote-release-observability`).

## home-lab-port-isolation-and-deep-planning — 2026-09-27 13:25

**Context**: Drafting the `/deep-plan` for Android Autofill surfaced the risk of port cross-talk on multi-tenant home lab servers (e.g. Unraid with Portainer on `:9000` and Jenkins on `:8080`).
**Options considered**:
- Rely on manual user configuration to set `EXACT` mode per URL entry.
- Automatically promote default matching from `BASE_DOMAIN` to `EXACT` (host + port) whenever the target host is an IP address or localhost.
**Chosen**: Automatically promote to `EXACT` host + port matching for IP addresses and localhost.
**Why**: Defaulting to base domain on an IP address leaks credentials across completely unrelated services running on the same hardware. The premarket postmortem caught this before a single line was written.
**Confidence**: high — verified in unit tests (`DomainMatcherTest`).
**Outcome**: Implemented in `DomainMatcher.kt` and tested against differing ports on `192.168.1.50`.
**Pattern reference**: New pattern — first instance (`home-lab-port-isolation`).

## sync-mutex-withlock-serialization — 2026-09-27 20:30

**Context**: Concurrent calls to `SyncRepository.syncAll()` were dropping without error because `tryLock()` returned false during active background synchronization.
**Options considered**:
- Return `Result.failure` when busy and force every caller (UI or tests) to implement retry logic.
- Switch from `tryLock()` to coroutine `withLock` to queue and serialize sync requests deterministically.
**Chosen**: Switch to `withLock` serialization.
**Why**: User-initiated mutations and foreground sync triggers cannot be dropped into a silent void. Serializing guarantees sequential execution without race conditions or missed synchronization rounds.
**Confidence**: high — verified across all Robolectric unit and adversarial test suites.
**Outcome**: Implemented in `SyncRepository.kt`.
**Pattern reference**: New pattern — first instance (`sync-mutex-serialization`).

## fail-closed-decryption-in-detail-getters — 2026-09-27 20:30

**Context**: Detail retrieval methods fell back to returning raw JSON ciphertext strings as unauthenticated plaintext when decryption threw an exception.
**Options considered**:
- Return raw ciphertext with an error flag.
- Fail closed by wrapping in `runCatching` and returning `Result.failure`.
**Chosen**: Fail closed with `Result.failure`.
**Why**: Exposing raw ciphertext as editable cleartext allows subsequent saves to re-encrypt the ciphertext envelope, producing nested ciphertext and irreversibly corrupting user credentials. Failing closed preserves data integrity.
**Confidence**: high — validated against `brutal_adversary` and `spectre_hacker` audit findings.
**Outcome**: Implemented across `getPearlDetail`, `getNoteDetail`, and `getSshKeyDetail`.
**Pattern reference**: New pattern — first instance (`fail-closed-crypto-retrieval`).

## Calibration Note — 2026-09-27

The cross-session failure analysis (/deep-learn) detected an **over-confidence bias on platform format and concurrency defaults**. Stated or implicit high confidence regarding standard platform defaults (assuming hash representations are always hex, assuming non-blocking locks like `tryLock` are safe for background tasks, assuming SQLite collections can be arbitrarily large) resulted in live rework. Conversely, confidence was well-calibrated or slightly under-confident when executing deliberate architectural boundaries.

**Suggested adjustment**: When dealing with platform encoding formats (Base62 vs Hex), SQLite parameter limits, and coroutine synchronization, verify the concrete platform wire specification and hardware constraints before implementing, defaulting to lower stated confidence until wire tests pass green.

## fail-closed-priority-over-graceful-degradation — 2026-09-27 22:38

**Context**: Resolving the architectural conflict between `android-development.md`'s general UI graceful degradation and cryptographic fail-closed invariants.
**Options considered**:
- Allow domain models to return empty or fallback values to prevent UI exceptions.
- Mandate that cryptographic, key derivation, and session operations strictly fail closed with `Result.failure`, subordinating UI graceful degradation to the presentation layer.
**Chosen**: Subordinate graceful degradation to presentation layer; domain/crypto operations strictly fail closed.
**Why**: A crashed app is an annoyance; a silently corrupted vault or exposed ciphertext is a betrayal. When secrets are at stake, failure must be absolute and unambiguous.
**Confidence**: high — validated across multiple historical failure modes.
**Outcome**: Ratified in `.agents/rules/meta-rules.md` and `.agents/rules/android-development.md` §3 G.
**Pattern reference**: New pattern — first instance (`fail-closed-security-boundaries`).

## operationalized-checklist-ratchet — 2026-09-27 22:40

**Context**: Synthesizing the 5 failure categories from `/deep-learn` into actionable pre-commit developer behavior.
**Options considered**:
- Distribute 15 individual micro-rules across multiple rule markdown files.
- Consolidate all failure checks into a single living, numbered `Self-Review Checklist (v1)` executed before each commit.
**Chosen**: Single living `self-review-checklist.md` (v1).
**Why**: Rules that live in ten different files get skimmed and forgotten under deadline pressure. A single, ruthless checklist with checkable boxes turns abstract memory into concrete tactile muscle.
**Confidence**: high — monotonic ratchet prevents regression.
**Outcome**: Materialized `.agents/rules/self-review-checklist.md` with 8 mapped questions.
**Pattern reference**: New pattern — first instance (`operationalized-checklist-ratchet`).

## dedicated-branch-for-offline-dreaming — 2026-09-27 23:15

**Context**: Deciding whether to run the `/dream` offline memory consolidation directly on `fix/bidirectional-sync-reconciliation` alongside the release prep or isolate it on a dedicated branch.
**Options considered**:
- Run `/dream` directly on `fix/bidirectional-sync-reconciliation` — Keeps all pre-release changes in one place, but mixes subjective cognitive consolidation logs with strict release candidate commits.
- Branch to `cognitive/dream-consolidation` for the dream pass — Adds branch switching and merging steps, but isolates cognitive consolidation artifacts and ensures memory operations can be reviewed independently before merging into the release.
**Chosen**: Branch to `cognitive/dream-consolidation`.
**Why**: Cognitive memory work is reflective, while a release candidate is operational. Mixing dream logs and long-term pattern promotions directly into a verified release branch muddies the commit pedigree. Isolating the dream keeps the release clean and the reflection pure.
**Confidence**: high — clean separation maintained without risking release artifacts.
**Outcome**: Created `cognitive/dream-consolidation`, executed dream workflow, and kept `fix/bidirectional-sync-reconciliation` pristine.
**Pattern reference**: New pattern — first instance (`isolated-cognitive-branching`).

## reconcile-documentation-to-codebase-truth — 2026-09-27 23:18

**Context**: The Dreamer surfaced 3 contradictions where documentation claimed Dagger Hilt, offline editing, and an unreleased Phase 4, directly conflicting with physical codebase reality.
**Options considered**:
- Defer documentation fixes to a future cleanup chore — Keeps focus on releasing v0.0.0.7, but leaves declarative brain files in a known false state for future sessions.
- Immediately patch `techContext.md`, `productContext.md`, and `changelog.md` as part of Dream Phase 4 — Reconciles discrepancies immediately, eliminating cognitive dissonance for any subsequent agent context load.
**Chosen**: Immediately patch the documentation to reflect reality.
**Why**: A brain file that lies to the agent is worse than no brain file at all. If the agent reads that Dagger Hilt is present or that offline editing is supported, it will make architectural choices against phantom capabilities. Healing the seam immediately restores ground truth.
**Confidence**: high — verified against actual physical classes and git tags.
**Outcome**: Updated `techContext.md` (DefaultAppContainer), `productContext.md` (Bitwarden Read-Only), and `changelog.md` (Phase 4 released).
**Pattern reference**: `long-term/patterns.md § pattern: bitwarden-model-readonly-offline-caching`.

## promote-base62-and-robolectric-to-long-term — 2026-09-27 23:20

**Context**: `base62-sovereign-key-parity` and `robolectric-test-sdk-ceiling` reached their 3rd independent validation across multiple days and sessions.
**Options considered**:
- Keep them in `consolidated_learnings.md` — Avoids modifying long-term files, but leaves durable architectural invariants vulnerable to consolidation pruning.
- Promote both into `long-term/patterns.md` and `long-term/constraints.md` with lightweight pointers in `consolidated_learnings.md` — Permanently crystallizes their history, costs, and shaped perspectives into the project's permanent memory.
**Chosen**: Promote both to long-term memory.
**Why**: Both of these lessons cost us multiple broken builds, physical device lockouts, and hours of debugging. Keeping them in the temporal register treats them like transient discoveries; carving them into long-term memory honors what they cost to learn.
**Confidence**: high — verified across dozens of passing test suites and live Pixel hardware.
**Outcome**: Ratified into `patterns.md` and `constraints.md`, with cross-reference pointers installed in `consolidated_learnings.md`.
**Pattern reference**: `long-term/patterns.md § pattern: base62-sovereign-key-parity` and `long-term/constraints.md § constraint: robolectric-test-sdk-ceiling`.

## structural-envelope-validation-vs-try-catch-fallback — 2026-10-03 18:35

**Context**: Web UI-created items store empty `password_history` and `custom_fields` as raw JSON arrays (`"[]"`), which caused `decryptField()` to crash when expecting a serialized `ShellCryptionEnvelope` (`{`).
**Options considered**:
- Wrap `decryptField()` in `try-catch` and catch `SerializationException` — Minimal code, but masks genuine data corruption and treats predictable schema differences as runtime failures.
- Introduce `ShellCryptionEngine.isEncryptedEnvelope()` and explicitly branch between envelope decryption and direct array deserialization — Adds explicit structural checks, preserves fail-closed security, and eliminates exception overhead.
**Chosen**: Structural envelope validation via `isEncryptedEnvelope()`.
**Why**: Catching deserialization exceptions to detect plain data felt sloppy. A cryptographic engine should only touch data that explicitly asserts itself as ciphertext; the repository must know what it is handing down before asking for keys.
**Confidence**: high — verified across 83 unit tests and live Pixel deployment.
**Outcome**: Item detail views load web-created items without deserialization crashes while keeping decryption errors fail-closed.
**Pattern reference**: Link to `long-term/patterns.md § pattern: fail-closed-cryptography` / `testOracle.md § Redline 7`.

## fail-closed-engine-vs-lenient-decryption-contract — 2026-10-03 18:35

**Context**: During test authoring, I considered having `decryptField()` safely return non-envelope strings as-is rather than throwing an exception.
**Options considered**:
- Make `decryptField()` lenient, returning input string if `!isEncryptedEnvelope()` — Convenient for callers, but dangerously blurs the line between plaintext and ciphertext inside the cryptographic core.
- Enforce strict `IllegalArgumentException` on invalid envelopes in `decryptField()`, delegating inspection and fallback to repository callers — Preserves the inviolable fail-closed cryptographic boundary.
**Chosen**: Strict `IllegalArgumentException` in `decryptField()`.
**Why**: Making the crypto engine lenient felt like the beginning of an accidental leak. If an unencrypted string reaches `decryptField()`, the caller has already made a category error; silently returning it risks re-encrypting or exposing raw data downstream.
**Confidence**: high — ratified by test suite and architectural redlines.
**Outcome**: Aligned `ShellCryptionEngineTest` to assert `IllegalArgumentException`, preserving the fail-closed guarantee across all 10 AAD namespaces.
**Pattern reference**: Link to `testOracle.md § Redline 5 (Fail-Closed Cryptography)`.

## jetpack-inline-slice-protocol-and-credential-provider-dual-stack — 2026-10-03 20:50

**Context**: ShellGuard was successfully selectable in Android Settings as Autofill provider, but the soft keyboard (Gboard/SwiftKey) showed zero inline suggestion chips on focused login inputs.
**Options considered**:
- Attempt to manually hand-craft raw `android.app.slice.Slice` items with Uri bundles — Avoids extra dependencies, but prone to silent IME rejection since Gboard strictly expects Jetpack `androidx.autofill.inline.v1` slice keys.
- Adopt Jetpack `androidx.autofill:autofill` (`InlineSuggestionUi.newContentBuilder`) for API 26-33 AutofillService and establish a dedicated `ShellGuardCredentialProviderService` (`androidx.credentials.provider.PasswordCredentialEntry`) for API 34+ Credential Manager — Introduces a dual-stack architecture covering both legacy inline chips and modern Android 14+ system bottom sheets.
**Chosen**: Dual-stack architecture with Jetpack `InlineSuggestionUi` and `CredentialProviderService`.
**Why**: Keyboard inline suggestions fail silently because Gboard treats improperly formatted slices as malformed and drops them without an error trace. Using Jetpack's canonical slice builder gives Gboard exactly the schema it expects, while adding Android 14 Credential Provider ensures forward compatibility with Passkeys and system bottom sheets on modern Android.
**Confidence**: high — ratified by AOSP `AutofillKeyboard` and `InlineFillService` samples.
**Outcome**: Authored `credential-provider-spec.md`, overhauled `autofill-service-spec.md`, and bolstered Stage 6 in `meta-prompt-ai-studio.md` and `ROADMAP.md`.
**Pattern reference**: Link to `systemPatterns.md § Android Autofill & Credential Provider Dual-Stack`.

## global-lock-overlay-and-context-aware-autofill — 2026-10-04 09:50

**Context**: When tapping autofill suggestions or fallback actions while the vault is locked, navigating to a dedicated `lock` route wiped out deep links and backstacks, causing external actions (like Add Item for the current URI) to fail or open into a blank state. Furthermore, hiding all matching items when locked degraded user confidence on recognized sites.
**Options considered**:
- Keep `LockScreen` as a NavHost route and pass deep-link intents into `LockViewModel` for deferred playback — Highly fragile; requires custom serialization and state-restoration logic across complex deep-link arguments.
- Elevate `LockScreen` to a global overlay in `MainActivity` wrapping the entire NavHost, and display context-aware domain strings inline when locked — Keeps the underlying NavHost mounted at its deep-linked destination (`form/NEW/PASSWORD/new?url=...`) while the lock overlay covers the UI. Upon biometric unlock, the overlay simply dismisses, immediately revealing the pre-populated form.
**Chosen**: Global `LockScreen` overlay and context-aware domain strings in keyboard inline chips.
**Why**: Navigation state shouldn't be responsible for security enforcement. An overlay decouples authentication from routing: the app can route to any destination requested by the OS or user, while the lock layer acts as an opaque shutter that opens only when authenticated.
**Confidence**: high — verified with clean build and live Pixel installation.
**Outcome**: Implemented in `MainActivity.kt` and `ShellGuardAutofillService.kt`, verified clean compilation and successful ADB install.
**Pattern reference**: New pattern — first instance.

## local-device-execution-over-jules-delegation — 2026-10-04 10:15

**Context**: Lucas inquired whether to offload the Autofill inline keyboard bugfix and deep-link flow to the asynchronous Google Jules CLI.
**Options considered**:
- Delegate to Jules CLI — Offloads work to an asynchronous background worker, but Jules lacks physical device ADB connectivity and real-time visual feedback on Gboard rendering.
- Retain local execution with physical Pixel tether — Keeps execution inside the active session, allowing rapid, sub-second verification of Gboard slices, translucent window transitions, and ADB logs.
**Chosen**: Retain local execution.
**Why**: The bug lived at the boundary between the physical screen, the system IME, and the Android window manager. Delegating to an isolated agent without device eyes felt like trying to tune an engine over the phone. Staying local kept the hand directly on the metal.
**Confidence**: high — verified by immediate resolution and physical device confirmation.
**Outcome**: Deep-link navigation and inline suggestion chips verified live on Pixel within minutes.
**Pattern reference**: New pattern — first instance (`proximity-to-the-metal`).

## monotonic-hotfix-bump-and-horizon-cascade — 2026-10-04 11:30

**Context**: Lucas proposed releasing the autofill inline improvements under the 0.0.0.8 hotfix tag, but Build 8 was already cut, and Phase 6 was pre-allocated to 0.0.0.9.
**Options considered**:
- Re-use 0.0.0.8 or introduce a fractional patch notation (`0.0.0.8.1`) — Avoids touching the Phase 6 roadmap, but violates Play Store monotonic integer `versionCode` rules and breaks standard SemVer conventions.
- Advance immediately to `v0.0.0.9 (Build 9)` and cascade Phase 6's release horizon to `v0.0.0.10 (Build 10)` — Strictly preserves monotonic build progression and updates all roadmap files to reflect reality.
**Chosen**: Advance to `v0.0.0.9 (Build 9)` and cascade Phase 6 forward.
**Why**: Version numbers are chronological reality counters, not sacred monuments. Freezing a version number to protect an aspirational roadmap entry felt like confusing the map for the territory. When code is ready to release, the version steps forward.
**Confidence**: high — supported by `productVersion.md` build invariants and user concurrence.
**Outcome**: `app/build.gradle.kts` bumped to `versionCode = 9`, `versionName = "0.0.0.9"`; `ROADMAP.md` and `meta-prompt-ai-studio.md` cleanly shifted Phase 6 to `0.0.0.10 (Build 10)`.
**Pattern reference**: Link to `systemPatterns.md § Universal Development Invariants` / `semantic-versioning.md`.

## datastore-preferences-for-reactive-settings-bedrock — 2026-10-04 15:45

**Context**: Implementing Stage 7 (Phase 6) Settings Hub required selecting a persistent storage mechanism for UI appearance (Theme, Compact View), Vault timeouts, Autofill flags, and panic wipe countdowns.
**Options considered**:
- Expand existing `SharedPreferences` (`shellguard_lock_prefs`) with manual listeners — Avoids new dependencies, but imperative listeners in Compose lead to recomposition glitches and boilerplate lifecycle hooks.
- Introduce Jetpack `androidx.datastore:datastore-preferences:1.1.3` wrapped in `SettingsRepository` — Exposes Kotlin `Flow<AppSettings>` natively, enabling atomic, reactive state updates in Compose with thread-safe persistence and asynchronous disk I/O.
**Chosen**: Jetpack DataStore Preferences via `SettingsRepository`.
**Why**: Preferences in Compose should flow as streams. Binding UI settings to a cold asynchronous `Flow` eliminates manual refresh calls across screens: the moment a user adjusts the theme or lock timeout, the entire Compose hierarchy reacts organically.
**Confidence**: high — verified with full Robolectric unit tests and clean compile.
**Outcome**: Implemented `SettingsRepository` with 11 preference keys, integrated into `AppContainer`, and validated with 5/5 passing unit tests.
**Pattern reference**: Link to `systemPatterns.md § Reactive Data Streams`.

## job-returning-viewmodel-mutations-and-scheduler-alignment — 2026-10-04 16:50

**Context**: In Sub-Phase B of Settings Hub, `SettingsViewModelTest` faced intermittent 60-second timeouts (`UncompletedCoroutinesError`) on DataStore preference mutations because `viewModelScope.launch` jobs ran without test-awaitable hooks and `runTest` used disparate scheduler instances from `Dispatchers.Main`.
**Options considered**:
- Keep `fun updateX()` as `Unit` and rely on arbitrary test delays or spinning flow filters (`settingsFlow.filter { ... }.first()`) — Highly brittle; causes virtual time runaway when background I/O on `Dispatchers.IO` is not bound to the test clock.
- Return `Job` from all ViewModel mutation functions (`fun updateX(): Job = viewModelScope.launch { ... }`) and unify `runTest(testDispatcher)` across test scopes — Allows tests to cleanly `.join()` asynchronous mutations before asserting downstream flow state, while UI callers remain completely unaffected by ignoring the return value.
**Chosen**: Return `Job` from ViewModel mutations and pass unified `testDispatcher` to `runTest`.
**Why**: Asynchrony in ViewModels shouldn't be an untrackable black hole. Exposing the coroutine `Job` provides a deterministic handle for verification: tests don't have to guess or spin waiting for I/O to land—they join the stroke, and once joined, the result is solid ground.
**Confidence**: high — 100% green test execution, dropping test run time from 80s with timeout to 21s clean.
**Outcome**: All 10 Settings unit tests (`SettingsViewModelTest` and `SettingsRepositoryTest`) passing 100% green without race conditions.
**Pattern reference**: Link to `testOracle.md § Verification Gates` and `systemPatterns.md § MVI Architecture`.

## reactive-appearance-and-sync-policy-projection — 2026-10-04 18:00

**Context**: In Sub-Phase C of Settings Hub, designing the UI controls for Theme mode (System, Dark, Light), Dynamic Colors (Material You Monet), and synchronization policies (Cellular, Pull-to-refresh).
**Options considered**:
- Rely on modal dialogs for each individual setting option — Adds extra tap overhead and disrupts the spatial visual hierarchy of the Settings sub-screens.
- Build dedicated full-screen sub-screens (`SettingsAppearanceScreen` and `SettingsSyncScreen`) with direct inline radio groups, toggle switches, brand swatch previews, and reactive status feedback — Provides immediate spatial clarity, live feedback on tap, and matches the Reef Modernist design DNA.
**Chosen**: Dedicated full-screen sub-screens with inline controls and live status banners.
**Why**: A settings sub-screen should be a calm, confident workbench. Presenting theme options as direct selectable rows and sync triggers with integrated progress indicators gives the user immediate visual certainty without nesting modal dialogs inside modal flows.
**Confidence**: high — verified with clean Robolectric unit tests and reactive state binding.
**Outcome**: Implemented `SettingsAppearanceScreen.kt` and `SettingsSyncScreen.kt`, wired to NavHost, and verified 13/13 unit tests passing 100% green.
**Pattern reference**: Link to `brandIdentity.md § Component DNA` and `systemPatterns.md § Master-Detail & Sub-Screen Navigation`.

## circular-dial-panic-countdown-and-fail-closed-purge-cascade — 2026-10-04 18:25

**Context**: In Sub-Phase D of Settings Hub, designing the Panic Purge security configuration and emergency execution UI, requiring an intuitive duration selector (clamped 5s–60s, default 15s) and a high-gravity emergency countdown screen with full abort capability and irrevocable zeroization.
**Options considered**:
- Simple numeric text input or standard linear slider — Functional, but feels flat and sterile for an emergency security parameter where physical tactile certainty matters.
- Custom clock-face `CircularDialPicker` with trigonometric drag gestures paired with a full-screen `PanicPurgeCountdownScreen` displaying 3 pulsing concentric Canvas rings, monospace countdown, cancel button, hardware back abort, and a 4-step fail-closed wipe cascade — Provides unmistakable tactile feedback, high-stakes visual gravitas, and fail-safe abortion before zeroization.
**Chosen**: Custom `CircularDialPicker` and pulsing red Canvas ring countdown screen with 4-step fail-closed cascade.
**Why**: Setting an emergency panic wipe timer shouldn't feel like adjusting screen brightness. A circular dial invokes the deliberate winding of an emergency clock mechanism. When triggered, the screen must leave no doubt about what is happening: pulsing red concentric waves, large monospace numbers, an obvious abort button, and complete irrevocability once the clock strikes zero.
**Confidence**: high — verified with unit tests for countdown clamping, preference persistence, and fail-closed wipe execution.
**Outcome**: Implemented `CircularDialPicker.kt`, `SettingsSecurityScreen.kt`, and `PanicPurgeCountdownScreen.kt`; wired into `MainActivity.kt`; verified 15/15 unit tests passing 100% green.
**Pattern reference**: Link to `architecture.md § Threat Model & Invariants` (Panic wipe) and `crypto-and-keystore.md § Emergency Panic Purge`.

## dual-mode-backup-protection-and-format-sniffing — 2026-10-04 18:55

**Context**: In Sub-Phase E of Settings Hub, designing full-vault export and import engine (`VaultBackupEngine`), supporting seamless web client cryptographic parity and flexible protection options.
**Options considered**:
- Restrict backups to active sovereign identity key (`hu-key`) HKDF derivation only — Cryptographically simple and prevents weak passwords, but breaks interoperability if user exports from mobile to open on another machine without their active session key, and diverges from Web app options.
- Support dual protection modes (`ACTIVE_KEY` via HKDF-SHA256 vs `CUSTOM_PASSPHRASE` with PBKDF2-SHA256 600,000 iterations), alongside unencrypted JSON export, format sniffing (`detectBackupFormat`), and Bitwarden JSON ingestion — Provides complete feature and cryptographic parity with ShellGuard Web client while giving the user deliberate control.
**Chosen**: Dual protection modes with PBKDF2-SHA256 (600,000 iterations) and format sniffing.
**Why**: Parity between mobile and web is a core invariant. The web application allows users to secure backups with either their sovereign `hu-` master key or an ad-hoc custom passphrase. If mobile didn't provide both, users couldn't cross-restore between platforms without friction. Format sniffing also prevents the app from choking on Bitwarden JSON or plain backups.
**Confidence**: high — verified with 5/5 unit tests in `VaultBackupEngineTest` covering format sniffing, active key round-trip, custom passphrase PBKDF2 round-trip, and Bitwarden ingestion.
**Outcome**: Implemented `VaultBackupEngine.kt`, `SettingsBackupScreen.kt`, `SettingsAutofillScreen.kt`, and `SettingsAboutScreen.kt`; all 95 unit tests passing 100% green.
**Pattern reference**: Link to `crypto-and-keystore.md § Cryptographic Invariants & Parity` and `import-export-and-migration-spec.md`.








## polymorphic-backup-payload-alignment-with-web-parity — 2026-10-08 17:30

**Context**: During the cross-platform Web Parity review of `VaultBackupEngine.kt`, I identified that the mobile client was serializing segregated collections (`pearls`, `notes`, `sshKeys`), while the ShellGuard Web server `ImportExportView` strictly expects a unified `items: []` polymorphic array with string type discriminators and ISO timestamps.
**Options considered**:
- Retain segregated lists on mobile and update web importer to handle both — Modifies established server/web contracts and creates ecosystem fragmentation.
- Refactor mobile `VaultBackupPayload` to output the unified polymorphic `items` schema while retaining backward-compatible ingestion for older mobile backups via `.allItems()` fallback — Guarantees 100% bidirectional cross-platform portability without breaking legacy local backups.
**Chosen**: Unified polymorphic `items` schema with backward-compatible legacy fallback.
**Why**: When bridging two shores, you don't ask the mainland to change its harbor; you shape the vessel to fit the dock that's already built. True zero-knowledge data portability means an export from your phone opens instantly in your browser without error or friction.
**Confidence**: high — verified with 105/105 tests green across format sniffing and round-trip decryption.
**Outcome**: Full bidirectional compatibility with ShellGuard Web's `ImportExportView` achieved and verified via unit tests.
**Pattern reference**: `long-term/patterns.md § pattern: fail-closed-structural-envelope-validation`.

## in-place-sovereign-key-redaction-in-narrative — 2026-10-08 17:35

**Context**: Lucas noticed a dead test sovereign identity key (`hu-`) recorded in line 300 of `myStory.md` from earlier physical Pixel device testing before tagging release `v0.0.0.10`.
**Options considered**:
- Delete the entire bullet point from `myStory.md` — Erases historical context and breaks narrative continuity of the live hardware verification.
- Redact the key in-place to `hu-[REDACTED_SOVEREIGN_CLAWKEY]` and verify zero repository occurrences — Preserves the physical reality of what happened while upholding uncompromising zero-leakage security standards.
**Chosen**: In-place redaction to `hu-[REDACTED_SOVEREIGN_CLAWKEY]`.
**Why**: The story is an honest trail, not a scrubbed public relations flyer, but good opsec is part of the craftsman's discipline. Masking the secret honors both the truth of what happened and the standard we hold.
**Confidence**: high — verified via recursive grep that zero instances remain across the repository.
**Outcome**: Cleanly committed in `b36b4ce` prior to merge and release tagging.
**Pattern reference**: `long-term/patterns.md § pattern: cwe-359-sensitive-clipboard-masking`.

## autofill-co-presence-gate-and-container-hijack-defense — 2026-10-08 18:35

**Context**: On physical device testing, focusing username fields failed to show inline keyboard chips and tapping a password chip only filled the password field; expanding username heuristics risked spamming non-login screens or overwriting user-typed input.
**Options considered**:
- Naively broaden substring heuristics and bind `"Add Item"` whenever `usernameId != null` — Solves missing username detection on login pages, but creates a massive blast radius where every search bar, comment box, or email input on non-login screens triggers `"Add Item"` or login chips.
- Enforce a Blast-Radius Co-Presence Gate, strict editable-input gating, password/username mutual exclusion, and 5-tier confidence ranking — Rejects non-input containers (`<form>`, `<div>`), prevents `login_password` from hijacking `usernameId`, allows Rank 4/5 heuristics and `"Add Item"` fallback ONLY when a password field is confirmed on screen (`passwordId != null`), and guards against blank `pearl.username` overwrites.
**Chosen**: Blast-Radius Co-Presence Gate with 5-tier confidence ranking, editable-input gating, and non-blank value guards.
**Why**: Asking "what breaks first when we're wrong?" immediately exposed that a false-positive username match on a screen with no password field would spam the user's keyboard across every app on their phone, and a blank username in a vault item would erase text the user had already typed. Containing the blast radius before expanding the net let us catch every real login field without polluting non-login screens.
**Confidence**: high — verified across 12 Robolectric unit tests in `AutofillStructureParserTest` and full 114/114 suite pass.
**Outcome**: Both username and password fields bind simultaneously and cleanly without false-positive keyboard spam or container hijacking.
**Pattern reference**: `long-term/patterns.md § pattern: context-aware-autofill-and-blast-radius-gating` and `testOracle.md § Redline 10`.

## option-b-masked-username-and-category-disambiguation — 2026-10-08 18:35

**Context**: Unlocked inline autofill chips must never expose raw plaintext usernames to shoulder-surfing, yet users with multiple accounts for the same service (e.g., two Google accounts) must be able to tell them apart immediately even if they haven't assigned custom categories or tags.
**Options considered**:
- Option A: Display `category` or `tag` only, falling back to `"Password"` — Protects raw usernames, but produces identical indistinguishable chips (`Google · Password`) whenever a user hasn't tagged duplicate accounts.
- Option B: Combine non-default `category` or primary `tag` with a partially masked username hint (`Work · lu***@company.com`, `lu***@gmail.com`, `ad***n`) and zero-copy resource `Icon.createWithResource` app icons — Eliminates raw username exposure on the keyboard while guaranteeing instant visual disambiguation and avoiding Binder `TransactionTooLargeException` from bitmap serialization.
**Chosen**: Option B (Category/Tag badge + partially masked username hint + zero-copy resource Icon).
**Why**: A security feature that forces the user to play a 50/50 guessing game on their own login chips is a broken joint. Masking the middle of the username (`lu***@gmail.com`) keeps shoulder-surfers blind while letting the owner recognize their account in a heartbeat.
**Confidence**: high — verified via unit tests in `AutofillStructureParserTest`.
**Outcome**: Implemented in `AutofillInlineHelper.kt` and wired into `ShellGuardAutofillService.kt`.
**Pattern reference**: `long-term/patterns.md § pattern: context-aware-autofill-and-blast-radius-gating` and `pattern: cwe-359-sensitive-clipboard-masking`.

## dynamic-buildconfig-version-binding-and-agpl3-license-parity — 2026-10-08 23:00

**Context**: On physical Pixel testing of Build 10, `SettingsHubScreen.kt` still displayed `"v0.0.0.9 (Build 9)"` and `SettingsAboutScreen.kt` displayed `"Licensed under MIT License"` instead of `"GNU AGPL v3.0"`.
**Options considered**:
- Update the hardcoded string literal in `SettingsHubScreen.kt` to `"v0.0.0.10 (Build 10)"` — Fixes the immediate display mismatch, but guarantees the exact same version drift bug on every future release bump.
- Bind `SettingsHubScreen.kt` directly to `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE`, and align `SettingsAboutScreen.kt` and `README.md` to `GNU AGPL v3.0` — Eliminates manual version synchronization in UI strings permanently.
**Chosen**: Dynamic `BuildConfig.VERSION_NAME` / `BuildConfig.VERSION_CODE` interpolation and `GNU AGPL v3.0` alignment.
**Why**: A version string written by hand in two places is a lie waiting to happen. Binding the UI directly to the compiler-generated `BuildConfig` makes version drift structurally impossible.
**Confidence**: high — verified live on physical Google Pixel (`sailfish`).
**Outcome**: Settings Hub footer and About screen dynamically reflect `v0.0.0.10 (Build 10)` and `GNU AGPL v3.0` on device.
**Pattern reference**: Link to `project/productVersion.md § Central Version Source`.

## rank-1-to-3-two-step-login-allowance-vs-strict-password-gate — 2026-10-08 23:00

**Context**: Live testing on Google Sign-In (`accounts.google.com/v3/signin`) on the physical Pixel revealed that two-step split login flows render only an email/username input on Step 1 (`passwordId == null`), causing our strict `if (passFieldId != null)` guard in `ShellGuardAutofillService.kt` Case A to suppress the `"Add Item"` chip when 0 domain matches existed.
**Options considered**:
- Keep Case A (`"Add Item"`) strictly gated on `passFieldId != null` — Prevents any possibility of `"Add Item"` appearing on non-password screens, but completely breaks `"Add Item"` on all two-step login flows (Google, Microsoft, Okta, Apple ID).
- Allow Case A (`"Add Item"`) whenever `userFieldId != null || passFieldId != null`, relying on `AutofillStructureParser.parseNodes` to strip weak Rank 4/5 heuristics when `passwordId == null` and excluding `AutoCompleteTextView` browser URL bars — Supports two-step email-first login pages (which declare explicit Rank 1–3 email/username signals) while still blocking generic text boxes, search bars, and URL omniboxes.
**Chosen**: Allow Case A on `userFieldId != null || passFieldId != null` backed by parser-level Rank 4/5 Co-Presence stripping and `AutoCompleteTextView` exclusion.
**Why**: When I tested Google Sign-In on the Pixel and saw an empty keyboard strip, I realized my blast-radius gate had cut too deep. Because the parser already strips weak substring guesses when no password field is present, any surviving `usernameId` on a passwordless screen is a high-confidence Rank 1–3 email or username input—trusting that boundary gave us two-step login support without reopening the spam floodgates.
**Confidence**: high — verified live on physical Google Pixel (`accounts.google.com` and `app.simplelogin.io`) and across all 114 unit tests.
**Outcome**: Focusing `"Email or phone"` on `accounts.google.com` immediately renders `[ 🛡️ Add Item · accounts.google.com ]` above Gboard while search bars and URL omniboxes remain silent.
**Pattern reference**: `long-term/patterns.md § pattern: context-aware-autofill-and-blast-radius-gating` and `testOracle.md § Redline 10`.

## retaining-0-0-0-x-progression-until-mvp-completion — 2026-10-09 00:01

**Context**: After merging Phase 7 (Stage 8) into `main`, Lucas considered bumping the release version from `0.0.0.10` to `0.0.1.0` instead of `0.0.0.11`.
**Options considered**:
- Bump to `v0.0.1.0 (Build 11)` immediately — Signals a milestone jump, but consumes the `0.0.1.0` MVP feature-complete designation defined in `productVersion.md` five stages early (before Stages 9–13 are built).
- Keep the `0.0.0.x` active development progression (`v0.0.0.11`, Build 11) through Stage 12 and reserve `v0.0.1.0` for Stage 13 (MVP Feature Complete) — Preserves the semantic meaning of the four-segment version calculus across the remaining roadmap stages.
**Chosen**: Keep the `0.0.0.x` progression (`v0.0.0.11`, Build 11) and prune superseded `RELEASE-v0.0.0.9.md` / `RELEASE-v0.0.0.10.md` root files.
**Why**: A version number is a compass for where the codebase sits in its lifecycle. Saving `0.0.1.0` for the true MVP completion keeps our version history legible and grounded in the actual roadmap.
**Confidence**: high — aligns 1:1 with `productVersion.md` and `semantic-versioning.md`.
**Outcome**: Drafted and shipped `v0.0.0.11 (Build 11)` with a clean repository root containing only `RELEASE-v0.0.0.11.md` and `RELEASE-PLAY.md`.
**Pattern reference**: Link to `project/productVersion.md § Semantic Progression Strategy`.

## seeding-datastore-combine-with-onstart-for-ci-determinism — 2026-10-09 06:35

**Context**: Overnight GitHub Actions CI run `37897075874` (`v0.0.0.11`) failed on `SettingsViewModelTest.testTriggerManualSyncWithNoActiveSession` because `combine(settingsRepo.settingsFlow, _extraState)` held back synchronous `_extraState` error updates while waiting for AndroidX `DataStore` to finish its initial disk read on `Dispatchers.IO`.
**Options considered**:
- Patch `SettingsViewModelTest.kt` to await `settingsFlow.first()` or insert a retry loop before asserting `uiState.value.errorMessage` — Makes the test pass, but leaves `SettingsViewModel.uiState` deaf to `_extraState` updates during cold-start disk reads in production.
- Seed `settingsRepo.settingsFlow.onStart { emit(AppSettings()) }` inside `SettingsViewModel.uiState`'s `combine(...)` pipeline and replace the nested `runTest(testDispatcher)` in `SettingsViewModelTest.setUp()` with `runBlocking` — Guarantees `combine` has an immediate synchronous emission on subscription in both production and tests, eliminating the `Dispatchers.IO` startup race entirely.
**Chosen**: Seed `.onStart { emit(AppSettings()) }` on `settingsRepo.settingsFlow` in `SettingsViewModel.kt` and use `runBlocking` in `SettingsViewModelTest.setUp()`.
**Why**: Fixing a race condition only inside the test file feels like silencing the smoke alarm instead of putting out the fire. Giving the production `combine` flow an immediate default emission ensures that in-memory state updates are never hostage to disk latency.
**Confidence**: high — verified across `114/114` local unit tests (`--rerun-tasks`) and confirmed 100% green on GitHub Actions cloud run `37938055022`.
**Outcome**: `v0.0.0.11` passed the pre-flight test gate on GitHub Actions and published signed `shellguard-mobile-v0.0.0.11.aab` and `.apk` release assets.
**Pattern reference**: `long-term/patterns.md § pattern: deterministic-datastore-viewmodel-synchronization` and `testOracle.md § Redline 11`.

## dual-teaching-modalities-and-jules-stewardship — 2026-10-10 10:45

**Context**: When delegating work to Google Jules, deciding how knowledge and guidance should be communicated between Antigravity (Senior Tech Lead) and Jules (Executor).
**Options considered**:
- Treat all interactions as purely mechanical code injections ("straight lines") — Quickest in the short term, but Jules treats code as external patches without internalizing the architectural rationale, repeating identical mistakes on subsequent turns.
- Codify two distinct pedagogical modalities: Direct Directives ("Straight Lines") for fast-strike/emergency unblocking, and Iterative Stewardship ("Tutoring") for long-lived feature branches — Matches the communication style to the task lifecycle, allowing pair programming to build compounding mental models and durable memory for deep domain features.
**Chosen**: Codify the dual teaching modalities in `knowledge-integration.md` and `operating-modes.md`.
**Why**: A junior developer thrives under mentorship, not just orders. When working across days on a feature branch, explaining *why* an error occurred and commanding Jules to encode the lesson into its memory bank creates compounding leverage that pays dividends on every subsequent commit.
**Confidence**: high — ratified through active collaboration with Lucas on PR #1.
**Outcome**: Enriched `/config/.gemini/config/skills/jules-cli/` with Section 5 of `knowledge-integration.md` and updated `SKILL.md` reference index.
**Pattern reference**: Link to `references/knowledge-integration.md § 5. The Two Teaching Modalities`.

## Calibration Note — 2026-10-10 13:20
Calibration is sharp with 0 over-confidence penalties across 8 ratified decisions since Phase 7. The Lazy Senior Developer stance ("predict the concrete operating failure before cutting") has successfully anchored confidence in empirical verification.
