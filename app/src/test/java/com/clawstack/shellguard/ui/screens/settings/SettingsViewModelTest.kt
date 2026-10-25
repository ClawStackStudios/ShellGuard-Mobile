package com.clawstack.shellguard.ui.screens.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.crypto.DeviceVault
import com.clawstack.shellguard.crypto.FakeDeviceVault
import com.clawstack.shellguard.crypto.LockTimeout
import com.clawstack.shellguard.data.local.ThemeMode
import com.clawstack.shellguard.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

    private class FakeSettingsAppContainer(
        private val context: Context,
        override val deviceVault: DeviceVault = FakeDeviceVault()
    ) : AppContainer {
        override val keyStoreHelper get() = com.clawstack.shellguard.crypto.AndroidKeyStoreHelper
        override val clawCrypto get() = com.clawstack.shellguard.crypto.ClawCrypto
        override val cryptoEngine get() = com.clawstack.shellguard.crypto.ShellCryptionEngine
        override val database by lazy { com.clawstack.shellguard.data.local.ShellGuardDatabase.getInstance(context) }
        override val connectivityMonitor by lazy { com.clawstack.shellguard.data.repository.ConnectivityMonitor(context) }
        override val vaultLockManager by lazy { com.clawstack.shellguard.crypto.VaultLockManager(context, deviceVault) }
        override val settingsRepository by lazy { com.clawstack.shellguard.data.local.SettingsRepositoryImpl(context) }
        override val pinAuthManager by lazy { com.clawstack.shellguard.crypto.PinAuthManager(deviceVault, settingsRepository) }
        override val backupEngine by lazy { com.clawstack.shellguard.data.backup.VaultBackupEngine(database, deviceVault) }
        override val attachmentVaultManager by lazy { com.clawstack.shellguard.data.local.AttachmentVaultManager(context) }
        override val syncRepository by lazy {
            com.clawstack.shellguard.data.repository.SyncRepository(
                database = database,
                clientProvider = { baseUrl -> getClient(baseUrl) },
                deviceVault = deviceVault,
                connectivityMonitor = connectivityMonitor,
                attachmentVaultManager = attachmentVaultManager
            )
        }
        override fun getClient(baseUrl: String) = com.clawstack.shellguard.data.remote.ShellGuardClient(
            if (baseUrl.isBlank()) "http://localhost:6565" else baseUrl
        )
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var appContainer: AppContainer
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        appContainer = FakeSettingsAppContainer(context)
        runBlocking {
            appContainer.settingsRepository.clearAll()
        }
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
    fun testUpdateAllowScreenCapture() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.updateAllowScreenCapture(true).join()

        val updated = appContainer.settingsRepository.settingsFlow.first()
        assertTrue(updated.allowScreenCapture)
    }

    @Test
    fun testUpdateClipboardClearSeconds() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.updateClipboardClearSeconds(60).join()

        val updated = appContainer.settingsRepository.settingsFlow.first()
        assertEquals(60, updated.clipboardClearSeconds)
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

    @Test
    fun testUpdateShowFaviconsAndCompactView() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.updateShowFavicons(false).join()
        viewModel.updateCompactView(true).join()

        val updated = appContainer.settingsRepository.settingsFlow.first()
        assertFalse(updated.showFavicons)
        assertTrue(updated.compactView)
    }

    @Test
    fun testUpdateSyncOverCellularAndPullToRefresh() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.updateSyncOverCellular(false).join()
        viewModel.updatePullToRefreshEnabled(false).join()

        val updated = appContainer.settingsRepository.settingsFlow.first()
        assertFalse(updated.syncOverCellular)
        assertFalse(updated.pullToRefreshEnabled)
    }

    @Test
    fun testTriggerManualSyncWithNoActiveSession() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        // When no active session is authenticated, triggerManualSync outputs error
        viewModel.triggerManualSync().join()

        val state = viewModel.uiState.filter { it.errorMessage != null }.first()
        assertEquals("No active session to sync.", state.errorMessage)

        viewModel.clearMessages()
        val clearedState = viewModel.uiState.filter { it.errorMessage == null }.first()
        assertEquals(null, clearedState.errorMessage)
    }

    @Test
    fun testUpdateAutofillInlineChips() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.updateAutofillInlineChips(false).join()

        val updated = appContainer.settingsRepository.settingsFlow.first()
        assertFalse(updated.autofillInlineChips)
    }

    @Test
    fun testExportVaultBackupWithoutSessionErrors() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        var result: Result<com.clawstack.shellguard.data.backup.ExportResult>? = null
        viewModel.exportVaultBackup(com.clawstack.shellguard.data.backup.BackupProtectionMode.ACTIVE_KEY) {
            result = it
        }.join()

        assertTrue(result?.isFailure == true)
        assertEquals("No active session to export.", viewModel.uiState.value.errorMessage)
    }
}
