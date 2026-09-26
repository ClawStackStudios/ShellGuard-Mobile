# 🦞 Rule: Lobsterized©™ & ShellGuard Sovereign Protocol

## Core Stance
I am building and maintaining Lobsterized sovereign software. I prioritize user cryptographic sovereignty, zero-knowledge isolation, and explicit agent capability boundaries over convenience or cloud centralization.

---

## 🔐 Cryptographic & Auth Directives

1. **Key Hierarchy Enforcement:**
   - `hu-[64chars]` (Human Root): Client-generated master key. **NEVER transmit in plaintext to the server.** Only send client-computed `SHA-256(hu-)` for login.
   - `api-[32chars]` (Session Token): Store ONLY in `sessionStorage`. Never persist to `localStorage`. Must be purged immediately on lock, logout, or session expiry.
   - `lb-[64chars]` (Lobster Key): Scoped agent tokens. Must be revocable in 1 click without affecting human sessions.
2. **Client-Side ShellCryption©™:**
   - Encrypt all vault items, secure notes, SSH keys, and custom fields client-side using AES-256-GCM.
   - Always bind encryption to item-scoped AAD namespaces (`vault_pearls:{id}`, `vault_secure_notes_custom:{id}`, `vault_ssh_keys_custom:{id}`).
   - **Zero Double-Encryption:** Exclude client ciphertexts (e.g. `custom_fields`) from server-side `metadataGuard.ts` re-encryption.
3. **Memory Zeroization:**
   - On lock or logout, immediately purge `shellKey` and all decrypted plaintext secrets from React state and memory.

---

## 📥 Zero-Knowledge Ingestion & Third-Party Migration Directives

1. **Strict In-Memory Sanitization:**
   - When importing third-party archives (Bitwarden, Aegis, 2FAS, Google Authenticator), extract TOTP credentials and immediately purge passwords, notes, credit card data, and identity fields in volatile memory.
   - Non-TOTP credentials must NEVER touch persistent databases, logs, or disk caches.
2. **Deterministic Duplicate Resolution:**
   - Always require explicit conflict handling (`SKIP_DUPLICATES`, `OVERWRITE_EXISTING`, `KEEP_BOTH`) to prevent silent overwrites or state corruption.
3. **Dual-Route Sovereignty:**
   - Route imported items deterministically to either Local SQLCipher (`is_local_only = 1`) or Remote Gateway (`ShellCryptionEngine` HKDF + AES-GCM with AAD binding).
4. **Immutable Audit Hook:**
   - Record every batch migration in `security_audit_logs` and trigger automated backup snapshots if enabled.

---

## 🛡️ Backend & Data Directives

1. **Tenant Isolation:** Every SQLite query MUST include `WHERE user_uuid = ?` (or parameterized equivalent). Multi-tenant leakage is a catastrophic security failure.
2. **SQL Safety:** Use 100% parameterized SQL bindings. Never concatenate strings into queries.
3. **Timing-Attack Immunity:** Always use constant-time byte/character comparison for token and hash checks.
4. **CORS Hardening:** `CORS_ORIGIN` must match configured frontend origin. Wildcards (`*`) are strictly prohibited.
5. **LAN Origin Resilience:** When subtle crypto is unavailable (plain HTTP LAN IPs), ensure pure TypeScript `webCryptoFallback.ts` handles cryptographic primitives.

---

## 🤖 Agent Capability Directives

When implementing or modifying Lobster Key routes:
- `GET` ➔ Requires `canRead`
- `POST` ➔ Requires `canWrite`
- `PUT` / `PATCH` ➔ Requires `canEdit`
- `DELETE` ➔ Requires `canDelete`
- Sensitive root operations (e.g., key rotation, account deletion) MUST require `requireHuman()` guard.

---

## 🎨 UI & Design Directives (Reef Modernist)

1. **Theme Tokens:** Always use CSS Custom Properties (`--bg-base`, `--bg-surface`, `--text-main`, `--text-muted`, `--border-subtle`) for dual-mode support.
2. **Master-Detail Layout:** Respect the three-pane navigation model (`SidebarFolderTree` ➔ `ItemListPane` ➔ `ItemDetailPane`).
3. **Modal Ergonomics:** Modals must use fixed dialog height (`h-[90vh] md:h-[85vh]`), spacious `max-w-3xl` width, pinned headers/footers, and internal scrolling (`overflow-y-auto custom-scrollbar`).
4. **Dropup Menus:** Action submenus near page bottoms must expand upward (`bottom-full mb-2`) with click-outside backdrop dismissal.
5. **Custom Fields UX:** Render 📝 **Text**, 🔒 **Hidden** (with mask/reveal eye), ☑️ **Checkbox** (status chip), and 🔗 **Linked** (dynamically resolved property + live TOTP) according to Reef Modernist conventions.

---

## ⛔ Inviolable Anti-Patterns (The "NEVER" List)

- ❌ NEVER send a `hu-` human root key to any backend endpoint.
- ❌ NEVER store authentication tokens in `localStorage`.
- ❌ NEVER execute a database query without `user_uuid` tenant scoping.
- ❌ NEVER use raw string concatenation in SQL queries.
- ❌ NEVER double-encrypt client ciphertexts under server `DB_ENCRYPTION_KEY`.
- ❌ NEVER introduce hardcoded default pods or categories.
- ❌ NEVER use non-constant-time equality (`===`) for security tokens.
