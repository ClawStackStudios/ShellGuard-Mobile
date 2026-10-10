# Synthesized Meta-Rules: ShellGuard Mobile

These meta-rules are synthesized from cross-session failure analyses and error taxonomies via `/deep-learn`. They represent durable, cross-cutting heuristics derived from empirical evidence across the project's development history.

---

## 1. meta-rule: fail-closed-security-boundaries

- **Scope**: Universal (Cryptographic, Biometric & Authentication Domains)
- **Heuristic**: When executing any cryptographic derivation, decryption, or session validation, prefer failing closed with explicit `Result.failure` or immediate session termination over graceful degradation. Graceful degradation across cryptographic boundaries exposes raw ciphertext or causes double-ciphertext data destruction.
- **Evidence**: 2 recorded instances (`2026-09-26` session key split-brain where token survived without decryption key, `2026-09-27` double-ciphertext corruption where caught decryption errors returned raw JSON ciphertext as cleartext).
- **External Evidence Framing**: The data shows a recurring failure mode: attempting to keep the UI from crashing by returning fallback data across cryptographic boundaries converts transient errors into permanent data corruption. The boundary is: failures occur when unauthenticated or untransformed payloads are permitted to escape the cryptographic engine into downstream domain state.
- **Application Note**: This is a heuristic. The context determines how the failure is presented to the user: domain operations fail closed, while UI layers handle the failure gracefully with non-blocking error presentation.

---

## 2. meta-rule: two-phase-reconciliation-invariants

- **Scope**: Domain-Specific (Synchronization, Room Persistence & Remote API)
- **Heuristic**: When reconciling local storage with an authoritative remote server, prefer queued mutex serialization (`withLock`) over non-blocking drops (`tryLock`), retain local tombstones until remote HTTP 2xx confirmation, and exclude locally pending IDs from downstream delta pulls.
- **Evidence**: 3 recorded instances (`2026-09-27` concurrent sync drops via `tryLock()`, zombie resurrection from premature tombstone deletion, downstream pull overwriting local `PENDING_SYNC` modifications).
- **External Evidence Framing**: The data shows that asynchronous synchronization pipelines fail when operations assume network success before acknowledgement or drop requests during busy states. The boundary is: operations succeed when mutations are executed as a two-phase transactional commit where local intent is preserved until remote confirmation.

---

## 3. meta-rule: testable-platform-abstraction

- **Scope**: Domain-Specific (Android Framework & Host JVM Test Harnesses)
- **Heuristic**: When wrapping Android system services (`ConnectivityManager`, `TelephonyManager`, `BiometricManager`, `WindowManager`), prefer constructor-level test overrides (`initialOverride: T? = null`) or explicit test mutators over ambient OS singletons.
- **Evidence**: 2 recorded instances (`2026-09-26` Robolectric API 36 crash in headless CI, `2026-09-27` `activeNetwork == null` false-negative offline state in host JVM).
- **External Evidence Framing**: The data shows that headless JVM and Robolectric test harnesses frequently provide stubbed or null representations of Android system services that diverge from physical hardware behavior. The boundary is: tests succeed when platform capability checks can be deterministically asserted via explicit parameter injection rather than environmental framework stubs.

---

## 4. meta-rule: bounded-sqlite-chunking

- **Scope**: Domain-Specific (Relational Database, Room & SQLite)
- **Heuristic**: When executing queries, bulk updates, or deletions referencing in-memory collections (`WHERE id IN (:list)` or `NOT IN (:list)`), prefer chunking the collection into batches of ≤ 500 items (`collection.chunked(500)`) over unbounded SQL parameter passing.
- **Evidence**: 2 recorded instances (`2026-09-24` CursorWindow crash on attachment blobs, `2026-09-27` SQLite 999 parameter ceiling crash on vault pruning).
- **External Evidence Framing**: The data shows that mobile relational database engines enforce hard platform resource ceilings (2MB CursorWindow, 999 host parameter variables) that fail silently on small datasets but crash catastrophically at scale. The boundary is: database operations succeed when batch operations partition dynamic collections into bounded chunks before dispatching SQL statements.

---

## 5. meta-rule: seeded-cross-dispatcher-combine

- **Scope**: Domain-Specific (MVI ViewModels, Kotlin Flows & AndroidX DataStore)
- **Failure Condition**: Combining an asynchronous disk/network `Flow` (`DataStore.data` on `Dispatchers.IO`) with a synchronous in-memory `MutableStateFlow` via `combine(...)` without an initial value causes `combine` to drop or stall in-memory UI updates whenever disk I/O is slower than main-thread execution (e.g. cold start or cloud CI runners).
- **Minimal-Code Heuristic**: Seed the disk flow with `.onStart { emit(DefaultState()) }` (1 line), return `Job` from ViewModel mutations, and reset singleton `DataStore` state in `@Before setUp()` via `runBlocking`.
- **Evidence**: 3 recorded instances (`2026-10-03` `SyncRepository` init collector, `2026-10-04` DataStore mutation `.join()`, `2026-10-09` `v0.0.0.11` CI failure in `SettingsViewModelTest`).
- **Application Note**: Write the one boring line that makes the race condition structurally impossible. Never mask an unseeded `combine` with artificial test delays or polling loops.

---

## 6. meta-rule: tight-loop-hardware-and-counterpart-calibration

- **Scope**: Universal (Cross-Platform Parity, OS Autofill Heuristics & Hardware Boundaries)
- **Failure Condition**: Treating synthetic unit test trees or single-repo mocks as the final completion gate leaves cross-repo schema mismatches (segregated arrays vs. polymorphic `items`) and real-world DOM quirks (container `<form>` IDs, 2-step split logins) undiscovered until post-merge.
- **Minimal-Code Heuristic**: Read the external counterpart's source code during **Plan**, gate OS view traversal on leaf editability (`isEditableInputNode`) and Rank 4/5 password co-presence (`passwordId != null`), and always close the loop on **Physical Hardware** (`Plan → Implement → Test Code → Test Physical Hardware`) before marking a feature complete.
- **Evidence**: 3 recorded instances (`2026-10-04` Web backup schema alignment, `2026-10-08` `<form>` container hijacking defense, `2026-10-08` 2-step Google Sign-In on physical Pixel).

