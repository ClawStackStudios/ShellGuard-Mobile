# Runtime Environment & Toolchain Truth

## Abstract Toolchain Contracts
- **JDK Runtime**: Java 17+ LTS (OpenJDK, Zulu, Eclipse Temurin, or IDE-bundled JBR).
- **Android Gradle Plugin (AGP)**: `9.1.1` (Gradle `9.3.1` wrapper).
- **Kotlin Standard**: `2.2.10` with Compose Compiler plugin.
- **Android Platform Bounds**:
  - `minSdk`: `24` (Android 7.0 Nougat)
  - `targetSdk`: `36` (Android 16 Baklava)
  - `compileSdk`: `36`
  - `robolectricSdk`: `34` (Strict ceiling to prevent `UnsupportedOperationException` on CI)

## Execution Invariants & JVM Mechanics
- **Container / Headless Memory Protection**:
  `-XX:-UsePerfData` prevents JVM shared-memory crash loops in containerized and memory-bounded subshell environments.
- **Filesystem Temp Isolation**:
  Isolate Java temp directory via `-Djava.io.tmpdir=$PWD/app/build/tmp` to avoid permission clashes, file locks, or host `/tmp` leaks.
- **Toolchain Resolution Invariant**:
  Any interactive or subshell invocation of Gradle, ADB, or JVM test tools must resolve `JAVA_HOME/bin` and `platform-tools` dynamically without hardcoding machine-specific absolute directories into repo sources.
- **Native Packaging & 16 KB Page Alignment**:
  - `jniLibs.useLegacyPackaging = false` in `app/build.gradle.kts` stores `.so` libraries uncompressed and aligned on 16 KB boundaries inside APKs and AABs.
  - SQLCipher bundled binaries (4.6.1+) comply with Android 15+ 16 KB ELF segment alignment.

## Network & Service Primitives
- **Cleartext HTTP Policy**:
  Intentional `<base-config cleartextTrafficPermitted="true">` in `network_security_config.xml` enables unencrypted HTTP over local private RFC 1918 subnets (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`) and virtual mesh routes (Tailscale / WireGuard).
- **Canonical Service Endpoints**:
  - `:6565` — Core Security Kernel REST API
  - `:6464` — Web Client Development Server
- **Transport Specs**:
  Ktor OkHttp engine configured with `ConnectionSpec.CLEARTEXT`, `COMPATIBLE_TLS`, and `MODERN_TLS`.
