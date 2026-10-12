---
roadmap_version: 2.0.0
last_updated: 2026-10-11
meta_phase: "Feature Verification and Polish"
current_position: "Stage 1 Sub-Phase: Vault Lock & Multi-Method Authentication (Tasks 02.A & 02.B) — Hardware Unblocker"
baseline_release: "v0.0.0.11 (Build 11) Published"
target_release: "v0.0.1.0 (Build 16) Production Release Candidate"
statistics:
  description: "Meta-Phase roadmap for holistic feature verification, full server sync parity, mental flow ergonomics, and Android production polish."
  stages_completed: "██░░░░░░░░ 20%"
  stages_in_progress: "██░░░░░░░░ 20%"
---

# Master Project Roadmap — Meta-Phase: Feature Verification and Polish

> **Milestone Note**: The Genesis MVP (Phases 1–7) is 100% complete, verified on Google Pixel glass, and published through `v0.0.0.11 (Build 11)`. The historical Genesis roadmap is archived at `.agents/internal/ROADMAP_MVP_GENESIS.md`.
>
> This active roadmap defines the **Feature Verification and Polish** meta-phase. It bridges remaining data gaps against the ShellGuard Web Server (Encrypted Attachments, Multi-URI), audits and polishes the full mental flow of the mobile client, and drives towards `structural_completion` at `v0.0.1.0`.

---

## 🛡️ Systemic Operating Invariant
"Build features around security, not security around features." Each stage adheres strictly to our **Lazy Senior Developer** tight loop:
`Plan (inspect both sides of the seam) ➔ Implement (minimal, robust code) ➔ Test Code (headless test suite) ➔ Test Physical Hardware (ADB + Google Pixel glass)`.

```mermaid
flowchart TD
    classDef completedStage fill:#16231C,stroke:#10B981,stroke-width:2px,color:#F0F6FC;
    classDef activeStage fill:#1E252C,stroke:#06B6D4,stroke-width:2.5px,color:#F0F6FC;
    classDef futureStage fill:#21262D,stroke:#3D484E,stroke-width:1.5px,color:#F0F6FC;
    classDef targetMilestone fill:#1A2332,stroke:#10B981,stroke-width:2px,color:#F0F6FC;

    S1["<b>Stage 1: Full Server Parity — Encrypted Attachments & Multi-URI Bridge</b><br/>✅ Complete · Hardware-Verified (115 Tests Green)<br/>• Task 01: Multipart Upload, Storage DI & Sync Bridge<br/>• Task 02: Multi-URI, Attachment Picker & Detail Actions"]:::completedStage

    S1_SUB["<b>Stage 1 Sub-Phase: Vault Lock & Multi-Method Authentication</b><br/>⚡ <b>Active Focus</b> · Hardware Unblocker<br/>• Task 02.A: PinAuthManager (PBKDF2) & Settings UnlockMethod<br/>• Task 02.B: Security Settings UI, Reef PinPad & LockScreen Overhaul"]:::activeStage

    S2["<b>Stage 2: Mental Flow & Ergonomic Form Polish</b><br/>• Task 03: [Functionality] Cold-Start Routing, Reconnect Recovery & Sub-16ms Search<br/>• Task 04: [UI/UX] Touch Targets (≥48dp), Haptic Feedback & Keyboard Insets"]:::futureStage

    S3["<b>Stage 3: Security Lifecycle, Biometrics & Offline Resilience</b><br/>• Task 05: [Functionality] Lock Persistence Across Process Death & Offline Mutation Queue<br/>• Task 06: [UI/UX] PIN Keypad Polish, Circular Dial Feel & Panic Destruction"]:::futureStage

    S4["<b>Stage 4: In-The-Wild Autofill Verification & Release Hardening</b><br/>• Task 07: [Functionality] Browser Testing (Chrome/Brave/Firefox) & Anti-AutoSpill Audit<br/>• Task 08: [Configuration] 16 KB Page-Size Audit, R8 Obfuscation Verification & v0.0.0.15"]:::futureStage

    S5["<b>Stage 5: First-Class HTTPS Transport & The Reef Handshake</b><br/>• Task 09: [Network Security] TOFU PinningTrustManager & Instance Anchor Auto-Heal<br/>• Task 10: [UI/UX & Release] Reef Handshake QR Parser, Dual-Homed Mobility & v0.0.1.0 Candidate"]:::futureStage

    M1["<b>🏆 Structural Completion: v0.0.1.0 (Build 16)</b><br/>Hardened, production-ready Android Password Manager & Sync-Native Vault"]:::targetMilestone

    S1 --> S1_SUB --> S2 --> S3 --> S4 --> S5 --> M1
```

---

## Stage 1: Full Server Parity — Encrypted Attachments & Multi-URI Bridge

> **Stage Objective**:
> Achieve 100% data layer and synchronization parity with the ShellGuard server database across all 4 core tables (`vault_pearls`, `vault_secure_notes`, `vault_ssh_keys`, and `vault_secure_attachments`). Uphold the CWE-400 2MB CursorWindow defense by streaming ciphertext files directly to internal storage while maintaining reactive Room metadata.

- [x] **Task 01: [Functionality] Attachments Network Engine, Hybrid Storage & Sync Repository Bridge**
  - Implement attachment endpoints in `ShellGuardClient.kt`:
    - `POST /api/attachments`: Multipart/form-data upload streaming pre-encrypted ShellCryption envelope bytes with progress tracking.
    - `GET /api/attachments`: Fetch metadata list (`id`, `title`, `size_bytes`, `file_name`, `mime_type`, `category`, `created_at`).
    - `GET /api/attachments/:id/file`: Streamed chunked (1MB) ciphertext BLOB download.
    - `DELETE /api/attachments/:id`: Delete attachment record.
  - Implement hybrid file-system vault in `data/local/`:
    - Ciphertext payloads stream to `context.filesDir/vault_attachments/{id}.enc` (never inline SQLite BLOBs).
    - Room `SecureAttachmentDao` stores lightweight metadata (`size_bytes`, `file_name`, `mime_type`, `local_file_path`).
  - Wire attachments into `SyncRepository.kt`:
    - `syncAll()` pulls remote attachment metadata and prunes deleted records.
    - `pushPendingChanges()` uploads staged local attachments.
  - Plumb `attachments` JSON string into `CreateNoteRequest` and `CreateVaultItemRequest` to achieve bidirectional attachment linking without schema loss.
  - Wire `AttachmentVaultManager` into `AppContainer` DI and `VaultBackupEngine.kt`.
  - *Success Criteria*: Unit tests verify multipart payload formatting, chunked stream saving to disk, and attachment metadata delta synchronization. (Verified 100% Green).

- [x] **Task 02: [UI/UX] Multi-URI Dynamic Form Editor, Attachment Picker, Room Persistence & Hardening**
  - In `ItemFormScreen.kt`:
    - Dynamic Multi-URI list builder: add, edit, and remove multiple login URLs for a single password entry (`vault_pearls.uris`), matching Bitwarden and ShellGuard Web.
    - System file picker integration (`ActivityResultContracts.GetContent`) to stage, encrypt (AAD `vault_secure_attachments:{id}`), and link attachments to Pearls and Notes.
  - In `ItemDetailScreen.kt`:
    - Multi-URI list display with launch-in-browser and copy actions.
    - Linked attachment section: file name, extension icon badge, humanized byte size, and tap-to-decrypt/share/view intent.
    - Enforce biometric Claw Re-Prompt gate on attachments when `state.reprompt == true`.
    - Resolve native `androidapp://` package URIs via `getLaunchIntentForPackage`.
  - In `VaultDashboardScreen.kt`:
    - Add `[Attachments]` Pod category filter chip and standalone attachment cards.
  - Update `DomainMatcher` to evaluate all configured `uris` for domain matching during autofill.
  - **Hotfix & Hardening**: Fixed Room persistence of `attachments` and `uris` during item edit saves in `SyncRepository.kt`; wired decrypted cache auto-scrubbing across cold boot, lock, logout, and emergency wipe; added cleartext HTTP amber transport warning pills.
  - *Success Criteria*: User can attach files in mobile, verify they upload and appear on web client, and view multi-URI entries in detail and autofill. (Verified on Google Pixel glass).

---

## Stage 1 Sub-Phase: Vault Lock Responsiveness, PIN Security & In-Place Re-Authentication

> **Sub-Phase Objective**:
> Resolve the physical device Lock Screen freeze caused by the `MainActivity` overlay trap. Introduce a full multi-method authentication system allowing users to unlock an active session via **Biometrics**, **PIN Code (4–8 digits)**, or **Sovereign Master Key / ClawKey File re-entry** without destroying active session state, losing form edits, or dropping deep links.

- [ ] **Task 02.A: [Crypto & Storage] PinAuthManager, SettingsRepository UnlockMethod & Master Key Validation**
  - In `data/local/SettingsRepository.kt`:
    - Declare `enum class UnlockMethod { BIOMETRIC, PIN, MASTER_KEY }`.
    - Add preference keys: `pref_unlock_method`, `pref_has_pin_configured`, and `pref_max_pin_attempts`.
    - Add fields to `AppSettings`: `unlockMethod`, `hasPinConfigured`, and `maxPinAttempts` (default 5).
    - Expose mutation methods: `setUnlockMethod()`, `setHasPinConfigured()`, and `setMaxPinAttempts()`.
  - Create `crypto/PinAuthManager.kt` (`com.clawstack.shellguard.crypto`):
    - Generate 16-byte cryptographically secure random salt (`SecureRandom`).
    - Derive 32-byte key via `PBKDF2WithHmacSHA256` (100,000 iterations).
    - Implement constant-time comparison via `MessageDigest.isEqual` to prevent timing attacks.
    - Implement attempt counting with lockout threshold triggering automatic fallback to Master Key.
  - In `crypto/EncryptedDeviceVault.kt`:
    - Store `pin_salt`, `pin_hash`, and failed attempt counter in hardware-backed storage.
    - Add `verifyMasterKey(huKey: String): Boolean` checking `ClawCrypto.hashHumanKey(huKey) == getHashedKey()`.
    - Add `verifyClawKeyJson(json: String): Boolean` extracting key and checking against `getHashedKey()`.
  - *Success Criteria*: Unit tests verify PBKDF2 salt derivation, constant-time verification, invalid PIN rejection, attempt threshold lockout, and sovereign key hash matching.

- [ ] **Task 02.B: [UI/UX] Security Settings Taxonomy, Reef PinPadView, LockScreen Hub & Overlay Seam**
  - In `ui/components/PinPadView.kt`:
    - Reusable Reef Modernist numeric keypad (0–9 digits, backspace, clear) with minimum 48dp touch targets.
    - Animated masked dot indicators (hollow circle at rest, glowing `ReefPink` or `BrandClawCyan` circle when filled).
    - Tactile haptic feedback on digit tap; shake animation on incorrect entry.
  - In `ui/screens/settings/SettingsSecurityScreen.kt`:
    - Add new **`VAULT UNLOCK METHOD`** section at the top of the LazyColumn above Auto-Lock.
    - Radio options for `Biometrics`, `PIN Code`, and `Master Key / ClawKey`.
    - PIN Setup modal: 2-phase entry ("Enter New PIN" ➔ "Confirm PIN") with error feedback.
    - Configuration card for `Max Failed PIN Attempts` (3, 5, 10).
  - In `ui/screens/lock/LockScreen.kt`:
    - Multi-method unlock presentation responding reactively to configured `UnlockMethod`:
      - If `PIN`: Render `PinPadView` for instantaneous unlock without soft keyboard.
      - If `BIOMETRIC`: Render fingerprint prompt button with fallback action.
      - If `MASTER_KEY` (or fallback tapped): Render inline modal for pasting `hu-` sovereign key or uploading `clawkey.json` file.
    - Preserve bottom "Log Out & Switch Server" button for explicit logout.
  - In `MainActivity.kt`:
    - Resolve the overlay trap: Master Key re-authentication executes in-place within `LockScreen`.
    - Upon successful PIN or Master Key validation, `vaultLockManager.unlockVault()` emits `false`, immediately dismissing the overlay and revealing the underlying active destination intact.
  - *Success Criteria*: Lock screen is fully responsive on physical Google Pixel glass; PIN unlocks vault immediately; Master Key re-entry unlocks vault without logging out; and "Log Out & Switch Server" still navigates cleanly to Gateway.

---

## Stage 2: Mental Flow & Ergonomic Form Polish

> **Stage Objective**:
> Eliminate cognitive friction, latency, and layout quirks across the app's three primary views (Gateway, Dashboard, Universal Form). Ensure seamless transitions, instant search, and ergonomic keyboard and touch interactions.

- [ ] **Task 03: [Functionality] Cold-Start Routing, Reconnect Recovery & Sub-16ms Search Performance**
  - Benchmark and optimize `observeUnifiedItems` search filtering across 100+ items to guarantee sub-16ms frame times.
  - Harden Gateway cold-start routing: validate saved Base62 `hu-` sovereign key format, auto-fill server host/port, and transition immediately to local offline cache with an amber status banner if the server is unreachable.
  - Implement automated network reconnect recovery: seamlessly transition from `OFFLINE_READ_ONLY` to `SYNCING` ➔ `ONLINE_SYNCED` without UI stutter.
  - *Success Criteria*: Cold launch with stored session navigates in <300ms; offline search responds instantaneously; reconnect syncs automatically.

- [ ] **Task 04: [UI/UX] Touch Targets (≥48dp), Haptic Feedback & Keyboard Insets**
  - Audit all interactive surfaces against `mobile-android-design`: ensure all buttons, toggles, icon buttons, and list items have touch targets ≥ 48dp.
  - Implement subtle, tactile haptic feedback on secret reveal, copy actions, and password generation.
  - Polish soft keyboard auto-scroll behavior (`.imePadding()` + `LaunchedEffect` scroll-to-focused): ensure active text fields remain comfortably visible above Gboard on all screen sizes.
  - Refine Secure Notes markdown rendering with clean monospace code blocks and formatted headers.
  - *Success Criteria*: Typing on physical Pixel never obscures input fields; touch targets feel effortless; copy actions produce distinct haptic confirmation.

---

## Stage 3: Security Lifecycle, Biometrics & Offline Resilience

> **Stage Objective**:
> Harden background lifecycle, KeyStore biometric unsealing, timeout enforcement, and emergency wipe cascades. Guarantee zero data loss when modifying items offline.

- [ ] **Task 05: [Functionality] Lock Persistence Across Process Death & Offline Mutation Queue**
  - Verify and harden `VaultLockManager` background timestamp tracking: ensure killing the app from recents or OS low-memory termination enforces the configured timeout upon relaunch.
  - Test and verify offline mutation queueing in `SyncRepository`: items created, edited, or deleted in Airplane Mode must persist locally through app restarts and flush cleanly to the server on reconnect without resurrecting deleted items.
  - Verify fail-closed decryption across all domains: corrupt or untrusted payloads must never leak ciphertext strings into editable form fields.
  - *Success Criteria*: Process kill enforces lock; offline creates/edits/deletes sync cleanly upon reconnect without data loss.

- [ ] **Task 06: [UI/UX] PIN Keypad Polish, Circular Dial Feel & Panic Destruction**
  - Polish `LockScreen.kt` numerical PIN keypad: responsive press states, masked PIN dots, and instantaneous fallback to Master ClawKey.
  - Polish `CircularDialPicker` gesture physics: smooth continuous arc dragging with clear duration snap points (5s–60s).
  - Verify `PanicPurgeCountdownScreen`: 3 pulsing concentric Canvas rings, 68sp monospace timer, hardware back button cancellation, and full 4-step fail-closed destruction cascade.
  - *Success Criteria*: PIN keypad unlocks reliably on device; panic countdown can be cancelled or executed cleanly, leaving zero database or key remnants.

---

## Stage 4: In-The-Wild Autofill Verification & Release Hardening

> **Stage Objective**:
> Rigorously verify system autofill in real third-party applications and browsers, audit Android 15+ 16 KB page-size compliance and ProGuard R8 rules, and package the `v0.0.1.0` production candidate.

- [ ] **Task 07: [Functionality] Real-World Browser & Native App Autofill Gauntlet**
  - Exercise system autofill across physical browsers (Google Chrome, Brave, Firefox) and native Android apps:
    - 2-step split login flows (`accounts.google.com`, Microsoft): verify Step 1 displays `"Add Item"` and Step 2 fills password.
    - Option B subtitle masking (`Work · lu***@company.com`): verify zero raw username leakage above keyboard.
    - Multi-field dataset injection: verify single tap fills both username and password simultaneously.
    - Anti-AutoSpill WebView isolation: verify WebView forms cannot inherit credentials from outer native views.
  - Verify direct Settings deep-link from `SettingsAutofillScreen` opens system autofill picker cleanly.
  - *Success Criteria*: Autofill triggers reliably across browsers and apps without UI flicker or crashes; sensitive credentials fill accurately.

- [ ] **Task 08: [Configuration & Release] 16 KB Page Alignment, ProGuard Audit & v0.0.0.15 Release**
  - Audit uncompressed native packaging: verify SQLCipher `.so` native libraries pass 16 KB ELF alignment verification (`16kb-page-size-alignment-guide.md`).
  - Audit R8 ProGuard preservation rules: verify release builds preserve Room DAOs, SQLCipher JNI bindings, and Kotlinx Serialization models.
  - Execute full regression test gauntlet (`./gradlew testDebugUnitTest assembleDebug`) verifying 100% green status.
  - Author `RELEASE-v0.0.0.15.md` and format `<en-US>` release notes in `RELEASE-PLAY.md`.
  - Bump monotonic `versionCode` and update `versionName` to `v0.0.0.15 (Build 15)`.
  - *Success Criteria*: Full test suite green; release APK compiles cleanly; 16 KB alignment verified; R8 rules verified against reflection bugs.

---

## Stage 5: First-Class HTTPS Transport & The Reef Handshake

> **Stage Objective**:
> Achieve production-grade **Structural Completion (`v0.0.1.0`)** by solving the cleartext transport dilemma through a bespoke, mobile-first sync architecture. Implement the **Reef Handshake QR Pairing protocol** in CameraX, replace permissive trust managers with an active **Trust-On-First-Use (TOFU) Pinning Engine**, and implement the **Instance Identity Anchor** to silently auto-heal through server certificate rotations without developer friction.

- [ ] **Task 09: [Network Security] TOFU PinningTrustManager & Instance Anchor Auto-Heal Engine**
  - Implement `PinningTrustManager` in `com.clawstack.shellguard.data.remote.security`:
    - SHA-256 fingerprint validation of server leaf certificate against `EncryptedDeviceVault`.
    - Auto-heal validator querying `/api/public/tls-manifest` when a rotated certificate is detected.
    - ECDSA-SHA256 signature verification of new fingerprint using pinned `instancePublicKey`.
    - Monotonic timestamp freshness enforcement (±10 minute clock skew tolerance).
    - Automatic, silent update of pinned fingerprint in `EncryptedDeviceVault` on valid rotation proof.
    - Fail-closed security redline: throw `UntrustedCertificateException` on invalid, expired, or unsigned certificates.
  - Configure `KtorClientProvider`: attach `PinningTrustManager` to OkHttp engine while preserving cleartext amber warnings for legacy setups.
  - *Success Criteria*: Unit tests pass for matching fingerprints, legitimate auto-healing rotations, and fail-closed rejections on forged signatures.

- [ ] **Task 10: [UI/UX & Release] Reef Handshake QR Parser, Dual-Homed Mobility & v0.0.1.0 Candidate**
  - Implement `shellguard://pair` URI ingestion in `QrScannerViewModel` and `QrScannerScreen`:
    - Parse `lan`, `ts`, `fp`, `ik`, and `alias` parameters from scanned QR code.
    - Persist pinned certificate fingerprint and instance public key in `EncryptedDeviceVault`.
    - Store `primaryLanUrl` and optional `fallbackMeshUrl` (Tailscale) connection profile.
    - Navigate to `GatewayScreen` pre-configured with HTTPS protocol and server parameters.
  - Polish `GatewayScreen.kt`:
    - Add "Scan Reef Pairing QR" visual button with QR icon above manual inputs.
    - Status indicator: render green shield badge `🔒 Verified Reef Connection` with fingerprint preview.
    - Dual-homed network probe: test `primaryLanUrl` first (1200ms timeout); automatically route to `fallbackMeshUrl` if unreachable.
  - Verification & Release:
    - Execute full regression test gauntlet (`./gradlew testDebugUnitTest assembleDebug`) verifying 100% green status.
    - Author production release manifest `RELEASE-v0.0.1.0.md` and format `<en-US>` release notes in `RELEASE-PLAY.md`.
    - Bump monotonic `versionCode = 16` and update `versionName = "0.0.1.0"`.
  - *Success Criteria*: 1-tap QR pairing configures secure connection; auto-healing verifies on physical Pixel; dual-homed fallback transitions smoothly; `structural_completion` achieved.

