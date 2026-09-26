# 📐 ShellGuard Mobile — 16 KB Memory Page-Size Alignment Guide

> **Android 15+ (API 35/36) 16 KB Page Alignment Guardrails & SQLCipher Native Integration**  
> *Targeted for Google AI Studio Android Application Generator.*

---

## 1. Context & Architectural Constraint

Starting with Android 15 (Vanilla Ice Cream / API 35) and continuing into Android 16 (Baklava / API 36), Google introduces support for 16 KB page-size memory architectures. Devices running kernels with 16 KB page sizes will immediately crash (`SIGSEGV` / `INSTALL_FAILED_INVALID_APK`) when loading native shared libraries (`.so`) compiled with legacy 4 KB segment alignment.

ShellGuard Mobile uses **SQLCipher** (`libsqlcipher.so`) for whole-database encryption at rest. Ensuring 16 KB compatibility is an inviolable build requirement.

---

## 2. Inviolable Dependencies & Configuration

### A. SQLCipher Dependency
In `gradle/libs.versions.toml`:
```toml
[versions]
sqlcipher = "4.6.1" # Must be 4.6.1 or higher (compiled with 16 KB ELF segment alignment)

[libraries]
sqlcipher-android = { module = "net.zetetic:sqlcipher-android", version.ref = "sqlcipher" }
```

### B. Gradle Uncompressed Packaging
In `app/build.gradle.kts`:
```kotlin
android {
    ...
    packaging {
        jniLibs {
            // Mandates that shared libraries are stored uncompressed and page-aligned
            // directly inside the APK/AAB zip file.
            useLegacyPackaging = false
        }
    }
}
```

---

## 3. Native ELF Verification Procedure

Verify that `.so` binaries within the built APK satisfy the 16 KB boundary alignment:

```bash
# 1. Unzip or inspect the built APK
unzip -p app/build/outputs/apk/debug/app-debug.apk lib/arm64-v8a/libsqlcipher.so > /tmp/libsqlcipher.so

# 2. Check ELF LOAD segment alignment via readelf
readelf -l /tmp/libsqlcipher.so | grep -A 1 LOAD
```

**Expected Output:**
```
  LOAD           0x0000000000000000 0x0000000000000000 0x0000000000000000
                 0x0000000000001000 0x0000000000001000  R      0x4000
```
Where `0x4000` is hexadecimal for `16384` bytes (16 KB). If the alignment reads `0x1000` (4 KB), the binary is invalid.

---

## 4. Invariants

1. **Never downgrade SQLCipher below 4.6.1**.
2. **Never enable `useLegacyPackaging = true`**, as compressed libraries cannot be memory-mapped directly and lose page alignment.
3. Every automated CI build must run the alignment check before generating release artifacts.
