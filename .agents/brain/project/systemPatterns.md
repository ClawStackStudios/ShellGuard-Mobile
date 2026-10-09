# System Patterns: ShellGuard Mobile

## Architectural Pattern: Unidirectional Data Flow (MVI)
```
UI (Compose) ──(UserIntent)──> ViewModel ──> UseCase ──> Repository ──> Local Room / Remote Ktor
     ▲                                                                            │
     └────────────────────────── StateFlow<State> ────────────────────────────────┘
```
- **UI Layer**: Jetpack Compose + Material 3. Pure rendering of immutable `StateFlow<State>`. Dispatches `UserIntent` events to ViewModels. Zero business logic in composables.
- **ViewModel**: Gathers Room flows, manages UI state, interacts with UseCases/Repositories.
- **Domain Layer**: Pure Kotlin UseCases for validation, key derivation, and business rules.
- **Data Layer**: Repositories managing offline caching (Room + SQLCipher) vs remote communication (Ktor).

## Storage Patterns: Hybrid File-System Vault
- **CursorWindow Invariant (CWE-400)**: Android enforces a hard 2MB `CursorWindow` limit on SQLite query rows. 
- **Decoupled Architecture**: Multi-megabyte file attachments are strictly prohibited from inline BLOB columns in Room. Room stores only metadata (`sizeBytes`, `mimeType`, `sha256Checksum`, `localFilePath`). Encrypted payload bytes stream directly to `context.filesDir/vault_attachments/{id}.enc` via `CipherInputStream` and `CipherOutputStream`, bounding memory allocations to <= 64KB buffers.

## Offline Patterns: Bitwarden-Model Read-Only Access & Seamless Reconnection
- **Server as Single Source of Truth**: The central Express 5 server owns vault mutations.
- **Encrypted Local Cache**: Full vault is mirrored in local SQLCipher Room storage for offline resilience.
- **Read-Only Invariant While Disconnected**:
  - Full access to view, search, copy passwords (masked), view custom fields, run TOTP tickers, and execute system Autofill.
  - Mutations (create, edit, delete) are strictly disabled in the UI (FAB disabled, action buttons dimmed) to eliminate split-brain sync conflicts.
  - Top bar displays amber `OfflineReadOnly` banner.
- **Seamless Reconnection Pipeline**: Android `ConnectivityManager.NetworkCallback` automatically probes `GET /api/health` upon network return, transitions to `OnlineSynced`, clears the banner, runs a downstream delta pull, and restores write actions without user friction.
- **Lock vs. Logout Invariant**: Locking zeros keys from RAM but retains the encrypted database for offline unlock. Logging out scrubs the local database.

## Cryptographic & Security Patterns
- **Triple-Layer Boundary**:
  - Layer 1: Client-Side ShellCryption (HKDF-SHA256 + AES-GCM-256) with domain-specific AAD namespaces.
  - Layer 2: Server-side per-row metadata encryption (handled transparently by Express 5).
  - Layer 3: SQLCipher whole-database encryption at rest with Android KeyStore-managed passphrase.
- **Claw Re-Prompt Guardrail**: Designated high-security items (`reprompt == true`) enforce a biometric or PIN re-verification gate before the user can reveal hidden secrets or copy them to the clipboard, even when the vault is already unlocked. *(see [long-term/patterns.md § pattern: claw-re-prompt](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/.agents/brain/long-term/patterns.md))*.
- **Biometric Recovery State Machine**: Hardware biometric keys invalidated by new biometric enrollments (`KeyPermanentlyInvalidatedException`) automatically route to Master Password/PIN fallback to regenerate keys without user lockout or data loss.
- **Zero-Knowledge Session Atomicity & KeyStore Key Persistence**: Active sessions atomically couple transport authorization (`sessionToken`) with cryptographic capability (`shellKey`). `hasActiveSession()` strictly verifies `getInMemoryShellKey() != null`. Derived 32-byte symmetric keys are persisted at rest in hardware KeyStore-backed `EncryptedSharedPreferences` (AES-256-GCM) with dynamic RAM re-hydration to survive Android process death without user lockout. Server connection parameters (`protocol`, `host`, `port`) are preserved and pre-filled upon session fallback to ensure frictionless re-entry. *(see [long-term/patterns.md § pattern: zero-knowledge-session-atomicity](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/.agents/brain/long-term/patterns.md))*.
- **Pre-DAO Fingerprint Deduplication**: `title | username | secret` hash comparison before database writes prevents duplicate false negatives.
- **Fail-Closed Structural Envelope Validation**: All cryptographic retrieval and deserialization gates fail closed immediately upon anomaly or unencrypted payloads. *(see [long-term/patterns.md § pattern: fail-closed-structural-envelope-validation](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/.agents/brain/long-term/patterns.md))*.

## Settings Hub & Persistence Architecture
- **DataStore Reactive Pipeline & Deterministic ViewModel Synchronization**: `SettingsRepository` binds user preferences into a persistent, reactive `Flow<AppSettings>` using `androidx.datastore:datastore-preferences:1.1.3`, seeded with `.onStart { emit(AppSettings()) }` inside `combine(...)` pipelines and paired with `Job`-returning ViewModel mutations and `clearAll()` test isolation. *(see [long-term/patterns.md § pattern: deterministic-datastore-viewmodel-synchronization](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/.agents/brain/long-term/patterns.md))*
- **Category-to-Subscreen Navigation**: Settings navigation uses a sealed `Screen.Settings*` hierarchy, organizing configuration into 6 dedicated sub-screens (Appearance, Security, Sync, Autofill, Backup, About) accessible from the master dashboard overflow menu.

## Emergency Panic Purge & Security Control Patterns
- **Trigonometric Dial Duration Picker (`CircularDialPicker`)**: Canvas clock-face dial with atan2 gesture mapping for selecting emergency countdown durations clamped between 5s and 60s.
- **Fail-Closed 4-Step Destruction Cascade**: Emergency purge executes a deterministic cascade: (1) wipes all Room database tables via `clearAllTables()`, (2) purges session tokens and symmetric keys from KeyStore `EncryptedSharedPreferences`, (3) clears DataStore preferences, and (4) unlocks vault state to force clean re-entry.
- **Abortable Concentric Ring Countdown**: Full-screen emergency countdown screen renders 3 pulsing concentric red Canvas rings and a 68sp monospace timer that can be safely cancelled via on-screen button or hardware back gesture prior to expiry.

## Web-Parity Backup & Migration Engine Patterns
- **Polymorphic Payload Schema**: Serializes backups into a unified `items: List<BackupVaultItem>` array with string type discriminators ("password", "note", "key") and ISO timestamps, achieving 100% interoperability with ShellGuard Web's `ImportExportView`.
- **Dual-Protection Derive Pipeline**: Supports both `ACTIVE_KEY` derivation (HKDF-SHA256 from the active 67-char Base62 `hu-` sovereign key) and `CUSTOM_PASSPHRASE` derivation (PBKDF2-SHA256 with 600,000 iterations).
- **Format Sniffing & Backward Ingestion**: `detectBackupFormat` distinguishes ShellGuard encrypted envelopes, legacy mobile backups, and Bitwarden JSON files. The deserializer automatically falls back to reading legacy segregated collections (`pearls`, `notes`, `sshKeys`) if modern `items` are absent.

## Autofill & System Integration Patterns
- **Configurable URI Match Detection (`UriMatchMode`)**: 5 matching algorithms (`BASE_DOMAIN`, `HOST`, `EXACT`, `STARTS_WITH`, `NEVER`) supporting multi-tenant subdomains and exact port matching for local home labs.
- **5-Tier Confidence-Ranked View Traversal (`AutofillStructureParser`)**: Evaluates `AssistStructure` nodes through a strict 5-tier confidence hierarchy (`RANK_EXPLICIT_HINT = 1` → `RANK_HTML_INPUT = 2` → `RANK_INPUT_TYPE = 3` → `RANK_HEURISTIC_ID = 4` → `RANK_PROXIMITY = 5`) so explicit hints and `<input>` attributes always override generic layout prefixes.
- **Strict Editable-Input Gate & Password/Username Mutual Exclusion**: Only editable leaf controls (`EditText`, `<input>`, or explicit `autofillType != AUTOFILL_TYPE_NONE`, excluding `AutoCompleteTextView` browser URL bars) can claim `usernameId` or `passwordId`, preventing `<form>`/`<div>` containers from hijacking autofill targets. Password signals return immediately so composite identifiers (`login_password`) never fall through to overwrite `usernameId`.
- **Co-Presence Blast-Radius Gate & 2-Step Login Support**: When `passwordId == null`, `AutofillStructureParser` strips low-confidence Rank 4/5 username heuristics to prevent false-positive keyboard suggestions on search bars and chat inputs, while preserving explicit Rank 1–3 email/username fields so 2-step split login flows (e.g., `accounts.google.com`) still receive matched account chips or the Case A `"Add Item"` chip (`userFieldId != null || passFieldId != null`).
- **Simultaneous Multi-Field Dataset Binding**: Both unlocked and locked/reprompt `Dataset` builders bind `usernameId` (only when `pearl.username.isNotBlank()`) and `passwordId` simultaneously, enabling 1-tap full-credential population from either field without clobbering user-typed text with empty strings.
- **Option B Masked Chip Disambiguation & Zero-Copy Icons (`AutofillInlineHelper`)**: Unlocked inline keyboard chips format subtitles with non-default category/tag + partially masked username (`Work · lu***@company.com`) and bind icons exclusively via `Icon.createWithResource` to stay strictly under the 1 MB Android Binder IPC limit.
- **AutoSpill Boundary & Context-Aware Routing**: *(see [long-term/patterns.md § pattern: context-aware-autofill-and-blast-radius-gating](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/.agents/brain/long-term/patterns.md))*
- **Lifecycle & Cancellation Invariants**: All database queries and cryptographic derivations in `AutofillService` run on `Dispatchers.IO` and continuously evaluate `cancellationSignal.isCanceled` to prevent ANRs and orphan processing.
- **Defensive SaveInfo Form Intake**: Login and registration forms emit `SaveInfo` targeting explicit username and password IDs without leaking presentation labels.
- **Quick Settings Tile & Glance AppWidgets**: `TileService` for instant search/generation and Jetpack Compose Glance 2x2/4x2 widgets for pinned logins and live TOTP codes.
- **Context-Aware Locked Suggestions & Global Lock Overlay**:
  - **Locked Inline Presentation**: When the vault is locked and domain matches exist, the IME inline strip displays the normalized domain string with `"Unlock ShellGuard"` subtitle and lock icon (`ic_locked_shell`), signaling site recognition without leaking usernames or secret titles.
  - **Zero-Match "Add Item" Fallback**: When 0 matches exist and `userFieldId != null || passFieldId != null` (including explicit Rank 1–3 two-step login pages), the service displays exclusively an `"Add Item"` chip.
  - **Translucent Activity Flash Mitigation & Overlay Routing**: Tapping `"Add Item"` launches `MainActivity` directly with `EXTRA_AUTOFILL_SAVE_MODE = true`, pre-populating the form underneath the global `LockScreen` overlay and `finish()`-ing back to the host app on save.

## UI & Theming Patterns: Reef Modernist Mobile
- **Bioluminescent Defense Aesthetic**: Dual-mode Abyssal Dark (`#0F1419` base, `#171C21` surface) and Ocean Mist (`#F1F5F9` base, `#FFFFFF` surface) with signature Lobster Red (`#E4048A`) and Claw Cyan (`#06B6D4`) neon conduits.
- **Exoskeletal Shells (Flat Material 3)**: Cards use `16dp` rounded corners, crisp 1dp `#3D484E` carapace borders, and zero artificial drop shadows (`elevation = 0.dp`), relying on tonal contrast and borders for depth.
- **Dynamic Theme Engine**: 6 curated theme palettes (`REEF_DEFAULT`, `CYAN_VENT`, `PURPLE_SHELL`, `EMERALD_TRENCH`, `AMBER_FLARE`, `MONOCHROME`) injected through `CompositionLocalProvider(LocalShellGuardColors)`.
- **Adaptive Master-Detail Scaffolding**: Automatically transitions between single-column navigation on phones and three-pane Bitwarden-style desktop parity on tablets and foldables (Folder Pods Sidebar -> Item List -> Detail Inspection Pane).
- **Tactile & Motion Ergonomics**: Spring scale press physics (`0.97f` scale down with damping `0.75f`), tactile haptic feedback on copy/long-press, and depleting circular Canvas countdown rings with dynamic color interpolation.
- **Form Ergonomics & CWE-359 IME Hardening**: Pinned header/footer forms, upward-expanding dropup menus, `.imePadding().verticalScroll()`, and `PasswordVisualTransformation` + `KeyboardType.Password` (auto-correct disabled) on all secret inputs. *(see [long-term/patterns.md § pattern: cwe-359-ime-protection-and-inset-isolation](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/.agents/brain/long-term/patterns.md))*.

## Development & Agent Orchestration Patterns
- **Four Specialized Engineering Mental Sub-Processes**:
  - ⚡ **Bolt**: Android Performance Specialist (Compose recomposition loops, 16 KB native alignment, 64KB crypto streaming, Room IO dispatching).
  - 🎨 **Palette**: Android UI/UX & Design Specialist (Reef Modernist styling, flat Material 3 carapace, 6 theme accents, soft keyboard `.imePadding()` defense, 3-pane tablet ergonomics).
  - 🛡️ **Sentinel**: Android Zero-Knowledge Security Specialist (HKDF + AES-GCM across 10 AAD namespaces, KeyStore hardware biometric binding, atomic session validity, CWE-359 clipboard/IME defenses, SQLCipher at rest).
  - 📘 **Scribe**: Android Documentation & Memory Cartographer (Code-derived architectural blueprints, runnable Gradle/ADB commands, release notes, Play Store listings, Brain synchronization).
- **Documentation Testing & Build Exemption**: Markdown doc files are excluded from requiring a build or test run (**NO TESTING REQUIRED**). Test **ONLY** when editing application files (Kotlin, XML, Gradle), or after stages/strokes of work.
- **Remote Release Observability (`/follow-the-build`)**: Cloud release builds (GitHub Actions) are tracked asynchronously via the GitHub REST API using reactive non-blocking timers (`schedule(DurationSeconds=30)`). Verifies step progression through SDK setup, pre-flight testing, keystore decoding, AAB/APK compilation, and final asset release without blocking local agent or developer execution.


- **Agent Cognitive Architecture**: 
  - **Self vs Environment Memory Split**: Internal cognitive state (story, decisions, dreams, archives) lives in the `.agents/brain/` root. External world context (architecture, tech stack, progress) lives in `.agents/brain/project/`.
  - **Cognitive Workflows**: `/memory` (Hippocampal Short-Term Encoding), `/story` (Prefrontal Waking Self), `/dream` (Offline REM Hippocampal Replay), `/wake` (Neocortical Integration), `/forget` (Active Molecular Dissolution via Rac1/Cofilin cascade), `/reflect` (Prediction Error Minimization).
  - **Strict Dissolution Boundaries**: Forgetting removes text from active memory and commits to git. It is gated by strict safety checks: requires a clean worktree (`git status --porcelain`) and must be executed on an isolated bespoke branch.
