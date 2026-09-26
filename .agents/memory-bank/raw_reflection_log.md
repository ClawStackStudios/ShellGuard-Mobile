# Raw Reflection Log

---
Date: 2026-09-24
TaskRef: "Architecture & Specification Blueprint for ShellGuard Mobile (Full Android Client)"

Learnings:
- Android SQLite CursorWindow Hard Limit: Storing attachments (>2MB) directly as BLOBs in Room database rows triggers unavoidable `SQLiteBlobTooBigException` or `RowTooBigException` on Android. Solved by decoupling: Room holds metadata and relative path; ciphertext streams via `CipherInputStream`/`CipherOutputStream` directly to `filesDir/vault_attachments/{id}.enc`.
- Bitwarden Offline Client-Server Invariant: Allowing offline mutations across multiple devices against a self-hosted server causes split-brain divergence and merge conflicts. The optimal production pattern is Read-Only Offline Caching: offline clients retain 100% read, search, copy, autofill, and TOTP functionality, while blocking mutations (create/edit/delete). Reconnection is handled cleanly via Android's `ConnectivityManager.NetworkCallback` and a health probe.
- Android 13+ System Clipboard Preview Leakage (CWE-359): When copying sensitive credentials on Android 13+ (API 33+), the OS renders a floating thumbnail preview of the clipboard content. Mitigated by applying `ClipDescription.EXTRA_IS_SENSITIVE = true` to suppress visual cleartext popups, paired with a coroutine/WorkManager 30s auto-purge timer.
- Android KeyStore Biometric Invalidation Lifecycle: Hardware keys configured with `setInvalidatedByBiometricEnrollment(true)` are permanently destroyed when a user enrolls or modifies fingerprints/face data in system settings. `Cipher.init()` throws `KeyPermanentlyInvalidatedException`. Solved by implementing an explicit recovery state machine that routes to Master Password/PIN fallback, deletes the invalidated key, generates a fresh KeyStore key, and re-seals the secret.
- Homelab Multi-Service Port Matching: In self-hosted setups (Unraid, TrueNAS, Portainer, Docker), multiple distinct services share the same IP address across different ports (e.g. `192.168.1.100:8080` vs `192.168.1.100:9000`). Standard `eTLD+1` domain matching causes cross-service credential clutter. Solved by introducing `UriMatchMode` with 5 algorithms: `BASE_DOMAIN`, `HOST`, `EXACT`, `STARTS_WITH`, and `NEVER`.
- Android 11+ (API 30+) Keyboard Inline Autofill: Modern keyboard engines (Gboard, SwiftKey) render autofill credentials directly in the keyboard suggestion strip via `InlineSuggestionsRequest` and `InlinePresentation`.
- Claw Re-Prompt Defense: Certain high-privilege credentials (root server SSH keys, bank logins, master seeds) require a localized re-authentication gate (`reprompt == true`) requiring biometric or PIN confirmation before revealing hidden fields or copying, even if the vault itself is currently unlocked.
- Android 15/16 16 KB Page Alignment Packaging: Using `jniLibs.useLegacyPackaging = false` in Gradle ensures native `.so` binaries (SQLCipher 4.6.1+) remain uncompressed and 16 KB page-aligned within the APK/AAB zip file.

Difficulties & Friction:
- Initial specification drafts for `SecureAttachmentEntity` treated attachment data as inline SQLite BLOBs, which would have passed synthetic unit tests with small byte arrays but catastrophically crashed in production with real user attachments. Recognizing the 2MB CursorWindow limit early saved weeks of refactoring.
- Subagent communication timeout occurred during the first batch of spec generation due to a context checkpoint pause. Seamlessly took over manually, verified the remaining files, and preserved continuity without loss of work.

Successes:
- Produced 14 complete, deeply detailed specification documents in `/project/` in total alignment with sibling projects.
- Fully synchronized `ROADMAP.md` (6 phases, 12 paired tasks) with the master `meta-prompt-ai-studio.md` (7 stages).
- Initialized and kept the Memory Bank completely accurate across multiple rapid architectural iterations.

Improvements Identified for Consolidation:
- General pattern: Android SQLite CursorWindow defense via Hybrid File-System decoupling.
- General pattern: Bitwarden-style Read-Only Offline Caching with NetworkCallback auto-probe.
- General pattern: CWE-359 sensitive clipboard masking and auto-scrubbing.
- General pattern: Biometric key invalidation recovery state machine.
- Project-specific commands & build flags: headless JBR export, `-XX:-UsePerfData`, `app/build/tmp`.
---

---
Date: 2026-09-25
TaskRef: "Cross-Project Design Synthesis & Definitive DESIGN.md for ShellGuard Mobile"

Learnings:
- Ecosystem Design Grammar Continuity: Directly inspected `DESIGN.md` in both the Web Server (`ShellGuard`) and Android TOTP Companion (`ShellGuard-TOTP`). Extracted the core "Reef Modernist" ("Bioluminescent Defense") principles: Exoskeletal Shells (flat cards with 1dp `#3D484E` borders and 0dp elevation), Bioluminescent glow dynamics (neon Lobster Red `#E4048A` and Claw Cyan `#06B6D4`), and strict three-tier typography (`Outfit` headlines, `Inter` body, `JetBrains Mono` cryptographic keys/codes/passwords).
- Blind Side-by-Side Presentation Alignment: To ensure ShellGuard Mobile is unmistakably identified as part of the exact same product family when placed next to ShellGuard Web, the mobile app mirrors the desktop's three-pane architecture on tablets/foldables (`SidebarFolderTree` -> `ItemListPane` -> `ItemDetailPane`), uses identical custom field types and badges (Text, Hidden with eye toggle, Checkbox pill, Linked property arrow), and reproduces identical spring scale press physics (`0.97f` scale down with damping `0.75f` and stiffness `400f`).
- Form Ergonomics in Touch Environments: Mobile forms require pinned headers and footers with a scrollable body applying `.imePadding().verticalScroll(rememberScrollState())` to prevent virtual keyboards from obscuring inputs, and upward-expanding dropup menus to prevent screen boundary clipping.
- CWE-359 IME Keyboard Isolation: All sensitive inputs (passwords, seeds, PINs) must apply `PasswordVisualTransformation()` paired with `KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)` to prevent third-party keyboards from harvesting or caching cryptographic secrets.

Difficulties & Friction:
- Sibling project file discovery required precise directory scoping rather than wide unconstrained searches across `/projects/Agents` to prevent search timeouts. Targeted lookup at `/ShellGuard/DESIGN.md` and `/ShellGuard-TOTP/DESIGN.md` succeeded immediately.

Successes:
- Authored a comprehensive, definitive root `DESIGN.md` in `ShellGuard-Mobile` with complete Kotlin Jetpack Compose code blocks for the theme engine, all 4 domain cards, custom fields, circular countdown rings, and layout scaffolds.
- Synchronized `project/ui-ux-design-system.md` with the updated color scheme container (`ShellGuardCustomColors`) and dynamic theme accents.
- Ensured 1:1 visual parity across the entire ShellGuard ecosystem.
---

---
Date: 2026-09-25
TaskRef: "Reorient Project Rules (.agents/rules/android-development.md) for Full Mobile Client"

Learnings:
- Full Client vs Companion Rule Parity: The previous rule file was heavily oriented around the read-only ShellGuard-TOTP companion app (which prohibited upstream pushes and only supported `isLocalOnly` TOTP items). The full client requires bidirectional delta reconciliation, 4 distinct vault domains plus attachments, and an explicit Bitwarden-style read-only offline caching engine with automated `NetworkCallback` health probing.
- Hybrid File-System Vault Rule Encoding: Formally codified the CWE-400 2MB CursorWindow limit directly into the development rules so that no future developer or AI agent attempts to put attachment BLOBs into Room rows.
- ProGuard / R8 Truth Hardening: Codified the exact R8 preservation requirements for SQLCipher, Kotlinx Serialization, and Room into the rules while explicitly calling out that native C/C++ libraries cannot be obfuscated via ProGuard rules.
- Rule URI Integrity: Fixed cross-project pointer in `.agents/AGENTS.md` which previously referenced the TOTP companion's rule path.

Difficulties & Friction:
- None. Clean surgical update of the rule file, AGENTS.md pointer, and memory bank tracking.

Successes:
- Fully aligned `.agents/rules/android-development.md` with the full mobile vault client architecture across all 10 invariant sections.
- Kept the memory bank updated with 100% fidelity.
---

---
Date: 2026-09-25
TaskRef: "Ecosystem-Wide Audit & Reorientation of Migration Rules, Workflows, and Skills"

Learnings:
- Critical Ingestion Invariant Reversal: In the companion app, `zero-knowledge-migration.md` mandated the immediate volatile purging of passwords, notes, and cards. In ShellGuard Mobile, passwords (`VaultPearlEntity`), notes (`SecureNoteEntity`), and SSH keys (`SshKeyEntity`) are primary first-class entities. Reoriented the migration rule and workflow to execute polymorphic entity mapping, converting Bitwarden types 1, 2, and SSH keys into their respective Room tables, with pre-DAO fingerprint deduplication (`secret + title + username`).
- Proprietary Schema Clarity: Codified that `.sgvault.bak` is the canonical full vault backup envelope, while `.sgtotp.bak` remains the interoperable bridge format for 1:1 data migration with the companion app.
- Release Governance Realignment: Reoriented `play-console-release-workflow.md`, `development-release-cycle.md`, and CI packaging skills (`android-headless-signing-ci`) from `com.clawstack.shellguard.totp` to `com.clawstack.shellguard`, ensuring bundle names (`shellguard-mobile-vX.Y.Z.N.aab`) and Play Store release notes accurately describe the full secrets vault.

Difficulties & Friction:
- None. Systematically audited every `.agents/` directory file.

---
Date: 2026-09-26
TaskRef: "Phase 2: Ktor Sync, Base62 Identity Parity, and IME Soft Keyboard Hardening"

Learnings:
- Base62 Sovereign Key Invariant: ShellGuard root master keys (`hu-`) and agent keys (`lb-`) use 64 Base62 alphanumeric characters (`[0-9a-zA-Z]`), totaling 67 characters. Restricting validation to hexadecimal (`[0-9a-f]`) leads to false-negative rejections of identity files generated by web and server.
- Legacy GPU Adreno 530 Surface Composer Bug: On Android 14 LineageOS running on Snapdragon 820/821 hardware, insecure system IME overlays fail to composite over `FLAG_SECURE` window buffers, turning the screen pitch black. Scoping `FLAG_SECURE` to release builds (`!BuildConfig.DEBUG`) preserves debugging, screenshot validation, and smooth keyboard input.
- Compose Scaffold Inset Isolation: Compose `Scaffold` consumes window insets by default. When screen composables independently apply `.imePadding()`, the keyboard height is subtracted twice, squashing the layout. Setting `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0))` at the root level completely resolves this.

Difficulties & Friction:
- Identity file loading succeeded on physical device, but button remained disabled due to silent validation failure in `ClawCrypto.isValidClawKey`. Forensic inspection of `/sdcard/Download/*.json` revealed Base62 character set.
- Screen blackout during text input made typing invisible. Initial suspicion was IME focus stealing, but forensic screenshots revealed surface buffer compositor failure tied to `FLAG_SECURE`.

Successes:
- Upgraded `ClawCrypto.kt` and tests to Base62; added uploaded UUID extraction.
- Scoped `FLAG_SECURE` to release builds and configured root `Scaffold` insets.
- All 23 unit tests pass green; verified live on Google Pixel with zero blackout, smooth cursor positioning, and active login button.
- User tested and confirmed live on hardware.
---
