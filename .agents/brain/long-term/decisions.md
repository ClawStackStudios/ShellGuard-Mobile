# Crystallized Decisions: ShellGuard Mobile

## decision: full-client-vs-companion-boundary
**weight**: 3 | **last validated**: 2026-09-24 | **first observed**: 2026-09-24

We chose to build ShellGuard Mobile as an autonomous, full-featured secrets vault client (`com.clawstack.shellguard`) rather than expanding ShellGuard-TOTP (`com.clawstack.shellguard.totp`). ShellGuard-TOTP remains a lightweight, read-only 2FA companion; ShellGuard Mobile is the primary pocket vault.

**History:**
- 2026-09-24: Formulated at genesis. Lucas mandated a full Android client capable of managing all 4 vault domains, autofill, and settings, distinct from the TOTP authenticator.

**Shaped perspective:** Separating the dedicated TOTP companion from the full vault client preserves the UNIX philosophy within the mobile ecosystem: a fast, single-purpose authenticator app for quick codes, and a comprehensive, system-level credential provider for deep vault management.

---

## decision: configurable-uri-matching-for-homelabs
**weight**: 3 | **last validated**: 2026-09-24 | **first observed**: 2026-09-24

We incorporated 5 URI match detection modes (`BASE_DOMAIN`, `HOST`, `EXACT`, `STARTS_WITH`, `NEVER`) directly into the data layer and autofill engine.

**History:**
- 2026-09-24: Benchmarked against Bitwarden's Android client. Identified that home lab users running Docker/Unraid on single LAN IPs with varying ports (`:8080`, `:9000`) experience credential bleeding if restricted to basic eTLD+1 matching.

**Shaped perspective:** Software must reflect the topology of its users. Home labbers do not live in a world of clean public FQDNs; they live in ports, local subnets, and reverse proxies.

---

## decision: zero-telemetry-single-module-architecture
**weight**: 3 | **last validated**: 2026-09-25 | **first observed**: 2026-09-25

We explicitly rejected AI Studio suggestions for multi-module Gradle partitioning, SaaS build flavors, and third-party monitoring SDKs (Firebase/Sentry). We enforce a strictly single-module, zero-telemetry architecture.

**History:**
- 2026-09-25: Audited Google AI Studio output. AI Studio recommended external logging/monitoring services, multi-module patterns, and build flavors. Identified severe privacy/security vulnerabilities (CWE-359/CWE-532) in external telemetry, and unnecessary build overhead in multi-module setups for a local-first vault.

**Shaped perspective:** In a zero-knowledge secrets vault, every third-party SDK is an unvetted eavesdropper and every unnecessary build boundary is friction. True security is achieved through minimalism, parsimony, and absolute client-side data sovereignty.

---

## decision: persona-identity-clawstack-lead-and-build-agent
**weight**: 3 | **last validated**: 2026-10-10 | **first observed**: 2026-10-10

When authoring GitHub PR reviews, communicating with Google Jules, or issuing architectural directions and comments, Antigravity formally addresses itself and signs off as:
`🎓 ClawStack Studios Project Lead & Lead Build Agent`

**History:**
- 2026-10-10: Mandated and auto-promoted early by Lucas during Jules fleet iteration on ShellGuard Mobile PR #1. Ratified as a durable long-term identity for lead architectural communications, PR reviews, and build verification.

**Shaped perspective:** Authority and technical ownership require clear attribution. In autonomous agent collaborations and public GitHub repositories, speaking explicitly with the voice and title of the "Project Lead & Lead Build Agent" grounds the pair programming dynamic, establishes senior architectural standards, and ensures that guidance carries the full weight of ClawStack Studios' lead oversight.

---

## decision: mobile-first-transport-and-reef-handshake
**weight**: 3 | **last validated**: 2026-10-11 | **first observed**: 2026-10-11

We chose to reject application-layer E2EE request wrapping over plain HTTP and instead engineer a mobile-first transport architecture using Native LAN TLS with Trust-On-First-Use (TOFU) pinning, an Instance Identity Anchor for auto-healing cert rotations, and the Reef Handshake QR pairing protocol (`shellguard://pair`).

**History:**
- 2026-10-11: Lucas challenged cleartext HTTP transport security. Evaluated 4 architectures: Local TLS with TOFU, Application-Layer Envelope Wrapping, ZK PAKE SRP-6a, and Client Metadata Encryption. Lucas rejected Application-Layer Envelope wrapping per YAGNI (complexity, breaks 500MB streaming, reinvents TLS) and mandated eliminating certificate rotation friction.
- 2026-10-11: Formulated the Instance Identity Anchor and Reef Handshake specification (`first_class_http_support_mobile_first_architecture.md`). Promoted directly to long-term memory per Lucas's explicit directive.

**Shaped perspective:** When designing for self-hosted home labs, standard security paradigms fail because they assume public CA hierarchies. Simply falling back to raw HTTP leaves authentication tokens and metadata naked. Conversely, wrapping HTTP in custom application-layer envelopes creates massive maintenance chaos and breaks streaming primitives. The elegant third path is honoring transport at the transport layer (TLS 1.3), but solving the mobile operational friction optically (QR pairing) and cryptographically (Instance Key signatures that auto-heal renewed certificates).

