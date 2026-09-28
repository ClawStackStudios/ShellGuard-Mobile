# Dream Consolidation
Promotion and decay events from memory consolidation passes.
This file tracks the movement of knowledge between registers.

## 2026-09-26 11:06 — Consolidation Pass

### Promotions (temporal → long-term)
- **constraint: cleartext-lan-and-tailscale-transport** → `long-term/constraints.md` (weight: 3, validated: 2026-09-24, 2026-09-25, 2026-09-26)
- **pattern: cwe-359-sensitive-clipboard-masking** → `long-term/patterns.md` (weight: 3, validated: 2026-09-24, 2026-09-25, 2026-09-26)

### Accumulating (not yet eligible)
- **pattern: base62-sovereign-key-parity** — weight: 2/3. Last validation: 2026-09-26. Needs 1 more independent confirmation across future sessions.
- **constraint: robolectric-test-sdk-ceiling** — weight: 2/3. Last validation: 2026-09-26. Needs 1 more independent confirmation across future CI/test runs.
- **pattern: ime-scaffold-inset-decoupling** — weight: 2/3. Last validation: 2026-09-26. Needs 1 more independent confirmation.

### Decay
*(None. All existing entries in `long-term/` were validated within the last 48 hours and remain hot.)*

### Reinforced
- **pattern: bitwarden-model-readonly-offline-caching** in `long-term/patterns.md` — weight incremented, `last validated` updated to 2026-09-26. Re-confirmed through Ktor sync and `ConnectivityMonitor` implementation.
- **pattern: hybrid-attachment-filesystem-vault** in `long-term/patterns.md` — status: hot.
- **constraint: 16kb-memory-page-alignment** in `long-term/constraints.md` — status: hot.
- **decision: zero-telemetry-single-module-architecture** in `long-term/decisions.md` — status: hot.

### Superseded (in long-term)
*(None)*

---

## 2026-09-27 23:05 — Consolidation Pass

### Promotions (temporal → long-term)
- **pattern: base62-sovereign-key-parity** → `long-term/patterns.md` (weight: 3, validated: 2026-09-26, 2026-09-26, 2026-09-27)
- **constraint: robolectric-test-sdk-ceiling** → `long-term/constraints.md` (weight: 3, validated: 2026-09-26, 2026-09-26, 2026-09-27)

### Accumulating (not yet eligible)
- **pattern: home-lab-port-isolation** — weight: 2/3. Last validation: 2026-09-27. Needs 1 more independent confirmation across future sessions.
- **pattern: fail-closed-crypto-retrieval** — weight: 2/3. Last validation: 2026-09-27. Needs 1 more independent confirmation across future sessions.
- **pattern: sync-mutex-withlock-serialization** — weight: 2/3. Last validation: 2026-09-27. Needs 1 more independent confirmation across future sessions.
- **pattern: tombstone-retention-anti-zombie** — weight: 1/3. Last validation: 2026-09-27. Needs 2 more independent confirmations.
- **pattern: splash-theme-actionbar-suppression** — weight: 1/3. Last validation: 2026-09-27. Needs 2 more independent confirmations.

### Decay
*(None. All existing entries in `long-term/` remain hot.)*

### Reinforced
- **decision: configurable-uri-matching-for-homelabs** in `long-term/decisions.md` — weight incremented, `last validated` updated to 2026-09-27.
- **pattern: bitwarden-model-readonly-offline-caching** in `long-term/patterns.md` — weight: 4. Re-confirmed through Hotfix 5.3 conflict protection.
- **pattern: zero-knowledge-session-atomicity** in `long-term/patterns.md` — weight: 3. Re-confirmed through `EncryptedDeviceVault` testing and testOracle redlines.

### Superseded (in long-term)
*(None)*
