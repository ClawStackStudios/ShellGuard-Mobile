package com.clawstack.shellguard.remote

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.crypto.EncryptedDeviceVault
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.data.local.ShellGuardDatabase
import com.clawstack.shellguard.data.local.entities.SecureNoteEntity
import com.clawstack.shellguard.data.local.entities.SshKeyEntity
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
import com.clawstack.shellguard.data.repository.ConnectivityMonitor
import com.clawstack.shellguard.data.repository.SyncRepository
import com.clawstack.shellguard.data.repository.SyncStatus
import com.clawstack.shellguard.data.repository.VaultItemDomain
import com.clawstack.shellguard.domain.models.CustomField
import com.clawstack.shellguard.domain.models.CustomFieldType
import com.clawstack.shellguard.domain.models.PearlDetail
import com.clawstack.shellguard.domain.models.SecureNoteDetail
import com.clawstack.shellguard.domain.models.SshKeyDetail
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
        syncRepository.cancelScope()
        database.close()
        context.getSharedPreferences("shellguard_device_vault_test", Context.MODE_PRIVATE).edit().clear().commit()
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
        val localSynced = database.vaultPearlDao().getSyncedItemIds(testOwnerUuid)
        val obsoleteIds = localSynced - remoteActiveIds.toSet()
        database.vaultPearlDao().deleteBatch(testOwnerUuid, obsoleteIds)

        val remaining = database.vaultPearlDao().observeAll(testOwnerUuid).first()
        assertEquals(1, remaining.size)
        assertEquals("Keep Me", remaining[0].title)
    }

    @Test
    fun testSaveAndGetPearlDetailRoundTrip() = runBlocking {
        val pearl = PearlDetail(
            id = "pearl-detail-1",
            ownerUuid = testOwnerUuid,
            title = "GitHub Work",
            secret = "SuperSecretPassword123!",
            username = "octocat-work",
            url = "https://github.com",
            category = "Dev",
            notes = "Work production login",
            totpSecret = "JBSWY3DPEHPK3PXP",
            customFields = listOf(
                CustomField(id = "cf-1", label = "Pin", value = "4829", type = CustomFieldType.HIDDEN),
                CustomField(id = "cf-2", label = "Backup", value = "ABC-123", type = CustomFieldType.TEXT)
            ),
            tags = listOf("work", "git"),
            reprompt = true
        )

        val saveRes = syncRepository.savePearlDetail(pearl)
        assertTrue(saveRes.isSuccess)

        val loadRes = syncRepository.getPearlDetail("pearl-detail-1")
        assertTrue(loadRes.isSuccess)
        val loaded = loadRes.getOrThrow()

        assertEquals("GitHub Work", loaded.title)
        assertEquals("SuperSecretPassword123!", loaded.secret)
        assertEquals("octocat-work", loaded.username)
        assertEquals("https://github.com", loaded.url)
        assertEquals("Dev", loaded.category)
        assertEquals(2, loaded.customFields.size)
        assertEquals("Pin", loaded.customFields[0].label)
        assertEquals("4829", loaded.customFields[0].value)
        assertEquals(CustomFieldType.HIDDEN, loaded.customFields[0].type)
        assertEquals(2, loaded.tags.size)
        assertTrue(loaded.reprompt)
    }

    @Test
    fun testPasswordHistoryAutoAppendsOnEdit() = runBlocking {
        val initialPearl = PearlDetail(
            id = "pearl-history-test",
            ownerUuid = testOwnerUuid,
            title = "Service Account",
            secret = "InitialPassword_v1",
            username = "svc_deploy"
        )
        syncRepository.savePearlDetail(initialPearl)

        val updatedPearl = initialPearl.copy(secret = "NewPassword_v2")
        syncRepository.savePearlDetail(updatedPearl)

        val loaded = syncRepository.getPearlDetail("pearl-history-test").getOrThrow()
        assertEquals("NewPassword_v2", loaded.secret)
        assertEquals(1, loaded.passwordHistory.size)
        assertEquals("InitialPassword_v1", loaded.passwordHistory[0].password)
    }

    @Test
    fun testSaveAndGetNoteDetailRoundTrip() = runBlocking {
        val note = SecureNoteDetail(
            id = "note-detail-1",
            ownerUuid = testOwnerUuid,
            title = "Database Seed Keys",
            content = "Encrypted private recovery seed words...",
            category = "Infrastructure",
            customFields = listOf(
                CustomField(id = "cf-note", label = "Cluster", value = "us-east-1", type = CustomFieldType.TEXT)
            ),
            tags = listOf("seed", "infra"),
            reprompt = false
        )

        val saveRes = syncRepository.saveNoteDetail(note)
        assertTrue(saveRes.isSuccess)

        val loaded = syncRepository.getNoteDetail("note-detail-1").getOrThrow()
        assertEquals("Database Seed Keys", loaded.title)
        assertEquals("Encrypted private recovery seed words...", loaded.content)
        assertEquals("Infrastructure", loaded.category)
        assertEquals(1, loaded.customFields.size)
        assertEquals("Cluster", loaded.customFields[0].label)
        assertEquals(2, loaded.tags.size)
    }

    @Test
    fun testSaveAndGetSshKeyDetailRoundTrip() = runBlocking {
        val sshKey = SshKeyDetail(
            id = "ssh-detail-1",
            ownerUuid = testOwnerUuid,
            title = "Bastion Admin Key",
            keyValue = "-----BEGIN OPENSSH PRIVATE KEY-----\nMIIEowIBAAKCAQEA0...",
            username = "ubuntu",
            category = "Servers",
            customFields = listOf(
                CustomField(id = "cf-ssh", label = "Port", value = "2222", type = CustomFieldType.TEXT)
            ),
            tags = listOf("ssh", "bastion"),
            reprompt = true
        )

        val saveRes = syncRepository.saveSshKeyDetail(sshKey)
        assertTrue(saveRes.isSuccess)

        val loaded = syncRepository.getSshKeyDetail("ssh-detail-1").getOrThrow()
        assertEquals("Bastion Admin Key", loaded.title)
        assertEquals("-----BEGIN OPENSSH PRIVATE KEY-----\nMIIEowIBAAKCAQEA0...", loaded.keyValue)
        assertEquals("ubuntu", loaded.username)
        assertEquals("Servers", loaded.category)
        assertEquals(1, loaded.customFields.size)
        assertTrue(loaded.reprompt)
    }

    @Test
    fun testDeleteItemRemovesFromRoom() = runBlocking {
        val pearl = PearlDetail(
            id = "pearl-delete-test",
            ownerUuid = testOwnerUuid,
            title = "Temporary Token",
            secret = "temp_123"
        )
        syncRepository.savePearlDetail(pearl)
        assertNotNull(database.vaultPearlDao().getById(testOwnerUuid, "pearl-delete-test"))

        val delRes = syncRepository.deleteItem(VaultItemDomain.PASSWORD, "pearl-delete-test")
        assertTrue(delRes.isSuccess)

        assertNull(database.vaultPearlDao().getById(testOwnerUuid, "pearl-delete-test"))
    }

    @Test
    fun testGetPearlDetailWithUnencryptedEmptyArraysFromWebUi() = runBlocking {
        val shellKey = deviceVault.getInMemoryShellKey()!!
        val encSecret = ShellCryptionEngine.encryptField(
            "my-password",
            shellKey,
            ShellCryptionEngine.AadNamespace.pearlSecret("web-ui-pearl")
        )

        // Web UI creates pearls where password_history and custom_fields are stored as "[]"
        val pearlFromWeb = VaultPearlEntity(
            id = "web-ui-pearl",
            ownerUuid = testOwnerUuid,
            title = "Web Created Pearl",
            secret = encSecret,
            username = "webuser",
            url = "https://example.com",
            type = "password",
            category = "Personal",
            totpSecret = "",
            customFields = "[]",
            passwordHistory = "[]",
            tags = "[]",
            reprompt = false,
            syncState = "SYNCED",
            createdAt = "1720000000000",
            localUpdatedAt = 1720000000000L,
            remoteUpdatedAt = 1720000000000L
        )
        database.vaultPearlDao().upsert(pearlFromWeb)

        val result = syncRepository.getPearlDetail("web-ui-pearl")
        assertTrue("getPearlDetail should succeed for web UI items", result.isSuccess)

        val detail = result.getOrThrow()
        assertEquals("Web Created Pearl", detail.title)
        assertEquals("my-password", detail.secret)
        assertEquals("webuser", detail.username)
        assertTrue("customFields should be empty list", detail.customFields.isEmpty())
        assertTrue("passwordHistory should be empty list", detail.passwordHistory.isEmpty())
    }
}

