# 🐚 ShellGuard Mobile — Release v0.0.0.9 (Build 9)

> **Phase 5: Context-Aware Autofill, Inline Chips & Add-Item Deep Linking**: versionCode 9 (`versionName = "0.0.0.9"`). Delivers context-aware keyboard inline suggestion chips for domain matching when locked, zero-match "Add Item" fallback chips with pre-populated URI deep linking into `ItemFormScreen`, a global `LockScreen` overlay architecture preserving backstack state, and crash hardening for WebView DOM boolean attributes. Verified with all 83 tests passing 100% green and live tested on physical Google Pixel hardware.

## *Phase 5: Context-Aware Autofill, Inline Chips & Add-Item Deep Linking*

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

Welcome to **v0.0.0.9** of **ShellGuard Mobile** — **Phase 5: Context-Aware Autofill, Inline Chips & Add-Item Deep Linking (Build 9)**. This milestone elevates ShellGuard's autofill experience to parity with top-tier sovereign password managers:

1. **Context-Aware Locked Suggestions**: When the vault is locked and matching accounts exist for the active domain, ShellGuard presents the clean domain string inline above the keyboard with a locked shell icon (🔒) and `"Unlock Vault"` subtitle, confirming site recognition without leaking sensitive plaintext titles or usernames.
2. **"Add Item" Fallback with Deep Linking**: When zero matches exist for the active website or package, ShellGuard exclusively renders an `"Add Item"` suggestion chip. Tapping it deep links directly into ShellGuard (`shellguard://app/form/NEW/PASSWORD/new?url=[encoded_domain]`), pre-filling the URL and title.
3. **Global LockScreen Overlay Architecture**: Decoupled `LockScreen` from being a fragile route inside `NavHost`. The lock layer now acts as an opaque shutter over the entire app. Tapping an action like "Add Item" while locked sets up the target form underneath and presents biometric unlock on top; once authenticated, the overlay dismisses, immediately revealing the pre-populated form!
4. **WebView DOM Stability**: Hardened `AutofillStructureParser` to defend against null boolean HTML attribute value pairs, eliminating WebView inflation crashes on complex authentication pages.

All **83 unit and domain tests pass 100% green**, verified with clean `./gradlew assembleDebug` and live execution on physical Google Pixel hardware.

---

## 💎 Key Themes & Highlights

### 🔒 1. Context-Aware Inline Suggestion Chips
* **Privacy-Preserving Recognition**: Displays the normalized website domain (e.g. `accounts.google.com`) with a locked shell icon rather than suppressing items or exposing plaintext secret titles.
* **Biometric Unsealing & Decryption**: Tapping a locked suggestion triggers `AutofillAuthActivity` to challenge the user via biometric or device credentials, decrypts credentials with hardware-backed KeyStore keys, and fills fields directly.

### ➕ 2. Direct "Add Item" Deep-Link Routing
* **Deep Link Registration**: Registered intent filters for `shellguard://app` in `AndroidManifest.xml` and Compose `NavHost`.
* **Pre-Populated Form State**: `ItemFormViewModel` automatically captures the deep-linked `url` parameter and seeds the form so users never have to copy-paste URLs manually.

### 🛡️ 3. Global Lock Screen Overlay Architecture
* **State Preservation**: Moving `LockScreen` out of the NavHost ensures deep links, navigation arguments, and existing scroll positions are never destroyed by authentication gates.
* **Seamless Unlock Dismissal**: Observing `VaultLockManager.isVaultLocked` automatically tears down the overlay when credentials succeed.

---

## 🧪 Verification & Test Oracle Parity

* **Full Unit Test Suite**: 83/83 unit and Robolectric tests passing 100% green (`BUILD SUCCESSFUL in 2m 41s`).
* **Live Hardware Verification (Google Pixel `sailfish`)**:
  - Gboard displays inline suggestion chips on focused login inputs.
  - Tapping "Add Item" deep links directly into the form with URL pre-filled.
  - Locked suggestions require biometrics and seamlessly fulfill credentials.
