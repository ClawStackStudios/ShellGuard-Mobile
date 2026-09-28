# Test Oracle & Verification Invariants

> *"A test oracle is my source of truth. I update this test oracle with new edge cases I find patterns for as I work."* — `AGENTS.md`

## 1. The 3 Verification Gates
Verification is non-negotiable. Passing tests alone do not guarantee a working artifact. We stack 3 gates:

1. **Gate 1: Unit & Domain Tests**:
   - Command: `./gradlew testDebugUnitTest`
   - Requirement: 100% green pass rate across all unit, crypto, and Robolectric suites.
2. **Gate 2: Build Compilation**:
   - Command: `./gradlew assembleDebug`
   - Requirement: Clean artifact compilation without unresolved symbols or packaging errors.
3. **Gate 3: Release Bundle & Keystore Signing**:
   - Command: `./gradlew bundleRelease assembleRelease`
   - Requirement: ProGuard/R8 passes without stripping serialization companions, SQLCipher JNI bindings, or Room DAOs.

## 2. Load-Bearing Redlines (Never Regress)
These are hard, non-negotiable invariants discovered through architectural audits and penetration tests:

1. **Zero-Knowledge Invariant**:
   The server NEVER receives the raw master identity key (`hu-` or `lb-`). The client derives and transmits `SHA-256(key)` exclusively.
2. **Session Atomicity Invariant**:
   An active session requires BOTH transport authorization (`sessionToken`) and cryptographic capability (`shellKey`). `hasActiveSession()` must strictly verify `getInMemoryShellKey() != null`. A session with a valid network token but a missing decryption key is an invalid split-brain state.
3. **Hybrid Vault Storage & CursorWindow Defense (CWE-400)**:
   Android SQLite enforces a hard 2MB `CursorWindow` row limit. Storing multi-megabyte attachments directly in Room BLOBs is strictly forbidden. Room stores metadata only; payloads stream to disk via `CipherInputStream` / `CipherOutputStream` in `<= 64KB` buffers.
4. **AutoSpill & WebView Hierarchy Isolation**:
   Autofill forms rendered inside WebViews must never match host native package IDs. The extracted web domain hierarchy takes precedence over the host container application.
5. **Fail-Closed Cryptography**:
   Any cryptographic MAC failure, decryption anomaly, or corrupted envelope must fail closed immediately, zeroizing volatile buffers and aborting. It must never expose raw ciphertext or fall into infinite retry loops.
6. **Robolectric Target SDK Ceiling (`sdk=34`)**:
   Target SDK 36 causes `UnsupportedOperationException` on host/CI Robolectric providers. All Robolectric test classes and `robolectric.properties` must enforce `sdk = 34`.

## 3. Active Test Suite Inventory
- **`ClawCryptoTest`**: ClawKey Base62 format validation (67 chars), SHA-256 hashing.
- **`ShellCryptionEngineTest`**: HKDF-SHA-256 derivation, AES-GCM-256 with 10 AAD namespaces, JSON envelope serialization.
- **`CustomFieldTest`**: Bitwarden polymorphic custom field JSON serialization (`TEXT`, `HIDDEN`, `BOOLEAN`, `LINKED`).
- **`SyncRepositoryTest`**: Room SQLCipher DAO interaction, conflict reconciliation, delta synchronization.
- **`EncryptedDeviceVaultTest`**: KeyStore key persistence, cold-restart key re-hydration, RAM zeroization.
- **`Base32DecoderTest`**: RFC 4648 Base32 decoding, padding handling, character sanitization.
- **`TotpEngineTest`**: RFC 6238 HMAC-SHA1/256/512, Steam Guard alphanumeric tokens, dynamic truncation.
- **`TotpUriParserTest`**: `otpauth://` URI parsing, secret extraction, algorithm parameter detection.
- **`PasswordGeneratorTest`**: Cryptographic SecureRandom entropy, diceware passphrase generation.
- **`VaultLockManagerTest`**: Auto-lock timeout state machine, background lock timers.
- **`DomainMatcherTest`**: URI match modes (`BASE_DOMAIN`, `HOST`, `EXACT`, `STARTS_WITH`, `NEVER`, and `androidapp://`).
- **`AutofillStructureParserTest`**: View hierarchy traversal, 64-level recursion ceiling, AutoSpill defense.
