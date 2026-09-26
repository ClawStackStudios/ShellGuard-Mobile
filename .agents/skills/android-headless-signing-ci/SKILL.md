---
name: android-headless-signing-ci
description: Comprehensive playbook for headless Android CI/CD release builds, Python 3 base64 keystore decoding, native AGP in-engine signing, dual binary distribution (.aab + .apk), and Google Play Console release notes formatting.
---

# 🔑 Skill: Android Headless CI Signing & Release Pipeline

This skill provides the definitive, battle-tested playbook for cryptographically signing Android App Bundles (`.aab`) and standalone APKs (`.apk`) inside headless CI/CD environments (e.g. GitHub Actions) without relying on deprecated third-party signing actions.

---

## 🛑 The Core Gotchas & Why Old Approaches Fail

1. **The GNU `base64` Trailing Newline Trap**:
   - Passing multi-line or base64-encoded keystores through `echo "$KEY" | base64 -d` on Linux runners often fails with `base64: invalid input` due to environment variable wrapping, padding truncation, or OS-specific base64 flags (`-d` vs `-D` vs `--decode`).
   - **The Fix**: Use Python 3's built-in `base64.b64decode(os.environ['KEY'].strip())`, which strips all whitespace, handles padding seamlessly, and writes raw binary `.jks` files with 100% reliability.

2. **The "Hardcoded Keystore in Gradle" Failure**:
   - If `build.gradle.kts` unconditionally references `file("my-upload-key.jks")`, local developer builds and unit tests fail whenever the private keystore is absent from disk.
   - **The Fix**: Conditional Gradle signing configuration.

3. **Deprecated Third-Party GitHub Actions**:
   - Actions like `r0adkll/sign-android-release` rely on deprecated Node.js runtimes (Node 12/16/20) and create brittle dependencies.
   - **The Fix**: Android Gradle Plugin (AGP) natively bundles `apksigner` and `bundletool`. Let Gradle sign the artifacts directly during compilation.

---

## 🛠️ 1. Unconditional AGP Signing in `app/build.gradle.kts`

> [!CAUTION]
> **Configuration Cache Trap**: Do NOT wrap `create("release")` in `if (releaseKeystoreFile.exists())`.
> When CI runs `testDebugUnitTest` before decoding the keystore, Gradle caches the project state *without* the release signing configuration. When `bundleRelease` runs later, Gradle reuses the cached configuration and silently drops signing or fails.

> [!IMPORTANT]
> **Google Play Target SDK Requirement**: All release bundles must declare `targetSdk = 36` (Android 16) in `defaultConfig`. Bundles targeting API 35 or lower will be rejected by Play Console during upload validation.

```kotlin
android {
    // ...
    signingConfigs {
        val releaseKeystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
        val releaseKeystoreFile = file(releaseKeystorePath)

        create("release") {
            val sPassword = System.getenv("STORE_PASSWORD")
            val kPassword = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: sPassword
            val kAlias = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "upload"

            storeFile = releaseKeystoreFile
            storePassword = sPassword
            keyAlias = kAlias
            keyPassword = kPassword
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
    }
}
```

---

## 🐍 2. Robust Python 3 Keystore Decoding in GitHub Actions

In `.github/workflows/release.yml`, decode the keystore using Python 3:

```yaml
      - name: Decode Android Keystore
        env:
          SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
        run: |
          python3 -c "import os, base64; open('my-upload-key.jks', 'wb').write(base64.b64decode(os.environ['SIGNING_KEY'].strip()))"

      - name: Build Signed App Bundle & Standalone APK
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEY_STORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: |
          ./gradlew bundleRelease assembleRelease

      - name: Shred Ephemeral Keystore (Immediate Cleanup)
        if: always()
        run: |
          rm -f my-upload-key.jks
```

---

## 📦 3. Dual Binary Packaging & Naming

Always stage and publish both formats with clean, predictable names:
- `shellguard-mobile-vX.Y.Z.N.aab` (For Google Play Console upload)
- `shellguard-mobile-vX.Y.Z.N.apk` (For F-Droid / GitHub Direct Install)

```bash
TAG="${GITHUB_REF#refs/tags/}"
cp app/build/outputs/bundle/release/app-release.aab "shellguard-mobile-${TAG}.aab"
cp app/build/outputs/apk/release/app-release.apk "shellguard-mobile-${TAG}.apk"
```

---

## 📝 4. Dedicated Google Play Store Notes (`RELEASE-PLAY.md`)

Google Play Console enforces a **500-character limit** for release notes per language.
Maintain a root file `RELEASE-PLAY.md` with `<en-US>` tags:

```markdown
<en-US>
- Full zero-knowledge secrets vault (passwords, notes, keys, TOTP).
- SQLCipher whole-database encrypted local persistence.
- Hardware Android KeyStore biometric sealing & Claw Re-Prompt.
- Android Autofill & Keyboard Inline Presentation.
- Bitwarden-style read-only offline vault caching.
- 16 KB memory page-size alignment for Android 15+.
</en-US>
```

---

## ☕ 5. Local Container JBR & Gradle Execution
In containerized environments lacking a system-wide JDK on `PATH`, utilize Android Studio's bundled JetBrains Runtime (JBR):
```bash
export JAVA_HOME=/config/Applications/android-studio/jbr
export PATH="$JAVA_HOME/bin:$PATH"
/config/.gradle/wrapper/dists/gradle-<version>/.../bin/gradle testDebugUnitTest
```
