# 🐚 ShellGuard Mobile — Release v0.0.0.6 (Build 6)

> **Phase 5: Android Autofill Framework, AutoSpill Defense & Adversarial Hardening**: versionCode 6 (`versionName = "0.0.0.6"`). Delivers the Android Autofill Framework service (`ShellGuardAutofillService`), resilient 4-tier heuristic parser (`AutofillStructureParser`) with AutoSpill WebView isolation, Android 11+ keyboard inline presentation suggestion chips, transparent biometric gate (`AutofillAuthActivity`), Bitwarden-parity TOTP auto-copy to sensitive clipboard with 30s auto-scrubbing, home lab port isolation in `DomainMatcher`, native system settings guidance dialog, and ruthless fail-closed remediations identified by our 30-year cryptologist adversary audit.

## *Phase 5: System Autofill & Adversarial Hardening*

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

Welcome to **v0.0.0.6** of **ShellGuard Mobile** — **Phase 5: Android Autofill Framework, AutoSpill Defense & Adversarial Hardening (Build 6)**. This release marks the completion of Phase 5 (Tasks 09 and 10) of the master roadmap, advancing ShellGuard Mobile to **83% completion**.

ShellGuard Mobile now acts as a first-class, system-level Android Autofill provider. Users can autofill credentials seamlessly across all third-party native apps and web browsers (Chrome, Firefox, Edge, Brave), with inline keyboard suggestion chips rendered natively above Gboard and SwiftKey (Android 11+). 

Importantly, this release was subjected to an aggressive adversarial security audit from a 30-year veteran cryptologist perspective, resolving subtle attack surfaces including AutoSpill WebView credential leaking, PendingIntent hash collisions, asymmetric package matching, and fail-open ciphertext fallbacks.

All **unit and Robolectric tests pass 100% green**, verified with clean `./gradlew assembleDebug` APK generation.

---

## 💎 Key Themes & Highlights

### ⚡ 1. System-Level Autofill & RemoteViews Architecture (Task 09)
* **Android Autofill Service (`ShellGuardAutofillService`)**: System service registered with `android.permission.BIND_AUTOFILL_SERVICE` and configuration XML.
* **4-Tier Heuristic Structure Parser (`AutofillStructureParser`)**: Traverses `AssistStructure` with a 64-level tree recursion ceiling, detecting fields via hints ➔ HTML attributes ➔ input types ➔ ID/content description heuristics.
* **Inline Presentation & Dropdown RemoteViews**: Renders custom RemoteViews dropdown items alongside Android 11+ (API 30+) keyboard suggestion chips (`InlinePresentation`).
* **Bitwarden-Parity TOTP Auto-Copy**: Automatically copies TOTP verification codes to the system clipboard upon credential selection, declaring `ClipDescription.EXTRA_IS_SENSITIVE = true` with a 30-second background scrubbing timer.

### 🛡️ 2. Domain Matcher, Biometric Auth Gate & AutoSpill Defense (Task 10 & 5.1/5.2)
* **Domain Matcher & Home Lab Port Isolation (`DomainMatcher`, `UriMatchMode`)**: eTLD+1 extraction, automatic promotion from `BASE_DOMAIN` to `EXACT` host/port matching for local IP addresses and localhost, and strict two-way `androidapp://` package matching.
* **AutoSpill & WebView Isolation Defense**: Propagates web domain context down the view hierarchy; strictly rejects binding credentials to native host fields when embedded inside WebViews, mitigating the Black Hat 2023 AutoSpill vulnerability (CWE-200 / CWE-1021).
* **Transparent Biometric Gate (`AutofillAuthActivity`)**: Lightweight `FragmentActivity` gate with `BiometricPrompt` and PIN fallback for locked vaults and Claw Re-Prompt items.
* **Autofill System Settings Guidance (`AutofillManagerHelper`, `AutofillSettingsDialog`)**: One-tap deep-link intent (`Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE`) and status banner dialog with dynamic `ON_RESUME` refresh in the Vault Dashboard overflow menu.
* **Adversary Sub-Agent Codification (`.agents/agents/adversary/`)**: Created permanent 30-year veteran cryptologist persona for ruthless, unflattering penetration testing.

---

## 🧪 Verification & Test Oracle Parity

* **100% Passing Test Oracle**: Unit and Robolectric tests pass green across domain matching, heuristic structure parsing, and crypto engines:
  - `DomainMatcherTest.kt` (eTLD+1, port isolation, asymmetric app cross-matching rejection)
  - `AutofillStructureParserTest.kt` (data class initialization, WebView state tracking)
  - `TotpEngineTest.kt`, `ShellCryptionEngineTest.kt`, `SyncRepositoryTest.kt`
* **Clean Pre-Flight Compilation**: Verified via `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` in containerized headless JBR environment.
