# Decision Log

## 2026-10-04 — circular dial countdown and fail-closed panic purge cascade
Engineered interactive `CircularDialPicker` with trigonometric angle mapping (5s–60s clamp) and full-screen `PanicPurgeCountdownScreen` with 3 pulsing concentric red Canvas rings. Implemented a 4-step fail-closed destruction cascade in `executePanicPurge` wiping Room tables, clearing EncryptedSharedPreferences session, wiping DataStore preferences, and unlocking vault state, verified green via Robolectric unit tests.

## 2026-10-04 — dual-mode backup parity and test database isolation
Engineered `VaultBackupEngine` supporting web-parity dual protection modes (`ACTIVE_KEY` via HKDF vs `CUSTOM_PASSPHRASE` with PBKDF2-SHA256 600,000 iterations), automatic format sniffing, and Bitwarden JSON ingestion. Resolved Robolectric `IllegalStateException` on main thread Room queries by initializing isolated in-memory test databases (`getInMemoryDatabase`) with explicit `.allowMainThreadQueries()`, passing 95/95 unit tests 100% green.

## 2026-10-04 — polymorphic payload schema and sovereign key alignment
While auditing the backup engine for Web Parity, I identified that the initial implementation exported isolated lists (`pearls`, `notes`, `sshKeys`), whereas the ShellGuard Web counterpart requires a unified polymorphic `items` JSON array. I refactored `VaultBackupPayload` to serialize and deserialize polymorphic `BackupVaultItem` objects using a `type` discriminator, and enforced Base62 `hu-` Sovereign ClawKey validation.

## 2026-10-08 — autofill container hijacking defense and blast-radius co-presence gate
Diagnosed missing username inline chips and single-field password autofill on device: pre-order traversal in `AutofillStructureParser` evaluated Rank 4 substring heuristics on non-input parent containers (`<form id="login-form">`) and password nodes (`id="login_password"`) before child `<input>` elements, stealing `usernameId`. Enforced editable-input gating (including `AutoCompleteTextView` browser URL bar exclusion), mutual exclusion between password and username nodes, a Co-Presence Gate suppressing weak Rank 4/5 heuristics when `passwordId == null` while permitting Case A `"Add Item"` chips on explicit Rank 1–3 two-step email-first login pages (`accounts.google.com`), non-blank username overwrite guards, and Option B masked username + category/tag disambiguation (`testOracle.md` § Redline 10).

## 2026-10-09 — datastore combine initial emission & ci test determinism
Diagnosed `AssertionError` in `SettingsViewModelTest.testTriggerManualSyncWithNoActiveSession` during GitHub Actions CI run `37897075874` (`v0.0.0.11`). Traced to `combine(settingsRepo.settingsFlow, _extraState)` in `SettingsViewModel` blocking `_extraState` emissions until AndroidX `DataStore` finished its asynchronous initial disk read on `Dispatchers.IO`. Seeded `settingsRepo.settingsFlow.onStart { emit(AppSettings()) }` and replaced nested `runTest` in `@Before setUp()` with `runBlocking`, verifying `114/114` unit tests 100% green.

## 2026-10-10 — dual feedback medium & jules pr comment orchestration
Codified dual-medium iteration protocol into global jules-cli skill: Jules natively ingests feedback through either private Web UI chat prompts or public GitHub PR review threads (reacting with 👀 and pushing commits). Standardized an agent triage question ("Do you want a prompt to give to Jules in the Web UI, or should we make a comment on the pull request?"), and established the critical invariant to NEVER tag @jules on GitHub (as that handle pings an unrelated human account, not the bot).

## 2026-10-10 — post-creation attachment persistence & room entity fidelity
Caught data-drop bug during live physical Pixel testing: adding attachments or secondary URIs when editing an existing vault item failed to persist across reloads. Traced to `SyncRepository.kt` (`savePearlDetail` and `saveNoteDetail`), which serialized `attachmentsJson` and `urisJson` for the network payload but hardcoded `"[]"` for `attachments` and `uris` in Room entities (`VaultPearlEntity` and `SecureNoteEntity`). Drafted architectural fix and dispatched hotfix review guidance comment #6104587759 to Jules on PR #1.

## 2026-10-10 — orthogonal task partitioning & jules pr kickoff
Audited codebase seams across UI, Autofill, and Settings/Storage debt for Stage 1 completion. To prevent merge conflicts in Jules sessions, partitioned remaining scope into two strictly orthogonal tasks (Task 1 for UI & Autofill, Task 2 for DI, Storage, Settings debt, and FileProvider) with zero file overlap, updated JULES.md with Compose line limits and bot tagging rules, and triggered Task 1 execution via GitHub PR comment on PR #1.

## 2026-10-11 — systemic transport security vs. security theatre
Addressed user challenge on cleartext HTTP over Tailscale. Uncovered that static `SHA-256(hu-)` transmission and unencrypted metadata over HTTP permit session hijacking and vault destruction, while `relaxedTrustManager` nullifies TLS integrity. Ratified the Systemic Zero-Knowledge Transport Invariant in `android-development.md` § 5, mandated UI amber transport warning pills, scheduled TOFU certificate pinning, and set roadmap trajectory toward zero-knowledge PAKE/SRP-6a authentication.

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
