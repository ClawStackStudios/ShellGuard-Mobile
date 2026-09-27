package com.clawstack.shellguard.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VaultLockManagerTest {

    private lateinit var context: Context
    private lateinit var deviceVault: EncryptedDeviceVault
    private lateinit var lockManager: VaultLockManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        deviceVault = EncryptedDeviceVault(context)
        deviceVault.clearSession()

        // Establish an active session
        val shellKey = ByteArray(32) { 0x01 }
        deviceVault.saveSession(
            token = "test-token",
            serverUrl = "http://192.168.1.1:6464",
            ownerUuid = "user-123",
            username = "admin",
            hashedKey = "hashed-key",
            shellKey = shellKey
        )

        lockManager = VaultLockManager(context, deviceVault)
    }

    @Test
    fun testInitialStateUnlocked() {
        assertFalse(lockManager.isVaultLocked.value)
    }

    @Test
    fun testExplicitLockAndUnlock() {
        lockManager.lockVaultNow()
        assertTrue(lockManager.isVaultLocked.value)

        lockManager.unlockVault()
        assertFalse(lockManager.isVaultLocked.value)
    }

    @Test
    fun testImmediateLockOnBackground() {
        lockManager.setLockTimeout(LockTimeout.IMMEDIATE)
        assertEquals(LockTimeout.IMMEDIATE, lockManager.getLockTimeout())

        lockManager.onAppBackgrounded(1000L)
        assertTrue(lockManager.isVaultLocked.value)
    }

    @Test
    fun testTimeoutElapsedTriggersLock() {
        lockManager.setLockTimeout(LockTimeout.FIVE_MINUTES)

        // Elapsed < 5 minutes (e.g. 2 minutes = 120_000ms)
        val t0 = 100_000L
        lockManager.onAppBackgrounded(t0)
        lockManager.onAppForegrounded(t0 + 120_000L)
        assertFalse(lockManager.isVaultLocked.value)

        // Elapsed >= 5 minutes (e.g. 6 minutes = 360_000ms)
        lockManager.onAppBackgrounded(t0)
        lockManager.onAppForegrounded(t0 + 360_000L)
        assertTrue(lockManager.isVaultLocked.value)
    }

    @Test
    fun testNeverTimeoutDoesNotLock() {
        lockManager.setLockTimeout(LockTimeout.NEVER)
        assertEquals(LockTimeout.NEVER, lockManager.getLockTimeout())

        val t0 = 100_000L
        lockManager.onAppBackgrounded(t0)
        lockManager.onAppForegrounded(t0 + 999_999_999L)
        assertFalse(lockManager.isVaultLocked.value)
    }
}
