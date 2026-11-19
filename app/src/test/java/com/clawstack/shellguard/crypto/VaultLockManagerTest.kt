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
    fun testColdStartWithActiveSessionStartsLocked() {
        assertTrue(lockManager.isVaultLocked.value)
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
        lockManager.unlockVault()
        lockManager.setLockTimeout(LockTimeout.IMMEDIATE)
        assertEquals(LockTimeout.IMMEDIATE, lockManager.getLockTimeout())

        lockManager.onAppBackgrounded(1000L)
        assertTrue(lockManager.isVaultLocked.value)
    }

    @Test
    fun testTimeoutElapsedTriggersLock() {
        lockManager.unlockVault()
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
        lockManager.unlockVault()
        lockManager.setLockTimeout(LockTimeout.NEVER)
        assertEquals(LockTimeout.NEVER, lockManager.getLockTimeout())

        val t0 = 100_000L
        lockManager.onAppBackgrounded(t0)
        lockManager.onAppForegrounded(t0 + 999_999_999L)
        assertFalse(lockManager.isVaultLocked.value)
    }

    @Test
    fun testNeverTimeoutStaysUnlockedOnColdStartUnlessExplicitlyLocked() {
        lockManager.unlockVault()
        lockManager.setLockTimeout(LockTimeout.NEVER)

        val unlockedRestart = VaultLockManager(context, deviceVault)
        assertFalse(unlockedRestart.isVaultLocked.value)

        unlockedRestart.lockVaultNow()
        val lockedRestart = VaultLockManager(context, deviceVault)
        assertTrue(lockedRestart.isVaultLocked.value)
    }

    @Test
    fun testProcessDeathWithBackgroundTimestampLocksOnRelaunch() {
        lockManager.unlockVault()
        lockManager.setLockTimeout(LockTimeout.FIVE_MINUTES)
        lockManager.onAppBackgrounded(100_000L)

        // Simulate OS process death & cold relaunch
        val relaunchedLockManager = VaultLockManager(context, deviceVault)
        assertTrue(relaunchedLockManager.isVaultLocked.value)
        assertTrue(deviceVault.hasActiveSession())
    }

    @Test
    fun testClockRollbackOnForegroundLocksFailClosed() {
        lockManager.unlockVault()
        lockManager.setLockTimeout(LockTimeout.FIVE_MINUTES)

        val t0 = 500_000L
        lockManager.onAppBackgrounded(t0)
        // System clock jumped backwards
        lockManager.onAppForegrounded(t0 - 10_000L)
        assertTrue(lockManager.isVaultLocked.value)
    }
}
