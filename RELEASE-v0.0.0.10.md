# 🐚 ShellGuard Mobile — Release v0.0.0.10 (Build 10)

> **Phase 6: Settings Hub, Security Controls & Web-Parity Backup Engine**: versionCode 10 (`versionName = "0.0.0.10"`). Delivers a comprehensive categorized settings architecture, customizable bioluminescent themes, granular security controls with an interactive circular dial panic purge cascade, and a full-vault backup & restore engine supporting Web-Parity polymorphic JSON export, Sovereign ClawKey HKDF encryption, and Bitwarden ingestion. Verified with all 105 tests passing 100% green.

## *Phase 6: Settings Hub, Security Controls & Web-Parity Backup Engine*

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

Welcome to **v0.0.0.10** of **ShellGuard Mobile** — **Phase 6: Settings Hub, Security Controls & Web-Parity Backup Engine (Build 10)**. This release completes the application's configuration surface and cross-platform data portability:

1. **Categorized Settings Hub (`SettingsHubScreen`)**: Implemented an intuitive, 6-category navigation hub powered by `androidx.datastore` preferences (`SettingsRepository`), covering Appearance, Security, Sync, Autofill, Backup, and About.
2. **Reef Modernist Theme Customization (`SettingsAppearanceScreen`)**: Full personalization with 6 curated bioluminescent theme accents (`REEF_DEFAULT`, `CYAN_VENT`, `PURPLE_SHELL`, `EMERALD_TRENCH`, `AMBER_FLARE`, `MONOCHROME`), dynamic Monet color toggles, Dark/Light/System theme modes, website favicon toggles, and compact view options.
3. **Server & Sync Controls (`SettingsSyncScreen`)**: Direct visibility into connected server endpoints, on-demand delta sync trigger with animated progress and status indicators, cellular sync restrictions, and zero-knowledge offline guarantees.
4. **Interactive Security & Panic Purge Flow (`SettingsSecurityScreen`, `PanicPurgeCountdownScreen`)**: Granular auto-lock timeouts, dynamic screenshot protection (`FLAG_SECURE`), sensitive clipboard scrub timers, custom `CircularDialPicker` with trigonometric gesture tracking, and a fail-closed 4-step emergency purge cascade with a 3-ring pulsing Canvas countdown.
5. **Web-Parity Backup & Restore Engine (`VaultBackupEngine`, `SettingsBackupScreen`)**: Dual-mode full-vault encryption supporting `ACTIVE_KEY` via HKDF-SHA256 with strict `hu-` Base62 sovereign key validation and `CUSTOM_PASSPHRASE` with PBKDF2-SHA256 (600,000 iterations) matching the Web Client. Features a polymorphic `items` JSON array schema with ISO timestamps, SHA-256 integrity checksums, automatic format detection, and Bitwarden unencrypted JSON intake.
6. **Autofill Status & System Diagnostics (`SettingsAutofillScreen`, `SettingsAboutScreen`)**: System autofill provider status detection, one-tap Android settings launcher, keyboard inline suggestions toggle, Stage 8 heuristics preview, and complete Android 15/16 16 KB page-size compliance diagnostics.

All **105 unit and Robolectric tests pass 100% green**, verified with clean compilation and full test oracle parity.

---

## 💎 Key Themes & Highlights

### 🎨 1. Reef Modernist Theme Customization
* **Bioluminescent Palettes**: Choose from 6 signature theme accents with real-time Compose preview and dynamic token resolution.
* **Monet Dynamic Theming**: Toggle wallpaper-derived Material You colors on Android 12+.
* **Layout Density**: Toggle between comfortable cards and compact rows for high-density vault browsing.

### 🛡️ 2. Security Controls & Emergency Panic Purge
* **Circular Dial Duration Picker**: Smooth Canvas clock-face dial with atan2 gesture mapping for selecting panic countdown durations (5s–60s).
* **Fail-Closed Destruction Cascade**: 4-step purge sequentially wipes Room tables (`clearAllTables()`), unbinds KeyStore session tokens, clears DataStore preferences, and resets vault lock state.
* **Hardware Back Abort**: Pulsing countdown screen can be safely cancelled via on-screen abort button or physical back navigation before timer expiry.

### 📦 3. Web-Parity Backup & Restore Engine
* **Polymorphic JSON Schema**: Unified `items` array with `type` discriminators ("password", "note", "key") matching ShellGuard Web's `ImportExportView`.
* **Sovereign Key Validation**: Enforces Base62 alphanumeric 67-character regex (`^hu-[0-9a-zA-Z]{64}$`) with live validity indicators, plus a device-only fallback toggle.
* **Backward & Forward Compatibility**: Deserializer seamlessly handles both modern unified `items` and legacy segregated lists (`pearls`, `notes`, `sshKeys`).

---

## 🧪 Verification & Test Oracle Parity

* **Full Unit Test Suite**: 105/105 unit and Robolectric tests passing 100% green (`BUILD SUCCESSFUL in 3m 32s`).
* **Active Suite Additions**:
  - `SettingsRepositoryTest`: DataStore preference persistence and reactive flow emissions.
  - `SettingsViewModelTest`: MVI state mutations, job synchronization, and lifecycle binding.
  - `VaultBackupEngineTest`: Format sniffing, HKDF/PBKDF2 export/decrypt cycles, and Bitwarden JSON ingestion.
* **Architecture Compliance**: Android 16 (API 36) targetSdk, 16 KB page-size uncompressed native JNI packaging (`useLegacyPackaging = false`), StrongBox AES-256 KeyStore isolation, and SQLCipher 4.6.1+ database encryption.
