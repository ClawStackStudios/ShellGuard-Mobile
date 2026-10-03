# Consolidated Learnings: ShellGuard Mobile

## Storage & Android SQLite Invariants
**Pattern: Hybrid File-System Storage (CursorWindow Defense)**
- **Rule**: NEVER store large file payloads (attachments > 500KB) as inline BLOB columns in SQLite/Room. Android limits SQLite query rows to a 2MB `CursorWindow`, causing uncatchable `SQLiteBlobTooBigException` crashes on `SELECT` queries.
- **Pattern**: Store only metadata in Room (`id`, `title`, `sizeBytes`, `mimeType`, `localFilePath`). Stream encrypted bytes directly to `context.filesDir/vault_attachments/{id}.enc` using zero-heap 64KB buffers via `CipherInputStream` and `CipherOutputStream`.
- *Rationale*: Guarantees predictable memory footprint under 64KB regardless of file size (even for 500MB uploads).

## Synchronization & Offline Vault Architecture
**Pattern: Bitwarden-Model Read-Only Offline Caching**
- **Rule**: When disconnected from the self-hosted server, enforce strict **Read-Only** access. Disallow local creations, edits, and deletions to eliminate split-brain divergence and complex three-way merge conflicts.
- **Access Scope**: Retain 100% functionality for viewing, searching, copying passwords (masked), running TOTP tickers, and serving Android Autofill.
- **Seamless Reconnect**: Use `ConnectivityManager.NetworkCallback` (`NET_CAPABILITY_INTERNET`) to trigger a lightweight `GET /api/health` probe upon network availability, automatically transitioning from `OfflineReadOnly` to `OnlineSynced` and executing a downstream delta pull.
- **Lock vs. Logout Invariant**:
  - *Lock*: Clears plaintext keys from RAM; preserves encrypted SQLCipher database on disk for offline unlock.
  - *Logout*: Purges local SQLCipher database and KeyStore credentials.

## Android Security & Cryptographic Invariants
**Pattern: Hardware Biometric Invalidation Recovery**
- When `AndroidKeyStore` secret keys use `setInvalidatedByBiometricEnrollment(true)`, modifying system biometrics permanently destroys the key, throwing `KeyPermanentlyInvalidatedException`.
- **Pattern**: Catch this exception, route the user to a Master Password/PIN fallback authentication flow, purge the invalidated key, generate a fresh KeyStore key, and re-wrap the vault secret without data loss.

**Pattern: Sensitive Clipboard Masking (CWE-359)**
- On Android 13+ (API 33+), copy operations trigger a floating visual preview. Suppress this for passwords/secrets by setting `ClipDescription.EXTRA_IS_SENSITIVE = true` on the `ClipData` description extras. Pair with a 30s/60s auto-scrub coroutine to purge the clipboard.

**Pattern: Claw Re-Prompt Guardrail**
- For high-privilege credentials (root keys, bank logins), enforce a localized biometric/PIN gate before revealing hidden text or copying, even if the vault itself is currently unlocked.

**Pattern: Base62 Sovereign Identity Key Parity**
→ Consolidated to `long-term/patterns.md § pattern: base62-sovereign-key-parity` (weight: 3, 2026-09-27)

**Pattern: IME Surface Composition & Double-Inset Hardening**
- **Rule**: Never enforce `FLAG_SECURE` unconditionally in debug builds. On Adreno 5xx GPUs under Android 14, blending insecure system IME overlays over secure surfaces causes complete screen blackout.
- **Rule**: Set `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0))` when child screens manage their own `.imePadding()`, preventing double keyboard height subtraction.

**Pattern: Zero-Knowledge Session Atomicity & Derived Key Persistence**
- **Rule**: In zero-knowledge architectures where records are encrypted client-side, an active session requires BOTH the authentication token and the symmetric decryption key.
- **Anti-Pattern**: Storing `shellKey` solely in volatile memory (`@Volatile var inMemoryShellKey`) while checking only persisted server tokens in `hasActiveSession()`. On cold restarts, the app enters an unauthenticated split-brain state where the dashboard opens but items throw `IllegalStateException: Vault locked or shellKey missing`.
- **Pattern**:
  1. Persist the derived 32-byte `shellKey` (Base64) in `EncryptedSharedPreferences` (AES-256-GCM hardware KeyStore protected).
  2. Implement lazy re-hydration in `getInMemoryShellKey()` to restore RAM state across process deaths.
  3. Enforce atomic session validation: `hasActiveSession()` returns `true` ONLY IF `getInMemoryShellKey() != null`.
  4. Pre-fill gateway parameters (`host`, `port`, `protocol`) from `deviceVault.getServerUrl()` on login fallbacks to eliminate user re-entry friction.
  5. Purge the persisted key on explicit logout or panic zeroization.

## Autofill & Home Lab Networking
**Pattern: Multi-Mode URI Match Detection**
- In self-hosted home labs (Unraid/Docker), multiple services run on the same IP across different ports (`192.168.1.100:8080` vs `192.168.1.100:9000`).
- **Pattern**: Support 5 match modes: `BASE_DOMAIN` (default eTLD+1), `HOST` (subdomains), `EXACT` (full URL + port + path), `STARTS_WITH` (prefix), and `NEVER` (suppress autofill).
- **Inline Autofill**: Detect Android 11+ `request.inlineSuggestionsRequest` to render suggestion chips directly in the keyboard strip (Gboard, SwiftKey).

## Build & CI Packaging Invariants
- **16 KB Page Alignment**: Android 15/16 mandates 16 KB page-aligned native libraries. Always use SQLCipher 4.6.1+ and set `packaging.jniLibs.useLegacyPackaging = false` in `app/build.gradle.kts`.
- **Headless Build Environment**: Always export bundled Android Studio JBR:
  ```bash
  export JAVA_HOME="/config/Applications/android-studio/jbr"
  export PATH="$JAVA_HOME/bin:/config/Android/Sdk/platform-tools:$PATH"
  export GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"
  ```
- **ProGuard / R8 Reality vs. Native Obfuscation Myth**: ProGuard/R8 operates strictly on JVM bytecode (`.class`/`.dex`); it CANNOT obfuscate precompiled native ELF `.so` libraries (`libsqlcipher.so`). R8 in release builds requires explicit preservation rules (`-keep class net.zetetic.** { *; }`, Kotlinx `@Serializable` companion serializers, Room DAOs) to prevent runtime `UnsatisfiedLinkError` and serialization crashes. Strip `Log.d`/`Log.v` debug logs via `-assumenosideeffects` to eliminate cleartext leakages.
- **Pattern: Robolectric Target SDK Ceiling (`sdk=34`)**:
  - *Rule*: Whenever `targetSdk` is set to Android 16 (API 36), configure `app/src/test/resources/robolectric.properties` with `sdk=34` and annotate Robolectric tests with `@Config(sdk = [34])`.
  - *Anti-Pattern*: Omitting SDK configuration causes `DefaultSdkProvider.java:170` to throw `UnsupportedOperationException` on CI runners during headless testing.
- **Pattern: Standardized GitHub Actions Release Signing Schema**:
  - *Rule*: Maintain strict repository secret parity across all projects:
    - `ANDROID_SIGNING_KEY`: Single-line `base64 -w 0` string of `my-upload-key.jks`.
    - `ANDROID_KEYSTORE_PASSWORD`: Keystore master password.
    - `ANDROID_KEY_ALIAS`: Key alias (default `upload`).
    - `ANDROID_KEY_PASSWORD`: Private key password.
  - *Decoding*: Always decode via Python 3 `base64.b64decode(os.environ['SIGNING_KEY'].strip())` to prevent GNU base64 newline/padding failures on Linux runners.

## Design System & Touch Form Ergonomics
**Pattern: Reef Modernist Mobile & Blind Side-by-Side Parity**
- **Exoskeletal Shells**: 16dp rounded corners, 1dp `#3D484E` borders, and zero artificial elevation shadows create identical physical tone between web and mobile.
- **Master-Detail Adaptation**: Seamlessly scales from single-column on phones to 3-pane Bitwarden-style desktop parity on tablets (`SidebarFolderTree` 240dp, `ItemListPane` 340dp, `ItemDetailPane` weight 1f).
- **Form Ergonomics**: Form screens apply pinned headers/footers with `.imePadding().verticalScroll(rememberScrollState())` and upward-expanding dropup menus to prevent soft keyboard truncation.
- **CWE-359 IME Keyboard Isolation**: Apply `PasswordVisualTransformation()` + `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` across all secret fields to prevent third-party keyboard telemetry scraping.

## Multi-Domain Migration & Ingestion Invariants
**Pattern: Polymorphic Entity Mapping & Fingerprint Deduplication**
- When ingesting Bitwarden JSON or external backups into a full vault client, map items polymorphically: Logins ➔ `VaultPearlEntity`, Notes ➔ `SecureNoteEntity`, SSH Keys ➔ `SshKeyEntity`, TOTP ➔ `VaultPearlEntity.totpSecret`. Purging passwords/notes is strictly a 2FA-companion anti-pattern.
- Calculate normalized `secret` + `title` + `username` fingerprints before Room DB insertion to eliminate duplicate false negatives.

## Zero-Knowledge Vault Threat Model (CWE-532)
**Pattern: Zero-Telemetry Isolation**
- Reject third-party monitoring/telemetry SDKs (Firebase Crashlytics, Sentry, Datadog) to prevent leaking decrypted passwords, `hu-` root keys, or TOTP seeds over network routes.
- All diagnostic and security events are logged exclusively to the local, encrypted Room `audit_logs` table.

## Bitwarden Parity & Autofill Ergonomics
**Pattern: Auto-Copy TOTP on Autofill Selection**
- Upon autofilling a login with an active TOTP secret, automatically calculate the current 6-digit TOTP code and copy it to the Android clipboard with `ClipDescription.EXTRA_IS_SENSITIVE = true` and a 30-second coroutine scrub timer. This enables instantaneous 2-step verification paste on two-factor challenge screens without app switching.

## Git & Environment Hygiene
**Pattern: Hardware Identifier Opsec & Unpushed Rebase Scrub**
- **Principle**: Physical device serial numbers (e.g. `adb devices` serials) and local machine filesystem paths must never leak into public git repositories.
- **Protocol**:
  - Keep machine-specific tooling in gitignored files (e.g. `.agents/TOOLS.md`, `local.properties`).
  - If committed locally on an unpushed branch, do not rely on subsequent `git rm --cached` commits (which leaves the serial in git history).
  - Use interactive rebase or clean branch re-anchoring to rewrite the introducing commit, add to `.gitignore`, and restore the physical file locally.
- *Rationale*: Protects physical device identity and keeps the public git log pristine without losing local developer tooling.

**Pattern: Markdown Documentation Testing Exemption**
- **Principle**: Differentiate between application code and documentation prose.
- **Rule**: Standard markdown doc edits (`.md`, Brain, specs) are fully exempt from Gradle build and test suites (**NO TESTING REQUIRED**). Test only when editing application files (Kotlin, XML, Gradle) or completing milestones.
- *Rationale*: Eliminates 20-30s test execution tax on prose and rule updates while preserving rigorous gates for application bytecode.

## Git & Release Automation Standards
**Pattern: Conventional Commits & Keep a Changelog 1.1.0 Pipeline**
- **Syntax**: `<type>[optional scope][optional !]: <summary>` coupled with two-layer `User:` / `AI:` attribution.
- **Mapping**:
  - `feat` ➔ `### Added`
  - `refactor` / `perf` ➔ `### Changed`
  - `revert` ➔ `### Removed`
  - `fix` ➔ `### Fixed`
  - `security` ➔ `### Security`
  - `feat!` / `BREAKING CHANGE:` ➔ Major SemVer increment.
- **Comparison Links**: All changelogs require full range comparison anchors (`vPrev...vCurr`) to facilitate automated release note tooling (e.g., `git-cliff`, `semantic-release`).

**Pattern: Dual-Tier Release Notes Architecture**
- **Public Layer**: Visual, emoji-anchored, concise notes for GitHub Releases and Google Play Store distribution (`RELEASE-PLAY.md` under 500 chars).
- **Internal Layer**: Strict engineering manifest (`RELEASE-vX.Y.Z.N.md`) documenting AAD integrity, Room schema versioning, dependency drift tables, and verified test gates.

**Pattern: Autosquash & Interactive History Curation with Safety Anchors**
- **Protocol**: During iterative task strokes, use `git commit --fixup <hash>` to capture targeted corrections without manual commit message rewriting.
- **Safety**: Always create `git branch backup/<branch>-pre-rebase` before running `git rebase -i --autosquash $(git merge-base HEAD main)`.
- **Attribution**: Ensure squashed commits preserve the canonical two-layer attribution format (`User:` / `AI:`).
- *Rationale*: Delivers clean, bisectable commit spines on `main` without risking loss of in-flight work during rebases.

**Pattern: Automated Gradle Test-Driven Git Bisect**
- **Protocol**: Automate regression identification by pairing `git bisect` with headless Gradle test execution:
  `git bisect run ./gradlew testDebugUnitTest --tests "<TargetClassTest>"`.
- **Precondition**: Requires a 100% clean working directory before starting bisect to prevent false positives.
- *Rationale*: Reduces bug localization time from hours of manual checkouts to automated binary search in minutes.

**Pattern: Worktree Isolation for Parallel Hotfixes & Subagents**
- **Protocol**: Use `git worktree add ../<dir> -b <branch>` for concurrent tasks rather than `git stash` on dirty trees.
- *Rationale*: Eliminates stash merge collisions, preserves IDE build caches, and allows concurrent agents to work in separate physical directories against the same repository.


