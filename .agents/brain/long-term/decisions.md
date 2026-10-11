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
