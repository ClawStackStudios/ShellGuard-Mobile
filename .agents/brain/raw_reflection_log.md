# Raw Reflection Log

<!-- Fresh entries from ongoing tasks will be recorded here and subsequently consolidated into consolidated_learnings.md -->

---
Date: 2026-09-27
TaskRef: "Bidirectional Sync Reconciliation & Dual Adversarial Hardening (Hotfix 5.3)"

Learnings:
- In Robolectric unit tests without simulated network capabilities, `ConnectivityManager.activeNetwork` is null, causing `ConnectivityMonitor` to default to offline and short-circuit `SyncRepository.syncAll()`. Adding `initialOnlineOverride: Boolean? = null` allows tests to evaluate online sync paths deterministically.
- Using `syncMutex.tryLock()` causes concurrent sync calls (e.g. from background triggers or tests) to silently drop and return `Result.success(Unit)` without performing work. `syncMutex.withLock` guarantees queueing and serialization.
- Server ID re-keying requires re-encrypting ciphertext on the server: if the server assigns an ID different from the client's temporary ID, the server holds ciphertext encrypted with AAD `{domain}:{tempId}`. An immediate `client.update*` with the newly assigned server ID reconciles the AAD binding.
- SQL queries with `IN (:collection)` fail when the collection exceeds SQLite's 999 parameter ceiling. Batch deletion in chunks of 500 (`chunked(500)`) safely evades this limit.

Difficulties:
- Silent fallback in detail getters (`getPearlDetail`, etc.) returned raw JSON ciphertext as cleartext on decryption failure. If edited, this resulted in double-ciphertext corruption. Resolved by failing closed with `Result.failure`.
- Tombstone resurrection: deleting local `PENDING_DELETE` records prior to verifying remote delete responses caused subsequent downstream pulls to re-download the remote item. Resolved by retaining tombstones until remote deletion is confirmed.

Successes:
- Subjecting sync and crypto code to a dual adversarial audit pass (ruthless critic + surgical underground hacker) exposed 7 critical vectors before production deployment.
- Created `SyncReconciliationAdversarialTest.kt` covering all 6 core attack vectors with 100% green verification.

Improvements_Identified_For_Consolidation:
- Fail-closed cryptographic detail retrieval pattern.
- Mutex serialization over non-blocking tryLock for mission-critical sync pipelines.
- Pruning via set difference of local synced IDs vs remote IDs in chunks of 500.
---
