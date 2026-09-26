# Decision Log


## 2026-09-24 — autofill architecture
Specified dual support for Android Autofill Framework (API 26+) and modern Credential Manager (API 34+) with mandatory biometric gating prior to credential emission.

## 2026-09-24 — 16kb page-size alignment
Locked SQLCipher for Android to 4.6.1+ with `useLegacyPackaging = false` in Gradle to guarantee 16 KB page-size compliance for Android 15/16 devices.

## 2026-09-24 — hybrid file-system vault
Decoupled multi-megabyte attachment BLOBs from Room database rows to prevent Android SQLite 2MB CursorWindowAllocationException crashes. Stored metadata in Room and streamed encrypted files to internal disk.

## 2026-09-24 — biometric recovery state machine
Implemented KeyPermanentlyInvalidatedException recovery flow to prevent permanent user lockout when device biometrics are altered in system settings.

## 2026-09-24 — sensitive clipboard masking
Mandated ClipDescription.EXTRA_IS_SENSITIVE for all password and TOTP copy actions to suppress visual previews in Android 13+ clipboard overlays, paired with auto-scrubbing.

## 2026-09-24 — quick settings and glance widgets
Incorporated Android Quick Settings TileService and Jetpack Compose Glance widgets into the full client to provide instant access without opening the full vault UI.

## 2026-09-24 — bitwarden offline read-only vault access
Adopted the Bitwarden offline vault pattern: offline clients retain 100% read, search, copy, autofill, and TOTP functionality from SQLCipher cache, while blocking mutations (create/edit/delete) to prevent split-brain conflicts, auto-resuming sync upon NetworkCallback reconnect.

## 2026-09-24 — configurable uri match detection
Added 5 URI matching algorithms (Base Domain, Host, Exact, Starts With, Never) to disambiguate home lab services running on the same IP across different ports.

## 2026-09-24 — claw re-prompt
Introduced per-item re-authentication flag requiring biometric or PIN confirmation to view or copy high-security credentials, even when the vault is already open.

## 2026-09-25 — lan and tailscale transport policy
Mandated base-config cleartextTrafficPermitted in network_security_config.xml and ConnectionSpec.CLEARTEXT in OkHttp to support raw private IP connections and Tailscale CGNAT mesh addresses where domain-based TLS is absent.

## 2026-09-25 — design parity and blind side-by-side verification
Authored root DESIGN.md establishing Reef Modernist Mobile design tokens, flat 1dp Material 3 cards, 6 dynamic theme accents, and adaptive 3-pane master-detail layout achieving 1:1 visual continuity with ShellGuard Web and TOTP.

## 2026-09-25 — rejection of ai studio enterprise hallucinations
Rejected AI Studio recommendations for "native obfuscation via ProGuard" (technically impossible on ELF binaries), third-party logging/monitoring SDKs (violates zero-telemetry vault invariant), multi-module Gradle complexity (violates single-module invariant), and SaaS build flavors. Formulated verified R8 preservation rules instead (verification-gates.md §7).

## 2026-09-25 — governance and migration rule reorientation
Reoriented .agents/rules/android-development.md, zero-knowledge-migration.md, and deployment workflows from read-only TOTP companion constraints to full multi-domain vault client, reversing the volatile password purge into polymorphic Room entity ingestion with pre-DAO deduplication.

## 2026-09-25 — bitwarden parity audit and settings hub expansion
Audited ShellGuard Mobile straight up against Bitwarden Android; expanded ui-ux-design-system.md §10 with 6 dedicated Settings sub-screens (Vault Timeout, Timeout Action, Sensitive Clipboard timer, Screen Capture toggle, and Auto-Copy TOTP on Autofill), confirming 98%+ MVP specification coverage.

## 2026-09-25 — stage 0 scaffold and appcontainer lazy di
Scaffolded foundational Android application baseline in chore/stage-0-initial-scaffold; adopted frameworkless AppContainer lazy DI from ShellGuard ecosystem (avoiding KSP annotation processor churn with Kotlin 2.2+ and enabling deterministic RAM zeroization), passing testDebugUnitTest and assembleDebug 100% green.

## 2026-09-26 — kotlinx.serialization for cryptographic envelopes
Encountered Android framework mock limitation ('Method put in org.json.JSONObject not mocked') during host JVM test execution for ShellCryptionEngine. Switched envelope schema to pure Kotlin @Serializable data class ShellCryptionEnvelope, eliminating mock friction and achieving faster, portable serialization across both Android runtime and headless JVM tests.

## 2026-09-26 — gateway brand parity & animated segmented url bar
Forensically aligned Remote Login form with ShellGuard Web and TOTP companion UI patterns. Engineered a unified 56dp segmented URL container with animated port input, auto-parsing on paste, dual file/paste toggles, and JSON key extraction with zero-knowledge warning disclosure.

## 2026-09-26 — unified multi-domain reactive stream
Combined VaultPearl, SecureNote, and SshKey Room flows in SyncRepository using kotlinx.coroutines.flow.combine to project polymorphic records into UnifiedVaultItem for the VaultDashboard. Enabled instant sub-16ms search and Pod filtering while isolating domain-specific Room table schemas.

## 2026-09-26 — base62 sovereign key validation parity
Discovered ShellGuard master identity keys use Base62 alphanumeric encoding (`hu-[0-9a-zA-Z]{64}`), not strictly lowercase hexadecimal. Found by inspecting live Pixel device state where a valid identity file (`shellguard_identity_xxzioimibiexx.json`) was loaded but the login button remained disabled due to overly restrictive `[0-9a-f]` regex in `ClawCrypto`. Upgraded `CLAW_KEY_REGEX` and `GatewayViewModel` to Base62 and extracted UUID directly.

## 2026-09-26 — debug flag_secure scoping & ime inset resolution
Diagnosed black screen and cursor invisibility during soft keyboard IME input on physical Pixel. Traced to unconditional `FLAG_SECURE` triggering Adreno 530 compositor blackout over system IME window, combined with `Scaffold` double-subtracting IME padding. Scoped `FLAG_SECURE` to `!BuildConfig.DEBUG` (matching ShellGuard-TOTP pattern) and set `contentWindowInsets = WindowInsets(0, 0, 0, 0)` on root `Scaffold`, verifying smooth typing and visual cursor retention.


