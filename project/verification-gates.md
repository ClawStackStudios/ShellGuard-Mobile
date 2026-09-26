# ✅ ShellGuard Mobile — Verification Gates Specification

> **Test Suites, Headless Robolectric Harness, Build Gates & Monotonic Release Protocol**  
> *Targeted for Google AI Studio Android Application Generator.*

---

## §1. The Android Test Harness

- **Unit & Robolectric Runner**: JUnit 4 + Robolectric (`org.robolectric:robolectric:4.14+`) for headless Android framework mocking without an emulator.
- **Coroutines Testing**: `kotlinx-coroutines-test` with `StandardTestDispatcher` and `runTest`.
- **Mocking**: `io.mockk:mockk` for repository and service boundaries.
- **Compose UI Tests**: `androidx.compose.ui:ui-test-junit4` with headless Robolectric Compose graphics runner.

---

## §2. Headless JVM KeyStore & SQLite Invariants

### 1. KeyStore Headless JVM Fallback
In headless containerized environments where Android KeyStore service is absent, `AndroidKeyStoreHelper` must fallback to an in-memory HMAC-derived `SecretKeySpec` to ensure unit tests run without failing.

### 2. Robolectric Framework SQLite Open Helper
When configuring the Room database for Robolectric tests, native SQLCipher `.so` binaries cannot load on the Linux host JVM without native wrappers. The database factory dynamically switches to `FrameworkSQLiteOpenHelperFactory()` when Robolectric is detected:

```kotlin
val isRobolectric = try {
    Class.forName("org.robolectric.Robolectric") != null
} catch (e: ClassNotFoundException) {
    false
}

val factory = if (isRobolectric) {
    FrameworkSQLiteOpenHelperFactory()
} else {
    SupportFactory(passphrase)
}
```

---

## §3. The Test Suites

| Test Suite | File | Proves |
|:---|:---|:---|
| **ShellCryption Suite** | `ShellCryptionEngineTest.kt` | Deterministic HKDF derivation, AES-GCM roundtrip, and tamper detection across all 10 AAD namespaces. |
| **Room Persistence Suite** | `RoomDatabaseTest.kt` | Encrypted CRUD, reactive `Flow` emissions, cascading deletes, and remote pruning. |
| **Bidirectional Sync Suite** | `SyncRepositoryTest.kt` | Upstream pending push, downstream pull, conflict resolution, and offline mutation preservation. |
| **TOTP RFC 6238 Suite** | `TotpEngineTest.kt` | Official RFC 6238 test vectors (SHA1, SHA256, SHA512), Steam Guard encoding, Base32 decoding. |
| **Autofill Matcher Suite** | `DomainMatcherTest.kt` | eTLD+1 extraction, subdomains, port normalization, and package name mapping. |
| **Import / Export Suite** | `BackupManagerTest.kt` | Bitwarden JSON parsing, `.sgtotp.bak` decryption, `.sgvault.bak` export/import, and fingerprint deduplication. |

---

## §4. Build Verification Gates

Before any commit or release, the code must pass the **Trilogy of Verification**:

```bash
# Export bundled Android Studio JBR
export JAVA_HOME="/config/Applications/android-studio/jbr"
export PATH="$JAVA_HOME/bin:/config/Android/Sdk/platform-tools:$PATH"
export GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"

# Gate 1: Unit & Robolectric Tests (100% Green)
./gradlew testDebugUnitTest

# Gate 2: Clean Debug Compilation & Assembly
./gradlew assembleDebug

# Gate 3: Lint & Packaging Check
./gradlew lintDebug
```

---

## §5. 16 KB Page Alignment Verification Gate

On Android 15+ (API 35/36), native binaries must be aligned to 16 KB boundaries. Verify the built APK:

```bash
# Check shared libraries inside APK
zipinfo -v app/build/outputs/apk/debug/app-debug.apk lib/arm64-v8a/libsqlcipher.so | grep "file offset"
```
Must produce an offset divisible by `16384` (16 KB), confirming `useLegacyPackaging = false` was honored.

## §6. Monotonic Versioning & Release Protocol

1. **Monotonic `versionCode`**: Every release bundle increments `versionCode` by exactly `+1`.
2. **Dynamic `versionName`**: Binds to `BuildConfig.VERSION_NAME` in user-facing settings.
3. **Release Grammar**:
   - `git checkout -b release/vX.Y.Z`
   - Run verification trilogy (`testDebugUnitTest` + `assembleDebug`).
   - Bump version in `app/build.gradle.kts`.
   - Update `CHANGELOG.md` with version header.
   - Tag `vX.Y.Z` and push to trigger GitHub Actions release workflow.

---

## §7. ProGuard & R8 Release Optimization & Obfuscation Rules

While native C/C++ libraries (`libsqlcipher.so`) cannot be obfuscated by JVM ProGuard rules (native code is precompiled ELF machine code), release builds with `isMinifyEnabled = true` require specific R8 preservation rules to prevent JNI breaks, serialization stripping, and Room crashes:

```proguard
# ProGuard & R8 Rules for ShellGuard Mobile (app/proguard-rules.pro)

# --- Kotlinx Serialization ---
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer $serializer;
}
-keepclassmembers class * {
    public static final *** Companion;
}
-keepclassmembers class * {
    public static final kotlinx.serialization.KSerializer serializer(...);
}

# --- Room & SQLCipher JNI Preservation ---
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# --- Ktor Client & OkHttp Engine ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okio.** { *; }

# --- AndroidX Security, Biometrics & WorkManager ---
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- ShellGuard Domain Entities & DTOs ---
-keep class com.clawstack.shellguard.data.remote.models.** { *; }
-keep class com.clawstack.shellguard.data.local.entities.** { *; }
-keep class com.clawstack.shellguard.engine.** { *; }
-keep class com.clawstack.shellguard.data.repository.** { *; }

# --- Zero-Knowledge Release Hygiene: Strip Debug Logs ---
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# --- Preserve Stack Trace Line Numbers ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
```

