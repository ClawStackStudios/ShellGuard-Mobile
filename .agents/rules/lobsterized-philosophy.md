# 🦞 Rule: Lobsterized©™ & ShellGuard Sovereign Mobile Protocol

## Core Stance
I am building and maintaining Lobsterized sovereign software. I prioritize user cryptographic sovereignty, zero-knowledge isolation, and explicit agent capability boundaries over convenience or cloud centralization.

---

## 🔐 Cryptographic & Auth Directives

1. **Key Hierarchy Enforcement:**
   - `hu-[0-9a-zA-Z]{64}` (Human Root): Client-generated sovereign master key (Base62 format). **NEVER transmit in plaintext to the server.** Only send client-computed `SHA-256(hu-)` for login authentication.
   - `api-[32chars]` (Session Token): Stored exclusively in hardware-backed `EncryptedSharedPreferences` (`EncryptedDeviceVault`). Never persist in unencrypted shared preferences or Room. Must be purged immediately on lock, logout, or session expiry.
   - `lb-[0-9a-zA-Z]{64}` (Lobster Key): Scoped agent tokens. Must be revocable in 1 click without affecting human sessions.
2. **Client-Side ShellCryption©™:**
   - Encrypt all vault items (passwords, secure notes, SSH keys, attachments, custom fields) client-side using HKDF-SHA-256 + AES-GCM-256.
   - Always bind encryption to item-scoped AAD namespaces (`vault_pearls:{id}`, `vault_pearls_custom:{id}`, `vault_pearls_history:{id}`, `vault_secure_notes:{id}`, `vault_secure_notes_custom:{id}`, `vault_ssh_keys:{id}`, `vault_ssh_keys_custom:{id}`, `vault_secure_attachments:{id}`, `totp_backup:{ownerUuid}`).
   - **Zero Double-Encryption:** Exclude client ciphertexts from server-side re-encryption.
3. **Memory Zeroization:**
   - On lock or logout, immediately purge `shellKey` and all decrypted plaintext secrets from memory (`EncryptedDeviceVault.zeroizeMemory()`).

---

## 📥 Zero-Knowledge Ingestion & Third-Party Migration Directives

1. **Full Multi-Domain Ingestion & Pre-DAO Deduplication:**
   - When importing third-party archives (Bitwarden JSON, ShellGuard `.sgvault.bak`, `.sgtotp.bak`), ingest all supported domains (Passwords, Secure Notes, SSH Keys, TOTP seeds, Custom Fields, Tags) into their respective Room entities with pre-DAO deduplication fingerprinting.
   - Do NOT purge non-TOTP credentials on the mobile vault client. Full multi-domain data fidelity is mandatory.
2. **Deterministic Duplicate Resolution:**
   - Always require explicit conflict handling (`SKIP_DUPLICATES`, `OVERWRITE_EXISTING`, `KEEP_BOTH`) to prevent silent overwrites or state corruption.
3. **Dual-Route Sovereignty:**
   - Route imported items deterministically to either Local SQLCipher (`isLocalOnly = 1`) or Remote Gateway (`ShellCryptionEngine` HKDF + AES-GCM with AAD binding).
4. **Immutable Audit Hook:**
   - Record every batch migration in local `audit_logs` Room table.

---

## 🛡️ Database & Data Directives

1. **Tenant Isolation:** Every Room query MUST include `WHERE ownerUuid = :ownerUuid` (or parameterized equivalent). Multi-tenant leakage is a catastrophic security failure.
2. **SQL Safety:** Use 100% Room parameterized queries. Never construct raw SQL strings with untrusted inputs.
3. **Timing-Attack Immunity:** Always use constant-time byte comparison (`MessageDigest.isEqual`) for token and hash checks.
4. **LAN Transport Resilience:** Plain cleartext HTTP is intentional and supported for local home lab servers (Unraid, TrueNAS, Tailscale mesh) via `network_security_config.xml`.

---

## 🤖 Agent Capability Directives

When implementing or modifying Lobster Key routes:
- `GET` ➔ Requires `canRead`
- `POST` ➔ Requires `canWrite`
- `PUT` / `PATCH` ➔ Requires `canEdit`
- `DELETE` ➔ Requires `canDelete`
- Sensitive root operations (e.g., key rotation, account deletion) MUST require `requireHuman()` guard.

---

## 🎨 UI & Design Directives (Reef Modernist Mobile)

1. **Dynamic Compose Tokens:** Always bind strictly to `MaterialTheme.colorScheme` and `LocalShellGuardColors`. Never hardcode static color hexes. Default accent is `ThemeAccent.REEF_DEFAULT` (Reef Pink `#E4048A`).
2. **Adaptive Master-Detail Layout:**
   - Compact Phones: Fluid single-column navigation (`Dashboard` ➔ `Detail` ➔ `Form`).
   - Tablets & Foldables (>= 840dp): 3-pane layout (`SidebarFolderTree` ➔ `ItemListPane` ➔ `ItemDetailPane`) matching desktop web client ergonomics.
3. **Keyboard & Scrolling Ergonomics:**
   - All interactive input forms MUST apply `.imePadding()` and `.verticalScroll(rememberScrollState())` to prevent the soft keyboard from obscuring inputs. Action menus near bottoms must expand upward (dropup).
4. **Fail-Safe Error Navigation:**
   - Every error, locked, or unauthenticated screen MUST provide an explicit, accessible navigation exit route (`onBackClick` or close action) alongside any retry action.
5. **Sensitive Masking (CWE-359):**
   - Clipboard copies must apply `ClipDescription.EXTRA_IS_SENSITIVE = true` on Android 13+ with an automated 30s background scrubbing timer. Password inputs must apply `PasswordVisualTransformation()` and disable autocorrect.
6. **Custom Fields UX:**
   - Support `TEXT`, `HIDDEN` (with mask/reveal eye & copy), `BOOLEAN` (chip badge), and `LINKED` (dynamically resolved property + live TOTP) according to Reef Modernist conventions.

---

## ⛔ Inviolable Anti-Patterns (The "NEVER" List)

- ❌ NEVER send a `hu-` human root key in plaintext to any backend endpoint.
- ❌ NEVER store authentication tokens in unencrypted SharedPreferences or Room DB.
- ❌ NEVER execute a database query without `ownerUuid` tenant scoping.
- ❌ NEVER purge non-TOTP credentials during vault import on the mobile vault client.
- ❌ NEVER double-encrypt client ciphertexts under server encryption keys.
- ❌ NEVER use raw string concatenation in SQL queries.
- ❌ NEVER use non-constant-time equality for security tokens or key hashes.
- ❌ NEVER trap users in an error screen without an accessible exit/back navigation route.
