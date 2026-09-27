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
