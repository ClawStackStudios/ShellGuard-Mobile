package com.clawstack.shellguard.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EncryptedDeviceVaultTest {

    private lateinit var context: Context
    private lateinit var deviceVault: EncryptedDeviceVault

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        deviceVault = EncryptedDeviceVault(context)
        deviceVault.clearSession()
    }

    @Test
    fun testSaveSessionPersistsShellKeyAndActiveSession() {
        val testHuKey = "hu-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val testUserUuid = "usr-12345"
        val shellKey = ShellCryptionEngine.deriveShellKey(testHuKey, testUserUuid)

        assertFalse(deviceVault.hasActiveSession())
        assertNull(deviceVault.getInMemoryShellKey())

        deviceVault.saveSession(
            token = "jwt-session-token",
            serverUrl = "http://192.168.1.5:6464",
            ownerUuid = testUserUuid,
            username = "admin",
            hashedKey = ClawCrypto.hashHumanKey(testHuKey),
            shellKey = shellKey
        )

        assertTrue(deviceVault.hasActiveSession())
        assertEquals("jwt-session-token", deviceVault.getSessionToken())
        assertEquals("http://192.168.1.5:6464", deviceVault.getServerUrl())
        assertEquals(testUserUuid, deviceVault.getOwnerUuid())
        assertEquals("admin", deviceVault.getUsername())

        val retrievedKey = deviceVault.getInMemoryShellKey()
        assertNotNull(retrievedKey)
        assertArrayEquals(shellKey, retrievedKey)
    }

    @Test
    fun testReinstatingVaultRestoresShellKeyFromPreferences() {
        val testHuKey = "hu-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val testUserUuid = "usr-12345"
        val shellKey = ShellCryptionEngine.deriveShellKey(testHuKey, testUserUuid)

        deviceVault.saveSession(
            token = "jwt-token",
            serverUrl = "http://192.168.1.5:6464",
            ownerUuid = testUserUuid,
            username = "admin",
            hashedKey = ClawCrypto.hashHumanKey(testHuKey),
            shellKey = shellKey
        )

        // Simulate a new process instance or cold boot where RAM was cleared
        val coldVault = EncryptedDeviceVault(context)
        assertTrue(coldVault.hasActiveSession())
        val restoredKey = coldVault.getInMemoryShellKey()
        assertNotNull(restoredKey)
        assertArrayEquals(shellKey, restoredKey)
    }

    @Test
    fun testMissingShellKeyInvalidatesActiveSession() {
        deviceVault.saveSession(
            token = "jwt-token",
            serverUrl = "http://192.168.1.5:6464",
            ownerUuid = "usr-12345",
            username = "admin",
            hashedKey = "hash",
            shellKey = null // No shellKey provided
        )

        assertFalse(deviceVault.hasActiveSession())
        assertNull(deviceVault.getInMemoryShellKey())
    }

    @Test
    fun testClearSessionPurgesAllState() {
        val shellKey = byteArrayOf(1, 2, 3, 4, 5)
        deviceVault.saveSession(
            token = "jwt-token",
            serverUrl = "http://localhost:6565",
            ownerUuid = "user-1",
            username = "admin",
            hashedKey = "hash",
            shellKey = shellKey
        )
        assertTrue(deviceVault.hasActiveSession())

        deviceVault.clearSession()
        assertFalse(deviceVault.hasActiveSession())
        assertNull(deviceVault.getSessionToken())
        assertNull(deviceVault.getServerUrl())
        assertNull(deviceVault.getOwnerUuid())
        assertNull(deviceVault.getInMemoryShellKey())
    }
}
