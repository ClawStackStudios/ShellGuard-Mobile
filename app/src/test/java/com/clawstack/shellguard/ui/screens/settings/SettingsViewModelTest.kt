package com.clawstack.shellguard.ui.screens.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.crypto.LockTimeout
import com.clawstack.shellguard.data.local.ThemeMode
import com.clawstack.shellguard.di.DefaultAppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var appContainer: DefaultAppContainer
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        appContainer = DefaultAppContainer(context)
        appContainer.settingsRepository.clearAll()
        viewModel = SettingsViewModel(appContainer)
    }

    @After
    fun tearDown() {
        viewModel.onCleared()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals(ThemeMode.SYSTEM, state.settings.themeMode)
        assertFalse(state.settings.dynamicColors)
        assertTrue(state.settings.showFavicons)
        assertEquals(LockTimeout.FIVE_MINUTES, state.settings.lockTimeout)
        assertFalse(state.settings.allowScreenCapture)
        assertEquals(15, state.settings.panicWipeCountdownSeconds)
    }

    @Test
    fun testUpdateThemeModeAndDynamicColors() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.updateThemeMode(ThemeMode.DARK).join()
        viewModel.updateDynamicColors(true).join()

        val updated = appContainer.settingsRepository.settingsFlow.first()
        assertEquals(ThemeMode.DARK, updated.themeMode)
        assertTrue(updated.dynamicColors)
    }

    @Test
    fun testUpdateLockTimeout() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.updateLockTimeout(LockTimeout.ONE_MINUTE).join()

        val updated = appContainer.settingsRepository.settingsFlow.first()
        assertEquals(LockTimeout.ONE_MINUTE, updated.lockTimeout)
        assertEquals(LockTimeout.ONE_MINUTE, appContainer.vaultLockManager.getLockTimeout())
    }

    @Test
    fun testUpdatePanicWipeCountdownClamped() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.updatePanicWipeCountdownSeconds(45).join()

        val updated = appContainer.settingsRepository.settingsFlow.first()
        assertEquals(45, updated.panicWipeCountdownSeconds)
    }

    @Test
    fun testExecutePanicPurgeWipesState() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        var completed = false
        viewModel.executePanicPurge {
            completed = true
        }.join()

        assertTrue(completed)
        assertFalse(appContainer.deviceVault.hasActiveSession())
    }
}
