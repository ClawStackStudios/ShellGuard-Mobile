# Ratified System Patterns: ShellGuard Mobile

## pattern: hybrid-attachment-filesystem-vault
**weight**: 3 | **last validated**: 2026-09-24 | **first observed**: 2026-09-24

Never store multi-megabyte attachment blobs inside SQLite database rows. Android's SQLite CursorWindow limits query rows to 2MB, causing unavoidable `CursorWindowAllocationException` or `SQLiteBlobTooBigException` crashes on SELECT. Decouple metadata into Room and stream encrypted ciphertext directly to private internal disk files (`filesDir/vault_attachments/{id}.enc`).

**History:**
- 2026-09-24: Audited `SecureAttachmentEntity` against web server 500MB attachment specification; realized inline BLOBs would cause fatal app crashes under Android SQLite CursorWindow limitations; refactored to Hybrid File-System Vault.

**Shaped perspective:** Databases are indexes, not file systems. Mobile relational databases should store structure and searchable pointers; the OS filesystem should store the weight.

---

## pattern: bitwarden-model-readonly-offline-caching
**weight**: 4 | **last validated**: 2026-09-26 | **first observed**: 2026-09-24
**pinned**: false
**status**: hot

When a mobile client is disconnected from its authoritative server, vault operations must be strictly Read-Only. Allow 100% read, search, copy, autofill, and TOTP functionality from the local encrypted cache, but block creation, editing, and deletion in the UI. Automatically probe server health via `NetworkCallback` upon connection restoration to resume online synchronization seamlessly.

**History:**
- 2026-09-24: Evaluated optimistic offline write queues vs read-only offline caching. Validated against Bitwarden's client-server architecture; confirmed that blocking offline mutations eliminates split-brain conflicts and guarantees vault integrity.
- 2026-09-26: Re-validated during Phase 2 Ktor client and SyncRepository implementation; mutation guards and ConnectivityMonitor live-tested on physical Pixel.

**Shaped perspective:** This holds because zero-knowledge encryption prevents the client or server from inspecting plaintext to merge diverged offline diffs intelligently. It would break if client users demanded multi-device offline write capabilities without an active authoritative connection. What it costs to maintain is strict UI gating (disabled FABs, dimmed edit/delete icons) and an automated background health probe to detect network restoration.

---

## pattern: cwe-359-sensitive-clipboard-masking
**weight**: 3 | **last validated**: 2026-09-26 | **first observed**: 2026-09-24
**pinned**: false
**status**: hot

All password, key, and TOTP clipboard operations must declare `ClipDescription.EXTRA_IS_SENSITIVE = true` on Android 13+ (API 33+) to suppress floating system thumbnail previews, paired with an automated 30s/60s background coroutine timer to purge transient secrets from the system pasteboard.

**History:**
- 2026-09-24: Identified CWE-359 clipboard leakage on Android 13+; codified in `crypto-and-keystore.md`.
- 2026-09-25: Audited against Bitwarden Android implementation; added configurable 30s/60s/120s timer settings in `ui-ux-design-system.md` §10.
- 2026-09-26: Enforced across `VaultDashboardScreen` and `SyncRepository` copy actions during live device verification.

**Shaped perspective:** This holds because modern mobile operating systems render persistent visual thumbnail previews of clipboard data that expose sensitive credentials to screen recorders, recents caches, and shoulder surfers. It would break if OEM background process killing abruptly terminates the scrubbing coroutine before the timer completes. What it costs to maintain is managing background lifecycle coroutines and educating users why copied credentials disappear from the clipboard after 30 seconds.

---

## pattern: zero-knowledge-session-atomicity
**weight**: 3 | **last validated**: 2026-09-27 | **first observed**: 2026-09-24
**pinned**: false
**status**: hot

An active mobile vault session must atomically couple transport authorization (`sessionToken`) with cryptographic capability (`shellKey`). `hasActiveSession()` must strictly verify `getInMemoryShellKey() != null`. Derived 32-byte symmetric keys must be persisted at rest in hardware KeyStore-backed `EncryptedSharedPreferences` (AES-256-GCM) with dynamic RAM re-hydration to survive Android process death without user lockout. On lock or logout, both volatile RAM references and persisted KeyStore preferences must be actively zeroized.

**History:**
- 2026-09-24: Formulated in `architecture.md` §4 as a core zero-knowledge invariant.
- 2026-09-26: Diagnosed split-brain failure on physical Pixel where RAM key was lost across restarts while session token survived; persisted `shellKey` in KeyStore `EncryptedSharedPreferences` and hardened `hasActiveSession()`.
- 2026-09-27: Re-validated during Phase 4 biometric lock and background timeout testing on hardware.

**Shaped perspective:** Mobile zero-knowledge architecture cannot assume persistent memory. Because the mobile operating system aggressively reclaims background process memory, separating authentication from decryption capability creates split-brain states where the UI appears unlocked but cannot read data. Persisting the derived symmetric key inside the hardware KeyStore enclave preserves the zero-knowledge guarantee at rest while preventing user disruption across cold restarts.

---

## pattern: cwe-359-ime-protection-and-inset-isolation
**weight**: 3 | **last validated**: 2026-09-27 | **first observed**: 2026-09-24
**pinned**: false
**status**: hot

All cryptographic and secret input fields (passwords, PINs, seeds, keys) must apply `PasswordVisualTransformation()` and `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` to prevent predictive dictionary learning. Interactive form screens must apply `.imePadding().verticalScroll(rememberScrollState())` to prevent the soft keyboard from obscuring inputs, while the root `Scaffold` must configure `contentWindowInsets = WindowInsets(0, 0, 0, 0)` to prevent destructive double-subtraction of IME height. `FLAG_SECURE` window shielding must be scoped strictly to release builds (`!BuildConfig.DEBUG`) to prevent Adreno GPU compositor blackouts over system keyboard overlays.

**History:**
- 2026-09-24: Specified CWE-359 keyboard telemetry prevention in `architecture.md` and `ui-ux-design-system.md`.
- 2026-09-26: Diagnosed Adreno GPU blackout and double keyboard inset subtraction on physical Pixel; scoped `FLAG_SECURE` and isolated root Scaffold insets.
- 2026-09-27: Verified universal form keyboard handling and secret input masking during Phase 4 live testing on hardware.

**Shaped perspective:** IME input on Android is an inter-process IPC boundary subject to GPU compositor limitations, keyboard service logging, and system inset negotiation. Failing to isolate root insets causes keyboard crushing, while unconditional `FLAG_SECURE` on legacy hardware drivers blanks out the entire window during text entry. Defensive UI design treats the keyboard overlay as a distinct external surface that must be isolated at both the view and window levels.

---

## pattern: base62-sovereign-key-parity
**weight**: 3 | **last validated**: 2026-09-27 | **first observed**: 2026-09-26
**pinned**: false
**status**: hot

ShellGuard master identity keys (`hu-`) and agent keys (`lb-`) use 64 alphanumeric Base62 characters (`[0-9a-zA-Z]`, 67 total string length). Enforcing lowercase hexadecimal validation (`[0-9a-f]`) falsely rejects authentic web-generated credentials and locks mobile users out of their vaults. All client regex patterns, form validators, and key decoders must accept the full Base62 character space.

**History:**
- 2026-09-26: Diagnosed disabled login button on physical Pixel despite valid JSON identity file loaded; traced to `[0-9a-f]` regex in `ClawCrypto` rejecting uppercase letters in web-generated `hu-` keys. Upgraded regex to Base62.
- 2026-09-26: Held in accumulating register during first dream cycle (weight 2/3).
- 2026-09-27: Re-validated during Phase 4 CameraX QR scanning, TOTP secret parsing, and codified as a load-bearing redline in `testOracle.md`.

**Shaped perspective:** This holds because cryptographic identity formats are dictated by the sovereign web authority that mints them, not the downstream mobile consumer. It would break if the core ShellGuard cryptographic specification altered its key-generation entropy encoding away from Base62. What it costs to maintain is ensuring that any future input masks, validators, or QR parsers consistently test against mixed alphanumeric strings rather than assuming standard hex byte serialization.

---

## pattern: claw-re-prompt
**weight**: 3 | **last validated**: 2026-09-27 | **first observed**: 2026-09-24
**pinned**: false
**status**: hot

Designated high-security items (`reprompt == true`) must enforce an explicit biometric or PIN re-verification gate before unmasking secrets, copying credentials to clipboard, or filling inputs, even when the parent vault is already in an unlocked state.

**History:**
- 2026-09-24: Formulated claw re-prompt specification in `architecture.md` and `room-storage-schema.md` for high-risk corporate and financial credentials.
- 2026-09-26: Implemented in `ItemDetailScreen` and `ItemDetailViewModel`, gating secret visibility and clipboard export behind hardware biometric challenge.
- 2026-09-27: Extended into `AutofillAuthActivity` transparent gate, intercepting autofill requests for re-prompt items and demanding biometrics before emitting autofill datasets.

**Shaped perspective:** This holds because vault unlock is a coarse-grained perimeter defense, whereas credentials inside a vault possess heterogeneous threat levels. A compromised device left unlocked on a desk or handed to a colleague breaches all items unless individual high-value pearls require re-authentication. What it costs to maintain is an extra cryptographic/biometric challenge state machine in detail views and autofill flows, along with educating users on why certain items challenge them again.


