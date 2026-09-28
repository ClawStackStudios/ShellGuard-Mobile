# Dream Learnings
Compressed receipts from memory consolidation passes.
Each entry is a dated block of distilled invariants, patterns, and flags.
This file is the "what the dream produced" artifact.

## 2026-09-26 11:06 — Consolidation Receipt

### Invariants
- **Zero-Knowledge Split-Brain Defense (Bitwarden Model)** (source: `systemPatterns.md`, `long-term/patterns.md`)
  When disconnected from the self-hosted server, local mutations (create/edit/delete) must be blocked at the UI gate because zero-knowledge encrypted blobs cannot be mathematically merged without plaintext exposure.
- **Sovereign Key Identity Alphabet** (source: `crypto/ClawCrypto.kt`, `GatewayViewModel.kt`, `consolidated_learnings.md`)
  Master identity keys (`hu-`) and agent keys (`lb-`) use strictly 64 Base62 characters (`[0-9a-zA-Z]`, 67 total length); enforcing hexadecimal validation falsely rejects authentic web-generated credentials.
- **Headless Test SDK Ceiling Decoupling** (source: `RoomDatabaseTest.kt`, `app/src/test/resources/robolectric.properties`, `android-development.md` §9)
  Robolectric test runners running on JVM host environments cannot shadow pre-release Android platforms (`targetSdk = 36`); test execution must be explicitly capped via `sdk=34` while compilation targets API 36.
- **Parent Scaffold Inset Zeroing** (source: `MainActivity.kt`, `consolidated_learnings.md`)
  When child screen composables declare `.imePadding()`, the parent Activity `Scaffold` must configure `contentWindowInsets = WindowInsets(0, 0, 0, 0)` to eliminate double-subtraction of the virtual keyboard height.

### High-Salience Patterns
- **Cleartext LAN & Tailscale CGNAT Mesh Transport** (source: `network_security_config.xml`, `techContext.md`, `KtorClientProvider.kt`)
  Local home lab vaults without public domain certificates require `<base-config cleartextTrafficPermitted="true">` because Android's `<domain>` manifest tag does not support CIDR subnet masks (`192.168.0.0/16`, `100.64.0.0/10`).
  seed: private-mesh-transport
- **CWE-359 Sensitive Clipboard Masking & Self-Scrubbing** (source: `consolidated_learnings.md`, `systemPatterns.md`)
  Setting `ClipDescription.EXTRA_IS_SENSITIVE = true` on copied secrets suppresses Android 13+ thumbnail overlays, while a bounded background coroutine timer purges transient secrets from the system pasteboard.
  seed: clipboard-isolation
- **Python 3 In-Memory Base64 Keystore Decoding** (source: `.github/workflows/release.yml`, `android-headless-signing-ci`)
  Passing binary keystores through Python's `base64.b64decode(os.environ['KEY'].strip())` completely eliminates GNU Linux base64 newline truncation and padding failures on CI runners.
  seed: ci-keystore-pipeline

### Contradictions Flagged
- **`progress.md` header vs actual completion**: `progress.md` line 3 states *"Current Status: Phase 1 Cryptographic Engine & Room Database Verified (Transitioning to Phase 2)"*, but Phase 2 (Tasks 03 & 04) is fully implemented, verified, and released as `v0.0.0.3` on GitHub. — resolution: Update `progress.md` header to Phase 2 Complete, Transitioning to Phase 3.
- **`changelog.md` temporal lag**: `brain/project/changelog.md` only documents through Phase 1 under `[0.0.0.3]`, whereas root `CHANGELOG.md` and `RELEASE-v0.0.0.3.md` document the complete Phase 2 release. — resolution: Update `brain/project/changelog.md` with Phase 2 release highlights.

### Superseded (archive)
- **Hexadecimal ClawKey Validator** (`hu-[0-9a-f]{64}`) — superseded by: Base62 ClawKey Validator (`hu-[0-9a-zA-Z]{64}`) on 2026-09-26.
- **Unconditional `FLAG_SECURE` in Debug Builds** — superseded by: Release-only scoped `FLAG_SECURE` (`if (!BuildConfig.DEBUG)`) to prevent Adreno 530 compositor blackout over system IME on 2026-09-26.

---

## 2026-09-27 23:05 — Consolidation Receipt

### Invariants
- **Fail-Closed Cryptographic Retrieval** (source: `testOracle.md`, `raw_reflection_log.md`, `decision-log.md`, `decisionsMade.md`)
  When cryptographic decryption fails, detail getters must fail closed with `Result.failure`, zeroize volatile buffers, and refuse to emit raw ciphertext. Emitting fallback ciphertext allows subsequent edits to re-encrypt ciphertext envelopes, causing irreversible double-ciphertext data corruption.
- **Synchronized Mutex Serialization** (source: `decision-log.md`, `decisionsMade.md`, `raw_reflection_log.md`, `changelog.md`)
  Background and foreground synchronization triggers must be queued and serialized via coroutine `withLock`. Non-blocking `tryLock()` causes silent dropped sync rounds under concurrency, creating an illusion of success while pending mutations linger uncommitted.
- **Tombstone Retention against Zombie Resurrection** (source: `decision-log.md`, `decisionsMade.md`, `raw_reflection_log.md`, `progress.md`)
  Local deletion tombstones (`PENDING_DELETE`) must be preserved until an authoritative remote HTTP 200/204 response confirms server deletion. Purging tombstones prematurely causes subsequent delta syncs to interpret the remote item as newly created and resurrect it locally.
- **Home Lab IP Port Isolation** (source: `decision-log.md`, `decisionsMade.md`, `deep_plan.md`, `progress.md`, `systemPatterns.md`)
  Whenever a target host is an IP address or localhost, domain matching must automatically promote from `BASE_DOMAIN` to `EXACT` (host + port) matching, preventing credential cross-contamination across multi-tenant containers sharing a single home lab IP.
- **Robolectric Target SDK Ceiling Decoupling** (source: `decision-log.md`, `decisionsMade.md`, `consolidated_learnings.md`, `runtimeEnv.md`, `testOracle.md`)
  Headless JVM test environments cannot shadow pre-release Android platforms (`targetSdk = 36`). Test runner execution must be explicitly capped to `sdk = 34` in `robolectric.properties` and `@Config(sdk = [34])` to prevent `UnsupportedOperationException`.
- **Base62 Sovereign Key Identity Alphabet** (source: `decision-log.md`, `consolidated_learnings.md`, `dreamLearnings.md`, `testOracle.md`)
  Master identity keys (`hu-`) and agent keys (`lb-`) are 67 characters consisting of a 3-character prefix and 64 Base62 characters (`[0-9a-zA-Z]`). Enforcing hexadecimal regex causes false-negative lockout of authentic web-molted identity files.

### High-Salience Patterns
- **Fail-Closed Cryptography over Fallback Degradation** (source: `meta-rules.md`, `self-review-checklist.md`, `testOracle.md`, `decision-log.md`)
  Cryptographic operations, key derivation, and session hydration must prioritize absolute failure over UI convenience. Presentation layer graceful degradation must be strictly subordinated to fail-closed cryptographic integrity.
  seed: fail-closed-crypto
- **Two-Phase Synchronization with Mutex Serialization** (source: `SyncRepository.kt`, `SyncReconciliationAdversarialTest.kt`, `raw_reflection_log.md`)
  Vault delta synchronization requires deterministic queueing via coroutine `withLock`, tombstone retention until remote confirmation, downstream conflict filtering against local pending edits, and chunked batch pruning in sets of 500 to evade SQLite parameter limits.
  seed: two-phase-sync
- **Home Lab Port Isolation & Automatic Mode Promotion** (source: `DomainMatcher.kt`, `DomainMatcherTest.kt`, `deep_plan.md`)
  Autofill matching automatically promotes `BASE_DOMAIN` to `EXACT` host and port matching whenever the target is an IP address or localhost, isolating Docker and Unraid services sharing a single server IP.
  seed: homelab-port-isolation

### Contradictions Flagged
- **`techContext.md` says Dagger Hilt (2.51+), while `decision-log.md` and codebase use `AppContainer` lazy DI** — proposed resolution: Update `techContext.md` line 8 from Dagger Hilt to `DefaultAppContainer` frameworkless lazy DI to match architectural reality.
- **`productContext.md` line 7 says "Offline editing capability that syncs automatically", while `long-term/patterns.md`, `systemPatterns.md`, and `progress.md` enforce Bitwarden Read-Only Offline Caching** — proposed resolution: Reconcile `productContext.md` line 7 to state *"100% offline vault accessibility (Bitwarden Read-Only model) with automatic delta synchronization upon reconnection"*.
- **`project/changelog.md` line 104 labels Phase 4 as `## [Unreleased]`, while lines 145 and 153 record `v0.0.0.5 (Build 5)` and `v0.0.0.6 (Build 6)` as published releases** — proposed resolution: Update `project/changelog.md` line 104 header to `## [0.0.0.5] - 2026-09-27 (Build 5) — Phase 4: Algorithmic TOTP Engine, CameraX Scanner, Password Generator & Biometrics`.
