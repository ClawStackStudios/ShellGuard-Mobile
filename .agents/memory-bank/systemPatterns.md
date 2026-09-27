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
- **Claw Re-Prompt Guardrail**: Designated high-security items (`reprompt == true`) enforce a biometric or PIN re-verification gate before the user can reveal hidden secrets or copy them to the clipboard, even when the vault is already unlocked.
- **Biometric Recovery State Machine**: Hardware biometric keys invalidated by new biometric enrollments (`KeyPermanentlyInvalidatedException`) automatically route to Master Password/PIN fallback to regenerate keys without user lockout or data loss.
- **Zero-Knowledge Session Atomicity & KeyStore Key Persistence**: Active sessions atomically couple transport authorization (`sessionToken`) with cryptographic capability (`shellKey`). `hasActiveSession()` strictly verifies `getInMemoryShellKey() != null`. Derived 32-byte symmetric keys are persisted at rest in hardware KeyStore-backed `EncryptedSharedPreferences` (AES-256-GCM) with dynamic RAM re-hydration to survive Android process death without user lockout. Server connection parameters (`protocol`, `host`, `port`) are preserved and pre-filled upon session fallback to ensure frictionless re-entry. *(see [long-term/patterns.md § pattern: zero-knowledge-session-atomicity](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/.agents/memory-bank/long-term/patterns.md))*.
- **Pre-DAO Fingerprint Deduplication**: `title | username | secret` hash comparison before database writes prevents duplicate false negatives.

## Autofill & System Integration Patterns
- **Configurable URI Match Detection (`UriMatchMode`)**: 5 matching algorithms (`BASE_DOMAIN`, `HOST`, `EXACT`, `STARTS_WITH`, `NEVER`) supporting multi-tenant subdomains and exact port matching for local home labs.
- **Android 11+ Inline Presentation**: Supports keyboard suggestion chips above Gboard/SwiftKey via `InlineSuggestionsRequest` alongside standard popup dropdowns.
- **Quick Settings Tile**: `TileService` for instant search and password generation from the notifications shade.
- **Glance AppWidgets**: Modern Jetpack Compose Glance 2x2 and 4x2 widgets for pinned logins and live TOTP codes.

## UI & Theming Patterns: Reef Modernist Mobile
- **Bioluminescent Defense Aesthetic**: Dual-mode Abyssal Dark (`#0F1419` base, `#171C21` surface) and Ocean Mist (`#F1F5F9` base, `#FFFFFF` surface) with signature Lobster Red (`#E4048A`) and Claw Cyan (`#06B6D4`) neon conduits.
- **Exoskeletal Shells (Flat Material 3)**: Cards use `16dp` rounded corners, crisp 1dp `#3D484E` carapace borders, and zero artificial drop shadows (`elevation = 0.dp`), relying on tonal contrast and borders for depth.
- **Dynamic Theme Engine**: 6 curated theme palettes (`REEF_DEFAULT`, `CYAN_VENT`, `PURPLE_SHELL`, `EMERALD_TRENCH`, `AMBER_FLARE`, `MONOCHROME`) injected through `CompositionLocalProvider(LocalShellGuardColors)`.
- **Adaptive Master-Detail Scaffolding**: Automatically transitions between single-column navigation on phones and three-pane Bitwarden-style desktop parity on tablets and foldables (Folder Pods Sidebar -> Item List -> Detail Inspection Pane).
- **Tactile & Motion Ergonomics**: Spring scale press physics (`0.97f` scale down with damping `0.75f`), tactile haptic feedback on copy/long-press, and depleting circular Canvas countdown rings with dynamic color interpolation.
- **Form Ergonomics & CWE-359 IME Hardening**: Pinned header/footer forms, upward-expanding dropup menus, `.imePadding().verticalScroll()`, and `PasswordVisualTransformation` + `KeyboardType.Password` (auto-correct disabled) on all secret inputs. *(see [long-term/patterns.md § pattern: cwe-359-ime-protection-and-inset-isolation](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/.agents/memory-bank/long-term/patterns.md))*.

