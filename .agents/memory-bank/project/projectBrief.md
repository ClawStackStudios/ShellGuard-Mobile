# Project Brief: ShellGuard Mobile

## Core Mission
ShellGuard Mobile is the official, full-featured native Android secrets vault application for the ShellGuard zero-knowledge ecosystem. Unlike the companion app (ShellGuard-TOTP) which only manages two-factor authentication tokens, ShellGuard Mobile provides complete bidirectional CRUD access to all vault domains:
1. **Passwords & Logins** (`/api/vault`)
2. **Secure Notes** (`/api/notes`)
3. **SSH Keys** (`/api/keys`)
4. **File Attachments** (`/api/attachments`)
5. **Integrated TOTP Verification Codes** (RFC 6238 & Steam Guard)
6. **Android Autofill Framework & Credential Provider**

## Core Operating Invariant
*"Build features around security, not security around features."*
- **Zero-Knowledge**: Client encrypts sensitive fields client-side via ShellCryption (HKDF-SHA256 + AES-GCM-256). Plaintext secrets and the root `hu-` key are NEVER transmitted to the server.
- **Offline-First**: SQLCipher whole-database encrypted local persistence with Room 2.7+. All vault items are accessible offline.
- **System Autofill**: System-level credential auto-population across native apps and browsers with biometric gating.

## Project Scope
- Native Android application built with Jetpack Compose & Material 3.
- Target SDK: 36 (Android 16), Min SDK: 24 (Android 7.0).
- Package Name: `com.clawstack.shellguard`.
- Scaffolded via Google AI Studio Android Application Generator using `project/meta-prompt-ai-studio.md`.
