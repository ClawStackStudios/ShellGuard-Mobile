# 🐚 ShellGuard Mobile — Release v0.0.0.3 (Build 3)

> **Phase 2: Ktor Sync, Vault Dashboard & IME Hardening**: versionCode 3 (`versionName = "0.0.0.3"`). Delivers the core networking layer (`ShellGuardClient`) with direct LAN and mesh support, bidirectional delta synchronization (`SyncRepository`) with Bitwarden-model Read-Only offline caching, the master-detail Vault Dashboard with instant debounced search and Pod filter chips, Base62 sovereign identity key parity, and soft keyboard IME hardening with visual cursor retention.

## *Phase 2: Ktor API Client, Bidirectional Sync, Vault Dashboard & IME Hardening*

```text
███████╗██╗   ██╗███████╗██╗     ██╗     ██████╗ ██╗   ██╗ █████╗ ██████╗ ██████╗ 
██╔════╝██║   ██║██╔════╝██║     ██║     ██╔════╝ ██║   ██║██╔══██╗██╔══██╗██╔══██╗
███████╗███████║█████╗   ██║     ██║     ██║  ███╗██║   ██║███████║██████╔╝██║   ██║
╚════██║██╔══██║██╔══╝   ██║     ██║     ██║   ██║██║   ██║██╔══██║██╔══██╗██║   ██║
███████║██║   ██║███████╗███████╗███████╗╚██████╔╝╚██████╔╝██║   ██║██║  ██║██████╔╝
╚══════╝╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝ ╚═════╝  ╚═════╝ ╚═╝   ╚═╝╚═╝  ╚═╝╚═════╝ 
                                                  ~ **ClawStack Mobile Studios©™** ~
```

---

## 🚀 The Core Summary

Welcome to **v0.0.0.3** of **ShellGuard Mobile** — **Phase 2: Ktor API Client, Bidirectional Sync, Vault Dashboard & IME Hardening (Build 3)**. This release delivers Tasks 03 and 04 of the master roadmap, establishing the direct communication bridge to self-hosted ShellGuard servers, the local multi-domain synchronization engine, and the primary secrets vault dashboard.

Vault users gain full multi-domain visibility across Passwords (Vault Pearls), Secure Notes, and SSH Keys within a unified reactive stream. Disconnected environments are protected by our Bitwarden-style Read-Only offline caching model, allowing 100% access to view, search, and copy credentials without split-brain divergence. Furthermore, forensic physical testing on Google Pixel hardware identified and permanently resolved Base62 identity key intake and soft keyboard surface composition blackouts.

Pre-flight verification passed 100% green across all 23 unit and Robolectric tests, verified live on connected Google Pixel hardware.

---

## 💎 Key Themes & Highlights

### 🌐 1. Ktor Network Layer & Home Lab Mesh Transport (Task 03)
* **Ktor Client with OkHttp Engine**: Direct, lightweight HTTP/HTTPS client communicating with the self-hosted ShellGuard Express 5 API (`:6464` / `:6565`).
* **Home Lab Cleartext Support**: Configured `ConnectionSpec.CLEARTEXT` enabling direct connection to local home lab servers (Unraid, TrueNAS, LAN IPs) and Tailscale/WireGuard CGNAT mesh routes without requiring external public domain TLS certificates.
* **Session Management (`EncryptedDeviceVault`)**: Anchors bearer session tokens, server URLs, owner UUIDs, and usernames inside Android KeyStore-backed `EncryptedSharedPreferences` with in-memory zeroization.

### 🔄 2. Bidirectional Delta Sync & Bitwarden Offline Engine (Task 03)
* **Multi-Domain Delta Synchronization (`SyncRepository`)**: Downstream pull across Pearls, Secure Notes, and SSH Keys reconciling server timestamps (`remote_updated_at`) and pruning deleted records.
* **Bitwarden-Model Read-Only Offline Caching**: When disconnected (`OfflineReadOnly`), the vault remains 100% accessible for viewing, searching, and copying credentials. UI mutation guards (disabled create/edit/delete actions) eliminate split-brain synchronization conflicts.
* **Automated Network Monitoring (`ConnectivityMonitor`)**: Active Android `NetworkCallback` listener triggering automated `GET /api/health` probes on network availability, automatically transitioning from `OfflineReadOnly` to `OnlineSynced`.

### 📦 3. Master-Detail Vault Dashboard & Pod Filters (Task 04)
* **Unified Reactive Stream**: Merges `vault_pearls`, `vault_secure_notes`, and `vault_ssh_keys` Room flows into a unified `UnifiedVaultItem` stream via `kotlinx.coroutines.flow.combine`.
* **Instant Debounced Search**: Sub-16ms real-time filtering across account titles, usernames, URLs, and notes.
* **Horizontal Pod Filter Chips**: Rapidly filter items by domain (`All`, `Passwords`, `Notes`, `SSH Keys`) with live item counters.
* **Server Health Banner**: Displays live server connectivity badges and offline status alerts.

### 🔑 4. Base62 Sovereign Identity Key Parity
* **Format Parity**: Upgraded `CLAW_KEY_REGEX` in `ClawCrypto` from strictly hexadecimal (`[0-9a-f]`) to 67-character Base62 (`[0-9a-zA-Z]`), restoring full compatibility with identity files exported by the ShellGuard web client and server.
* **Identity File Extraction**: Added automatic UUID parsing from uploaded identity files (`uploadedUuid`) passing directly into gateway authentication.

### ⌨️ 5. Soft Keyboard IME Hardening & Visual Cursor Retention
* **Release-Scoped `FLAG_SECURE`**: Scoped `FLAG_SECURE` in `MainActivity` strictly to release builds (`if (!BuildConfig.DEBUG)`), eliminating a hardware surface composer failure on Adreno 530 GPUs where insecure system IME overlays blacked out the window.
* **Scaffold Double Inset Isolation**: Set `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0))` on the root Activity Scaffold to prevent double-subtraction of keyboard height when child screens apply `.imePadding()`.
* **Smooth Focused Auto-Scroll**: `GatewayScreen` automatically scrolls focused URL and port inputs cleanly into view above the soft keyboard.

---

## 🧪 Verification Record

* **23 unit and Robolectric tests** passing 100% green (`./gradlew testDebugUnitTest`).
* **Clean build verification**: `./gradlew assembleDebug` compiled and packaged cleanly.
* **Pre-flight invariants**: `targetSdk = 36`, `sqlcipher 4.6.1+` (16 KB-aligned ELF segments), `jniLibs.useLegacyPackaging = false`, dynamic theming with `LocalShellGuardColors`.
* **Physical Device Walkthrough (Google Pixel LineageOS Android 14)**:
  - Verified Gateway segmented URL input with soft keyboard open.
  - Verified illuminated screen and crisp pink cursor retention during text entry.
  - Verified Base62 identity file loading and active "Login with Identity File" button.
  - Verified Compose navigation transition between Gateway and Dashboard.

---

<div align="center">
  <sub>Engineered with precision for the ClawStack / ShellGuard ecosystem.</sub>
</div>
