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

---

## constraint: cleartext-lan-and-tailscale-transport
**weight**: 3 | **last validated**: 2026-09-26 | **first observed**: 2026-09-24
**pinned**: false
**status**: hot

Local private IP addresses (`192.168.0.0/16`, `10.0.0.0/8`) and Tailscale CGNAT subnets (`100.64.0.0/10`) must be permitted to transmit cleartext HTTP without requiring public domain TLS certificates. Android network security configuration must declare `<base-config cleartextTrafficPermitted="true">` because the platform's `<domain>` tag does not support CIDR subnet masks, and OkHttp must register `ConnectionSpec.CLEARTEXT`.

**History:**
- 2026-09-24: Specified in `architecture.md` and `routes-and-contracts.md` to support Unraid, TrueNAS, and Docker self-hosters.
- 2026-09-25: Formally tested and committed in `network_security_config.xml` and `techContext.md`.
- 2026-09-26: Validated in `KtorClientProvider.kt` and live-verified via physical Google Pixel connecting directly to LAN server port.

**Shaped perspective:** This holds because self-hosted home lab topology exists largely on local subnets and WireGuard overlays where domain name registrars and public CA authorities have no presence. It would break if Android OS networking strictly mandated public X.509 PKI chains for all outbound sockets without runtime exception. What it costs to maintain is rigorous client-side payload encryption (ShellCryption) so the application never relies on transport security for data confidentiality.

---

## constraint: robolectric-test-sdk-ceiling
**weight**: 3 | **last validated**: 2026-09-27 | **first observed**: 2026-09-26
**pinned**: false
**status**: hot

Robolectric test runners running on JVM host environments cannot shadow pre-release Android platforms (`targetSdk = 36`). Test execution must be explicitly decoupled by setting `sdk=34` in `app/src/test/resources/robolectric.properties` and annotating Robolectric test classes with `@Config(sdk = [34])`, allowing application compilation to target Android 16 while tests run reliably on Android 14.

**History:**
- 2026-09-26: GitHub Actions release workflow failed during `testDebugUnitTest` with `UnsupportedOperationException` from `DefaultSdkProvider` due to `targetSdk = 36`. Decoupled host test execution by pinning `sdk=34` in `robolectric.properties`.
- 2026-09-26: Evaluated during first dream cycle; held as accumulating (weight 2/3).
- 2026-09-27: Re-validated across 63 passing unit and Robolectric tests in Phase 4, Phase 5, and Hotfix 5.3, and ratified as a permanent redline in `testOracle.md`.

**Shaped perspective:** This holds because developer tooling and JVM shadow providers inherently lag behind forward-looking OS API platform drops. It would break if Robolectric releases native shadow support for API 36+ or if the application requires API 36-specific runtime behavior under headless host JVM simulation. What it costs to maintain is maintaining `robolectric.properties` and remembering to annotate any new Robolectric test classes with `@Config(sdk = [34])`.
