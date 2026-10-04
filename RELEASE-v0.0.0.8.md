# 🐚 ShellGuard Mobile — Release v0.0.0.8 (Build 8)

> **Hotfix 5.4: Web Interoperability & Secure Note Parity**: versionCode 8 (`versionName = "0.0.0.8"`). Resolves critical deserialization edge case on Web UI items storing unencrypted empty JSON arrays (`"[]"`), introduces `ShellCryptionEngine.isEncryptedEnvelope()` structural validation, and achieves 1:1 parity with the Web client by enforcing masked default display for Secure Notes alongside the Eye-beside-Copy action cluster and biometric re-prompt gating. Verified with 83 tests passing 100% green and live tested on physical Google Pixel hardware.

## *Hotfix 5.4: Web Interoperability & Secure Note Parity*

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

Welcome to **v0.0.0.8** of **ShellGuard Mobile** — **Hotfix 5.4: Web Interoperability & Secure Note Parity (Build 8)**. This hotfix delivers two essential improvements ensuring flawless day-to-day vault operation across Web and Mobile:

1. **Web UI Interoperability & Envelope Structural Validation**: Eliminates the `Unexpected JSON token at offset 0: Expected start of the object '{'. but had '[' instead` exception when loading items minted in the Web UI where `password_history` or `custom_fields` were stored as unencrypted empty JSON arrays (`"[]"`).
2. **Secure Note Masking & Action Cluster Parity**: Brings Android's Secure Note view into full 1:1 visual and cryptographic alignment with ShellGuard Web. Notes now default to masked monospace bullet glyphs (`••••••••••••••••••••••••••••••••`), feature the canonical Eye-beside-Copy header action cluster, and enforce biometric / PIN re-prompt checks before revealing or copying sensitive content.

All **83 unit and domain tests pass 100% green**, verified with clean `./gradlew assembleDebug` and live execution on physical Google Pixel hardware.

---

## 💎 Key Themes & Highlights

### ⚡ 1. Web UI Envelope Interoperability & Structural Validation
* **`ShellCryptionEngine.isEncryptedEnvelope`**: Introduced structural envelope detection checking for `v: 1`, `alg: "AES-GCM-256"`, and required ciphertext/IV/AAD fields before passing strings to the cryptographic deserializer.
* **Graceful Array Normalization**: Sanitized `getPearlDetail`, `getNoteDetail`, `getSshKeyDetail`, and downstream pull reconciliation passes in `SyncRepository.kt` to safely treat raw unencrypted JSON arrays (`"[]"`) as empty collections rather than crashing.
* **Fail-Closed Decryption Boundary**: Preserved strict fail-closed security by enforcing `IllegalArgumentException` in `decryptField` whenever an unrecognized or corrupt payload is encountered (ratified as **Redline 7** in `testOracle.md`).

### 👁️ 2. Secure Note Masking & The Eye-Beside-Copy Cluster
* **Default Masked Display**: In `ItemDetailScreen.kt`, Secure Notes (`VaultItemDomain.NOTE`) now load masked by default with monospace bullet glyphs (`••••••••••••••••••••••••••••••••`) and a `"Tap or click eye to reveal"` hint.
* **The Eye-Beside-Copy Cluster**: Elevated the note header to feature adjacent Eye toggle (`Visibility` / `VisibilityOff`) and Copy (`ContentCopy`) action buttons matching the design system standard.
* **Biometric Re-Prompt Enforcement**: Both reveal and copy actions enforce `state.reprompt`, triggering biometric or device credential prompts before exposing secrets.
* **Login Notes Clipboard Export**: Added a quick-copy action icon button to login item notes sections for seamless credential export.

---

## 🧪 Verification & Test Oracle Parity

* **Full Unit Test Suite**: 83/83 unit and Robolectric tests passing 100% green (`BUILD SUCCESSFUL in 3m 6s`).
* **Live Hardware Verification (Google Pixel `sailfish`)**:
  - Web UI-created items load and sync without exceptions.
  - Secure Notes open masked by default; tapping the eye or card reveals cleartext; tapping mask re-masks immediately.
  - Biometric re-prompt challenges verified.
