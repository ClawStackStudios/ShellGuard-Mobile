# The Trail: ShellGuard Mobile

Present tense. The hand talking as it works.

## 2026-09-25 — Stage 0 Scaffold & Security Baseline

I initialize the Git repository on `main` and branch immediately into `chore/stage-0-initial-scaffold`.
I inspect the grain of the repository and the sibling ecosystem (`ShellGuard` web and `ShellGuard-TOTP`).
I find the established `AppContainer` pattern in `ShellGuard/.agents/memory-bank/android/di-container.md` and recognize the grain: application-scoped lazy dependency injection gives sub-100ms startup, avoids KSP code-generation churn, and enables deterministic RAM zeroization on lock. I adopt this over heavy framework DI.

I lay down the build toolchain:
- Gradle 9.3.1 wrapper copied from the tested companion setup.
- Root `build.gradle.kts` and `settings.gradle.kts` naming the root project `ShellGuard`.
- Sanitized `gradle/libs.versions.toml` targeting Android 36, Kotlin 2.2.10, SQLCipher 4.6.1, Room 2.7.0, Compose BOM 2024.09.00, and Ktor 2.3.12, with zero third-party telemetry dependencies.
- `local.properties` pinned to `/config/Android/Sdk`.
- `.gitignore` defending against Keystores, build outputs, local paths, and agent scratchpads.
- `app/build.gradle.kts` enforcing `jniLibs.useLegacyPackaging = false` to guarantee 16 KB ELF segment page-size alignment for Android 15/16.
- `app/proguard-rules.pro` locking down SQLCipher JNI bindings and stripping debug logs in release.
- `network_security_config.xml` declaring `<base-config cleartextTrafficPermitted="true">` so the app can communicate with local home lab servers over private LAN IPs and Tailscale CGNAT addresses.
- `app/src/main/res/xml/data_extraction_rules.xml` blocking OS cloud backup of the SQLCipher database and KeyStore credentials.
- `ShellGuardApp.kt` loading SQLCipher native binaries and holding `AppContainer`.
- `MainActivity.kt` setting `FLAG_SECURE` on `window` to prevent screen capture and recents leakage.
- `crypto/ClawCrypto.kt` validating `hu-` format, generating keys, computing SHA-256 digests, and HMAC calculations.
- `crypto/AndroidKeyStoreHelper.kt` managing hardware AES-GCM keys with headless JVM fallback.
- `ui/theme/Color.kt` and `Theme.kt` establishing Reef Modernist Dark tokens and 6 curated theme accents.
- `crypto/ClawCryptoTest.kt` verifying SHA-256 test vectors, format validation, and HMAC consistency.

Now I test the joint:
- The initial daemon start warned that `app/build/tmp` was missing; I created it before invocation.
- In `app/build.gradle.kts`, `libs.sqlcipher-android` was flagged with unresolved reference minus operator; I corrected it to the Gradle accessor `libs.sqlcipher.android`.
- The stroke lands: `./gradlew testDebugUnitTest` compiles clean and passes 100% green across all 32 tasks in 7m 43s.
- The build gate holds: `./gradlew assembleDebug` packages the 64MB `app-debug.apk` in 6m 43s with SQLCipher uncompressed and 16 KB page-aligned.
- `git status` verifies `.gitignore` successfully shields all build artifacts, caches, and local configurations.
