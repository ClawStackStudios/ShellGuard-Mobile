# Ratified Constraints: ShellGuard Mobile

## constraint: 16kb-memory-page-alignment
**weight**: 3 | **last validated**: 2026-09-24 | **first observed**: 2026-09-24

Android 15+ (API 35/36) enforces 16 KB page-aligned ELF segments for native binaries. Any `.so` library compiled with 4 KB alignment will crash with `SIGSEGV` or `INSTALL_FAILED_INVALID_APK`. SQLCipher must remain at 4.6.1+ with `jniLibs.useLegacyPackaging = false` in `app/build.gradle.kts`.

**History:**
- 2026-09-24: Ratified across the ShellGuard Android ecosystem as an inviolable packaging requirement.

**Shaped perspective:** Low-level OS platform evolutions do not compromise with applications. The build system must be configured to emit binaries that conform to hardware truth, not legacy defaults.

---

## constraint: zero-plaintext-transient-memory
**weight**: 3 | **last validated**: 2026-09-24 | **first observed**: 2026-09-24

Decrypted secrets and derived `shellKey` instances must never be cached across lifecycle boundaries. Decrypted items live in memory only for the duration of the active render tick or clipboard transfer. Window protection via `FLAG_SECURE` blocks OS task snapshots.

**History:**
- 2026-09-24: Formulated to ensure total compliance with CWE-359 and zero-knowledge invariants.

**Shaped perspective:** If memory is not scrubbed, encryption at rest is merely theater.
