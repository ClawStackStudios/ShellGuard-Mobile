# Release Manifest: v0.0.0.14 (Build 14) — 2026-10-19

> **Security & Transport Sovereignty Hotfix**: versionCode 14 (`versionName = "0.0.0.14"`). Introduces an in-app user consent toggle for self-signed TLS certificates on the Gateway screen, enabling seamless connection to self-hosted HTTPS instances without forcing users to install root CAs into Android OS settings. Closes CWE-532 by redacting raw unparseable 400 Bad Request error response bodies from log outputs. Resolves physical UI layout stacking in GatewayScreen, expanding touch targets to ≥48dp with accessible clickability. Realigns development roadmap to decade build intervals (`Build 20` Stage 2, `Build 30` Stage 3, `Build 40` Stage 4, `Build 50` v0.0.1.0 Milestone) reserving intermediate hotfix capacity. Verified 100% green across 127 tests on Cloud CI and validated live on physical Google Pixel glass.

## *Security & Transport Sovereignty Hotfix*

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

Welcome to **v0.0.0.14** of **ShellGuard Mobile** — **Security & Transport Sovereignty Hotfix (Build 14)**. This release addresses critical transport security, privacy compliance, and physical screen ergonomics:

1. **Consent-Gated Self-Signed TLS Trust Model (`KtorClientProvider.kt`, `GatewayScreen.kt`)**: Enables homelab self-hosted operators using private or self-signed HTTPS certificates to connect frictionlessly. Users can explicitly toggle "Trust self-signed certificates" on the Gateway login screen. When enabled, the client accepts self-signed certificates for the session while displaying an amber UI warning indicator (`⚠️ Strict HTTPS validation disabled.`). When disabled (the default), standard strict platform TLS is enforced.
2. **Elimination of OS Root CA Installation Burden**: Solves the homelab TLS friction point without compromising device-wide security. Users no longer need to install untrusted self-signed root certificates into Android OS system settings, eliminating alarming persistent Android security notifications.
3. **CWE-532 Sensitive Information Logging Redaction (`ShellGuardClient.kt`)**: Remediated potential plaintext/credential leakage by redacting raw unparseable 400 Bad Request error response bodies from log statements. Error messages now report clean generic error descriptors, preventing authentication payloads or sensitive server tokens from leaking into device logcat or telemetry pipelines.
4. **Physical On-Glass Layout Polish (`GatewayScreen.kt`)**: Fixed Compose layout defect discovered during physical Google Pixel testing where the self-signed consent row stacked directly over the segmented URL bar. Reordered the row into the scrollable `Column` container below the URL bar and expanded the touch target to a minimum of 48dp with full-row clickability (`heightIn(min = 48.dp)`).
5. **Protocol-Aware UI Visibility**: The self-signed trust toggle dynamically mounts only when `HTTPS` protocol is selected; selecting `HTTP` hides the toggle, avoiding protocol state ambiguity.
6. **Roadmap Realignment (Option 1 Decade Intervals)**: Re-versioned future roadmap milestones to decade spacing (`v0.0.0.20` Stage 2, `v0.0.0.30` Stage 3, `v0.0.0.40` Stage 4, `v0.0.1.0 Build 50` Structural Completion), establishing an engineering runway of 5–10 build numbers between stages for intermediate hotfix patches.

All **127 unit and Robolectric tests pass 100% green** on GitHub Actions Cloud CI.

---

## 💎 Key Themes & Highlights

### 🔒 1. Transport Sovereignty & Zero-Friction HTTPS
* **Self-Hosted Reality**: Most self-hosted homelabs operate HTTPS behind self-signed certificates or private LAN domains where public Let's Encrypt certificates are impractical.
* **In-App User Consent**: Rather than unconditionally allowing untrusted certificates or forcing users to install root CAs into Android OS trust stores, ShellGuard provides an explicit, opt-in in-app consent switch.
* **Amber UI Indicators**: Whenever self-signed trust or cleartext HTTP is active, the app displays prominent amber warning indicators reinforcing the security boundary.

### 🛡️ 2. CWE-532 Log Redaction
* **Zero Credential Leaks**: In line with ShellGuard's Immutable Rules, raw server error responses that fail JSON deserialization are sanitized before logging. Raw request/response payloads containing sovereign keys or encrypted envelopes never enter plaintext device logs.

### 📱 3. Physical Device Verification Loop
* **On-Glass Audited**: Verified on physical Google Pixel running Android 14 (LineageOS) over wireless TLS ADB. Confirmed flawless touch target compliance, smooth Compose animations, and proper protocol-reactive state changes.

---

## 🧪 Verification & Test Oracle

```text
[✓] Cloud CI Unit Test Suite: 127/127 tests passing (Zulu JDK 17, Robolectric SDK 34)
[✓] Documentation Drift Audit: python3 scripts/audit_docs_drift.py verified 100% green
[✓] Play Store Release Notes: 337 / 500 characters, zero markdown leakage
[✓] Physical Hardware Verification: Verified on Google Pixel (LineageOS / Android 14 / SDK 34)
```

---

## 📦 Distribution Artifacts
* **Target SDK**: API 36 (Android 16 Baklava)
* **Minimum SDK**: API 24 (Android 7.0 Nougat)
* **Page-Size Alignment**: 16 KB native library alignment (`libsqlcipher.so`)
* **Release Artifacts**:
  - `shellguard-mobile-v0.0.0.14.aab` (Google Play Console internal testing bundle)
  - `shellguard-mobile-v0.0.0.14.apk` (Direct sideloading release package)
