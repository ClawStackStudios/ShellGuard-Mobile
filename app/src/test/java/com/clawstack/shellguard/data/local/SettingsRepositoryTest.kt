package com.clawstack.shellguard.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.crypto.LockTimeout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        repository = SettingsRepositoryImpl(context)
        repository.clearAll()
    }

    @Test
    fun testDefaultSettings() = runTest {
        val settings = repository.settingsFlow.first()
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertFalse(settings.dynamicColors)
        assertTrue(settings.showFavicons)
        assertFalse(settings.compactView)
        assertEquals(LockTimeout.FIVE_MINUTES, settings.lockTimeout)
        assertFalse(settings.allowScreenCapture)
        assertEquals(30, settings.clipboardClearSeconds)
        assertEquals(15, settings.panicWipeCountdownSeconds)
        assertTrue(settings.autofillInlineChips)
        assertTrue(settings.syncOverCellular)
        assertTrue(settings.pullToRefreshEnabled)
    }

    @Test
    fun testUpdateThemeModeAndDynamicColors() = runTest {
        repository.setThemeMode(ThemeMode.DARK)
        repository.setDynamicColors(true)

        val updated = repository.settingsFlow.first()
        assertEquals(ThemeMode.DARK, updated.themeMode)
        assertTrue(updated.dynamicColors)

        repository.setThemeMode(ThemeMode.LIGHT)
        val lightState = repository.settingsFlow.first()
        assertEquals(ThemeMode.LIGHT, lightState.themeMode)
    }

    @Test
    fun testUpdateLockTimeout() = runTest {
        repository.setLockTimeout(LockTimeout.ONE_MINUTE)
        val updated = repository.settingsFlow.first()
        assertEquals(LockTimeout.ONE_MINUTE, updated.lockTimeout)
    }

    @Test
    fun testUpdatePanicWipeCountdownWithClamping() = runTest {
        repository.setPanicWipeCountdownSeconds(45)
        assertEquals(45, repository.settingsFlow.first().panicWipeCountdownSeconds)

        // Below minimum (5s) -> clamped to 5
        repository.setPanicWipeCountdownSeconds(2)
        assertEquals(5, repository.settingsFlow.first().panicWipeCountdownSeconds)

        // Above maximum (60s) -> clamped to 60
        repository.setPanicWipeCountdownSeconds(120)
        assertEquals(60, repository.settingsFlow.first().panicWipeCountdownSeconds)
    }

    @Test
    fun testToggles() = runTest {
        repository.setShowFavicons(false)
        repository.setCompactView(true)
        repository.setAllowScreenCapture(true)
        repository.setClipboardClearSeconds(60)
        repository.setAutofillInlineChips(false)
        repository.setSyncOverCellular(false)
        repository.setPullToRefreshEnabled(false)

        val updated = repository.settingsFlow.first()
        assertFalse(updated.showFavicons)
        assertTrue(updated.compactView)
        assertTrue(updated.allowScreenCapture)
        assertEquals(60, updated.clipboardClearSeconds)
        assertFalse(updated.autofillInlineChips)
        assertFalse(updated.syncOverCellular)
        assertFalse(updated.pullToRefreshEnabled)
    }
}
