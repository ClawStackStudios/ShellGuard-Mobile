# 📱 Google Play Console Release Notes (`RELEASE-PLAY.md`)

> **Single Source of Truth for Google Play Store What's New Notes**  
> *Google Play enforces a strict 500-character limit per localized language block.*

---

## `v0.0.0.10` — Phase 6: Settings Hub, Security Controls & Web-Parity Backup Engine (Build 10)

```xml
<en-US>
• Settings Hub: 6-category navigation hub powered by DataStore.
• Theme Personalization: 6 bioluminescent accents, Monet & compact view.
• Security & Panic Purge: Canvas dial timer & 4-step fail-closed wipe.
• Web-Parity Backup: Full-vault export with HKDF hu- key & PBKDF2 pass.
• Bitwarden Ingestion: Format-sniffing intake with deduplication.
• 100% Green Test Suite: 105 unit & Robolectric tests verified.
</en-US>
```

---

## `v0.0.0.9` — Phase 5: Context-Aware Autofill, Inline Chips & Deep Linking (Build 9)

```xml
<en-US>
• Locked Inline Chips: Keyboard shows recognized domain with lock icon & unlock prompt.
• Add Item Deep Link: Zero matches offer single Add Item chip pre-filling website URL.
• Global Lock Overlay: Decoupled lock screen preserves in-flight form state & arguments.
• WebView Stability: Hardened against null boolean attributes on login web pages.
• 100% Green Test Suite: 83 unit & Robolectric tests verified on physical Pixel.
</en-US>
```

---

## `v0.0.0.8` — Hotfix 5.4: Web Interoperability & Secure Note Parity (Build 8)

```xml
<en-US>
• Web UI Interoperability: Seamless loading & sync of items created in Web Vault.
• Structural Envelope Validation: Safe handling of raw arrays with fail-closed security.
• Secure Note Masking: Notes load masked by default with bullet glyphs for privacy.
• Eye-Beside-Copy Cluster: Quick-action cluster for reveal & copy with biometric re-prompt.
• Login Notes Export: Quick copy button added to login notes.
• 100% Green Test Suite: 83 unit & Robolectric tests verified.
</en-US>
```

---

## `v0.0.0.7` — Hotfix 5.3: Bidirectional Sync Reconciliation & Adversarial Hardening (Build 7)

```xml
<en-US>
• Bidirectional Sync: Saved items push, pull, and reconcile seamlessly with server.
• Adversarial Hardening: Dual-adversary audit pass resolved 7 sync & crypto edge cases.
• Fail-Closed Crypto: Guaranteed protection against double-ciphertext data corruption.
• Zombie Item Immunity: Tombstone retention prevents deleted items from resurrecting.
• Batch Pruning: Evades SQLite 999 parameter limits on large vaults.
• 100% Green Test Suite: 18 remote & adversarial unit tests verified.
</en-US>
```

---

## `v0.0.0.6` — Phase 5: Autofill Framework, AutoSpill Defense & Adversary Hardening (Build 6)

```xml
<en-US>
• System Autofill: Seamless credential autofill for apps & browsers with inline keyboard chips.
• AutoSpill Defense: Web domain isolation protecting credentials in WebViews from host app leaks.
• Biometric Auth Gate: BiometricPrompt & PIN challenge for locked vaults & sensitive items.
• TOTP Auto-Copy: Sensitive clipboard copy with 30s auto-scrub.
• Fail-Closed Security: Zero-knowledge cryptographic hardening verified by adversarial audit.
• 100% Passing Test Oracle: Full suite verified.
</en-US>
```

---

## `v0.0.0.5` — Phase 4: Algorithmic TOTP, CameraX Scanner & Biometrics (Build 5)

```xml
<en-US>
• Algorithmic TOTP: RFC 6238 codes with dynamic truncation & smooth Canvas countdown ring.
• CameraX QR Scanner: ML Kit barcode detection to bind otpauth:// secrets to pearls.
• Password Generator: Cryptographic passwords and passphrases with custom character sets.
• Biometric Lifecycle: KeyStore biometric challenge and auto-lock on app background.
• Splash Theme Parity: Android 12+ splash screen with clean TopAppBar layout.
• 100% Passing Test Oracle: 63 unit & Robolectric tests verified.
</en-US>
```

---

## `v0.0.0.4` — Phase 3: Vault Domains, Universal Item Editor & Session Atomicity (Build 4)

```xml
<en-US>
• Vault Domains: Full CRUD across Passwords, Notes, and SSH Keys.
• Custom Fields: Bitwarden-style text, hidden, boolean, and linked fields.
• Password History: Automatic versioning of password changes.
• Universal Editor: Create and edit items with tags and dynamic fields.
• Polymorphic Details: Masked passwords, 30s clipboard auto-scrub, and Claw Re-Prompt.
• KeyStore Session Atomicity: Zero-knowledge session persistence across cold restarts.
</en-US>
```

---

## `v0.0.0.3` — Phase 2: Ktor Sync, Vault Dashboard & IME Hardening (Build 3)

```xml
<en-US>
• Ktor Network & Sync: Direct connection to self-hosted ShellGuard servers over LAN or mesh.
• Vault Dashboard: Unified view of passwords, notes, and SSH keys with real-time search & pod filters.
• Bitwarden-Model Offline Caching: Full read, search, and copy access with zero split-brain conflicts.
• Base62 Key Parity: Native support for all sovereign ShellKey identity files.
• Soft Keyboard Polish: Smooth cursor retention with zero blackout.
• Hardware Security: Android KeyStore, SQLCipher AES-256, 16 KB page-aligned.
</en-US>
```

---

## `v0.0.0.2` — Phase 1: Cryptographic Engine, Room Storage & Gateway UI (Build 2)

```xml
<en-US>
• ShellCryption Engine: Zero-knowledge AES-GCM-256 encryption across all 10 domain namespaces.
• Room SQLCipher Database: Hardware-encrypted local storage across 7 core entities.
• Gateway Login: Segmented URL bar, dual upload/paste toggles, and client-side SHA-256 key hashing.
• Reef Modernist Design: 6 curated marine theme accents with Reef Pink default.
• Hardware Security: Android KeyStore, SQLCipher AES-256, 16 KB page-aligned.
</en-US>
```

---

## `v0.0.0.1` — Stage 0: Initial Application Baseline (Build 1)

```xml
<en-US>
• Foundational Release: Sovereign zero-knowledge secrets vault native Android client.
• Modern Architecture: Kotlin 2.2+, Jetpack Compose Material 3, Unidirectional Data Flow.
• 16 KB Page Alignment: Native SQLCipher binaries compiled for Android 15/16 kernel compliance.
• LAN Transport: High-performance cleartext HTTP and TLS mesh network support.
</en-US>
```

---

<div align="center">
  <sub>Engineered with precision for the ClawStack / ShellGuard ecosystem.</sub>
</div>
