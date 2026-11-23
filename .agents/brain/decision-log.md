# Decision Log

## 2026-10-11 — docs bow to code and ephemeral cache spec alignment
Executed `/walk-the-docs` and `/draft-release` for Stage 1 (`v0.0.0.12 (Build 12)`). Reconciled all 5 domain specs in `project/` to physical code reality (hybrid attachment storage, `clearDecryptedCache` lifecycle, `password_history` empty string defaults, `androidapp://` routing, and amber cleartext warnings), cementing the principle that documentation must reflect physical code rather than aspirations.

## 2026-10-11 — mobile-first transport inversion & instance identity anchor
Inverted the traditional server-first/mobile-as-bolt-on paradigm into a sync-native mobile-first transport architecture. Solved certificate rotation friction by decoupling ephemeral TLS certs from a persistent Instance Identity Key that signs rotation proofs for silent auto-healing, designed zero-touch Reef Handshake QR pairing (`shellguard://pair`), and integrated dual-homed mobility and Stage 5 prompt into `project/meta-prompt-ai-studio.md` and `ROADMAP.md` as the `v0.0.1.0 (Build 16)` structural completion milestone.

## 2026-10-12 — session credential decoupling & in-place vault re-auth
Diagnosed unresponsive unlock button on physical Google Pixel: `hasActiveSession()` required volatile `getInMemoryShellKey() != null`, but `zeroizeMemory()` purged persistent preferences on lock, collapsing session state and trapping the UI in an un-dismissible overlay. Decoupled persistent KeyStore-wrapped credentials from RAM zeroization, implemented PBKDF2-SHA256 `PinAuthManager` with attempt lockout counters, dynamic `LockScreen` presentation with `PinPadView`, and an in-place Master Key modal, verifying seamless re-auth on physical hardware.

## 2026-10-12 — cloud ci offload and zero local build strain
Created `.github/workflows/ci.yml` and `scripts/audit_docs_drift.py` for ShellGuard-Mobile, mirroring the ShellGuard-TOTP cloud CI pattern. Updated `git-hygiene.md` and `walk-the-docs.md` to eliminate local Gradle build strain: developers and agents run an instant 1-second Python drift audit locally, push to feature branches, and tail cloud test and APK compilation runs via `gh run watch`.

## 2026-10-12 — release manifest cleanup & dual-branch pr merge sync
Purged legacy Build 11 release manifest to ensure only current release documentation resides at root. Harmonized all relative markdown links against code ground truth and synchronized Jules's PR #1 merge with Stage 1.5 in-place re-auth features for v0.0.0.12.

## 2026-10-12 — pre-draft alignment artifact gate & single manifest invariant
Codified the Pre-Draft Alignment Artifact Gate into /draft-release and /walk-the-docs, mandating an interactive analysis artifact covering what needs alignment, how to align it, and a sequential roll-off checklist before mutating code or docs. Ratified the Single Active Root Manifest invariant and Cloud CI offload across hygiene rules.

## 2026-10-12 — pre-work assumptions & jules cli dispatch
Codified `.agents/workflows/pre-work-assumptions.md` to run anticipatory reflection while waiting for upstream Jules builds, surfacing the CWE-384 in-memory backgroundTimestamp lock bypass ahead of Stage 3. Dispatched Jules CLI session 1446424829708118836 on `feat/stage-2-mental-flow-and-ergonomics` with partitioned task cards and physical Pixel test readiness.

## 2026-10-12 — pr #2 line-level code review & coroutine job-returning fix
Conducted forensic review of Jules PR #2 on Cloud CI. Caught two structural seams: `GatewayViewModel` pre-populating `clawKey` with 64-char SHA-256 hashes instead of sovereign `hu-` keys (breaking `isValidClawKey` regex and double-hashing on login), and `VaultDashboardViewModel.triggerSync()` returning `Unit` instead of `Job` while relying on `allItems.value` under `WhileSubscribed` (racing on `Dispatchers.IO` network timeouts). Submitted line-level GitHub PR review comments with Room fallback and `.join()` testing patterns.

## 2026-10-14 — disciplined pr review protocol & zero-tolerance security gate
Executed formal `/git-review-pr` protocol on PR #2 (`.reviews/PR-2-review.md`). Verified commit `2c7aae1` healed the test synchronization race with 126 green tests on Cloud CI and scrubbed all 6 patch residue files. Closed duplicate PR #3. Enforced zero-tolerance security gate by isolating OWASP M2 post-lock memory retention in `VaultDashboardViewModel.lockVault()` (wiping `_searchQuery` and `uiState`), OWASP M1 IME keyboard hardening (`autoCorrect = false`), and `GatewayViewModel.init` 64-char hash pre-population guard, dispatching line-level fix guidance to Jules.

## 2026-10-15 — pixel hardware verification & proactive proactivity seeding
Installed Cloud CI debug APK (`2b2f34c`) on physical Google Pixel (`sailfish`) over TLS ADB, verifying sub-0.5ms search, OWASP M1 IME hardening, soft keyboard dismiss on Enter, (X) clear button, empty search state reset, and OWASP M2 post-lock memory zeroization on glass. Designed comprehensive finish package capitalizing on Jules's hot session: deep /walk-the-docs alignment, meta-prompt sub-phase spine, Android deprecation cleanup (`Icons.AutoMirrored.Filled.OpenInNew`), class-level Json instance hoisting, and proactive `// TODO:` code anchoring across UI components to seed Jules's background Suggested Tasks scanner for Task 04.

## 2026-10-16 — finish package review & git-review-pr hard gate
Executed final `/git-review-pr` pass on Jules PR #2 (commit `d62100b`). Verified all 7 completion criteria: 127/127 Cloud CI tests passing, zero-drift doc audit, deprecation migrations, and proactive `// TODO:` task anchors. Authored timestamped review artifact `pr_review_2_2026_10_16_1520.md` and established MERGE recommendation at the Phase 5 hard user gate.

## 2026-10-17 — direct file writes, push clearance & hot-session chaining codification
Re-mapped freshly updated `jules-cli` skill ecosystem and codified 4 durable invariants across rules and skills: (1) Direct File Writes Invariant (strictly banning `sed`/regex script mutations on nested source files), (2) Subagent Feature-Branch Push Pre-Clearance (authorizing non-destructive `git push origin HEAD` without stalling), (3) Hot-Session Multi-Phase Chaining under the Lucas Pattern, and (4) Compose IME & Scroll Redundancy Invariant.

## 2026-10-17 — jules knowledge store migration & tier-2 architecture
Deprecated legacy `.jules/JULES.md` and transitioned repository knowledge into `.jules/KNOWLEDGE.md` to complement root `AGENTS.md`. Established atomic declarative units for zero-knowledge decryption, CursorWindow limits, storage lifecycle parity, direct file writes, Compose keyboard scroll clearance, and subagent push pre-clearance.

## 2026-10-17 — release v0.0.0.13 alignment & single active manifest rollout
Executed formal `/draft-release` alignment for `v0.0.0.13 (Build 13)`. Purged legacy `RELEASE-v0.0.0.12.md` from root per the Single Active Manifest Invariant, drafted `RELEASE-v0.0.0.13.md`, compressed Play Store release notes to 459 characters, aligned version badges and changelog, and verified 100% green Python doc-drift audit across all anchors.

## 2026-10-17 — proactive todo mapping & suggested tasks alignment
Audited all inline code comments across repository, verifying zero stale comments from prior tasks. Seeded 11 domain-scoped proactive `// TODO:` anchors across `ItemDetailScreen.kt`, `VaultDashboardScreen.kt`, `ItemFormScreen.kt`, `VaultLockManager.kt`, `SyncRepository.kt`, `CircularDialPicker.kt`, `AutofillStructureParser.kt`, and `KtorClientProvider.kt` matching Task 04 UI/UX polish, Stage 3 security lifecycle, and future transport hardening. Jules's Suggested Tasks scanner can autonomously ingest these anchors to populate candidate tasks.

## 2026-10-18 — frictionless self-signed transport sovereignty & pr 5 refinement
Evaluated Jules PR #4 and PR #5 for `KtorClientProvider.kt`. While PR #4 (CWE-532 log redaction) passed cleanly to MERGE, PR #5 unconditionally removed all self-signed SSL handling, which would break homelab self-hosters and force painful Android OS CA installation. Refined PR #5 verdict to REFINE-AND-RE-PR: mandated standard platform TLS by default, while gating self-signed trust behind an explicit user consent opt-in (`allowSelfSigned: Boolean = false`) to maintain symbiotic compatibility with Era 1 servers until Era 2 Stage 5 TOFU certificate pinning lands. Codified the invariant into root `AGENTS.md` and `.jules/KNOWLEDGE.md` (`e262231`).

## 2026-10-19 — on-glass pixel verification & compose layout bounding
Merged PR #4 & PR #5 into `feat/security-remediations-integration` and deployed to physical Google Pixel (`lineage_sailfish`) over TLS ADB. Caught a visual defect where `GatewayScreen.kt` placed the self-signed consent row inside `Box` rather than `Column`, causing direct stacking on top of the segmented URL bar. Reordered the row into the vertical `Column`, enforced an accessible 48dp clickable container (`heightIn(min = 48.dp)`), and verified clean layout rendering, dynamic amber warning feedback, and protocol unmounting on physical glass.

## 2026-10-19 — option 1 decade spacing & v0.0.0.14 hotfix alignment
Realigned roadmap to Option 1 decade spacing (`v0.0.0.14` Build 14 Hotfix ➔ `v0.0.0.20` Build 20 Stage 2 ➔ `v0.0.0.30` Build 30 Stage 3 ➔ `v0.0.0.40` Build 40 Stage 4 ➔ `v0.0.1.0` Build 50 Structural Completion). Re-versioned `app/build.gradle.kts`, `ROADMAP.md`, `project/meta-prompt-ai-studio.md`, `productVersion.md`, and cut `RELEASE-v0.0.0.14.md` with zero doc drift (<0.08s Python audit).

## 2026-10-24 — encrypted device vault test ergonomics & constructor sealing
Reviewed Jules PR #6 eliminating the cleartext `SharedPreferences` fallback in `EncryptedDeviceVault`. While the fail-closed `SecurityException` was 100% verified, constructor parameter pollution (`customPrefs: SharedPreferences? = null`) and `settings.gradle.kts` plugin deletion introduced architectural friction. Isolated the smooth path: sealing the constructor to private, exposing `@VisibleForTesting fun createForTesting(context, testPrefs)` to protect production callers from unencrypted storage, and holding merge for unified integration.

## 2026-10-27 — cross-repository sync reconnaissance & stage 2 sub-phase integration
Mapped the main ShellGuard repository against mobile sync pathways using elite Ponytail scout. Uncovered two breaking upstream sync seams: missing default `owner_uuid` in mobile DTOs throwing `MissingFieldException` on trimmed server mutation receipts, and missing `parent_id`/`parent_type` breaking attachment uploads under the server's Phase 23 Bitwarden Item Integrity model. Wrapped findings into Stage 2 Sub-Phase (Task 04.A) in `ROADMAP.md` and `meta-prompt-ai-studio.md` alongside Task 04.B, scheduling Room `MIGRATION_1_2` and wire parity fixes ahead of release `v0.0.0.20`.

## 2026-11-07 — github native control plane offload
Offloaded repository roadmap tracking, release anchors, and surgical task orders from flat markdown into native GitHub primitives. Provisioned 5 GitHub Milestones (Build 12 closed, Builds 20, 30, 40, 50 open), created stage and domain labels, and ingested Issues #9–#21 into GitHub Project #1 ('ShellGuard Core & Distributed Roadmap') without deleting original meta-prompt lines.

## 2026-11-22 — identity projection over vault_pearls & custom_field wire parity
Traced Identity item storage and backup import failures across `ShellGuard` Web/Server and `ShellGuard-Mobile` (see `systemPatterns.md`). Discovered that Identities live in `vault_pearls(type = 'identity')` with 18 attributes inside `custom_fields`, and that mobile imports failed because `CustomField` used `label`/`TEXT` instead of Web's `name`/`text` while `VaultBackupEngine.detectBackupFormat()` required legacy `"pearls"` and omitted `coerceInputValues = true`. Implemented `CustomFieldWireSerializer`, `IdentityFieldMapper`, lenient backup decoding, and modular `IdentityFormSection`/`IdentityDetailSection` UI under Issue #35 (`58371fc`).

