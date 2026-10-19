# 🐙 Jules Knowledge Store: ShellGuard Mobile

> **Tier 2 Repository Knowledge Store**: Empirical learnings, distilled architectural patterns, and verified failure modes for autonomous agents collaborating on ShellGuard Mobile.
> *Authority: Root `AGENTS.md` defines system architecture and persona; this file holds auditable, empirical ground-truth units.*

---

## 🏛️ Architectural & Cryptographic Invariants
- **Zero-Knowledge Decryption**: The server is strictly untrusted storage. Master `hu-` sovereign keys and plaintext secrets NEVER touch the wire or persistent unencrypted flash; client generates domain AES-GCM-256 keys via HKDF-SHA256 (`info = "clawchives-shellcryption-v1"`).
- **Strict AAD Binding**: Every cryptographic operation must bind Associated Additional Data to record UUID and domain namespace (`vault_pearls:{id}`, `vault_secure_notes:{id}`, etc.). Payloads fail GCM authentication if swapped across records or namespaces.
- **CursorWindow Limit Defense (CWE-400)**: Android Room SQLite has a strict 2MB CursorWindow Binder limit per query. NEVER store attachment payloads as inline Room BLOBs. Store metadata only in Room (`size_bytes`, `mime_type`, `file_name`, `local_file_path`) and stream encrypted payloads to `context.filesDir/vault_attachments/{id}.enc` using 64KB buffers.
- **Storage Lifecycle Parity**: When vault items or attachments are purged, deleted, or wiped during Panic Purge, the physical ciphertext file on disk (`vault_attachments/{id}.enc`) and any temporary plaintext preview files (`cacheDir/decrypted_attachments/*`) MUST be zeroized and deleted. Never orphan encrypted artifacts on disk.
- **Session Decoupling & In-Place Re-Auth**: Vault lock zeroizes volatile RAM secrets and derived HKDF keys, but preserves KeyStore-sealed session tokens and PBKDF2 PIN state in encrypted storage so users re-authenticate in-place without remote server round-trips.
- **Frictionless Self-Signed Transport Sovereignty**: ShellGuard is engineered for homelab operators where self-signed certificates are the default reality. Enforce strict platform TLS by default on WAN/HTTPS, but support self-signed certificates via explicit in-app user consent ("Trust self-signed certs" toggle) without forcing users to install root CAs into Android OS settings. Always retain `ConnectionSpec.CLEARTEXT` for local LAN HTTP.

---

## 📐 Code Quality, UI & Compose Invariants
- **Direct File Writes Invariant (NO Script Patching)**: NEVER use `sed`, `awk`, Python regex, or bash string replacement scripts to edit Kotlin, XML, or Gradle files. String replacement scripts repeatedly introduce duplicate arguments (e.g. duplicate `modifier = `), unbalanced braces/parentheses, and invalid `@Composable` invocations inside loops. Always perform direct block file writes or use structured editor tools.
- **Compose Soft-Keyboard & Scroll Alignment**: When a parent container applies `.imePadding()` and an inner column uses `.verticalScroll(rememberScrollState())`, Compose natively auto-scrolls focused fields into the viewport upon keyboard opening. Do NOT inject individual `BringIntoViewRequester` modifiers onto each field or inside iterative loops (`forEachIndexed`). Guarantee keyboard clearance by placing adequate bottom spacing at the end of the scrollable column (`Spacer(modifier = Modifier.height(48.dp))`).
- **Touch Target Compliance (≥48dp)**: Every interactive surface (IconButton, Button, OutlinedButton, filter chip, and PinPad key) must maintain an effective touch target of `≥48dp` (using `Modifier.size(48.dp)` or `Modifier.heightIn(min = 48.dp)`).
- **Compose File Granularity**: Target ~250 lines per `.kt` file; 500 lines hard ceiling. Decompose complex screen sections into dedicated sub-components.
- **Robolectric Test Target**: Robolectric tests must specify `@Config(sdk = [34])` to align with the supported Android 14 test runner target.

---

## 🛡️ Git Grounding & Operational Redlines
- **⚠️ NEVER Tag `@jules`**: When writing PR comments, commit messages, or issue responses, NEVER tag `@jules`. That handle belongs to an innocent third-party human user. Address Jules using plain text `Hey Jules` or `@google-labs-jules[bot]`.
- **Feature-Branch Push Pre-Clearance**: Non-destructive pushes to your assigned task/feature branch (`git push origin HEAD` or `git push origin <branch>`) are explicitly pre-authorized and expected once local tests pass green. Do not stall asking for permission to push to your own feature branch.
- **Zero Raw Force Pushes**: Never run `git push --force` or `--force-with-lease`. Never push directly to `main`.
- **Two-Layer Commit Attribution**: Every commit uses Conventional Commits with `User:` / `AI:` attribution blocks.
- **Sub-Second Pre-Commit Verification**: Run `python3 scripts/audit_docs_drift.py` (<1s) locally; run unit tests with `./gradlew testDebugUnitTest --no-daemon`.
