package com.clawstack.shellguard.remote

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.crypto.EncryptedDeviceVault
import com.clawstack.shellguard.data.local.ShellGuardDatabase
import com.clawstack.shellguard.data.local.entities.SecureNoteEntity
import com.clawstack.shellguard.data.local.entities.SshKeyEntity
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
import com.clawstack.shellguard.data.repository.ConnectivityMonitor
import com.clawstack.shellguard.data.repository.SyncRepository
import com.clawstack.shellguard.data.repository.SyncStatus
import com.clawstack.shellguard.data.repository.VaultItemDomain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: ShellGuardDatabase
    private lateinit var deviceVault: EncryptedDeviceVault
    private lateinit var connectivityMonitor: ConnectivityMonitor
    private lateinit var syncRepository: SyncRepository

    private val testOwnerUuid = "test-user-uuid-1234"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ShellGuardDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        deviceVault = EncryptedDeviceVault(context)
        connectivityMonitor = ConnectivityMonitor(context)
        syncRepository = SyncRepository(
            database = database,
            clientProvider = { baseUrl -> com.clawstack.shellguard.data.remote.ShellGuardClient(baseUrl) },
            deviceVault = deviceVault,
            connectivityMonitor = connectivityMonitor
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testUnifiedItemsObservationCombinesAllDomains() = runBlocking {
        // Insert a Pearl
        val pearl = VaultPearlEntity(
            id = "pearl-1",
            ownerUuid = testOwnerUuid,
            title = "AWS Console",
            secret = "enc_secret",
            username = "cloudadmin",
            url = "https://aws.amazon.com",
            type = "password",
            category = "Cloud",
            reprompt = true,
            syncState = "SYNCED",
            createdAt = "1000",
            localUpdatedAt = 1000L,
            remoteUpdatedAt = 1000L
        )
        database.vaultPearlDao().upsert(pearl)

        // Insert a Secure Note
        val note = SecureNoteEntity(
            id = "note-1",
            ownerUuid = testOwnerUuid,
            title = "Server Recovery Codes",
            content = "enc_content",
            category = "Recovery",
            reprompt = true,
            syncState = "SYNCED",
            createdAt = "2000",
            localUpdatedAt = 2000L,
            remoteUpdatedAt = 2000L
        )
        database.secureNoteDao().upsert(note)

        // Insert an SSH Key
        val sshKey = SshKeyEntity(
            id = "ssh-1",
            ownerUuid = testOwnerUuid,
            title = "Production Bastion Key",
            keyValue = "enc_key",
            username = "root",
            category = "DevOps",
            reprompt = false,
            syncState = "SYNCED",
            createdAt = "3000",
            localUpdatedAt = 3000L,
            remoteUpdatedAt = 3000L
        )
        database.sshKeyDao().upsert(sshKey)

        // Collect unified stream
        val items = syncRepository.observeUnifiedItems(testOwnerUuid).first()

        assertEquals(3, items.size)
        // Should be ordered by most recent timestamp descending (ssh-1 at 3000L first)
        assertEquals("Production Bastion Key", items[0].title)
        assertEquals(VaultItemDomain.SSH_KEY, items[0].domain)

        assertEquals("Server Recovery Codes", items[1].title)
        assertEquals(VaultItemDomain.NOTE, items[1].domain)

        assertEquals("AWS Console", items[2].title)
        assertEquals(VaultItemDomain.PASSWORD, items[2].domain)
        assertTrue(items[2].reprompt)
    }

    @Test
    fun testPruningRemovesRemoteDeletedRecords() = runBlocking {
        val pearl1 = VaultPearlEntity(
            id = "item-keep",
            ownerUuid = testOwnerUuid,
            title = "Keep Me",
            secret = "enc_secret",
            type = "password",
            syncState = "SYNCED",
            createdAt = "100",
            localUpdatedAt = 100L,
            remoteUpdatedAt = 100L
        )
        val pearl2 = VaultPearlEntity(
            id = "item-deleted-remotely",
            ownerUuid = testOwnerUuid,
            title = "Delete Me",
            secret = "enc_secret",
            type = "password",
            syncState = "SYNCED",
            createdAt = "200",
            localUpdatedAt = 200L,
            remoteUpdatedAt = 200L
        )
        database.vaultPearlDao().upsertAll(listOf(pearl1, pearl2))

        val remoteActiveIds = listOf("item-keep")
        database.vaultPearlDao().pruneDeletedRemoteItems(testOwnerUuid, remoteActiveIds)

        val remaining = database.vaultPearlDao().observeAll(testOwnerUuid).first()
        assertEquals(1, remaining.size)
        assertEquals("Keep Me", remaining[0].title)
    }
}
