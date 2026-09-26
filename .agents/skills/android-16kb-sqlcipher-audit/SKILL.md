---
name: android-16kb-sqlcipher-audit
description: Diagnostic playbook and audit procedures for Android 15+ 16 KB memory page-size compliance, SQLCipher native libraries, uncompressed JNI packaging, and ProGuard R8 obfuscation protection.
---

# 🧱 Skill: Android 15+ 16 KB Page-Size & SQLCipher Audit

This skill provides step-by-step diagnostic and remediation protocols for ensuring that native C/C++ libraries (such as Zetetic SQLCipher) and compiled APK/AAB packages meet Android 15+ (API 35/36) 16 KB memory page-size alignment mandates.

---

## 🛑 The Core Gotchas & Why Apps Crash on Android 15

1. **The 4 KB vs 16 KB Segment Alignment SIGSEGV**:
   - Android kernels traditionally used 4 KB virtual memory page sizes. Android 15+ introduces support for 16 KB memory pages on ARM64 devices.
   - If an uncompressed `.so` library in an APK or AAB has ELF load segments aligned to 4 KB instead of 16 KB (16,384 bytes), `dlopen()` fails or the app process crashes immediately with a `SIGSEGV` at runtime upon database initialization.

2. **Gradle JNI Compression Bug**:
   - By default, Android Gradle Plugin compressed `.so` files into the APK, causing the OS package manager to extract them into app private storage at install time with OS-dependent alignment.
   - **The Fix**: Mandate `useLegacyPackaging = false` so native libraries are stored uncompressed and page-aligned directly in the APK zip.

3. **R8 / ProGuard Minification Removing JNI Glue**:
   - R8 minification strips internal SQLite/SQLCipher native bridge classes if keep rules are absent, causing runtime `UnsatisfiedLinkError` or `ClassNotFoundException`.

---

## 🛠️ 1. Gradle Configuration in `app/build.gradle.kts`

```kotlin
android {
    // ...
    packaging {
        jniLibs {
            // Mandate uncompressed packaging so native .so files are aligned directly in the APK/AAB
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // SQLCipher 4.6.1+ includes 16 KB page-aligned ELF segment binaries
    implementation(libs.sqlcipher.android) // "net.zetetic:sqlcipher-android:4.6.1"
    implementation(libs.sqlite.ktx)
}
```

---

## 🔍 2. Auditing APK / AAB Alignment

Run the following terminal commands to verify that `.so` native libraries inside the built package meet 16 KB (16384 byte) alignment:

### A. Check Zip Segment Offsets via `zipinfo`:
```bash
zipinfo -v app/build/outputs/apk/release/app-release.apk "lib/arm64-v8a/*.so" | grep "offset of local header"
```
*Verification Rule*: The local header offset integer divided by `4096` and `16384` MUST have a remainder of `0`.

### B. Audit ELF Program Headers via `readelf`:
```bash
# Extract the native library
unzip -p app-release.apk lib/arm64-v8a/libsqlcipher.so > /tmp/libsqlcipher.so
readelf -l /tmp/libsqlcipher.so | grep -A 1 "LOAD"
```
*Verification Rule*: Look for `Align 0x4000` (16 KB) or `Align 0x10000` (64 KB). If it displays `Align 0x1000` (4 KB), the binary is non-compliant and must be upgraded.

---

## 🛡️ 3. ProGuard & R8 Keep Rules (`proguard-rules.pro`)

Add these comprehensive keep rules to prevent release build obfuscation regressions:

```proguard
# Net.Zetetic SQLCipher
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.**

# AndroidX Room & SQLite
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor & OkHttp Client
-keep class io.ktor.** { *; }
-keep class okhttp3.** { *; }
-dontwarn io.ktor.**
-dontwarn okhttp3.**
```
