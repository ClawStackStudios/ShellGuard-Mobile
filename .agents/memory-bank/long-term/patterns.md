# Ratified System Patterns: ShellGuard Mobile

## pattern: hybrid-attachment-filesystem-vault
**weight**: 3 | **last validated**: 2026-09-24 | **first observed**: 2026-09-24

Never store multi-megabyte attachment blobs inside SQLite database rows. Android's SQLite CursorWindow limits query rows to 2MB, causing unavoidable `CursorWindowAllocationException` or `SQLiteBlobTooBigException` crashes on SELECT. Decouple metadata into Room and stream encrypted ciphertext directly to private internal disk files (`filesDir/vault_attachments/{id}.enc`).

**History:**
- 2026-09-24: Audited `SecureAttachmentEntity` against web server 500MB attachment specification; realized inline BLOBs would cause fatal app crashes under Android SQLite CursorWindow limitations; refactored to Hybrid File-System Vault.

**Shaped perspective:** Databases are indexes, not file systems. Mobile relational databases should store structure and searchable pointers; the OS filesystem should store the weight.

---

## pattern: bitwarden-model-readonly-offline-caching
**weight**: 4 | **last validated**: 2026-09-26 | **first observed**: 2026-09-24
**pinned**: false
**status**: hot

When a mobile client is disconnected from its authoritative server, vault operations must be strictly Read-Only. Allow 100% read, search, copy, autofill, and TOTP functionality from the local encrypted cache, but block creation, editing, and deletion in the UI. Automatically probe server health via `NetworkCallback` upon connection restoration to resume online synchronization seamlessly.

**History:**
- 2026-09-24: Evaluated optimistic offline write queues vs read-only offline caching. Validated against Bitwarden's client-server architecture; confirmed that blocking offline mutations eliminates split-brain conflicts and guarantees vault integrity.
- 2026-09-26: Re-validated during Phase 2 Ktor client and SyncRepository implementation; mutation guards and ConnectivityMonitor live-tested on physical Pixel.

**Shaped perspective:** This holds because zero-knowledge encryption prevents the client or server from inspecting plaintext to merge diverged offline diffs intelligently. It would break if client users demanded multi-device offline write capabilities without an active authoritative connection. What it costs to maintain is strict UI gating (disabled FABs, dimmed edit/delete icons) and an automated background health probe to detect network restoration.

---

## pattern: cwe-359-sensitive-clipboard-masking
**weight**: 3 | **last validated**: 2026-09-26 | **first observed**: 2026-09-24
**pinned**: false
**status**: hot

All password, key, and TOTP clipboard operations must declare `ClipDescription.EXTRA_IS_SENSITIVE = true` on Android 13+ (API 33+) to suppress floating system thumbnail previews, paired with an automated 30s/60s background coroutine timer to purge transient secrets from the system pasteboard.

**History:**
- 2026-09-24: Identified CWE-359 clipboard leakage on Android 13+; codified in `crypto-and-keystore.md`.
- 2026-09-25: Audited against Bitwarden Android implementation; added configurable 30s/60s/120s timer settings in `ui-ux-design-system.md` §10.
- 2026-09-26: Enforced across `VaultDashboardScreen` and `SyncRepository` copy actions during live device verification.

**Shaped perspective:** This holds because modern mobile operating systems render persistent visual thumbnail previews of clipboard data that expose sensitive credentials to screen recorders, recents caches, and shoulder surfers. It would break if OEM background process killing abruptly terminates the scrubbing coroutine before the timer completes. What it costs to maintain is managing background lifecycle coroutines and educating users why copied credentials disappear from the clipboard after 30 seconds.
