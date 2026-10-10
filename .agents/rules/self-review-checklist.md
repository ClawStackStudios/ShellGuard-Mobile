# Self-Review Checklist (v3)
*Last updated: 2026-10-10. Prior version: v2 (2026-10-09).*

Run this checklist before finalizing any task, commit, or pull request:

- [ ] **1. Fail-Closed Cryptography**: Does any catch block in key derivation, token handling, or field decryption return raw ciphertext, default strings, or null without propagating a `Result.failure`? → *maps to: Graceful-Degradation Security Antipattern*
- [ ] **2. Tombstone Confirmation**: Are local `PENDING_DELETE` records retained until an explicit HTTP 200/204 response is received from the server? → *maps to: Optimistic Mutation & Premature Deletion*
- [ ] **3. Mutex Queuing**: Does any synchronization or write pipeline use `tryLock()` instead of `withLock`, risking silent operation drops? → *maps to: Optimistic Mutation & Premature Deletion*
- [ ] **4. Downstream Collision Defense**: Does the downstream delta pull verify that incoming remote entities do not overwrite local items marked `PENDING_SYNC` or `PENDING_DELETE`? → *maps to: Optimistic Mutation & Premature Deletion*
- [ ] **5. SQLite Parameter Bounding**: Are all Room collection queries (`IN (:ids)` / `NOT IN (:ids)`) bounded with `.chunked(500)` to prevent the SQLite 999 parameter limit crash? → *maps to: Format & Scale Blind Spots*
- [ ] **6. Wire Encoding & Port Verification**: Have master key regexes, hash representations, and URI matchers been verified against Base62 alphanumeric encoding and multi-tenant IP:port combinations? → *maps to: Format & Scale Blind Spots*
- [ ] **7. Test Double Platform Overrides**: Does any system service monitor (e.g. `ConnectivityMonitor`) provide an explicit test override so headless JVM/Robolectric tests do not falsely trigger offline/disabled states? → *maps to: Host JVM vs. Device Runtime Stubs*
- [ ] **8. Window Decorator Suppression**: If adding or altering activities, are legacy native ActionBars suppressed in `themes.xml` (`windowActionBar=false`, `windowNoTitle=true`), and do biometric activities inherit `FragmentActivity`? → *maps to: Window Inset & Decorator Collisions*
- [ ] **9. Minimum-Code & Operating Failure Check**: Did I read both sides of the seam first, predict the concrete operating failure, and solve it with the smallest possible amount of boring code? → *maps to: Lazy Senior Calibration*
- [ ] **10. Seeded Cross-Dispatcher Combine & Job Returns**: Does every ViewModel `combine(...)` joining a `DataStore` or `Dispatchers.IO` flow with in-memory UI state include `.onStart { emit(Default) }`, do mutation methods return `Job`, and do `@Before setUp()` fixtures use `runBlocking`? → *maps to: Cross-Dispatcher CI Race*
- [ ] **11. Dynamic BuildConfig, License & Release Root Hygiene**: Are all user-facing version strings bound to `BuildConfig.VERSION_NAME` / `BuildConfig.VERSION_CODE`, license strings aligned with `GNU AGPL v3.0`, and superseded `RELEASE-v*.md` files pruned from root on release bump? → *maps to: Static Literal & Release Drift*
- [ ] **12. Autofill Editable-Leaf, Co-Presence & Physical Glass Loop**: Are `AssistStructure` heuristics gated by `isEditableInputNode()` (excluding `AutoCompleteTextView`), password/username mutual exclusion, and Rank 4/5 Co-Presence stripping, and has the flow been exercised on physical hardware? → *maps to: Tight Hardware Loop*
- [ ] **13. Storage Lifecycle & Panic Purge Parity**: Does `executePanicPurge()` wipe all physical filesystem directories (`vault_attachments/`, download caches) alongside Room and DataStore, and does backup/restore account for them? → *maps to: Cascading Storage Destruction Blind Spots*
- [ ] **14. Large-File Mutation & Patch Guard**: For files >500 lines or delegated tasks, are changes applied via direct section replacements rather than fragile shell unified diff scripts? → *maps to: Diff Patch Drift & Large-File Fragility*
- [ ] **15. Bot Handle & Forge Entity Verification**: In GitHub comments and automated tooling, are mentions validated against actual bot entities (never pinging `@jules`)? → *maps to: Platform Contract & Handle Mirroring Drift*

---

### Diff from v2
**Added**: 3 items (Items 13–15: Storage Lifecycle & Panic Purge Parity, Large-File Mutation & Patch Guard, Bot Handle & Forge Entity Verification).  
**Removed**: 0 items.  
**Unchanged**: 12 items (Items 1–12).
