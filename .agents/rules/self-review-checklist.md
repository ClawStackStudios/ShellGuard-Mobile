# Self-Review Checklist (v1)
*Last updated: 2026-09-27. Prior version: None (Genesis).*

Run this checklist before finalizing any task, commit, or pull request:

- [ ] **1. Fail-Closed Cryptography**: Does any catch block in key derivation, token handling, or field decryption return raw ciphertext, default strings, or null without propagating a `Result.failure`? → *maps to: Graceful-Degradation Security Antipattern*
- [ ] **2. Tombstone Confirmation**: Are local `PENDING_DELETE` records retained until an explicit HTTP 200/204 response is received from the server? → *maps to: Optimistic Mutation & Premature Deletion*
- [ ] **3. Mutex Queuing**: Does any synchronization or write pipeline use `tryLock()` instead of `withLock`, risking silent operation drops? → *maps to: Optimistic Mutation & Premature Deletion*
- [ ] **4. Downstream Collision Defense**: Does the downstream delta pull verify that incoming remote entities do not overwrite local items marked `PENDING_SYNC` or `PENDING_DELETE`? → *maps to: Optimistic Mutation & Premature Deletion*
- [ ] **5. SQLite Parameter Bounding**: Are all Room collection queries (`IN (:ids)` / `NOT IN (:ids)`) bounded with `.chunked(500)` to prevent the SQLite 999 parameter limit crash? → *maps to: Format & Scale Blind Spots*
- [ ] **6. Wire Encoding & Port Verification**: Have master key regexes, hash representations, and URI matchers been verified against Base62 alphanumeric encoding and multi-tenant IP:port combinations? → *maps to: Format & Scale Blind Spots*
- [ ] **7. Test Double Platform Overrides**: Does any system service monitor (e.g. `ConnectivityMonitor`) provide an explicit test override so headless JVM/Robolectric tests do not falsely trigger offline/disabled states? → *maps to: Host JVM vs. Device Runtime Stubs*
- [ ] **8. Window Decorator Suppression**: If adding or altering activities, are legacy native ActionBars suppressed in `themes.xml` (`windowActionBar=false`, `windowNoTitle=true`), and do biometric activities inherit `FragmentActivity`? → *maps to: Window Inset & Decorator Collisions*

---

### Diff from v0
**Added**: 8 items (Categories 1–5).  
**Removed**: 0 items.  
**Unchanged**: 0 items.
