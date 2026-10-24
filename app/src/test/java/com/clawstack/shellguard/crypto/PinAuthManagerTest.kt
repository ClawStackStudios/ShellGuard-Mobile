package com.clawstack.shellguard.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.data.local.SettingsRepository
import com.clawstack.shellguard.data.local.SettingsRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PinAuthManagerTest {

    private lateinit var context: Context
    private lateinit var deviceVault: EncryptedDeviceVault
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var pinAuthManager: PinAuthManager

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("test_pin_auth_mgr", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        deviceVault = EncryptedDeviceVault(context, prefs)
        deviceVault.clearSession()
        deviceVault.clearPin()

        settingsRepository = SettingsRepositoryImpl(context)
        settingsRepository.clearAll()

        pinAuthManager = PinAuthManager(deviceVault, settingsRepository)
    }

    @Test
    fun testSetupAndVerifyValidPin() = runBlocking {
        assertFalse(pinAuthManager.hasPin())

        pinAuthManager.setupPin("123456")

        assertTrue(pinAuthManager.hasPin())
        assertTrue(settingsRepository.settingsFlow.first().hasPinConfigured)

        // Correct PIN verifies true
        assertTrue(pinAuthManager.verifyPin("123456"))
        assertEquals(0, deviceVault.getFailedPinAttempts())
    }

    @Test
    fun testVerifyInvalidPinRejection() = runBlocking {
        pinAuthManager.setupPin("987654")

        assertFalse(pinAuthManager.verifyPin("123456"))
        assertEquals(1, deviceVault.getFailedPinAttempts())
        assertEquals(4, pinAuthManager.getRemainingAttempts(maxAttempts = 5))

        assertFalse(pinAuthManager.verifyPin("000000"))
        assertEquals(2, deviceVault.getFailedPinAttempts())
        assertEquals(3, pinAuthManager.getRemainingAttempts(maxAttempts = 5))
    }

    @Test
    fun testLockoutTriggeredWhenThresholdExceeded() = runBlocking {
        pinAuthManager.setupPin("5555")

        assertFalse(pinAuthManager.isLockedOut(maxAttempts = 3))

        pinAuthManager.verifyPin("1111")
        pinAuthManager.verifyPin("2222")
        assertFalse(pinAuthManager.isLockedOut(maxAttempts = 3))

        pinAuthManager.verifyPin("3333")
        assertTrue(pinAuthManager.isLockedOut(maxAttempts = 3))
        assertEquals(0, pinAuthManager.getRemainingAttempts(maxAttempts = 3))

        // Reset clears lockout
        pinAuthManager.resetAttempts()
        assertFalse(pinAuthManager.isLockedOut(maxAttempts = 3))
        assertEquals(3, pinAuthManager.getRemainingAttempts(maxAttempts = 3))
    }

    @Test
    fun testClearPinWipesStorageAndPreferences() = runBlocking {
        pinAuthManager.setupPin("4321")
        assertTrue(pinAuthManager.hasPin())

        pinAuthManager.clearPin()

        assertFalse(pinAuthManager.hasPin())
        assertFalse(settingsRepository.settingsFlow.first().hasPinConfigured)
        assertNull(deviceVault.getPinHash())
        assertNull(deviceVault.getPinSalt())
    }

    @Test
    fun testMasterKeyVerificationOnDeviceVault() {
        val testHuKey = "hu-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val wrongKey = "hu-ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

        deviceVault.saveSession(
            token = "jwt",
            serverUrl = "http://192.168.1.18:6565",
            ownerUuid = "usr-1",
            username = "lucas",
            hashedKey = ClawCrypto.hashHumanKey(testHuKey),
            shellKey = ByteArray(32)
        )

        assertTrue(deviceVault.verifyMasterKey(testHuKey))
        assertFalse(deviceVault.verifyMasterKey(wrongKey))
    }

    @Test
    fun testVerifyClawKeyJsonContent() {
        val testHuKey = "hu-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        deviceVault.saveSession(
            token = "jwt",
            serverUrl = "http://192.168.1.18:6565",
            ownerUuid = "usr-1",
            username = "lucas",
            hashedKey = ClawCrypto.hashHumanKey(testHuKey),
            shellKey = ByteArray(32)
        )

        val jsonContent = """{"token":"$testHuKey","version":1}"""
        assertTrue(deviceVault.verifyClawKeyJson(jsonContent))

        val invalidJson = """{"token":"hu-wrongkey00000000000000000000000000000000000000000000000000000000"}"""
        assertFalse(deviceVault.verifyClawKeyJson(invalidJson))
    }
}
