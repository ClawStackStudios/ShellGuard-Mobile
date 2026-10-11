# 📋 Task 2: Core Storage Wiring, Settings Debt & Panic Purge Parity

> **Target Objective**: Resolve all architectural storage and dependency injection debt for attachments, fix the physical ciphertext leak on Panic Purge, introduce the Default URI Match Mode preference, configure the Android FileProvider, and protect backup schema parity.

---

## 🏛️ Invariants & Rules
1. **Physical Destruction on Panic Wipe (Storage Lifecycle Parity)**:
   A panic purge that leaves encrypted ciphertext files in internal storage is an architectural vulnerability. `attachmentVaultManager.clearAll()` MUST be invoked during `executePanicPurge()`.
2. **Frameworkless DI Contract**:
   `AppContainer` maintains lazy instantiation. Do NOT introduce reflection or external DI frameworks.
3. **⚠️ Invariant**: NEVER tag `@jules` in PR comments or commit messages. Plain text `Hey Jules` or `@google-labs-jules[bot]` only.
4. **Attribution Format**: All commits must follow the two-layer format (`User: ...` / `AI: ...`).

---

## 📁 Files in Scope (Strictly Orthogonal)
- `app/src/main/java/com/clawstack/shellguard/di/AppContainer.kt`
- `app/src/main/java/com/clawstack/shellguard/data/local/SettingsRepository.kt`
- `app/src/main/java/com/clawstack/shellguard/ui/screens/settings/SettingsViewModel.kt`
- `app/src/main/java/com/clawstack/shellguard/ui/screens/settings/SettingsAutofillScreen.kt`
- `app/src/main/res/xml/attachment_file_paths.xml`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/clawstack/shellguard/data/backup/VaultBackupEngine.kt`
- `app/src/test/java/com/clawstack/shellguard/ui/screens/settings/SettingsViewModelTest.kt`
- `app/src/test/java/com/clawstack/shellguard/data/local/SettingsRepositoryTest.kt`
- `app/src/test/java/com/clawstack/shellguard/data/backup/VaultBackupEngineTest.kt`

---

## 🛠️ Step-by-Step Implementation Guide

### 1. AppContainer DI Wiring (`AppContainer.kt`)
1. In `interface AppContainer`:
   Declare:
   ```kotlin
   val attachmentVaultManager: com.clawstack.shellguard.data.local.AttachmentVaultManager
   ```
2. In `class DefaultAppContainer(private val context: Context) : AppContainer`:
   Implement lazy property:
   ```kotlin
   override val attachmentVaultManager: com.clawstack.shellguard.data.local.AttachmentVaultManager by lazy {
       com.clawstack.shellguard.data.local.AttachmentVaultManager(context)
   }
   ```
3. In `syncRepository`:
   Pass `attachmentVaultManager = attachmentVaultManager` to the `SyncRepository` constructor!

---

### 2. Panic Purge Storage Parity (`SettingsViewModel.kt`)
In `SettingsViewModel.kt`, locate `executePanicPurge(onCompleted: () -> Unit)`:
```kotlin
fun executePanicPurge(onCompleted: () -> Unit): Job = viewModelScope.launch {
    _extraState.update { it.copy(isWiping = true) }
    try {
        // 1. Wipe Room Database tables
        appContainer.database.clearAllTables()
        // 2. Wipe physical encrypted attachment files from internal storage
        appContainer.attachmentVaultManager.clearAll()
        // 3. Zeroize in-memory secrets and clear EncryptedSharedPreferences session
        appContainer.deviceVault.clearSession()
        // 4. Clear settings repository preferences
        appContainer.settingsRepository.clearAll()
        // 5. Reset vault lock manager
        appContainer.vaultLockManager.unlockVault()
    } catch (t: Throwable) {
        // Ensure session & attachments are wiped even if database wipe throws
        try { appContainer.attachmentVaultManager.clearAll() } catch (_: Throwable) {}
        appContainer.deviceVault.clearSession()
    } finally {
        _extraState.update { it.copy(isWiping = false) }
        onCompleted()
    }
}
```

---

### 3. Default URI Match Mode Settings (`SettingsRepository.kt` & `SettingsAutofillScreen.kt`)
1. In `com.clawstack.shellguard.data.local.SettingsRepository.kt`:
   - Import `com.clawstack.shellguard.domain.matcher.UriMatchMode`.
   - Update `data class AppSettings`:
     ```kotlin
     data class AppSettings(
         // existing fields...
         val defaultUriMatchMode: UriMatchMode = UriMatchMode.BASE_DOMAIN,
         // ...
     )
     ```
   - In `interface SettingsRepository`:
     Add `suspend fun setDefaultUriMatchMode(mode: UriMatchMode)`.
   - In `class SettingsRepositoryImpl`:
     Add `val DEFAULT_URI_MATCH_MODE = stringPreferencesKey("pref_default_uri_match_mode")` to `PreferencesKeys`.
     Read in `settingsFlow` mapping (default to `UriMatchMode.BASE_DOMAIN` if null or invalid).
     Implement `setDefaultUriMatchMode`:
     ```kotlin
     override suspend fun setDefaultUriMatchMode(mode: UriMatchMode) {
         dataStore.edit { preferences ->
             preferences[PreferencesKeys.DEFAULT_URI_MATCH_MODE] = mode.name
         }
     }
     ```
2. In `SettingsAutofillScreen.kt`:
   - Add a preference item/card for **"Default URI Match Detection"**.
   - Show the current mode (e.g. "Base Domain (Recommended)").
   - Provide a selection dialog or radio choices:
     - `BASE_DOMAIN`: Base Domain (matches subdomains)
     - `HOST`: Exact Host
     - `EXACT`: Exact URL
     - `STARTS_WITH`: Starts With
     - `NEVER`: Never Match
   - When chosen, invoke `settingsViewModel.setDefaultUriMatchMode(mode)`.

---

### 4. FileProvider Declaration (`attachment_file_paths.xml` & `AndroidManifest.xml`)
1. Create `app/src/main/res/xml/attachment_file_paths.xml`:
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <paths>
       <!-- Temporary decrypted previews for external apps (zeroized post-view) -->
       <cache-path name="decrypted_attachments" path="decrypted_attachments/" />
   </paths>
   ```
2. In `app/src/main/AndroidManifest.xml`, add inside `<application>`:
   ```xml
   <!-- Secure Attachment File Sharing Provider -->
   <provider
       android:name="androidx.core.content.FileProvider"
       android:authorities="${applicationId}.fileprovider"
       android:exported="false"
       android:grantUriPermissions="true">
       <meta-data
           android:name="android.support.FILE_PROVIDER_PATHS"
           android:resource="@xml/attachment_file_paths" />
   </provider>
   ```

---

### 5. Backup Schema Fidelity (`VaultBackupEngine.kt`)
In `com.clawstack.shellguard.data.backup.VaultBackupEngine.kt`:
In `data class BackupVaultItem`:
Add:
```kotlin
val attachments: String = "[]",
```
Ensure that when exporting `VaultPearlEntity` and `SecureNoteEntity`, `attachments` is included in the exported item, and when restoring, `attachments` is restored to the entity.

---

### 6. Unit Testing
1. In `SettingsViewModelTest.kt`:
   Add test `executePanicPurge_clearsAttachmentFiles`: verify `attachmentVaultManager.clearAll()` is called.
2. In `SettingsRepositoryTest.kt`:
   Add test `defaultUriMatchMode_persistsAndUpdates`: verify round-trip persistence of `UriMatchMode`.
3. In `VaultBackupEngineTest.kt`:
   Add test `exportAndRestore_preservesAttachments`: verify items retain their `attachments` JSON string.

---

## 🧪 Verification Gate
Run and ensure 100% green:
```bash
./gradlew testDebugUnitTest --no-daemon
./gradlew assembleDebug --no-daemon
```
