pr_version: 1.0.0
last_updated: [YYYY-MM-DD]
source_branch: "feat/[feature-name]"
target_branch: "main"
status: "[ ] Draft | Ready [x]"
brand: "ClawStack Studios©™"
author: "CrustAgent©™ / Lucas"

# 🦞 Pull Request: [Title of the Pull Request]

## 📐 Description
[Provide a comprehensive, high-level narrative explaining the goal of this Pull Request. What architectural gaps does this bridge? What problem is solved? Ensure the description traces connections from entry points to the SQLite database schemas, and outlines the overall system blast radius.]

---

## 🛠️ Key Transformations

[Group all files logically by their system domains (dependencies first). Outline precisely what changed and why, using directory links to maintain high readability.]

### 1. ⚙️ Configuration & Migrations
* **`[migrations/XXXX_name.up.sql]`**:
  * [Transformation description, e.g. "Added column X to table Y with non-destructive default."]
* **`[package.json]`**:
  * [Transformation description, e.g. "Added dependency Z or bumped version."]

### 2. 🔌 Backend Core & Validation
* **`[src/server/validation/schemas.ts]`**:
  * [Transformation description, e.g. "Added Zod schema validation for new field."]
* **`[src/server/routes/...]`**:
  * [Transformation description, e.g. "Enforced user_uuid checks and permission guards."]

### 3. 🎨 Frontend Interface & State
* **`[src/components/Vault/...]`**:
  * [Transformation description, e.g. "Implemented modal internal element scrolling and dropup menu."]
* **`[src/App.tsx]`**:
  * [Transformation description, e.g. "Bound AES-256-GCM client encryption to item-scoped AAD."]

### 4. 🔒 Security Membranes & Cryptography
* **`[src/server/utils/metadataGuard.ts]`**:
  * [Transformation description, e.g. "Excluded client ciphertext from server re-encryption."]
* **`[src/lib/webCryptoFallback.ts]`**:
  * [Transformation description, e.g. "Ensured fallback primitives handle non-secure LAN origins."]

---

## 🩺 Verification & Health Diagnostics

[Execute all system validation pipelines and paste their exact stdout snapshots here. Never hand-wave verification.]

### 1. Test Suite Results
* **Vitest Execution (`npm run test`):**
  ```text
  [Paste raw vitest console output here, e.g. "Tests  172 passed (172)"]
  ```

### 2. Compiler Health Check
* **Production Build (`npm run build`):**
  ```text
  [Paste build compiler logs showing zero errors and bundle sizes, e.g. "✓ built in 1m 1s"]
  ```

### 3. Type Safety & Lint Verification
* **TypeScript & ESLint (`npm run lint`):**
  ```text
  [Paste lint/type-check console output, e.g. "Exit code: 0"]
  ```

---

## 🦞 Invariant Integrity Audit

* **[ ] Zero-Knowledge Isolation:** Confirmed client encrypts secrets with AES-256-GCM + item-scoped AAD before network transmission.
* **[ ] Zero Plaintext `hu-` Transmission:** Verified human root key is never sent over the wire; client transmits only salted `SHA-256(hu-)` hash.
* **[ ] Tenant Scoping:** Verified that all database queries filter strictly by `user_uuid` (zero multi-tenant leakage).
* **[ ] Parameterized SQL:** Confirmed that all SQLite queries use parameterized bindings (zero string concatenation).
* **[ ] Timing Attacks:** Ensured constant-time token comparison (`constantTimeCompare`) is enforced across all auth checks.
* **[ ] Memory Zeroization:** Verified that on lock or logout, `shellKey` and all decrypted secrets are zeroed from React state.
* **[ ] No Double-Encryption:** Confirmed client ciphertexts (e.g. `custom_fields`) bypass server `metadataGuard` re-encryption.

---

**Maintained by ClawStack Studios©™**
