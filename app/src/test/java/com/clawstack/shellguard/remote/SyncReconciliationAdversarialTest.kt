package com.clawstack.shellguard.remote

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.crypto.EncryptedDeviceVault
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.data.local.ShellGuardDatabase
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
import com.clawstack.shellguard.data.remote.ShellGuardClient
import com.clawstack.shellguard.data.remote.models.CreateVaultItemRequest
import com.clawstack.shellguard.data.remote.models.PearlDto
import com.clawstack.shellguard.data.remote.models.SecureNoteDto
import com.clawstack.shellguard.data.remote.models.SshKeyDto
import com.clawstack.shellguard.data.repository.ConnectivityMonitor
import com.clawstack.shellguard.data.repository.SyncRepository
import com.clawstack.shellguard.data.repository.SyncStatus
import com.clawstack.shellguard.domain.models.PearlDetail
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class SyncReconciliationAdversarialTest {

    private lateinit var context: Context
    private lateinit var database: ShellGuardDatabase
    private lateinit var deviceVault: EncryptedDeviceVault
    private lateinit var connectivityMonitor: ConnectivityMonitor
    private lateinit var fakeClient: FakeShellGuardClient
    private lateinit var syncRepository: SyncRepository

    private val testOwnerUuid = "adversary-test-owner-uuid"
    private val testHuKey = "hu-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private lateinit var shellKey: ByteArray

    open class FakeShellGuardClient : ShellGuardClient("http://fake-server:6565") {
        var healthResult: Result<Boolean> = Result.success(true)
        var remotePearls: MutableList<PearlDto> = mutableListOf()
        var remoteNotes: MutableList<SecureNoteDto> = mutableListOf()
        var remoteKeys: MutableList<SshKeyDto> = mutableListOf()

        var deletePearlResult: Result<Boolean> = Result.success(true)
        var createPearlResult: Result<PearlDto>? = null
        var updatePearlResult: Result<PearlDto>? = null
        val deletedPearlIds = mutableListOf<String>()
        val updatedPearlRequests = mutableMapOf<String, CreateVaultItemRequest>()
        val createdPearlRequests = mutableListOf<CreateVaultItemRequest>()

        var createPearlCustomResponse: ((CreateVaultItemRequest) -> PearlDto)? = null

        override suspend fun getHealth(): Result<Boolean> = healthResult

        override suspend fun fetchVault(sessionToken: String): Result<List<PearlDto>> =
            Result.success(remotePearls.toList())

        override suspend fun fetchNotes(sessionToken: String): Result<List<SecureNoteDto>> =
            Result.success(remoteNotes.toList())

        override suspend fun fetchKeys(sessionToken: String): Result<List<SshKeyDto>> =
            Result.success(remoteKeys.toList())

        override suspend fun deleteVaultItem(sessionToken: String, id: String): Result<Boolean> {
            deletedPearlIds.add(id)
            return deletePearlResult
        }

        override suspend fun createVaultItem(sessionToken: String, request: CreateVaultItemRequest): Result<PearlDto> {
            createdPearlRequests.add(request)
            createPearlResult?.let { return it }
            val custom = createPearlCustomResponse?.invoke(request)
            val dto = custom ?: PearlDto(
                id = request.id ?: "generated-id",
                owner_uuid = "owner",
                title = request.title,
                secret = request.secret,
                type = request.type
            )
            remotePearls.removeAll { it.id == dto.id }
            remotePearls.add(dto)
            return Result.success(dto)
        }

        override suspend fun updateVaultItem(sessionToken: String, id: String, request: CreateVaultItemRequest): Result<PearlDto> {
            updatedPearlRequests[id] = request
            updatePearlResult?.let { return it }
            val dto = PearlDto(
                id = id,
                owner_uuid = "owner",
                title = request.title,
                secret = request.secret,
                type = request.type
            )
            remotePearls.removeAll { it.id == id }
            remotePearls.add(dto)
            return Result.success(dto)
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ShellGuardDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        deviceVault = EncryptedDeviceVault(context)
        shellKey = ShellCryptionEngine.deriveShellKey(testHuKey, testOwnerUuid)
        deviceVault.saveSession(
            token = "session-token-xyz",
            serverUrl = "http://fake-server:6565",
            ownerUuid = testOwnerUuid,
            username = "lucas",
            hashedKey = "hash-xyz",
            shellKey = shellKey
        )

        connectivityMonitor = ConnectivityMonitor(context, initialOnlineOverride = true)
        fakeClient = FakeShellGuardClient()

        syncRepository = SyncRepository(
            database = database,
            clientProvider = { fakeClient },
            deviceVault = deviceVault,
            connectivityMonitor = connectivityMonitor,
            cryptoEngine = ShellCryptionEngine
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Finding 1.1: Zombie Resurrection Attack Vector
     * When remote delete fails (network timeout or server 500), local tombstone MUST NOT be deleted,
     * and subsequent downstream pull MUST NOT resurrect the secret.
     */
    @Test
    fun testZombieResurrectionPreventedWhenRemoteDeleteFails() = runBlocking {
        val pearlId = "pearl-zombie-target"
        val encryptedSecret = ShellCryptionEngine.encryptField(
            "SecretToDie",
            shellKey,
            ShellCryptionEngine.AadNamespace.pearlSecret(pearlId)
        )

        // Local item marked as PENDING_DELETE
        val tombstone = VaultPearlEntity(
            id = pearlId,
            ownerUuid = testOwnerUuid,
            title = "Secret Target",
            secret = encryptedSecret,
            type = "password",
            syncState = "PENDING_DELETE",
            createdAt = "100",
            localUpdatedAt = 150L,
            remoteUpdatedAt = 100L
        )
        database.vaultPearlDao().upsert(tombstone)

        // Server still has the pearl and remote delete fails
        fakeClient.remotePearls.add(
            PearlDto(
                id = pearlId,
                owner_uuid = testOwnerUuid,
                title = "Secret Target",
                secret = encryptedSecret,
                type = "password"
            )
        )
        fakeClient.deletePearlResult = Result.failure(Exception("500 Server Internal Error"))

        // Run sync
        val syncResult = syncRepository.syncAll(testOwnerUuid)
        assertTrue(syncResult.isSuccess)

        // Verify: Tombstone was NOT deleted locally
        val entityInDb = database.vaultPearlDao().getById(testOwnerUuid, pearlId)
        assertNotNull(entityInDb)
        assertEquals("PENDING_DELETE", entityInDb?.syncState)

        // Verify: Downstream pull did not resurrect it to user-facing flow
        val visiblePearls = database.vaultPearlDao().observeAll(testOwnerUuid).first()
        assertTrue("Item should remain hidden by tombstone filter", visiblePearls.isEmpty())
    }

    /**
     * Finding 2.1: Conflict-Aware Downstream Pull
     * Downstream delta pull must NEVER overwrite an item with local PENDING_SYNC edits
     * even if stale remote versions arrive concurrently.
     */
    @Test
    fun testConflictAwarePullDoesNotClobberLocalPendingSyncEdits() = runBlocking {
        val pearlId = "pearl-conflict-item"
        val localSecret = ShellCryptionEngine.encryptField(
            "FreshOfflineSecret",
            shellKey,
            ShellCryptionEngine.AadNamespace.pearlSecret(pearlId)
        )
        val staleRemoteSecret = ShellCryptionEngine.encryptField(
            "StaleRemoteSecret",
            shellKey,
            ShellCryptionEngine.AadNamespace.pearlSecret(pearlId)
        )

        // Local item has pending edits
        val localEntity = VaultPearlEntity(
            id = pearlId,
            ownerUuid = testOwnerUuid,
            title = "Local Fresh Title",
            secret = localSecret,
            type = "password",
            syncState = "PENDING_SYNC",
            createdAt = "100",
            localUpdatedAt = 200L,
            remoteUpdatedAt = 100L
        )
        database.vaultPearlDao().upsert(localEntity)

        // Upstream push fails so item stays PENDING_SYNC
        fakeClient.updatePearlResult = Result.failure(Exception("Upstream push rejected"))

        // Server returns stale remote version
        fakeClient.remotePearls.add(
            PearlDto(
                id = pearlId,
                owner_uuid = testOwnerUuid,
                title = "Stale Remote Title",
                secret = staleRemoteSecret,
                type = "password"
            )
        )

        // Run syncAll
        syncRepository.syncAll(testOwnerUuid)

        // Local version must win and not be clobbered
        val retained = database.vaultPearlDao().getById(testOwnerUuid, pearlId)
        assertNotNull(retained)
        assertEquals("Local Fresh Title", retained?.title)
        assertEquals(localSecret, retained?.secret)
        assertEquals("PENDING_SYNC", retained?.syncState)
    }

    /**
     * Finding 3.1 & 3.2: Chunked Batch Pruning & Empty Remote Vault Pruning
     * Pruning when remote vault is empty must purge synced local items while preserving PENDING_SYNC.
     */
    @Test
    fun testPruningHandlesEmptyRemoteVaultWithoutPurgingPendingSync() = runBlocking {
        val syncedPearl = VaultPearlEntity(
            id = "synced-pearl-1",
            ownerUuid = testOwnerUuid,
            title = "Synced Item",
            secret = "enc",
            type = "password",
            syncState = "SYNCED",
            createdAt = "100",
            localUpdatedAt = 100L,
            remoteUpdatedAt = 100L
        )
        val pendingPearl = VaultPearlEntity(
            id = "pending-pearl-2",
            ownerUuid = testOwnerUuid,
            title = "Pending Offline Item",
            secret = "enc",
            type = "password",
            syncState = "PENDING_SYNC",
            createdAt = "200",
            localUpdatedAt = 200L,
            remoteUpdatedAt = 0L
        )
        database.vaultPearlDao().upsertAll(listOf(syncedPearl, pendingPearl))

        // Pending pearl cannot be pushed to remote yet
        fakeClient.createPearlResult = Result.failure(Exception("Cannot push offline item"))

        // Remote vault is completely empty (remotePearls is empty)
        fakeClient.remotePearls.clear()

        syncRepository.syncAll(testOwnerUuid)

        // Synced pearl should be pruned
        assertNull(database.vaultPearlDao().getById(testOwnerUuid, "synced-pearl-1"))
        // Pending sync pearl MUST be preserved!
        assertNotNull(database.vaultPearlDao().getById(testOwnerUuid, "pending-pearl-2"))
        assertEquals("PENDING_SYNC", database.vaultPearlDao().getById(testOwnerUuid, "pending-pearl-2")?.syncState)
    }

    /**
     * Finding 4.3 & Spectre Probe 3: Fail-Closed Decryption & No Plaintext Double-Ciphertext Fallback
     */
    @Test
    fun testFailClosedDecryptionPreventsDoubleCiphertextDestruction() = runBlocking {
        val pearlId = "corrupted-cipher-pearl"
        val corruptedPayload = "{\"v\":1,\"iv\":\"fake\",\"ct\":\"tampered\",\"tag\":\"bad\"}"

        val entity = VaultPearlEntity(
            id = pearlId,
            ownerUuid = testOwnerUuid,
            title = "Corrupted Pearl",
            secret = corruptedPayload,
            type = "password",
            syncState = "SYNCED",
            createdAt = "100",
            localUpdatedAt = 100L,
            remoteUpdatedAt = 100L
        )
        database.vaultPearlDao().upsert(entity)

        // Calling getPearlDetail MUST fail-closed (Result.failure)
        val result = syncRepository.getPearlDetail(pearlId)
        assertTrue("Decryption of tampered ciphertext must fail", result.isFailure)
    }

    /**
     * Finding 4.1: Server Health Probe Failure Blocks ONLINE_SYNCED Status
     */
    @Test
    fun testServerHealthCheckFailureBlocksOnlineStatus() = runBlocking {
        fakeClient.healthResult = Result.failure(Exception("Host unreachable"))

        val result = syncRepository.syncAll(testOwnerUuid)
        assertTrue("Sync should fail when health probe fails", result.isFailure)
        assertEquals(SyncStatus.OFFLINE_READ_ONLY, syncRepository.syncStatus.value)
    }

    /**
     * Spectre Probe 2: Server ID Re-Keying Updates Server With Matching Ciphertext
     */
    @Test
    fun testServerIdReKeyingReUpdatesServerWithConsistentCiphertext() = runBlocking {
        val clientTempId = "temp-client-uuid"
        val serverAssignedId = "srv-assigned-uuid-999"

        fakeClient.createPearlCustomResponse = { request ->
            PearlDto(
                id = serverAssignedId, // Server assigns different ID
                owner_uuid = testOwnerUuid,
                title = request.title,
                secret = request.secret,
                type = request.type
            )
        }

        val pearl = PearlDetail(
            id = clientTempId,
            ownerUuid = testOwnerUuid,
            title = "Proton Mail",
            secret = "ProtonPassword456!",
            username = "lucas@proton.me"
        )

        val saveRes = syncRepository.savePearlDetail(pearl)
        assertTrue(saveRes.isSuccess)

        // 1. Temporary ID deleted from Room
        assertNull(database.vaultPearlDao().getById(testOwnerUuid, clientTempId))

        // 2. Server ID present in Room
        val savedEntity = database.vaultPearlDao().getById(testOwnerUuid, serverAssignedId)
        assertNotNull(savedEntity)

        // 3. Saved entity can be cleanly decrypted with serverAssignedId AAD
        val decryptedSecret = ShellCryptionEngine.decryptField(
            savedEntity!!.secret,
            shellKey,
            ShellCryptionEngine.AadNamespace.pearlSecret(serverAssignedId)
        )
        assertEquals("ProtonPassword456!", decryptedSecret)

        // 4. Server was updated with the re-encrypted ciphertext under serverAssignedId
        assertTrue(fakeClient.updatedPearlRequests.containsKey(serverAssignedId))
        val serverReq = fakeClient.updatedPearlRequests[serverAssignedId]
        val serverDecrypted = ShellCryptionEngine.decryptField(
            serverReq!!.secret,
            shellKey,
            ShellCryptionEngine.AadNamespace.pearlSecret(serverAssignedId)
        )
        assertEquals("ProtonPassword456!", serverDecrypted)
    }
}
