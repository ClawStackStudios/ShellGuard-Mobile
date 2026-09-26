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
- **`changelog.md` temporal lag**: `memory-bank/changelog.md` only documents through Phase 1 under `[0.0.0.3]`, whereas root `CHANGELOG.md` and `RELEASE-v0.0.0.3.md` document the complete Phase 2 release. — resolution: Update `memory-bank/changelog.md` with Phase 2 release highlights.

### Superseded (archive)
- **Hexadecimal ClawKey Validator** (`hu-[0-9a-f]{64}`) — superseded by: Base62 ClawKey Validator (`hu-[0-9a-zA-Z]{64}`) on 2026-09-26.
- **Unconditional `FLAG_SECURE` in Debug Builds** — superseded by: Release-only scoped `FLAG_SECURE` (`if (!BuildConfig.DEBUG)`) to prevent Adreno 530 compositor blackout over system IME on 2026-09-26.
