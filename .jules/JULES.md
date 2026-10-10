# 🤖 Jules Fleet Instructions: ShellGuard Mobile

Welcome to **ShellGuard Mobile**, the native Android client for the ShellGuard Zero-Knowledge Vault.

---

## 🏛️ Core Architectural Invariants

1. **Zero-Knowledge Invariant**:
   Plaintext secrets and `hu-` sovereign keys NEVER touch the server or unencrypted storage. The client generates and holds encryption keys; the server stores only opaque ShellCryption envelopes (`{"v":1,"alg":"AES-GCM-256","iv":"...","ct":"...","aad":"..."}`).

2. **Additional Authenticated Data (AAD) Invariant**:
   Every encrypted field is strictly bound to its canonical domain AAD namespace:
   - `vault_pearls:{id}` — Pearl secret (password)
   - `vault_pearls_totp:{id}` — Pearl TOTP secret
   - `vault_pearls_custom:{id}` — Pearl custom fields
   - `vault_pearls_history:{id}` — Pearl password history
   - `vault_secure_notes:{id}` — Note content
   - `vault_secure_notes_custom:{id}` — Note custom fields
   - `vault_ssh_keys:{id}` — SSH key private value
   - `vault_ssh_keys_custom:{id}` — SSH key custom fields
   - `vault_secure_attachments:{id}` — Attachment file payload

3. **CursorWindow Limit Defense (CWE-400)**:
   Android Room SQLite has a strict 2MB CursorWindow limit per query. **NEVER store attachment payloads as inline Room BLOBs**. Room stores metadata only (`size_bytes`, `mime_type`, `file_name`, `local_file_path`). Encrypted files stream directly to internal storage: `context.filesDir/vault_attachments/{id}.enc`.

4. **Storage Lifecycle Parity Invariant**:
   Whenever a vault item or attachment is purged, deleted, or wiped during a Panic Purge, the physical ciphertext file on disk (`context.filesDir/vault_attachments/{id}.enc`) and any temporary plaintext preview files (`context.cacheDir/decrypted_attachments/*`) MUST be zeroized and deleted. Never orphan encrypted artifacts on disk.

5. **⚠️ CRITICAL BOT TAGGING INVARIANT (NEVER Tag @jules)**:
   When writing PR comments, commit messages, or issue responses, **NEVER tag `@jules`**. That handle belongs to an innocent third-party human user. Refer to Jules simply as plain text `Hey Jules` or the official bot handle `@google-labs-jules[bot]`.

6. **Git Grounding & Attribution**:
   - Work strictly on your assigned task branch.
   - All commits must follow the two-layer attribution format:
     ```
     <type>(<scope>): <summary>

     User: <intent, design requirements, or architecture direction>
     AI: <implementation, classes modified, and test verification>
     ```
   - Before modifying files, verify repository files using `git ls-files`.
   - DO NOT force-reset, rebase root, or force-push `main`.

---

## 📐 Code Quality & UI Modularity Standards

1. **Compose File Granularity**:
   Aim for modular composables (~250 line target, 500 line hard ceiling). Decompose complex screen sections (e.g., Multi-URI builder, Attachment list, Custom fields editor) into dedicated composable functions or sub-components rather than bloating a single screen file.
2. **Robolectric Test Target**:
   Robolectric tests must specify `@Config(sdk = [34])` to align with the supported Android 14 test runner target.
3. **Markdown Documentation Exemption**:
   Markdown documentation updates are exempt from running Gradle builds and tests. However, Kotlin, XML, and Gradle modifications REQUIRE running unit tests to guarantee 100% green builds.

---

## ⚙️ Tech Stack & Verification Commands

- **Language**: Kotlin 2.2+
- **UI**: Jetpack Compose + Material 3 (Reef Modernist design system)
- **Local Database**: Room 2.7+ with SQLCipher 4.6.1+
- **Network**: Ktor Client 2.3.12 (OkHttp engine) consuming Express 5 REST API
- **Verification Commands**:
  - Run Unit & Robolectric Tests: `./gradlew testDebugUnitTest --no-daemon`
  - Compile Debug APK: `./gradlew assembleDebug --no-daemon`

Always ensure all unit tests pass 100% green before submitting your Pull Request!

---

## 📋 Active Task Pipeline

The remaining work for Stage 1 is divided into two atomic, orthogonal tasks with zero file overlap:

| Task File | Domain | Scope | Status |
|---|---|---|---|
| [task-1-multi-uri-and-attachment-ui.md](tasks/task-1-multi-uri-and-attachment-ui.md) | UI & Autofill | Multi-URI editor, Attachment picker/viewer, Dashboard pod chip, Autofill hook | Ready |
| [task-2-settings-debt-and-storage-wiring.md](tasks/task-2-settings-debt-and-storage-wiring.md) | Core & Settings | AppContainer DI, Panic Purge disk wipe, URI match mode preference, FileProvider, Backup schema | Ready |

Jules agents can pick up either task independently without git conflicts.
