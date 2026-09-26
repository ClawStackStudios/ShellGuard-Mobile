# 🚀 Google Play Console Release & Deployment Workflow

> **Standard Operating Procedure for ShellGuard Mobile Releases**  
> *Engineered for Android App Bundle (`.aab`) Generation, Keystore Signing, 16 KB Alignment Verification, and Play Console Distribution.*

---

## 📋 Workflow Overview

```mermaid
flowchart TD
    PreFlight["1. 🧪 Pre-Flight Gate<br/>(Unit Tests & 16 KB Alignment Check)"]
    Version["2. 🔢 Version Bump<br/>(versionCode + 1, versionName SemVer)"]
    Bundle["3. 📦 Generate AAB Bundle<br/>(./gradlew bundleRelease)"]
    Validate["4. 🔍 Inspect Bundle<br/>(bundletool / APK Analyzer)"]
    Upload["5. 🌐 Upload to Play Console<br/>(Internal / Closed Testing Track)"]
    Rollout["6. 👥 Tester Distribution<br/>(Share Opt-in Link)"]
    Tag["7. 🏷️ Git Tag & Memory Bank<br/>(git tag vX.Y.Z.W & changelog.md)"]

    PreFlight --> Version --> Bundle --> Validate --> Upload --> Rollout --> Tag
```

---

## 🛠️ Step 1: Pre-Flight Verification Gate

Before building a release artifact, run the full verification gate to ensure zero regressions:

```bash
# Clean build cache and execute all unit and UI tests
./gradlew clean testDebugUnitTest
```

### Invariants to Verify:
- [ ] **Target SDK API 36**: Google Play Console strictly mandates `targetSdk = 36` (Android 16). Any bundle targeting API 35 or lower will be rejected during upload validation.
- [ ] **Tests Passing**: 100% pass rate across all Robolectric and unit tests.
- [ ] **16 KB Alignment**: `gradle/libs.versions.toml` specifies `sqlcipher = "4.6.1"` and `app/build.gradle.kts` specifies `jniLibs.useLegacyPackaging = false`.
- [ ] **Security Boundaries**: `FLAG_SECURE` active for production builds (`!BuildConfig.DEBUG` in `MainActivity.kt`).
- [ ] **Network Security**: Cleartext permitted strictly for local LAN / VPN origins in `res/xml/network_security_config.xml`.
- [ ] **ProGuard Rules**: `app/proguard-rules.pro` preserves SQLCipher JNI, Kotlinx Serialization, and Room DAOs.

---

## 🔢 Step 2: Versioning Protocol

Every release bundle uploaded to Google Play **MUST** have a strictly higher `versionCode` than any previously uploaded bundle.

Edit [`app/build.gradle.kts`](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/app/build.gradle.kts):

```kotlin
android {
    defaultConfig {
        applicationId = "com.clawstack.shellguard"
        minSdk = 24
        targetSdk = 36
        versionCode = N + 1         // Increment monotonically (+1 integer for every upload: 1, 2, 3...)
        versionName = "X.Y.Z.N"     // SemVer display version (e.g. "0.0.0.1", "0.0.0.2", "1.0.0.0"...)
    }
}
```

> [!TIP]
> **Durable Conflict Resolution: Updating a Release Without Bumping Version Name**:
> If you need to re-upload or hotfix a release under the same user-facing version name (e.g. `vX.Y.Z.N`), keep `versionName = "X.Y.Z.N"` unchanged and increment `versionCode` (e.g. `N` ➔ `N + 1`). In Google Play Console, it will appear as `X.Y.Z.N (Build N+1)`, and the app UI will seamlessly continue displaying `vX.Y.Z.N`.

---

## 📦 Step 3: Generate Signed Release Artifacts

### 🚀 Recommended: Automated Cloud Build via GitHub Actions (Zero Local CPU Drain)
To avoid local machine resource exhaustion, use the cloud release pipeline:

1. **Trigger via Commit Flag or Git Tag**:
   ```bash
   # Method A: Commit flag on main
   git commit -m "chore(release): release vX.Y.Z.N (Build N) --release vX.Y.Z.N"
   git push origin main

   # Method B: Git tag
   git tag vX.Y.Z.N
   git push origin vX.Y.Z.N
   ```
2. **Download Signed Artifacts**:
   - Navigate to GitHub **Actions** or **Releases** on `ClawStackStudios/ShellGuard-Mobile`.
   - Download the signed `shellguard-mobile-vX.Y.Z.N.aab` (for Google Play Console) and `shellguard-mobile-vX.Y.Z.N.apk` (for direct sideloading).

### 💻 Alternative: Local Build (Requires Local Keystore)
If building locally, run:
```bash
./gradlew bundleRelease assembleRelease
```
Artifacts output to:
- `app/build/outputs/bundle/release/app-release.aab`
- `app/build/outputs/apk/release/app-release.apk`
> User-facing version labels dynamically bind to `BuildConfig.VERSION_NAME`.

---

## 🔑 Step 4: Keystore Generation & GitHub Actions Secrets Setup

Google Play uses **Play App Signing**. You sign the bundle with your **Upload Key**, and Google signs the final APK delivered to user devices with the App Signing Key.

### A. Generate Upload Keystore (`keytool`)
Run `keytool` (bundled with Android Studio's JBR) to generate `my-upload-key.jks`:

```bash
# 1. Generate the RSA 2048-bit JKS Keystore (valid for 25+ years)
/config/Applications/android-studio/jbr/bin/keytool -genkeypair -v \
  -keystore my-upload-key.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype JKS
```
*Prompt tips:*
- Enter a strong keystore password and key password.
- When prompted for "What is your first and last name?", enter `ClawStack Studios`.
- Fill in organizational details as desired, then type `yes` to confirm.

### B. Base64 Encode for GitHub Secrets
Convert the binary `.jks` file into a single-line base64 string:

```bash
# 2. Encode to single-line base64 (no line wrapping)
base64 -w 0 my-upload-key.jks > upload-key.base64.txt
```

> [!CAUTION]
> **Android Secrets Safety**: `my-upload-key.jks` and `*.base64.txt` contain private signing keys and must **NEVER** be committed to Git. Both patterns are already guarded in `.gitignore`. Store the `.jks` in a secure offline location (e.g., your ShellGuard vault!).

### C. Configure GitHub Repository Secrets
In your GitHub repository (`ClawStackStudios/ShellGuard-Mobile`), navigate to **Settings** → **Secrets and variables** → **Actions** → **New repository secret**:

| Secret Name | Value | Description |
| :--- | :--- | :--- |
| `ANDROID_SIGNING_KEY` | *(Paste entire contents of `upload-key.base64.txt`)* | Base64-encoded upload keystore |
| `ANDROID_KEYSTORE_PASSWORD` | *(Your keystore password)* | Master password used in `keytool` |
| `ANDROID_KEY_ALIAS` | `upload` | Alias specified during `-alias upload` |
| `ANDROID_KEY_PASSWORD` | *(Your key password)* | Key password used in `keytool` |

---

## 🔍 Step 5: Validate 16 KB Page-Size Alignment

Before uploading to Play Console, verify native binary page-size alignment:

```bash
# Verify libsqlcipher.so is 16 KB aligned
zipinfo -v app/build/outputs/bundle/release/app-release.aab | grep -A 2 "libsqlcipher.so"
```
The file offset must be evenly divisible by `16384` (16 KB), confirming Android 15/16 compatibility.

---

## 🌐 Step 6: Google Play Console Release Upload

1. Navigate to the **[Google Play Console](https://play.google.com/console)**.
2. Select **ShellGuard Mobile** (`com.clawstack.shellguard`).
3. In the left-hand sidebar, navigate to **Testing** → **Internal testing** (or **Closed testing**).
4. Click **Create new release** (top right).
5. **Upload Bundle**: Drag and drop `shellguard-mobile-vX.Y.Z.N.aab` into the upload zone.
6. **Release Name**: Play Console will automatically populate `X.Y.Z.N (Build N)` matching your Gradle config.
7. **Release Notes**: Copy the formatted `<en-US>` notes directly from [`RELEASE-PLAY.md`](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/RELEASE-PLAY.md) (<500 characters):
   ```xml
   <en-US>
   • Ktor Network & Sync: Direct connection to self-hosted ShellGuard servers over LAN or mesh.
   • Vault Dashboard: Unified view of passwords, notes, and SSH keys with real-time search & pod filters.
   • Bitwarden-Model Offline Caching: Full read, search, and copy access with zero split-brain conflicts.
   • Base62 Key Parity: Native support for all sovereign ShellKey identity files.
   • Soft Keyboard Polish: Smooth cursor retention with zero blackout.
   • Hardware Security: Android KeyStore, SQLCipher AES-256, 16 KB page-aligned.
   </en-US>
   ```
8. Click **Next** → **Save** → **Review release** → **Start rollout to Internal testing**.

---

## 👥 Step 7: Manage & Distribute to Testers

1. In Play Console under **Internal testing**, open the **Testers** tab.
2. Create an **Email list** (e.g. `ShellGuard Alpha Testers`) and add your testers' Google account emails.
3. Scroll down to **How testers join your test** and copy the **"Join on Android"** link or **"Join on the web"** link.
4. Share the link with your testers:
   - Testers tap the link on their Android device.
   - Tap **Accept Invite**.
   - Download/update the app directly from the Google Play Store!

---

## 🏷️ Step 8: Post-Release Tagging & Memory Bank Sync

After rollout, tag the repository and update project history:

```bash
# Tag the git commit matching the release version
git tag -a v0.0.0.1 -m "Release v0.0.0.1 (Build 1) to Google Play Internal Testing"
git push origin v0.0.0.1
```

### Update Memory Bank:
1. Append details to [`.agents/memory-bank/changelog.md`](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/.agents/memory-bank/changelog.md).
2. Record the rollout event in [`.agents/memory-bank/activeContext.md`](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/.agents/memory-bank/activeContext.md).
