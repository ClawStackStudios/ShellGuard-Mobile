# 🤖 Jules Fleet Instructions: ShellGuard Mobile

Welcome to **ShellGuard Mobile**, the native Android client for the ShellGuard Zero-Knowledge Vault.

---

## 🏛️ Invariants & Architectural Rules

1. **Zero-Knowledge Invariant**: Plaintext secrets and `hu-` sovereign keys NEVER touch the server or unencrypted storage. The client generates and holds encryption keys; the server stores only opaque ShellCryption envelopes (`{"v":1,"alg":"AES-GCM-256","iv":"...","ct":"...","aad":"..."}`).
2. **Additional Authenticated Data (AAD) Invariant**: Every encrypted field is strictly bound to its canonical domain AAD namespace:
   - `vault_pearls:{id}` — Pearl secret (password)
   - `vault_pearls_totp:{id}` — Pearl TOTP secret
   - `vault_pearls_custom:{id}` — Pearl custom fields
   - `vault_pearls_history:{id}` — Pearl password history
   - `vault_secure_notes:{id}` — Note content
   - `vault_secure_notes_custom:{id}` — Note custom fields
   - `vault_ssh_keys:{id}` — SSH key private value
   - `vault_ssh_keys_custom:{id}` — SSH key custom fields
   - `vault_secure_attachments:{id}` — Attachment file payload
3. **CursorWindow Limit Defense (CWE-400)**: Android Room SQLite has a strict 2MB CursorWindow limit per query. **NEVER store attachment payloads as inline Room BLOBs**. Room stores metadata only (`size_bytes`, `mime_type`, `file_name`, `local_file_path`). Encrypted files stream directly to internal storage: `context.filesDir/vault_attachments/{id}.enc`.
4. **Git Grounding**:
   - Work strictly from branch `main`.
   - Before modifying files, verify repository files using `git ls-tree -r --name-only HEAD`.
   - DO NOT force-reset, rebase root, or force-push `main`.

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
