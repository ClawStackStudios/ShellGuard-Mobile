# This is the ShellGuard Mobile project. Project rules are as follows.

## Top-Level Invariables
- **Security-First Boundary**: Build features around security, not security around features. Trust boundary precedes consumer code.
- **Zero-Knowledge Decryption**: Client derives domain AES-GCM-256 keys via HKDF from Base62 `hu-` master sovereign key; server stores strictly opaque ciphertext envelopes.
- **Offline-First Resilience**: 100% of read, search, copy, and TOTP ticker operations function with zero network from local encrypted database; delta sync reconciles upon reconnection.
- **16 KB Memory Page Alignment**: Native libraries (`.so`) packaged uncompressed and 16 KB page-aligned (`jniLibs.useLegacyPackaging = false`).
- **Two-Layer Commit Attribution**: Every commit uses Conventional Commits with `User:` / `AI:` attribution blocks. No raw force pushes (`git push --force`).
- **Sub-Second Pre-Commit Verification**: Run `python3 scripts/audit_docs_drift.py` (<1s) locally; offload compilation and test suites to Cloud CI.

## Immutable Rules
1. **Zero Credential Leaks**: Never commit `.jks`, `.keystore`, `local.properties`, `.env`, or hardware serials.
2. **Strict AAD Binding**: Every cryptographic operation must bind associated data to record UUID and domain namespace.
3. **Session Decoupling**: Vault lock zeroizes volatile RAM secrets; preserves KeyStore-sealed session and PBKDF2 PIN state.
4. **Code-Bow Principle**: Documentation reflects physical code reality; code and governing documentation land in the same branch.
5. **Branch Isolation**: Never push directly to `main`; execute all work on scoped feature branches.

---

# 1. Project Meaning & Purpose

- **Audience**: Self-hosters and privacy-conscious users managing credentials, notes, SSH keys, attachments, and 2FA tokens via self-hosted ShellGuard servers or air-gapped devices.
- **Differentiation**: Server is untrusted storage. Full bidirectional CRUD + native system Autofill and Credential Provider services in a single client.
- **Market Position**: Intersection of Bitwarden offline resilience and sovereign zero-knowledge homelab infrastructure (ClawStack ecosystem). 1:1 parity with ShellGuard Web.
- **Mechanism**: HKDF-SHA256 across 10 AAD namespaces; Room encrypted at rest via SQLCipher 4.6.1; hybrid 64KB chunked disk storage for binary attachments; PBKDF2-SHA256 PIN manager (100,000 rounds).
- **Rationale**: AAD prevents record-swapping; chunked disk streaming bypasses Android 2MB `CursorWindow` Binder limits; KeyStore session preservation eliminates lockscreen lockouts.
- **Mission**: Absolute cryptographic sovereignty on mobile: *"Your reef. Your keys. Your vault. In your pocket."*

---

# 2. Project Rules

### Architectural Invariants
- **Runtime & Target**: Kotlin 2.2+, Jetpack Compose, Material 3, Android SDK 36 (Android 16), Min SDK 24.
- **Topology**: Clean Architecture MVI (`presentation -> domain <- data`). Feature-first decomposition.
- **Dependency Injection**: Application-scoped lazy DI (`AppContainer`). No KAPT/Hilt annotation overhead.
- **File Discipline**: Target ~250 lines per `.kt` file; 500 lines hard ceiling. One primary unit per file.
- **Network Mobility & Transport Sovereignty**: Cleartext HTTP permitted for local LAN (`<base-config cleartextTrafficPermitted="true">`). Strict platform TLS enforced by default on WAN/HTTPS. Self-signed HTTPS certificates must be supported frictionlessly via explicit in-app user consent ("Trust self-signed certificates" toggle) without forcing users to install root CAs into Android OS settings. Amber UI warning pills enforced on all unverified or cleartext endpoints.

### Invariant Redlines
| Boundary | Invariant Constraint | Observable Failure If Violated |
| :--- | :--- | :--- |
| **Heap Memory** | `zeroizeMemory()` on lock clears plaintext & keys | Plaintext secrets extractable via process heap dump (OWASP M2) |
| **Network Config** | `<base-config>` cleartext, never CIDR notation | App crashes on boot: Android rejects CIDR masks in domain tags |
| **Transport Trust** | In-app user consent toggle for self-signed TLS; never mandate OS CA installation | Homelab operators locked out of self-hosted HTTPS or burdened by alarming OS credential warnings |
| **IME Keyboards** | `KeyboardOptions(autoCorrect = false)` on inputs | Custom keyboards cache secrets to disk dictionary (OWASP M1) |
| **Ergonomics** | Minimum 48dp touch targets; `.imePadding()` on inputs | Clipped buttons; software keyboard covers active form inputs |
| **Build Economy** | Fast Python drift gate locally; offload Gradle to CI | Local workstation CPU exhaustion; build freezes during dev loops |

---

# 3. Project Patterns

### 1. Reef Modernist UI
Flat carapace: `elevation = 0.dp`, 1dp `#3D484E` borders, 12dp rounded corners, `#161B22` container fill. 6 curated accents (`REEF_DEFAULT` `#E4048A`, `CYAN_VENT` `#00D8F6`, etc.). Adaptive 3-pane topology on screens ≥ 840dp. Minimum 48dp touch targets; mandatory `.imePadding()` on input screens.

### 2. ShellCryption HKDF + AAD Binding
Every domain derives its symmetric key:
$$K_{\text{domain}} = \text{HKDF-Expand}(\text{PRK}, \text{"ShellGuard-Domain-" + ns}, 32)$$
Associated Additional Data (AAD) bound to payload:
$$\text{AAD} = \text{itemUuid} \parallel \text{ns}$$
Payloads fail GCM authentication tag verification if swapped across records or namespaces.

### 3. Session Decoupling & In-Place Re-Auth
Lock state zeroizes volatile heap memory (decrypted items, derived HKDF keys) while preserving KeyStore-wrapped session tokens and PBKDF2 PIN state in encrypted storage. Re-authentication unlocks in-place via biometrics or `PinPadView` without remote server round-trips.

### 4. Hybrid Attachment Streaming
Metadata stored in Room entity. Encrypted binary payload streamed directly to `filesDir/vault_attachments/{uuid}.enc` using 64KB buffers on `Dispatchers.IO`, bypassing Android's 2MB Binder `TransactionTooLargeException`.

### 5. Autofill Co-Presence Gate
Parser inspects child inputs directly. Suppresses weak username heuristics (Rank 4/5) when no password field is present, preventing false-positive chips on search fields. Permits email-first chips on explicit high-confidence login domains.

---

# 4. Project Navigation

> **Search Directive**: Do not memorize paths. Map both sides of the seam before cutting using grep, file searches, or sub-agents (`research`).
> **`project/` Directory Redline**: The `project/` directory contains deep domain specs, schemas, and historical scaffolds. It is large. **DO NOT** map or traverse it unless the active task explicitly demands it. Map only what is needed—never explore it simply because it is interesting.

| Domain | Search Anchor / Symbol | Key Invariant to Inspect |
| :--- | :--- | :--- |
| **Crypto** | `ShellCryptionEngine`, `PinAuthManager`, `VaultLockManager` | Constant-time PIN equality; zeroize volatile RAM on lock |
| **Storage** | `ShellGuardDatabase`, `AttachmentVaultManager` | SQLCipher encryption at rest; 64KB chunked stream buffer |
| **Network** | `ShellGuardClient`, `KtorClientProvider` | Cleartext HTTP on LAN; user-consent self-signed TLS; amber UI warning pills |
| **Sync** | `SyncRepository`, `ConnectivityMonitor` | Mutex-serialized delta reconciliation; offline-first |
| **Autofill** | `ShellGuardAutofillService`, `AutofillStructureParser` | AssistStructure child traversal; Co-Presence Gate |
| **UI** | `*ViewModel`, `*Screen`, `ui/theme/` | 0dp elevation, 1dp borders, 48dp targets, `.imePadding()` |
| **DI** | `AppContainer`, `DefaultAppContainer` | Lazy application singletons; zero annotation tax |

---

# 5. Project Skills

| Skill | Operational Scope | Trigger / Command |
| :--- | :--- | :--- |
| **`doc-automation`** | Sub-second doc drift gate & release manifests | `python3 scripts/audit_docs_drift.py` |
| **`android-mobile-design`** | Compose M3, Reef tokens, 48dp targets, IME insets | UI implementation & styling |
| **`android-16kb-sqlcipher-audit`** | 16 KB ELF segment alignment & uncompressed JNI | Packaging & database audit |
| **`android-keystore-cold-restart-testing`** | StrongBox keys & cold process survival | Biometrics & lock validation |
| **`android-headless-signing-ci`** | Headless AGP signing & dual APK/AAB distribution | CI release builds & tagging |
| **`adb-ui-input`** | Driving UI over TLS ADB on physical Pixel | On-glass verification loop |
| **`zero-knowledge-crypto`** *(Seed)* | HKDF derivation & AAD tag verification | Crypto boundary additions |
| **`room-sqlcipher-persistence`** *(Seed)* | Room migrations & CursorWindow optimization | Schema updates & sync caching |
| **`autofill-heuristics-engine`** *(Seed)* | AssistStructure parsing & inline suggestion chips | Autofill compatibility |
| **`jules-cli-orchestration`** *(Seed)* | Asynchronous subagent dispatch & PR review loops | Background task delegation |

---

# References

- **Specifications**: [ARCHITECTURE.md](ARCHITECTURE.md) · [SECURITY.md](SECURITY.md) · [DESIGN.md](DESIGN.md) · [project/crypto-and-keystore.md](project/crypto-and-keystore.md) · [project/room-storage-schema.md](project/room-storage-schema.md) · [project/routes-and-contracts.md](project/routes-and-contracts.md) · [project/autofill-service-spec.md](project/autofill-service-spec.md)
- **Roadmap & Chronicle**: [ROADMAP.md](ROADMAP.md) · [CHANGELOG.md](CHANGELOG.md)
- **Repository Rules**: [.agents/rules/android-development.md](.agents/rules/android-development.md) · [.agents/rules/git-hygiene.md](.agents/rules/git-hygiene.md) · [.agents/rules/docs-hygiene.md](.agents/rules/docs-hygiene.md) · [.agents/rules/brain.md](.agents/rules/brain.md)
- **Operational Skills**: [.agents/skills/doc-automation/SKILL.md](.agents/skills/doc-automation/SKILL.md) · [.agents/skills/android-mobile-design/SKILL.md](.agents/skills/android-mobile-design/SKILL.md) · [.agents/skills/android-16kb-sqlcipher-audit/SKILL.md](.agents/skills/android-16kb-sqlcipher-audit/SKILL.md) · [.agents/skills/android-keystore-cold-restart-testing/SKILL.md](.agents/skills/android-keystore-cold-restart-testing/SKILL.md)
