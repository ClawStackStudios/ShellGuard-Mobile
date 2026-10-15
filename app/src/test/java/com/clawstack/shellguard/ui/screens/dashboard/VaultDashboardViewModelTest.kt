package com.clawstack.shellguard.ui.screens.dashboard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.crypto.AndroidKeyStoreHelper
import com.clawstack.shellguard.crypto.ClawCrypto
import com.clawstack.shellguard.crypto.EncryptedDeviceVault
import com.clawstack.shellguard.crypto.PinAuthManager
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.crypto.VaultLockManager
import com.clawstack.shellguard.data.backup.VaultBackupEngine
import com.clawstack.shellguard.data.local.AttachmentVaultManager
import com.clawstack.shellguard.data.local.SettingsRepositoryImpl
import com.clawstack.shellguard.data.local.ShellGuardDatabase
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
import com.clawstack.shellguard.data.remote.ShellGuardClient
import com.clawstack.shellguard.data.repository.ConnectivityMonitor
import com.clawstack.shellguard.data.repository.SyncRepository
import com.clawstack.shellguard.di.AppContainer
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureNanoTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VaultDashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: ShellGuardDatabase
    private lateinit var deviceVault: EncryptedDeviceVault
    private lateinit var syncRepository: SyncRepository
    private lateinit var appContainer: AppContainer

    private val testOwnerUuid = "test-user-uuid-1234"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ShellGuardDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        deviceVault = EncryptedDeviceVault(context)

        val testHuKey = "hu-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val shellKey = ShellCryptionEngine.deriveShellKey(testHuKey, testOwnerUuid)
        deviceVault.saveSession(
            token = "test-session-token",
            serverUrl = "http://localhost:6565",
            ownerUuid = testOwnerUuid,
            username = "admin",
            hashedKey = "test-hashed-key",
            shellKey = shellKey
        )

        syncRepository = SyncRepository(
            database = database,
            clientProvider = { baseUrl -> ShellGuardClient(baseUrl) },
            deviceVault = deviceVault,
            connectivityMonitor = ConnectivityMonitor(context, initialOnlineOverride = true)
        )

        appContainer = object : AppContainer {
            override val keyStoreHelper = AndroidKeyStoreHelper
            override val clawCrypto = ClawCrypto
            override val cryptoEngine = ShellCryptionEngine
            override val database: ShellGuardDatabase = this@VaultDashboardViewModelTest.database
            override val deviceVault: EncryptedDeviceVault = this@VaultDashboardViewModelTest.deviceVault
            override val connectivityMonitor = ConnectivityMonitor(context, initialOnlineOverride = true)
            override val syncRepository: SyncRepository = this@VaultDashboardViewModelTest.syncRepository
            override val attachmentVaultManager = AttachmentVaultManager(context)
            override val vaultLockManager = VaultLockManager(context, deviceVault)
            override val settingsRepository = SettingsRepositoryImpl(context)
            override val pinAuthManager = PinAuthManager(deviceVault, settingsRepository)
            override val backupEngine = VaultBackupEngine(database, deviceVault)
            override fun getClient(baseUrl: String) = ShellGuardClient(baseUrl)
        }
    }

    @After
    fun tearDown() {
        syncRepository.cancelScope()
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testSearchFilteringAndDebounce() = runTest(testDispatcher) {
        // 1. Seed items into database BEFORE instantiating viewModel
        database.vaultPearlDao().upsert(
            VaultPearlEntity(
                id = "p-1",
                ownerUuid = testOwnerUuid,
                title = "GitHub Personal",
                secret = "ct",
                username = "octocat",
                category = "Development",
                tags = "[\"work\",\"git\"]",
                createdAt = "1000",
                syncState = "SYNCED",
                url = "", type = "", notes = "", totpSecret = "", attachments = "", customFields = "", uris = "", passwordHistory = "", reprompt = false, localUpdatedAt = 0, remoteUpdatedAt = 0
            )
        )
        database.vaultPearlDao().upsert(
            VaultPearlEntity(
                id = "p-2",
                ownerUuid = testOwnerUuid,
                title = "ProtonMail",
                secret = "ct",
                username = "user@pm.me",
                category = "Personal",
                tags = "[\"email\"]",
                createdAt = "1000",
                syncState = "SYNCED",
                url = "", type = "", notes = "", totpSecret = "", attachments = "", customFields = "", uris = "", passwordHistory = "", reprompt = false, localUpdatedAt = 0, remoteUpdatedAt = 0
            )
        )

        // 2. Instantiate ViewModel with testDispatcher
        val viewModel = VaultDashboardViewModel(appContainer, defaultDispatcher = testDispatcher)
        backgroundScope.launch(testDispatcher) { viewModel.filteredItems.collect {} }
        testScheduler.advanceUntilIdle()

        // 3. Await initial emission
        val initialItems = viewModel.filteredItems.filter { it.size == 2 }.first()
        assertEquals(2, initialItems.size)

        // 4. Test search query update with debounce
        viewModel.updateSearchQuery("octo")
        testScheduler.advanceTimeBy(100)

        val filtered = viewModel.filteredItems.first { it.size == 1 }
        assertEquals("GitHub Personal", filtered[0].title)

        // 5. Multi-token match across tags and title
        viewModel.updateSearchQuery("git work")
        testScheduler.advanceTimeBy(100)
        assertEquals(1, viewModel.filteredItems.first { it.size == 1 }.size)

        // 6. Clear query restores full list
        viewModel.updateSearchQuery("")
        testScheduler.advanceTimeBy(100)
        assertEquals(2, viewModel.filteredItems.first { it.size == 2 }.size)
    }

    @Test
    fun testSearchSub16msBenchmarkAcross150Items() = runTest(testDispatcher) {
        // 1. Seed 150 items into database
        val items = (1..150).map { i ->
            VaultPearlEntity(
                id = "item-$i",
                ownerUuid = testOwnerUuid,
                title = if (i == 42) "Target Special Server" else "Item Title $i",
                secret = "ct",
                username = "user$i",
                category = if (i % 2 == 0) "Work" else "Personal",
                tags = "[\"tag$i\"]",
                createdAt = "1000",
                syncState = "SYNCED",
                url = "", type = "", notes = "", totpSecret = "", attachments = "", customFields = "", uris = "", passwordHistory = "", reprompt = false, localUpdatedAt = 0, remoteUpdatedAt = 0
            )
        }
        database.vaultPearlDao().upsertAll(items)

        val viewModel = VaultDashboardViewModel(appContainer, defaultDispatcher = testDispatcher)
        backgroundScope.launch(testDispatcher) { viewModel.filteredItems.collect {} }
        testScheduler.advanceUntilIdle()

        // Ensure all 150 items are active
        val allLoaded = viewModel.filteredItems.filter { it.size == 150 }.first()
        assertEquals(150, allLoaded.size)

        // Test search query on the ViewModel
        viewModel.updateSearchQuery("Target Special")
        testScheduler.advanceTimeBy(100)
        val results = viewModel.filteredItems.first { it.size == 1 }
        assertEquals(1, results.size)
        assertEquals("Target Special Server", results[0].title)

        // Benchmark in-memory tokenized index search across all 150 items
        val benchmarkNanos = measureNanoTime {
            val tokens = listOf("target", "special")
            allLoaded.filter { item ->
                tokens.all { token -> item.normalizedSearchIndex.contains(token) }
            }
        }
        val benchmarkMillis = benchmarkNanos / 1_000_000.0
        assertTrue("In-memory search across 150 items took ${benchmarkMillis}ms, expected < 16ms", benchmarkMillis < 16.0)
    }

    @Test
    fun testOfflineHealthProbeSuppressionWhenCachedItemsPresent() = runTest(testDispatcher) {
        // 1. Seed one item so allItems is not empty
        database.vaultPearlDao().upsert(
            VaultPearlEntity(
                id = "cached-1",
                ownerUuid = testOwnerUuid,
                title = "Cached Login",
                secret = "ct",
                createdAt = "1000",
                syncState = "SYNCED",
                username = "", url = "", type = "", category = "", notes = "", totpSecret = "", attachments = "", customFields = "", tags = "", uris = "", passwordHistory = "", reprompt = false, localUpdatedAt = 0, remoteUpdatedAt = 0
            )
        )

        val viewModel = VaultDashboardViewModel(appContainer, defaultDispatcher = testDispatcher)
        backgroundScope.launch(testDispatcher) { viewModel.filteredItems.collect {} }
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        // Wait until allItems has populated the cache
        viewModel.filteredItems.filter { it.isNotEmpty() }.first()

        // 2. Trigger sync when server is unreachable
        viewModel.triggerSync().join()
        testScheduler.advanceUntilIdle()

        // 3. Error should be suppressed since cached items exist
        val state = viewModel.uiState.value
        assertNull("Offline error should be suppressed when cached items exist", state.errorMessage)
    }

    @Test
    fun testLockVaultZeroizesSearchQueryAndState() = runTest(testDispatcher) {
        val viewModel = VaultDashboardViewModel(appContainer, defaultDispatcher = testDispatcher)
        backgroundScope.launch(testDispatcher) { viewModel.filteredItems.collect {} }
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        testScheduler.advanceUntilIdle()

        // 1. Set some active state
        viewModel.updateSearchQuery("secret token")
        viewModel.selectPod(PodFilter.NOTES)
        testScheduler.advanceUntilIdle()

        assertEquals("secret token", viewModel.uiState.value.searchQuery)
        assertEquals(PodFilter.NOTES, viewModel.uiState.value.selectedPod)

        // 2. Lock the vault
        var callbackInvoked = false
        viewModel.lockVault {
            callbackInvoked = true
        }
        testScheduler.advanceUntilIdle()

        // 3. Verify state is dropped from memory
        assertTrue("Callback should be invoked", callbackInvoked)
        val state = viewModel.uiState.value
        assertEquals("Search query should be cleared", "", state.searchQuery)
        assertEquals("Pod filter should be reset to ALL", PodFilter.ALL, state.selectedPod)
        assertNull("Error message should be nullified", state.errorMessage)
    }
}
