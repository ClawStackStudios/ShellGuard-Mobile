# 🐚 ShellGuard Mobile — Release v0.0.0.4 (Build 4)

> **Phase 3: Vault Domains, Universal Item Editor & Zero-Knowledge Session Atomicity**: versionCode 4 (`versionName = "0.0.0.4"`). Delivers the multi-domain secrets data layer across Passwords, Secure Notes, and SSH Keys, Bitwarden-style Custom Fields (`TEXT`, `HIDDEN`, `BOOLEAN`, `LINKED`), defensive automated Password History versioning, the polymorphic Universal ItemFormScreen and ItemDetailScreen, and KeyStore-backed Zero-Knowledge Session Atomicity with cold-restart persistence.

## *Phase 3: Vault Domains, Universal Item Editor & Zero-Knowledge Session Atomicity*

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

Welcome to **v0.0.0.4** of **ShellGuard Mobile** — **Phase 3: Vault Domains, Universal Item Editor & Zero-Knowledge Session Atomicity (Build 4)**. This release marks the completion of Phase 3 (Tasks 05 and 06) of the master roadmap, achieving **50% completion** of the full mobile vault client.

This release unlocks comprehensive polymorphic CRUD operations across all three core secrets domains: **Passwords (Vault Pearls)**, **Secure Notes**, and **SSH Keys**. Users can now create, edit, inspect, and delete items with full cryptographic isolation. Custom fields modeled after Bitwarden (`TEXT`, `HIDDEN`, `BOOLEAN`, `LINKED`) and automated password history are encrypted under domain-specific HKDF-SHA256 + AES-GCM-256 AAD namespaces (`vault_*_custom`, `vault_pearls_history`).

Crucially, physical device testing on Google Pixel hardware identified and resolved a split-brain lifecycle defect: derived symmetric keys (`shellKey`) are now securely persisted at rest inside Android KeyStore-backed `EncryptedSharedPreferences` (AES-256-GCM), guaranteeing seamless dynamic re-hydration across cold starts and process terminations without sacrificing zero-knowledge security guarantees.

Pre-flight verification passed **100% green across all 32 unit and Robolectric tests**, verified live and unmasked on physical Google Pixel hardware.

---

## 💎 Key Themes & Highlights

### 🧩 1. Multi-Domain Vault Architecture & Custom Fields (Task 05)
* **Polymorphic Data Layer (`VaultDomainModels`)**: Unified support across Passwords (Pearls), Secure Notes, and SSH Keys with polymorphic detail and editor projections.
* **Bitwarden-Style Custom Fields Engine (`CustomField`)**: Supports 4 distinct field types (`TEXT`, `HIDDEN`, `BOOLEAN`, `LINKED`) with JSON serialization and domain-bound ShellCryption AAD namespaces (`vault_*_custom:{id}`).
* **Defensive Password History Tracking**: Automatic client-side versioning of password changes with ISO timestamps and encrypted history storage (`vault_pearls_history:{id}`), capped at twenty revisions.

### 📝 2. Universal Item Editor & Polymorphic Detail Screens (Task 06)
* **Universal Item Editor (`ItemFormScreen`, `ItemFormViewModel`)**: Single unified create/edit screen with pinned header/footer, domain selector, tags chip builder, custom field creator dialog, and `.imePadding().verticalScroll()` IME protection.
* **Polymorphic Item Detail Views (`ItemDetailScreen`, `ItemDetailViewModel`)**: Rich polymorphic inspection views for passwords, notes, and SSH keys with sensitive clipboard auto-scrubbing (30s timer) and Claw Re-Prompt biometric gates.
* **Accessible Fail-Safe Navigation**: Added high-contrast `Back` and `Retry` actions on item detail error screens to prevent user entrapment.

### 🔐 3. Zero-Knowledge Session Atomicity & KeyStore Key Persistence
* **Encrypted KeyStore Persistence (`EncryptedDeviceVault`)**: Persisted derived 32-byte `shellKey` Base64 in Android KeyStore-backed `EncryptedSharedPreferences` (AES-256-GCM), adding dynamic RAM re-hydration to survive Android process terminations and cold starts.
* **Atomic Session Validation**: Hardened `hasActiveSession()` to strictly require `getInMemoryShellKey() != null`, eliminating unauthenticated split-brain states where the dashboard could open without a valid decryption key.
* **Frictionless Gateway Re-entry (`GatewayViewModel`)**: Pre-filled server parameters (`protocol`, `host`, `port`) from stored server URL when returning to Gateway upon lock/fallback.

---

## 🧪 Verification & Hardware Testing Report

| Gate | Target | Result | Duration |
| :--- | :--- | :--- | :--- |
| **Gate 1: Tests** | `./gradlew testDebugUnitTest` | **32 / 32 Passed (100% Green)** | 28s |
| **Gate 2: Build** | `./gradlew assembleDebug` | **Clean Build (0 errors)** | 42s |
| **Gate 3: Live Run** | Physical Google Pixel (`sailfish`, Android 14) | **Verified (Decryption, Unmasking, Cold-Restart)** | Live |

### Hardware Verification Trail
- **Sovereign Key Auth**: Successfully authenticated using Base62 master key (`hu-WP4UjN...`).
- **Item Decryption**: Opened "ShellGuard" item with full secret, username, and URL decryption.
- **Unmasking & Clipboard**: Toggled password visibility eye icon (`x=838, y=474`); revealed plaintext secret with zero crashes or leaks.
- **Cold-Restart Resilience**: Force-stopped application via `am force-stop com.clawstack.shellguard` and relaunched via `am start`. The application cold-started directly to the dashboard, restored `shellKey` from KeyStore preferences, and opened/decrypted items immediately without error.
