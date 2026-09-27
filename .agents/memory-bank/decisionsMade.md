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
**Why**: A scar requires time and pressure to form. Promoting a fix after a single victory turns the long-term bank into another changelog rather than crystallized truth.
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



