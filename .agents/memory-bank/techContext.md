# Technical Context: ShellGuard Mobile

## Core Stack & Dependencies
- **Platform**: Native Android (Kotlin 2.2+, JDK 17).
- **Target SDK**: 36 (Android 16 Baklava), **Min SDK**: 24 (Android 7.0 Nougat).
- **Package Name**: `com.clawstack.shellguard`.
- **UI Framework**: Jetpack Compose + Material 3 + Compose Navigation.
- **Dependency Injection**: Dagger Hilt (`2.51+`).
- **Local Database**: AndroidX Room (`2.7.0`) with KSP.
- **At-Rest Encryption**: SQLCipher for Android (`net.zetetic:sqlcipher-android:4.6.1`).
- **Networking**: Ktor Client (`2.3.12`) with OkHttp engine & Kotlinx Serialization.
- **Biometrics & Security**: AndroidX Biometric (`1.2.0-alpha05`), AndroidX Security Crypto (`1.1.0-alpha06`).
- **Autofill**: Android Autofill Framework (API 26+) & Credential Manager (API 34+).
- **Camera & Scanning**: CameraX (`1.5.0`) & Google ML Kit Barcode Scanning (`17.3.0`).
- **Background Work**: AndroidX WorkManager (`2.9.0`).

## Development & Build Environment Invariants
```bash
export JAVA_HOME="/config/Applications/android-studio/jbr"
export PATH="$JAVA_HOME/bin:/config/Android/Sdk/platform-tools:$PATH"
export GRADLE_OPTS="-XX:-UsePerfData -Djava.io.tmpdir=$PWD/app/build/tmp"
```

## 16 KB Page Alignment Packaging
In `app/build.gradle.kts`:
```kotlin
packaging {
    jniLibs {
        useLegacyPackaging = false
    }
}
```
Ensures `.so` libraries remain uncompressed and 16 KB page-aligned inside APKs/AABs for Android 15+.

## Network Invariants: Cleartext HTTP, LAN & Tailscale
1. **Network Security Config (`res/xml/network_security_config.xml`)**:
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <network-security-config>
       <base-config cleartextTrafficPermitted="true">
           <trust-anchors>
               <certificates src="system" />
               <certificates src="user" />
           </trust-anchors>
       </base-config>
   </network-security-config>
   ```
   *Critical Invariant*: Android's `<domain>` tag does NOT support CIDR subnet notation (`192.168.0.0/16` or `100.64.0.0/10`). Declaring `<base-config cleartextTrafficPermitted="true" />` is strictly mandatory for raw IP address connections.
2. **Manifest Binding**: `AndroidManifest.xml` must declare:
   - `android:networkSecurityConfig="@xml/network_security_config"`
   - `android:usesCleartextTraffic="true"`
3. **OkHttp Engine Configuration**:
   - Must register `ConnectionSpec.CLEARTEXT` alongside `ConnectionSpec.MODERN_TLS` and `ConnectionSpec.COMPATIBLE_TLS`.
   - Relaxed `X509TrustManager` and permissive `HostnameVerifier` for private IP subnets and Tailscale MagicDNS (`*.ts.net`).
   - Enable `followRedirects(true)` and `followSslRedirects(true)` for VPN routing.
