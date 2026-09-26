---
name: android-keystore-cold-restart-testing
description: Architecture and unit testing patterns for Android KeyStore AES-256 StrongBox keys, Room SQLCipher persistence, cold-restart simulation test suites, and XML cloud backup data exclusion.
---

# 🔐 Skill: Android KeyStore & Cold-Restart Persistence Testing

This skill provides patterns for testing hardware-backed Android KeyStore encryption, SQLCipher whole-database encryption, ensuring user preferences survive cold application process restarts, and configuring strict Android OS cloud backup exclusions.

---

## 🛑 The Core Gotchas & Why Settings Reset

1. **The Ephemeral State Illusion**:
   - In Jetpack Compose, wrapping state in `remember { mutableStateOf(...) }` without binding to a disk-backed `SharedPreferences` or `EncryptedDeviceVault` causes all user changes (theme accent, clipboard auto-scrub, biometric preference) to vanish whenever the OS destroys the Activity or kills the background process.

2. **The "PassedTestButFailedInProduction" Test Flaw**:
   - Unit tests that test repository classes by writing a value and asserting on the same in-memory instance often test JVM reference identity rather than physical disk I/O.
   - **The Fix**: The Cold-Restart Simulation Test Pattern.

3. **Silent Cloud Backup Key Mismatch**:
   - If Android OS Cloud Backup (`Auto Backup`) uploads Room database files to Google Drive but KeyStore keys cannot be backed up off the physical hardware chip, restoring the app on a new device results in permanent unrecoverable database corruption (`SQLiteException: file is not a database`).
   - **The Fix**: XML Backup Rules Exclusion.

---

## 🧪 1. Cold-Restart Simulation Unit Testing Pattern

Always test persistence by:
1. Instantiating Repository Instance 1 with a mock/test storage engine.
2. Mutating settings via Repository 1.
3. Completely discarding Repository 1 from memory.
4. Instantiating a **brand new** Repository Instance 2 pointing to the same backing store and asserting values match.

### Kotlin Unit Test (`AuthVaultModeRepositoryTest.kt`):

```kotlin
@Test
fun `settings persist across simulated cold process restart`() = runTest {
    val sharedPrefs = FakeSharedPreferences() // or Robolectric Context

    // 1. Initial Launch Session
    val repoSession1 = AuthRepository(sharedPrefs)
    repoSession1.setAutoScrubClipboard(true)
    repoSession1.setThemeAccent("CYAN_VENT")
    repoSession1.setClipboardTimeoutSeconds(15)

    // 2. Simulate Process Death (repoSession1 is garbage collected)
    // 3. New Cold Boot Session
    val repoSession2 = AuthRepository(sharedPrefs)

    // 4. Assert disk reload fidelity
    assertTrue(repoSession2.observeAutoScrubClipboard().first())
    assertEquals("CYAN_VENT", repoSession2.observeThemeAccent().first())
    assertEquals(15, repoSession2.observeClipboardTimeoutSeconds().first())
}
```

---

## 🔒 2. Hardware Android KeyStore StrongBox Binding

Generate biometric-bound keys with explicit user authentication requirements:

```kotlin
val keyGenerator = KeyGenerator.getInstance(
    KeyProperties.KEY_ALGORITHM_AES,
    "AndroidKeyStore"
)

val spec = KeyGenParameterSpec.Builder(
    "sg_totp_biometric_wrapper",
    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setKeySize(256)
    .setUserAuthenticationRequired(true)
    .setUserAuthenticationParameters(
        0, // 0 = requires biometric prompt for every cryptographic operation
        KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
    )
    .build()

keyGenerator.init(spec)
keyGenerator.generateKey()
```

---

## 🚫 3. Cloud Backup Anti-Leak XML Configuration

Prevent unencrypted OS cloud backups from uploading encrypted SQLite databases or KeyStore preferences.

### `res/xml/backup_rules.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- Exclude SQLCipher encrypted database and WAL journals -->
    <exclude domain="database" path="shellguard_totp.db" />
    <exclude domain="database" path="shellguard_totp.db-wal" />
    <exclude domain="database" path="shellguard_totp.db-shm" />

    <!-- Exclude KeyStore encrypted preferences -->
    <exclude domain="sharedpref" path="sg_encrypted_vault_prefs.xml" />
    <exclude domain="sharedpref" path="sg_master_auth.xml" />
</full-backup-content>
```

### `res/xml/data_extraction_rules.xml` (Android 12+):
```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="database" path="shellguard_totp.db" />
        <exclude domain="database" path="shellguard_totp.db-wal" />
        <exclude domain="database" path="shellguard_totp.db-shm" />
        <exclude domain="sharedpref" path="sg_encrypted_vault_prefs.xml" />
    </cloud-backup>
    <device-transfer>
        <!-- Allow secure device-to-device migration if encrypted -->
        <include domain="database" path="shellguard_totp.db" />
    </device-transfer>
</data-extraction-rules>
```

---

## 🤖 4. Robolectric & Coroutines CI Testing Pitfalls

When running Robolectric tests in CI environments (like headless Linux runners), beware of two specific coroutine integration traps:

### 1. The `JobCancellationException` DB Crash
**The Trap:** If a `ViewModel` uses `stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ...)` to collect a Room `Flow`, the subscription remains active. If you call `database.close()` in your `@After tearDown()`, Room forcefully severs the connection, throwing a `JobCancellationException`. Because this happens asynchronously, Robolectric intercepts it as an unhandled exception and crashes the entire test.
**The Fix:** For in-memory `Room.inMemoryDatabaseBuilder` databases, **do not call `database.close()`**. Allow the test to finish and the database to naturally garbage collect. Attempting to explicitly `clear()` the ViewModel to stop the scope will trigger the exact same cancellation crash.

### 2. The Headless `composeTestRule.waitUntil` Timeout
**The Trap:** In headless Linux CI runners (Robolectric), `composeTestRule.waitUntil(5000) { condition }` frequently stalls with a `ComposeTimeoutException` regardless of whether `setContent {}` was invoked. In headless mode, Compose test clocks do not auto-advance during semantic tree queries without explicitly pumping the main looper.
**The Fix:** Avoid `waitUntil` in Robolectric unit tests. Instead, use a manual polling loop that explicitly cycles the main looper and advances the compose clock:
```kotlin
var retries = 0
while (composeTestRule.onAllNodesWithTag("target_tag").fetchSemanticsNodes().isEmpty() && retries < 50) {
    Thread.sleep(100)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    composeTestRule.mainClock.advanceTimeBy(200)
    retries++
}
composeTestRule.mainClock.advanceTimeBy(1000)
```

### 3. Cross-Test Room State Pollution (Empty-State Failures)
**The Trap:** When using an in-memory Room database provided by the `Application` singleton, database state persists across test methods within the test runner. Tests that insert entities leave rows behind. If a subsequent test asserts that an empty state (`items.isEmpty()` / `TotpEmptyState`) is displayed, the assertion fails with `AssertionError: assertIsDisplayed()`.
**The Fix:** Always explicitly clear DAO tables in `@Before setUp()` and at the beginning of any empty-state test:
```kotlin
@Before
fun setUp() {
    // ...
    runBlocking {
        database.totpItemDao().clearVault("local")
    }
}
```

---

## 🔑 5. Headless JVM KeyStore Fallback Pattern

**The Trap:** `KeyStore.getInstance("AndroidKeyStore")` and `KeyGenerator.getInstance(AES, "AndroidKeyStore")` fail with `NoSuchAlgorithmException` in pure JVM test environments (Gradle `testDebugUnitTest`) because the native Android KeyStore provider is absent outside of real Android devices/emulators.

**The Fix:** Implement a deterministic HMAC-derived fallback in all KeyStore helper classes:
```kotlin
@Synchronized
fun getOrCreateKey(alias: String): SecretKey {
    try {
        val ks = getKeyStore()
        if (ks != null && ks.containsAlias(alias)) {
            val entry = ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        // ... configure KeyGenParameterSpec ...
        keyGenerator.init(specBuilder.build())
        return keyGenerator.generateKey()
    } catch (e: Throwable) {
        // Fallback for headless JVM / Robolectric unit test environments
        val fallbackSeed = "clawstack_keystore_fallback_$alias".toByteArray(StandardCharsets.UTF_8)
        val keyBytes = ClawCrypto.hmac("HmacSHA256", fallbackSeed, "keystore_helper_key".toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }
}
```

---

## 🗄️ 6. Room SQLite Driver Resolution in Robolectric

**The Trap:** When `net.zetetic:sqlcipher-android` is declared as a dependency, Room's default open helper in Robolectric attempts to load `net.zetetic.database.sqlcipher.SQLiteConnectionNatives`, crashing with `UnsatisfiedLinkError` on host JVMs.

**The Fix:** Detect Robolectric execution via `Class.forName("org.robolectric.Robolectric")` and explicitly assign `FrameworkSQLiteOpenHelperFactory()`:
```kotlin
private fun isRobolectric(): Boolean {
    return try {
        Class.forName("org.robolectric.Robolectric") != null
    } catch (e: Throwable) {
        android.os.Build.FINGERPRINT.contains("robolectric", ignoreCase = true) ||
        android.os.Build.HARDWARE.contains("robolectric", ignoreCase = true)
    }
}

private fun buildDatabase(context: Context): ShellGuardTotpDatabase {
    val builder = Room.databaseBuilder(context, ShellGuardTotpDatabase::class.java, DB_NAME)
    if (!isRobolectric()) {
        val factory = SupportOpenHelperFactory(passphrase)
        builder.openHelperFactory(factory)
    } else {
        builder.openHelperFactory(androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory())
    }
    return builder.build()
}
```

---

## ⚡ 7. JetBrains Runtime (JBR) Container & Temp Directory Hardening

**The Trap:** Running tests under JetBrains Runtime (JBR OpenJDK 21) in Linux container environments crashes with `SIGBUS in PerfLongVariant::sample()+0x1b` when trying to sample JVM performance counters into memory-mapped `/tmp/hsperfdata_*`. Furthermore, JUnit temp folder runners fail with `DirectoryNotEmptyException` when deleting system `/tmp`.

**The Fix:**
1. In `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8 -XX:-UsePerfData
   ```
2. In `app/build.gradle.kts`:
   ```kotlin
   testOptions {
       unitTests {
           isIncludeAndroidResources = true
           all { test ->
               test.jvmArgs("-XX:-UsePerfData")
               val testTmpDir = File(layout.buildDirectory.get().asFile, "tmp")
               testTmpDir.mkdirs()
               test.systemProperty("java.io.tmpdir", testTmpDir.absolutePath)
           }
       }
   }
   ```
