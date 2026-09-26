pr_version: 1.0.0
last_updated: 2026-08-29
source_branch: "feat/custom-fields-bitwarden"
target_branch: "main"
status: "[x] Ready | Draft [ ]"
brand: "ClawStack Studios©™"
author: "CrustAgent©™ / Lucas"

# 🦞 Pull Request: feat: Implement Zero-Knowledge Bitwarden Custom Fields & Master-Detail Modal Ergonomics

## 📐 Description
This Pull Request introduces client-side **Bitwarden-Style Custom Fields** (`Text`, `Hidden`, `Checkbox`, and dynamic `Linked` properties) across vault items, secure notes, and SSH keys. Every custom field is encrypted client-side using AES-256-GCM under item-scoped AAD namespaces (`vault_pearls_custom:{id}`, `vault_secure_notes_custom:{id}`, `vault_ssh_keys_custom:{id}`), backed by database migration `0003_custom_fields.up.sql`. Furthermore, this PR refactors data-entry modal dialogs with fixed `h-[90vh] md:h-[85vh]` viewport height, pinned headers/footers, sleek internal element scrolling, and an upward-expanding dropup menu.

---

## 🛠️ Key Transformations

### 1. ⚙️ Types, Validation & Database Schema
* **`src/types.ts`**:
  * Added `CustomField`, `CustomFieldType`, and `CustomFieldLinkedProperty` interfaces; added `custom_fields?: string` to `VaultItem`, `SecureNote`, and `SSHKey`.
* **`src/server/validation/schemas.ts`**:
  * Added `custom_fields: z.string().max(500000).optional()` validator to all item schemas.
* **`migrations/0003_custom_fields.up.sql`**:
  * Added `custom_fields TEXT DEFAULT ''` across `vault_pearls`, `vault_secure_notes`, and `vault_ssh_keys`.

### 2. 🔌 Backend Core & Zero Double-Encryption
* **`src/server/utils/metadataGuard.ts`**:
  * Excluded `custom_fields` column from server-side `metadataGuard` encryption, preventing double-encryption over client ciphertext.
* **`src/server/routes/vault.ts`, `notes.ts`, `sshKeys.ts`**:
  * Handled `custom_fields` persistence on create and update endpoints with tenant isolation.

### 3. 🎨 Frontend Interface & Component Parity
* **`src/components/Vault/ItemFormModal.tsx`**:
  * Pinned dialog header and action footer; enabled internal element scrolling (`flex-1 overflow-y-auto custom-scrollbar`).
  * Unified "Add Extra Field" into an animated upward dropup menu with click-outside dismissal.
* **`src/components/Vault/ItemDetailPane.tsx`**:
  * Added masked secret inspection cards, copy actions, and dynamic linked property resolution with live TOTP timer rings.
* **`src/index.css`**:
  * Added translucent `.custom-scrollbar` CSS utility.

### 4. 🔒 Cryptographic Membranes
* **`src/App.tsx`**:
  * Encrypted custom fields in-memory with AES-256-GCM before API mutations and decrypted on vault load.

---

## 🩺 Verification & Health Diagnostics

### 1. Test Suite Results
* **Vitest Execution (`npm run test`):**
  ```text
  ✓ tests/unit/customFields.test.ts (8 tests)
  ✓ tests/unit/webCryptoFallback.test.ts (8 tests)
  ✓ tests/vault-crud.test.ts (24 tests)
  Test Files  11 passed (11)
       Tests  172 passed (172)
    Start at  00:04:12
    Duration  11.45s
  ```

### 2. Compiler Health Check
* **Production Build (`npm run build`):**
  ```text
  vite v5.4.14 building for production...
  ✓ 2173 modules transformed.
  dist/index.html                   2.14 kB │ gzip:  0.89 kB
  dist/assets/index-D8x_1K2a.js   1,412.30 kB │ gzip: 421.15 kB
  dist/assets/index-B7y9_XqL.css    28.12 kB │ gzip:  6.44 kB
  ✓ built in 1m 1s
  Exit code: 0
  ```

### 3. Type Safety & ESLint Verification
* **TypeScript & ESLint Check (`npm run lint`):**
  ```text
  Exit code: 0 (0 errors, 0 warnings)
  ```

---

## 🦞 Invariant Integrity Audit

* **[x] Zero-Knowledge Isolation:** Verified client-side AES-256-GCM encryption under item-scoped AAD namespaces.
* **[x] Zero Plaintext `hu-` Transmission:** Ensured master human keys never cross the wire.
* **[x] User Isolation:** Confirmed all SQLite queries filter strictly by `user_uuid`.
* **[x] Timing Attacks:** Enforced constant-time token comparison across all auth paths.
* **[x] No Double-Encryption:** Verified `custom_fields` is omitted from `metadataGuard.ts`.
* **[x] Memory Zeroization:** Confirmed `shellKey` and custom fields purge from React memory on lock.

---

**Maintained by ClawStack Studios©™**
