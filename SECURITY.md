# 🛡️ Security Policy & Vulnerability Disclosure

**ShellGuard Mobile** is a sovereign, zero-knowledge, offline-first secrets vault native Android client engineered for self-hosted infrastructure and privacy-conscious operators. Security is our primary design constraint: *we build features around security, never security around features*.

---

## 🔒 Supported Versions

We actively maintain and provide security patches for the latest release and the current active milestone:

| Version | Supported | Status |
| :--- | :--- | :--- |
| `0.0.0.10` (Build 10) | ✅ Yes | Current Production / Milestone Release |
| `0.0.0.9` (Build 9) | ⚠️ Maintenance | Security critical patches only |
| `< 0.0.0.9` | ❌ No | End of Life — upgrade to latest release |

---

## 🏛️ Core Cryptographic & Security Invariants

All contributions, audits, and security evaluations must be grounded in ShellGuard Mobile's core defensive invariants:

### 1. Zero-Knowledge Encryption Model
- **Client-Side Secrecy**: The server NEVER receives plaintext credentials, master passwords, or un-hashed sovereign keys (`hu-`).
- **Cryptographic Envelopes**: All vault records are sealed using **AES-GCM-256** with randomized 96-bit IVs and versioned `@Serializable ShellCryptionEnvelope` objects (`v=1`, `alg=AES-GCM-256`, `iv`, `ct`, `aad`).
- **Additional Authenticated Data (AAD) Binding**: Ciphertext is cryptographically bound to its unique record ID and namespace (`vault_pearls:{id}`, `vault_secure_notes:{id}`, etc.). Ciphertext cannot be transplanted between database rows or records without triggering authentication tag verification failure.
- **Fail-Closed Retrieval**: Detail decrypters fail closed via `Result.failure`. Raw JSON ciphertext is NEVER returned as plaintext fallbacks, preventing double-ciphertext re-encryption data loss.

### 2. Identity & Key Derivation (HKDF-SHA-256)
- Master cryptographic keys (`shellKey`) are derived on-device using RFC 5869 **HKDF-SHA-256**:
  $$\text{PRK} = \text{HKDF-Extract}(\text{salt} = \text{userUuid}, \text{IKM} = \text{hu-key})$$
  $$\text{OKM} = \text{HKDF-Expand}(\text{PRK}, \text{info} = \text{"clawchives-shellcryption-v1"}, L = 32)$$
- Authentication to remote ShellGuard servers sends strictly `SHA-256(hu-)` digests over TLS or private LAN routes.
- Identity keys use 64 Base62 characters (`[0-9a-zA-Z]`, 67 total length with `hu-` / `lb-` prefix).

### 3. Hardware KeyStore & Biometric Protection
- **Hardware-Backed Cryptography**: Derived session keys are sealed at rest in Android KeyStore-backed `EncryptedSharedPreferences` using AES-256-GCM.
- **Biometric Invalidation Recovery**: Keys configured with `setInvalidatedByBiometricEnrollment(true)` automatically fail closed if system fingerprints/faces are altered, routing the user to re-authenticate with their root sovereign key without data destruction.
- **Session Atomicity**: `hasActiveSession()` strictly enforces that the active symmetric key is present in memory, eliminating unauthenticated split-brain states.

### 4. Storage & Kernel Page-Size Invariants
- **Whole-Database Encryption**: All SQLite databases (`shellguard.db`) are encrypted at rest with **SQLCipher for Android 4.6.1** (AES-256-CBC with PBKDF2 HMAC-SHA512 key derivation).
- **16 KB Memory Page-Size Alignment**: Native ELF libraries (`libsqlcipher.so`) are uncompressed and aligned to 16 KB segment boundaries (`packaging.jniLibs.useLegacyPackaging = false`) in compliance with Android 15/16 kernel memory architecture.
- **CWE-400 CursorWindow Defense**: Encrypted binary attachments stream directly to `filesDir/vault_attachments/{id}.enc` using bounded 64KB heap buffers, keeping SQLite query cursors lean and preventing uncatchable `SQLiteBlobTooBigException` crashes.

### 5. Memory Hygiene, Autofill Blast-Radius & Leak Defenses
- **Window Capture Shielding**: `FLAG_SECURE` is active in production builds, blocking OS screen recording, screenshots, and task switcher thumbnail retention.
- **Sensitive Clipboard Masking (CWE-359)**: Password and secret copy actions declare `ClipDescription.EXTRA_IS_SENSITIVE = true` to suppress visual system previews on Android 13+, accompanied by a 30-second coroutine background scrub.
- **Masked Secret Display & Biometric Re-Prompt Invariant**: Sensitive secrets (passwords and secure notes) render masked by default with bullet glyphs (`••••••••••••••••`) alongside adjacent Eye-beside-Copy controls. When `reprompt = true`, toggling visibility to reveal or copying the payload MUST challenge the user via biometric or device credential prompt before releasing the secret.
- **Autofill Blast-Radius Containment & AutoSpill Defense**: Entering a WebView (`webDomain != null`) immediately invalidates outer native package matching and clears any fields captured outside the WebView. Only verified editable leaf nodes (`isEditableInputNode`, excluding `AutoCompleteTextView` URL bars) may bind `AutofillId` targets. Unlocked inline keyboard chips mask usernames (`Work · lu***@company.com`) to prevent shoulder-surfing and use zero-copy `Icon.createWithResource` references to prevent Binder `TransactionTooLargeException`.
- **IME Hardening**: All secret text inputs use `KeyboardType.Password` with `autoCorrectEnabled = false` to block third-party predictive keyboard scraping.
- **Zero External Telemetry**: Zero analytics, crash reporters, or tracking SDKs. All audit events log exclusively to the local encrypted `audit_logs` table.

### 6. Transport Security & Safe LAN Policy
- Cleartext HTTP is explicitly permitted **strictly** for private RFC 1918 local subnets (`192.168.0.0/16`, `10.0.0.0/8`, `172.16.0.0/12`), localhost, and Tailscale CGNAT addresses (`100.64.0.0/10`) to accommodate self-hosted home lab instances (Unraid, TrueNAS, Docker). Public domains strictly enforce TLS 1.3 / 1.2 with HSTS.

---

## 🚨 Reporting a Vulnerability

We deeply appreciate the efforts of security researchers and practitioners who help keep ShellGuard Mobile sovereign and secure.

### How to Report
Please report security vulnerabilities through **one of the following private channels**:
1. **GitHub Private Vulnerability Reporting (Preferred)**: Navigate to the repository's **Security** tab and click **"Report a vulnerability"**.
2. **Encrypted Security Email**: If GitHub reporting is unavailable, email our security team directly at **`security@clawstack.com`**.

> [!CAUTION]
> **Do NOT file public issues**: Never disclose security bugs, proof-of-concept exploits, or potential vulnerabilities in public GitHub issues, discussions, or pull requests.

### What to Include
To expedite investigation and remediation, please include:
- A detailed description of the vulnerability and attack vector.
- Affected application versions, build numbers, and Android OS versions / device models tested.
- Step-by-step reproduction instructions or a minimal proof of concept (PoC).
- Expected vs. actual behavior and assessed impact (e.g. data exposure, privilege escalation, bypass).
- Any proposed mitigations or patch suggestions.

---

## ⏱️ Response & Remediation SLA

Our security team adheres to the following response timeline:
- **Initial Acknowledgment**: Within **48 hours** of report receipt.
- **Triage & Reproducibility Assessment**: Within **7 business days**.
- **Status Updates**: Regular progress updates every **7 days** until resolution.
- **Coordinated Disclosure**: We aim to release a verified security patch within **30 days** of validation before public disclosure.

---

## 🚫 Out of Scope / Non-Vulnerabilities

The following scenarios are considered out of scope for our threat model:
- **Rooted / Compromised Devices**: Attacks requiring root (`su`), kernel instrumentation, or compromised Android Zygote processes where system memory isolation is fundamentally broken.
- **Physical Device Seizure without Screen Lock**: Attacks requiring physical possession of an unlocked device where the user has explicitly disabled lockscreen security or auto-lock timeouts.
- **Modified Third-Party Binaries**: Issues present exclusively in un-signed, decompiled, or repacked APKs obtained outside official GitHub Releases or Google Play tracks.
- **Theoretical Brute Force without Bypass**: Volatile memory string retention within normal JVM garbage collection lifecycles where memory cannot be inspected without root access.
- **Home Lab Subnet MITM**: Cleartext traffic interception on home lab networks where the user has deliberately chosen cleartext HTTP over private Wi-Fi instead of self-signed TLS or Tailscale WireGuard encryption.
