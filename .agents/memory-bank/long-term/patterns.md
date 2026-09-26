# Ratified System Patterns: ShellGuard Mobile

## pattern: hybrid-attachment-filesystem-vault
**weight**: 3 | **last validated**: 2026-09-24 | **first observed**: 2026-09-24

Never store multi-megabyte attachment blobs inside SQLite database rows. Android's SQLite CursorWindow limits query rows to 2MB, causing unavoidable `CursorWindowAllocationException` or `SQLiteBlobTooBigException` crashes on SELECT. Decouple metadata into Room and stream encrypted ciphertext directly to private internal disk files (`filesDir/vault_attachments/{id}.enc`).

**History:**
- 2026-09-24: Audited `SecureAttachmentEntity` against web server 500MB attachment specification; realized inline BLOBs would cause fatal app crashes under Android SQLite CursorWindow limitations; refactored to Hybrid File-System Vault.

**Shaped perspective:** Databases are indexes, not file systems. Mobile relational databases should store structure and searchable pointers; the OS filesystem should store the weight.

---

## pattern: bitwarden-model-readonly-offline-caching
**weight**: 3 | **last validated**: 2026-09-24 | **first observed**: 2026-09-24

When a mobile client is disconnected from its authoritative server, vault operations must be strictly Read-Only. Allow 100% read, search, copy, autofill, and TOTP functionality from the local encrypted cache, but block creation, editing, and deletion in the UI. Automatically probe server health via `NetworkCallback` upon connection restoration to resume online synchronization seamlessly.

**History:**
- 2026-09-24: Evaluated optimistic offline write queues vs read-only offline caching. Validated against Bitwarden's client-server architecture; confirmed that blocking offline mutations eliminates split-brain conflicts and guarantees vault integrity.

**Shaped perspective:** In a zero-knowledge ecosystem, conflict resolution cannot inspect plaintext to merge diffs intelligently. The server must remain the singular authoritative ledger; the mobile client is an unyielding, resilient lens that views the ledger and waits to record new truths until it can talk to the source.
