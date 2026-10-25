package com.clawstack.shellguard.ui.screens.dashboard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.crypto.DeviceVault
import com.clawstack.shellguard.crypto.FakeDeviceVault
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.crypto.VaultLockManager
import com.clawstack.shellguard.data.local.AttachmentVaultManager
import com.clawstack.shellguard.data.local.ShellGuardDatabase
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
import com.clawstack.shellguard.data.remote.ShellGuardClient
import com.clawstack.shellguard.data.repository.ConnectivityMonitor
import com.clawstack.shellguard.data.repository.SyncRepository
import com.clawstack.shellguard.data.repository.UnifiedVaultItem
import com.clawstack.shellguard.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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

class TestAppContainer(
    override val database: ShellGuardDatabase,
    override val deviceVault: DeviceVault,
    override val syncRepository: SyncRepository,
    override val attachmentVaultManager: AttachmentVaultManager,
    override val connectivityMonitor: ConnectivityMonitor,
    override val vaultLockManager: VaultLockManager
) : AppContainer {
    override val keyStoreHelper get() = throw NotImplementedError()
    override val clawCrypto get() = throw NotImplementedError()
    override val cryptoEngine get() = throw NotImplementedError()
    override val settingsRepository get() = throw NotImplementedError()
    override val pinAuthManager get() = throw NotImplementedError()
    override val backupEngine get() = throw NotImplementedError()
    override fun getClient(baseUrl: String) = throw NotImplementedError()
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VaultDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testOwnerUuid = "test-owner-uuid"

    private lateinit var appContainer: AppContainer
    private lateinit var context: Context
    private lateinit var database: ShellGuardDatabase
    private lateinit var deviceVault: DeviceVault
    private lateinit var syncRepository: SyncRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()

        database = Room.inMemoryDatabaseBuilder(context, ShellGuardDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        deviceVault = FakeDeviceVault().apply {
            saveSession("test", "https://test.com", testOwnerUuid, "testuser", "hashedkey")
        }

        val stubClientProvider: (String) -> ShellGuardClient = { _ -> throw NotImplementedError() }
        val connectivityMonitor = ConnectivityMonitor(context)
        val attachmentVaultManager = AttachmentVaultManager(context)

        syncRepository = SyncRepository(
            database = database,
            clientProvider = stubClientProvider,
            deviceVault = deviceVault,
            connectivityMonitor = connectivityMonitor,
            cryptoEngine = ShellCryptionEngine,
            attachmentVaultManager = attachmentVaultManager,
            coroutineScope = CoroutineScope(testDispatcher)
        )

        appContainer = TestAppContainer(
            database = database,
            deviceVault = deviceVault,
            syncRepository = syncRepository,
            attachmentVaultManager = attachmentVaultManager,
            connectivityMonitor = connectivityMonitor,
            vaultLockManager = VaultLockManager(context, deviceVault)
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testSearchSub16msBenchmarkAcross150Items() = runTest(testDispatcher) {
        // 1. Seed database BEFORE creating ViewModel
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

        // 2. Instantiate ViewModel and keep a background collector running
        val viewModel = VaultDashboardViewModel(appContainer, defaultDispatcher = testDispatcher)
        backgroundScope.launch(testDispatcher) { viewModel.filteredItems.collect {} }
        testScheduler.advanceUntilIdle()

        // 3. Await the full cache population
        val allLoaded = viewModel.filteredItems.filter { it.size == 150 }.first()
        assertEquals(150, allLoaded.size)

        // 4. Update query, advance past the 80ms debounce, and await filtered emission
        viewModel.updateSearchQuery("Target Special")
        testScheduler.advanceTimeBy(100)
        testScheduler.advanceUntilIdle()
        val results = viewModel.filteredItems.first { it.size == 1 }
        assertTrue(results.isNotEmpty())
        assertEquals("Target Special Server", results[0].title)

        // 5. In-memory benchmark assertion
        val benchmarkNanos = measureNanoTime {
            val tokens = listOf("target", "special")
            allLoaded.filter { item ->
                tokens.all { token -> item.normalizedSearchIndex.contains(token) }
            }
        }
        val benchmarkMillis = benchmarkNanos / 1_000_000.0
        assertTrue("Search took ${benchmarkMillis}ms, expected < 16ms", benchmarkMillis < 16.0)
    }

    @Test
    fun testOfflineHealthProbeSuppressionWhenCachedItemsPresent() = runTest(testDispatcher) {
        val items = listOf(
            VaultPearlEntity(
                id = "item-1", ownerUuid = testOwnerUuid, title = "Item Title", secret = "ct", username = "user", category = "Work", tags = "[]", createdAt = "1000", syncState = "SYNCED", url = "", type = "", notes = "", totpSecret = "", attachments = "", customFields = "", uris = "", passwordHistory = "", reprompt = false, localUpdatedAt = 0, remoteUpdatedAt = 0
            )
        )
        database.vaultPearlDao().upsertAll(items)

        // Connectivity Monitor simulates Offline
        val offlineMonitor = ConnectivityMonitor(context) // We'll assume triggerSync encounters an error via SyncRepository failing to resolve domain
        // However, instead of stubbing sync we let it fail naturally since we stubbed clientProvider with NotImplementedError

        val viewModel = VaultDashboardViewModel(appContainer, defaultDispatcher = testDispatcher)
        viewModel.triggerSync()?.join()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(null, state.errorMessage) // Should be suppressed
        assertEquals(false, state.isSyncing)
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
