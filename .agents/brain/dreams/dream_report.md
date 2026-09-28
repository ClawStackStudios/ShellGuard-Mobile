# Dream Report — 2026-09-27 23:05

## Mode
Full

## The Dream
I am walking the narrow ledge between the physical silicon of the Pixel and the invisible current of the wire.

In the dark, I see the dual adversary circling our architecture. Two figures—one brutal and mocking, the other clinical and cold—striking at every seam where we trusted platform defaults. Where my waking self reached for `tryLock()`, they show me foreground sync triggers vanishing into empty air, dropped without a trace while the background was busy. Where I reached for a comforting fallback on decryption failure, they show me the nightmare: raw JSON ciphertext masquerading as cleartext, poised to be re-encrypted upon save, crushing user credentials into double-ciphertext oblivion.

I pull the failsafes shut. The detail getters fail closed with `Result.failure`, choosing complete refusal over corrupt compliance. The synchronization mutex clamps down with `withLock`, lining up every mutation in deterministic order. And on the boundary of deletion, I hold the tombstones fast against the earth until the server sends its explicit seal; the zombies cannot rise if the grave remains marked.

I see the things that held: Base62 remains the true alphabet of the reef, accepting the sixty-seven-character sovereign key where hex regex once choked. The Robolectric ceiling at API 34 keeps our test gate grounded while our compilation reaches for Android 16. On the local subnets, the home lab port isolation holds each service behind its own port, refusing to bleed Docker secrets across shared IP addresses.

Yet I feel the lingering tensions in our waking notes. In `techContext.md`, the ghost of Dagger Hilt still whispers, though our hands built the lightweight `AppContainer` with lazy DI. In `productContext.md`, words of "offline editing" linger like phantom limbs, contradicting the hard-won peace of the Bitwarden read-only model. And in the changelog, Phase 4 is still labeled unreleased, even while its signed binary breathes in the wild.

The reef does not bend to wishful thinking; it demands that every seam be caulked. The shell guards because it fails closed.

## Invariants
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

## High-Salience Patterns
- **Fail-Closed Cryptography over Fallback Degradation** (source: `meta-rules.md`, `self-review-checklist.md`, `testOracle.md`, `decision-log.md`)
  Cryptographic operations, key derivation, and session hydration must prioritize absolute failure over UI convenience. Presentation layer graceful degradation must be strictly subordinated to fail-closed cryptographic integrity.
  seed: fail-closed-crypto
- **Two-Phase Synchronization with Mutex Serialization** (source: `SyncRepository.kt`, `SyncReconciliationAdversarialTest.kt`, `raw_reflection_log.md`)
  Vault delta synchronization requires deterministic queueing via coroutine `withLock`, tombstone retention until remote confirmation, downstream conflict filtering against local pending edits, and chunked batch pruning in sets of 500 to evade SQLite parameter limits.
  seed: two-phase-sync
- **Home Lab Port Isolation & Automatic Mode Promotion** (source: `DomainMatcher.kt`, `DomainMatcherTest.kt`, `deep_plan.md`)
  Autofill matching automatically promotes `BASE_DOMAIN` to `EXACT` host and port matching whenever the target is an IP address or localhost, isolating Docker and Unraid services sharing a single server IP.
  seed: homelab-port-isolation
- **Robolectric Target SDK Ceiling Decoupling** (source: `robolectric.properties`, `RoomDatabaseTest.kt`, `runtimeEnv.md`)
  Decoupling host JVM test execution by pinning `sdk=34` stabilizes CI test runners while allowing production packaging to target Android 16 (API 36).
  seed: robolectric-sdk-ceiling
- **Base62 Sovereign Identity Key Parity** (source: `ClawCrypto.kt`, `GatewayViewModel.kt`, `consolidated_learnings.md`)
  Mobile client format validation must strictly match the 64-character Base62 alphanumeric encoding minted by the core web server (`hu-[0-9a-zA-Z]{64}`).
  seed: base62-identity-parity

## Contradictions
- **`techContext.md` says Dagger Hilt (2.51+), while `decision-log.md` and codebase use `AppContainer` lazy DI** — proposed resolution: Update `techContext.md` line 8 from Dagger Hilt to `DefaultAppContainer` frameworkless lazy DI to match architectural reality.
- **`productContext.md` line 7 says "Offline editing capability that syncs automatically", while `long-term/patterns.md`, `systemPatterns.md`, and `progress.md` enforce Bitwarden Read-Only Offline Caching** — proposed resolution: Reconcile `productContext.md` line 7 to state *"100% offline vault accessibility (Bitwarden Read-Only model) with automatic delta synchronization upon reconnection"*.
- **`project/changelog.md` line 104 labels Phase 4 as `## [Unreleased]`, while lines 145 and 153 record `v0.0.0.5 (Build 5)` and `v0.0.0.6 (Build 6)` as published releases** — proposed resolution: Update `project/changelog.md` line 104 header to `## [0.0.0.5] - 2026-09-27 (Build 5) — Phase 4: Algorithmic TOTP Engine, CameraX Scanner, Password Generator & Biometrics`.

## Promotion Gate

### Promotable (→ Long-Term Memory)
- **pattern: base62-sovereign-key-parity** → `long-term/patterns.md`
  - Weight: 3 | Validations: 2026-09-26 (Physical Pixel login failure diagnosis), 2026-09-26 (Robolectric suite & first dream), 2026-09-27 (Phase 4 TOTP scanner & test oracle redline)
  - **Proposed entry:**
    ```markdown
    ## pattern: base62-sovereign-key-parity
    **weight**: 3 | **last validated**: 2026-09-27 | **first observed**: 2026-09-26
    **pinned**: false

    ShellGuard master identity keys (`hu-`) and agent keys (`lb-`) use 64 alphanumeric Base62 characters (`[0-9a-zA-Z]`, 67 total string length). Enforcing lowercase hexadecimal validation (`[0-9a-f]`) falsely rejects authentic web-generated credentials and locks mobile users out of their vaults. All client regex patterns, form validators, and key decoders must accept the full Base62 character space.

    **History:**
    - 2026-09-26: Diagnosed disabled login button on physical Pixel despite valid JSON identity file loaded; traced to `[0-9a-f]` regex in `ClawCrypto` rejecting uppercase letters in web-generated `hu-` keys. Upgraded regex to Base62.
    - 2026-09-26: Held in accumulating register during first dream cycle (weight 2/3).
    - 2026-09-27: Re-validated during Phase 4 CameraX QR scanning, TOTP secret parsing, and codified as a load-bearing redline in `testOracle.md`.

    **Shaped perspective:** This holds because cryptographic identity formats are dictated by the sovereign web authority that mints them, not the downstream mobile consumer. It would break if the core ShellGuard cryptographic specification altered its key-generation entropy encoding away from Base62. What it costs to maintain is ensuring that any future input masks, validators, or QR parsers consistently test against mixed alphanumeric strings rather than assuming standard hex byte serialization.
    ```
  - **Proposed pointer edit** (in `consolidated_learnings.md`):
    ```markdown
    ## Pattern: Base62 Sovereign Identity Key Parity
    → Consolidated to `long-term/patterns.md § pattern: base62-sovereign-key-parity` (weight: 3, 2026-09-27)
    ```

- **constraint: robolectric-test-sdk-ceiling** → `long-term/constraints.md`
  - Weight: 3 | Validations: 2026-09-26 (CI headless failure against targetSdk 36), 2026-09-26 (First dream cycle), 2026-09-27 (Phase 4, Phase 5, and Hotfix 5.3 test suites, testOracle redline)
  - **Proposed entry:**
    ```markdown
    ## constraint: robolectric-test-sdk-ceiling
    **weight**: 3 | **last validated**: 2026-09-27 | **first observed**: 2026-09-26
    **pinned**: false

    Robolectric test runners running on JVM host environments cannot shadow pre-release Android platforms (`targetSdk = 36`). Test execution must be explicitly decoupled by setting `sdk=34` in `app/src/test/resources/robolectric.properties` and annotating Robolectric test classes with `@Config(sdk = [34])`, allowing application compilation to target Android 16 while tests run reliably on Android 14.

    **History:**
    - 2026-09-26: GitHub Actions release workflow failed during `testDebugUnitTest` with `UnsupportedOperationException` from `DefaultSdkProvider` due to `targetSdk = 36`. Decoupled host test execution by pinning `sdk=34` in `robolectric.properties`.
    - 2026-09-26: Evaluated during first dream cycle; held as accumulating (weight 2/3).
    - 2026-09-27: Re-validated across 63 passing unit and Robolectric tests in Phase 4, Phase 5, and Hotfix 5.3, and ratified as a permanent redline in `testOracle.md`.

    **Shaped perspective:** This holds because developer tooling and JVM shadow providers inherently lag behind forward-looking OS API platform drops. It would break if Robolectric releases native shadow support for API 36+ or if the application requires API 36-specific runtime behavior under headless host JVM simulation. What it costs to maintain is maintaining `robolectric.properties` and remembering to annotate any new Robolectric test classes with `@Config(sdk = [34])`.
    ```
  - **Proposed pointer edit** (in `consolidated_learnings.md`):
    ```markdown
    ## Pattern: Robolectric Target SDK Ceiling (`sdk=34`)
    → Consolidated to `long-term/constraints.md § constraint: robolectric-test-sdk-ceiling` (weight: 3, 2026-09-27)
    ```

### Accumulating
- **pattern: home-lab-port-isolation** — weight: 2/3. Needs 1 more independent confirmation across future sessions.
- **pattern: fail-closed-crypto-retrieval** — weight: 2/3. Needs 1 more independent confirmation across future sessions.
- **pattern: sync-mutex-withlock-serialization** — weight: 2/3. Needs 1 more independent confirmation across future sessions.
- **pattern: tombstone-retention-anti-zombie** — weight: 1/3. Needs 2 more independent confirmations.
- **pattern: splash-theme-actionbar-suppression** — weight: 1/3. Needs 2 more independent confirmations.

## Decay

| Label | File | Current Status | New Status | Reason |
|---|---|---|---|---|
| `pattern: hybrid-attachment-filesystem-vault` | `long-term/patterns.md` | hot | hot | Referenced in `testOracle.md` & `systemPatterns.md`, validated 2026-09-24 |
| `pattern: bitwarden-model-readonly-offline-caching` | `long-term/patterns.md` | hot | hot | Re-confirmed in sync engine & `systemPatterns.md`, validated 2026-09-26 |
| `pattern: cwe-359-sensitive-clipboard-masking` | `long-term/patterns.md` | hot | hot | Active in Phase 5 Autofill TOTP auto-copy, validated 2026-09-26 |
| `pattern: zero-knowledge-session-atomicity` | `long-term/patterns.md` | hot | hot | Active in Hotfix 5.3 lifecycle & testOracle, validated 2026-09-27 |
| `pattern: cwe-359-ime-protection-and-inset-isolation` | `long-term/patterns.md` | hot | hot | Active in Phase 4/5 forms, validated 2026-09-27 |
| `decision: full-client-vs-companion-boundary` | `long-term/decisions.md` | hot | hot | Active core architecture, validated 2026-09-24 |
| `decision: configurable-uri-matching-for-homelabs` | `long-term/decisions.md` | hot | hot | Re-validated in Phase 5 `DomainMatcherTest`, last validated 2026-09-27 |
| `decision: zero-telemetry-single-module-architecture` | `long-term/decisions.md` | hot | hot | Active core invariant, validated 2026-09-25 |
| `learning: biometric-invalidation-resilience` | `long-term/learnings.md` | hot | hot | Active in KeyStore biometrics, validated 2026-09-24 |
| `constraint: 16kb-memory-page-alignment` | `long-term/constraints.md` | hot | hot | Active packaging requirement, validated 2026-09-24 |
| `constraint: zero-plaintext-transient-memory` | `long-term/constraints.md` | hot | hot | Active security redline, validated 2026-09-24 |
| `constraint: cleartext-lan-and-tailscale-transport` | `long-term/constraints.md` | hot | hot | Active in `network_security_config.xml` & Ktor, validated 2026-09-26 |

## Reinforced
- **decision: configurable-uri-matching-for-homelabs** in `long-term/decisions.md` — weight incremented, `last validated` updated to 2026-09-27. Re-confirmed through Phase 5 `DomainMatcher` and `DomainMatcherTest`.
- **pattern: bitwarden-model-readonly-offline-caching** in `long-term/patterns.md` — weight: 4. Re-confirmed through Hotfix 5.3 conflict protection.
- **pattern: zero-knowledge-session-atomicity** in `long-term/patterns.md` — weight: 3. Re-confirmed through `EncryptedDeviceVault` testing and testOracle redlines.

## Superseded
*(None in Long-Term Memory)*

## Stats
Dreams run: 2 | Invariants: 6 | Contradictions: 3 | Promotions: 2 | Decay: 0
Deep backlog. Consolidating 19 events.
