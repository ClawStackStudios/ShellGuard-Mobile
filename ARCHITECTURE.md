# 🏗️ ShellGuard Mobile — Client Architecture & Boundaries

> **Technical Blueprint: Client-Server Relationships, System Boundaries & Mobile Architecture**  
> *Targeted for Google AI Studio Android Application Generator.*

---

## 1. System Role & Philosophy

The **ShellGuard Mobile** Android application operates as the **full secrets vault native Android client** connected to self-hosted ShellGuard servers or operating in sovereign offline autonomy.

- **Full Secrets Vault**: Provides full bidirectional access across all secrets domains: **Vault Pearls (Logins/Passwords)**, **Secure Notes**, **SSH Keys**, **Secure Attachments**, and integrated **TOTP generation**.
- **Autonomous Operability**: Follows the Bitwarden offline paradigm: when disconnected from the server, 100% of read, search, copy, autofill, and TOTP ticker functionality remains operational. Mutation actions (create/edit/delete) are guarded to prevent split-brain conflicts.
- **Zero-Knowledge Decryption**: The server stores only opaque `ShellCryption` ciphertext envelopes. The Android client independently derives domain-specific AES-GCM-256 keys from the human master key (`hu-`) using HKDF-SHA256 and decrypts secrets strictly in client volatile memory.

```mermaid
flowchart LR
    subgraph ServerDomain ["Server Boundary (Untrusted Storage)"]
        Server[ShellGuard Express API]
        ServerDB[(SQLite Bedrock<br/>Encrypted Blobs)]
        ServerAudit[(Segregated Audit DB)]
        Server <--> ServerDB
        Server --> ServerAudit
    end

    subgraph Boundary ["Transport Boundary (HTTP/HTTPS / Mesh)"]
        Payload["ShellResponse JSON<br/>(ShellCrypted Blobs + Metadata)"]
    end

    subgraph ClientDomain ["Android Client Boundary (Trusted Execution)"]
        KeyStore["Android KeyStore<br/>(Hardware-backed Key)"]
        ShellEngine["ShellCryption Engine<br/>(HKDF + AES-GCM-256)"]
        LocalDB[("Room Database<br/>(SQLCipher Encrypted)")]
        FSVault[("Hybrid File-System Vault<br/>filesDir/vault_attachments/*.enc")]
        TotpEngine["RFC 6238 Generator"]
        UI["Jetpack Compose UI"]

        KeyStore <--> ShellEngine
        ShellEngine <--> LocalDB
        ShellEngine <--> FSVault
        LocalDB --> TotpEngine
        LocalDB --> UI
        TotpEngine --> UI
    end

    ServerDomain <==> Boundary <==> ClientDomain
```

---

## 2. Client-Server Division of Responsibilities

| Responsibility Area | ShellGuard Web Server | ShellGuard Mobile Android Client |
|---|---|---|
| **Identity & Authentication** | Validates SHA-256 key hash of `hu-` key; issues short-lived bearer session token. | Captures `hu-` / `lb-` key via file upload or paste, validates Base62 format, hashes key locally, and requests session token. |
| **Secrets Decryption** | **NEVER** decrypts secrets. Stores only ShellCryption ciphertext envelopes. | Derives domain-bound AES keys via HKDF-SHA256 and decrypts passwords, notes, and SSH keys in ephemeral memory. |
| **Storage & Persistence** | Authoritative remote store (`vault_pearls`, `vault_secure_notes`, `vault_ssh_keys`). | Local offline cache in SQLCipher Room DB. Full vault access persists across app restarts without network. |
| **Attachment Handling** | Stores encrypted binary blobs in filesystem storage. | Hybrid File-System Vault: metadata in Room, ciphertext streamed to disk in 64KB buffers to avoid 2MB `CursorWindow` limits. |
| **Delta Synchronization** | Serves full/filtered domain records and accepts mutation requests. | Bidirectional delta sync: reconciles server timestamps, pushes local pending changes, and prunes remotely deleted items. |
| **Biometric Security** | No awareness of biometrics. | Protects cached vault keys in hardware `AndroidKeyStore` with `BiometricPrompt` and key-invalidation recovery. |

---

## 3. Lifecycle & Operational Modes

### Mode A: Initial Setup & Gateway Authentication (Online)
1. **User Identity Pairing**: User loads their `shellguard_identity_*.json` file or pastes their 67-character Base62 `hu-...` Human Identity Key.
2. **Server Discovery**: User configures server URL (e.g. `http://192.168.1.5:6464` or `https://vault.example.com`).
3. **Session Authentication**: Client sends `SHA-256(huKey)` and owner UUID to `POST /api/auth/token`, receiving session token `api-...`.
4. **Master Key Sealing**: If biometrics are enabled, the raw key is encrypted using a KeyStore-generated AES-256-GCM key and saved in `EncryptedSharedPreferences`.
5. **Initial Vault Hydration**: Client initiates downstream delta sync across all domains (`vault_pearls`, `vault_secure_notes`, `vault_ssh_keys`), decrypts items, and populates the local Room database.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant App as Android Client
    participant KS as Android KeyStore / DeviceVault
    participant Room as Room Encrypted DB
    participant Server as ShellGuard API

    User->>App: Load Identity JSON & Server URL
    App->>App: Validate Base62 hu- Key & Compute SHA-256
    App->>Server: POST /api/auth/token { keyHash, uuid }
    Server-->>App: { token: "api-...", user: { uuid, username } }
    App->>KS: Persist Session & Wrap Master Key
    App->>Server: GET /api/vault (Bearer api-...)
    Server-->>App: List<PearlDto>
    App->>Server: GET /api/notes
    Server-->>App: List<SecureNoteDto>
    App->>Server: GET /api/keys
    Server-->>App: List<SshKeyDto>
    App->>App: Derive HKDF Domain Keys & Decrypt Records
    App->>Room: Upsert Decrypted Vault Entities
    App-->>User: Display Vault Dashboard with Pod Filters
```

---

### Mode B: Everyday Offline Operation (Zero Network)
1. **App Launch**: User opens app without internet connection.
2. **Biometric Unlock**: Biometric prompt verifies identity and unseals database encryption keys.
3. **Instant Cached Display**: Room DB streams unified items (`UnifiedVaultItem`) via Kotlin `Flow`.
4. **Full Read-Only Access**: View passwords, copy credentials with sensitive masking, read notes, inspect SSH keys, and run TOTP countdown rings.
5. **Mutation Guards**: UI dims create/edit/delete controls and displays an amber `OfflineReadOnly` banner, preventing split-brain divergence.

```mermaid
flowchart TD
    Launch[User Launches App Offline] --> Bio{Biometric Prompt / PIN}
    Bio -->|Success| Unlock[KeyStore Unlocks SQLCipher Room DB]
    Bio -->|Failure| Fallback[Prompt Master hu- Key]
    Unlock --> Flow[Observe Room DAO Combined Flow]
    Flow --> UI[Render Unified Dashboard & Pod Filter Chips]
    UI --> Actions[Masked Copy, Search, TOTP Tickers, Autofill]
```

---

### Mode C: Background / Foreground Bidirectional Sync (Connected)
1. **Trigger**: Foreground pull-to-refresh, navigation focus, or Android `ConnectivityManager.NetworkCallback` reconnect.
2. **Health Probe**: Client probes `GET /api/health` to confirm server reachability.
3. **Token Refresh**: If session token has expired, silent re-authentication executes using stored key hash.
4. **Reconciliation**:
   - Pushes any local pending changes.
   - Pulls server deltas across Pearls, Notes, and SSH Keys.
   - Timestamp comparison determines server-wins resolution.
   - Prunes local records deleted remotely.
   - Updates `SyncMetadataEntity.lastSyncTimestamp`.

---

## 4. Mobile Threat Model & Defenses

```mermaid
mindmap
  root((Mobile Security Boundaries))
    Physical Device Theft
      Hardware-backed Android KeyStore
      Biometric authentication required
      SQLCipher AES-256 whole-database encryption
      Automatic biometric invalidation recovery
    Memory Forensics
      Plaintext char arrays zeroized after use
      No debug logs of decrypted secrets
      Release-scoped FLAG_SECURE window shielding
    Network Interception
      Zero plaintext secrets transmitted
      Session tokens expire in 24 hours
      Client-side HKDF-SHA256 derivation
      Relaxed trust-on-first-use for LAN/mesh
    App Sandboxing
      Internal scoped storage only
      Hybrid file-system attachments in filesDir
      No third-party analytics or crash SDKs
```

1. **Hardware-Backed Cryptographic Isolation**: Master keys never live in unencrypted preferences. Key derivation utilizes `AndroidKeyStore` with `setUserAuthenticationRequired(true)`.
2. **Memory Hygiene & Secret Zeroization**: Plaintext secrets are cleared from RAM upon session lock or screen exit.
3. **Screen Capture Defense (`FLAG_SECURE`)**: Scoped to release builds, blocking screen recorders, malware overlays, and task-switcher previews while preserving developer inspection in debug builds.
4. **Sensitive Clipboard Masking (CWE-359)**: Password copies apply `ClipDescription.EXTRA_IS_SENSITIVE = true` to suppress visual cleartext previews in Android 13+ clipboard overlays, paired with an automated 30s background scrub timer.
5. **Emergency Panic Purge (`ACTION_PANIC_WIPE`)**: Broadcast receiver executes an instant irreversible wipe across KeyStore aliases, EncryptedSharedPreferences, Room database files, and terminates the process immediately.

---

<div align="center">
  <sub>Engineered with precision for the ClawStack / ShellGuard ecosystem.</sub>
</div>
