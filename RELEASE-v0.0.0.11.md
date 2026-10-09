# 🐚 ShellGuard Mobile — Release v0.0.0.11 (Build 11)

> **Phase 7: Context-Aware Autofill Expansion, 2-Step Login Support & Blast-Radius Containment**: versionCode 11 (`versionName = "0.0.0.11"`). Delivers a 5-tier confidence-ranked `AssistStructure` parser, simultaneous multi-field dataset injection, 2-step email-first login support (e.g., `accounts.google.com`), Option B masked username inline keyboard chip disambiguation (`Work · lu***@company.com`), zero-copy Binder resource iconography, and dynamic `BuildConfig` version & `GNU AGPL v3.0` license alignment. Verified with all 114 unit and Robolectric tests passing 100% green and live on-device testing on Google Pixel (`sailfish`).

## *Phase 7: Context-Aware Autofill Expansion, 2-Step Login Support & Blast-Radius Containment*

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

Welcome to **v0.0.0.11** of **ShellGuard Mobile** — **Phase 7: Context-Aware Autofill Expansion, 2-Step Login Support & Blast-Radius Containment (Build 11)**. This release upgrades the Android Autofill engine from password-centric matching to intelligent, context-aware multi-field credential injection with strict zero-knowledge privacy on the keyboard strip:

1. **5-Tier Confidence-Ranked Autofill Parser (`AutofillStructureParser.kt`)**: Implemented a 5-tier confidence hierarchy (`RANK_EXPLICIT_HINT = 1` through `RANK_PROXIMITY = 5`) that inspects standard Android `autofillHints`, HTML `<input type="email|password">` and `autocomplete` attributes, Android `InputType` variations, editable resource ID/hint heuristics, and preceding-editable-input positional proximity.
2. **Container Hijack, Omnibox & Mutual Exclusion Defenses**: Added `isEditableInputNode` and `isExcludedNonCredentialInput` (`autocompletetextview`, `url_bar`, `omnibox`, `search`, `otp`) so parent `<form>`/`<div>`/`LinearLayout` containers and browser URL bars never hijack `usernameId`. Enforced password-first evaluation so composite password IDs (`id="login_password"`) never overwrite `usernameId`.
3. **Co-Presence Blast-Radius Gate & 2-Step Login Support (`ShellGuardAutofillService.kt`)**: When no password field is on screen (`passwordId == null`), the Co-Presence Gate strips weak Rank 4/5 substring heuristics to prevent keyboard spam on search/profile screens, while preserving high-confidence Rank 1–3 email/username fields so 2-step split login flows (such as `accounts.google.com`) render inline chips and `"Add Item"` deep links on Step 1.
4. **Simultaneous Multi-Field Dataset Binding (`ShellGuardAutofillService.kt`, `AutofillAuthActivity.kt`)**: Bound both `usernameId` (when `pearl.username.isNotBlank()`) and `passwordId` into unlocked, locked/reprompt, and `"Add Item"` datasets, enabling 1-tap simultaneous autofill of both username and password from either field without overwriting user-typed usernames with blank strings.
5. **Option B Inline Chip Disambiguation & Zero-Copy Iconography (`AutofillInlineHelper.kt`)**: Formatted unlocked keyboard chips with non-default category/tag + partially masked username (`Work · lu***@company.com`, `lu***@gmail.com`, `ad***n`), eliminating shoulder-surfing exposure while keeping multiple accounts recognizable. Resolved app icons via zero-copy `Icon.createWithResource` to prevent Binder `TransactionTooLargeException`.
6. **Dynamic Version Footer & GNU AGPL v3.0 Alignment (`SettingsHubScreen.kt`, `SettingsAboutScreen.kt`)**: Bound the Settings Hub version footer dynamically to `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE`, and aligned license attribution to `GNU AGPL v3.0`.

All **114 unit and Robolectric tests pass 100% green**, verified live on physical Google Pixel (`sailfish`) hardware across SimpleLogin and Google Sign-In.

---

## 💎 Key Themes & Highlights

### 🧠 1. 5-Tier Confidence Heuristics & 2-Step Login Support
* **Deterministic Precedence**: Explicit `autofillHints` (Rank 1), HTML `<input type="email">` / `autocomplete` (Rank 2), and `InputType` variations (Rank 3) always supersede generic resource ID heuristics (Rank 4) and positional proximity (Rank 5).
* **Split Login Compatibility**: 2-step email-first login flows (`accounts.google.com`, Microsoft, Okta) display matched account chips or `"Add Item"` deep links on the Step 1 email screen while non-credential screens remain completely quiet.

### 🛡️ 2. Option B Masked Disambiguation & Blast-Radius Containment
* **Zero Raw Username Exposure**: Raw plaintext usernames are never rendered on the Android 11+ IME strip; Option B combines category/tag badges with partial masking (`Work · lu***@company.com`).
* **AutoSpill & Omnibox Isolation**: Browser `AutoCompleteTextView` address bars are excluded from capture, and entering a `webDomain` subtree immediately clears any outer native host fields.

### ⚡ 3. Simultaneous Multi-Field Injection & Binder Safety
* **1-Tap Dual Fill**: Tapping a chip while focused on either the username or password input populates both fields in a single transaction.
* **Zero-Copy Resource Icons**: Native target app icons use `Icon.createWithResource` with automatic fallback to `R.drawable.ic_locked_shell`, avoiding Bitmap IPC serialization overhead.

---

## 🧪 Verification & Test Oracle Parity

* **Full Unit Test Suite**: 114/114 unit and Robolectric tests passing 100% green across 15 test suites.
* **Active Suite Additions**:
  - `AutofillStructureParserTest` (12 tests): Confidence-tier precedence, `<form>`/`LinearLayout` container hijack rejection, `AutoCompleteTextView` URL-bar exclusion, `login_password` mutual exclusion, Co-Presence Gate suppression, WebView AutoSpill reset, and Option B subtitle masking.
* **Architecture Compliance**: Android 16 (API 36) `targetSdk`, 16 KB page-size uncompressed native JNI packaging (`useLegacyPackaging = false`), StrongBox AES-256 KeyStore isolation, and SQLCipher 4.6.1+ database encryption.
