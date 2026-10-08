package com.clawstack.shellguard.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.crypto.EncryptedDeviceVault
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.data.local.ShellGuardDatabase
import com.clawstack.shellguard.data.local.entities.SecureNoteEntity
import com.clawstack.shellguard.data.local.entities.SshKeyEntity
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VaultBackupEngineTest {

    private lateinit var context: Context
    private lateinit var database: ShellGuardDatabase
    private lateinit var deviceVault: EncryptedDeviceVault
    private lateinit var backupEngine: VaultBackupEngine

    private val testHuKey = "hu-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private val testOwnerUuid = "owner-uuid-1234"
    private lateinit var testShellKey: ByteArray

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        database = ShellGuardDatabase.getInMemoryDatabase(context)

        deviceVault = EncryptedDeviceVault(context)
        deviceVault.clearSession()
        testShellKey = ShellCryptionEngine.deriveShellKey(testHuKey, testOwnerUuid)
        deviceVault.saveSession("token-test", "http://127.0.0.1:6464", testOwnerUuid, "tester", "hashedKey", testShellKey)

        backupEngine = VaultBackupEngine(database, deviceVault)
    }

    @After
    fun tearDown() {
        database.close()
        deviceVault.clearSession()
    }

    @Test
    fun testDetectBackupFormat() {
        val sgEncrypted = """{"format":"shellguard-vault-backup-v1","salt":"abc","iv":"def","payload":"ghi"}"""
        val sgPlain = """{"version":"1.0","ownerUuid":"123","pearls":[]}"""
        val bwJson = """{"encrypted":false,"items":[{"id":"1","type":1,"name":"Test"}]}"""
        val bwEncrypted = """{"encrypted":true,"items":[]}"""
        val unknown = "random garbage string"

        assertEquals(BackupFormatType.SHELLGUARD_ENCRYPTED, backupEngine.detectBackupFormat(sgEncrypted))
        assertEquals(BackupFormatType.SHELLGUARD_PLAIN, backupEngine.detectBackupFormat(sgPlain))
        assertEquals(BackupFormatType.BITWARDEN_JSON, backupEngine.detectBackupFormat(bwJson))
        assertEquals(BackupFormatType.BITWARDEN_ENCRYPTED, backupEngine.detectBackupFormat(bwEncrypted))
        assertEquals(BackupFormatType.UNKNOWN, backupEngine.detectBackupFormat(unknown))
    }

    @Test
    fun testExportAndDecryptWithActiveClawKey() = runTest {
        // 1. Insert seed data into Room
        val pearlId = UUID.randomUUID().toString()
        val encSecret = ShellCryptionEngine.encryptField("SuperPassword123!", testShellKey, ShellCryptionEngine.AadNamespace.pearlSecret(pearlId))
        database.vaultPearlDao().upsert(
            VaultPearlEntity(
                id = pearlId,
                ownerUuid = testOwnerUuid,
                title = "GitHub Personal",
                secret = encSecret,
                username = "octocat",
                url = "https://github.com",
                createdAt = "2026-10-04T12:00:00Z"
            )
        )

        val noteId = UUID.randomUUID().toString()
        val encNote = ShellCryptionEngine.encryptField("Secret Server Instructions", testShellKey, ShellCryptionEngine.AadNamespace.secureNoteContent(noteId))
        database.secureNoteDao().upsert(
            SecureNoteEntity(
                id = noteId,
                ownerUuid = testOwnerUuid,
                title = "Server Keys",
                content = encNote,
                createdAt = "2026-10-04T12:00:00Z"
            )
        )

        // 2. Export with ACTIVE_KEY
        val exportResult = backupEngine.exportVault(
            ownerUuid = testOwnerUuid,
            protectionMode = BackupProtectionMode.ACTIVE_KEY,
            activeClawKey = testHuKey
        ).getOrThrow()

        assertTrue(exportResult.isEncrypted)
        assertEquals(BackupProtectionMode.ACTIVE_KEY, exportResult.protectionMode)
        assertEquals(1, exportResult.pearlsCount)
        assertEquals(1, exportResult.notesCount)
        assertEquals(0, exportResult.sshKeysCount)
        assertTrue(exportResult.jsonString.contains("shellguard-vault-backup-v1"))
        assertTrue(exportResult.jsonString.contains("\"kdf\":\"hkdf\""))

        // 3. Decrypt with the active hu-key
        val decrypted = backupEngine.decryptBackupEnvelope(exportResult.jsonString, testHuKey).getOrThrow()
        
        val decryptedItems = decrypted.allItems()
        val pearlItems = decryptedItems.filter { it.type == "password" }
        val noteItems = decryptedItems.filter { it.type == "note" }
        
        assertEquals(1, pearlItems.size)
        assertEquals("GitHub Personal", pearlItems[0].title)
        assertEquals("SuperPassword123!", pearlItems[0].secret)
        assertEquals("octocat", pearlItems[0].username)

        assertEquals(1, noteItems.size)
        assertEquals("Server Keys", noteItems[0].title)
        assertEquals("Secret Server Instructions", noteItems[0].secret)
    }

    @Test
    fun testExportAndDecryptWithCustomPassphrase() = runTest {
        val pearlId = UUID.randomUUID().toString()
        val encSecret = ShellCryptionEngine.encryptField("PassphraseProtectedKey", testShellKey, ShellCryptionEngine.AadNamespace.pearlSecret(pearlId))
        database.vaultPearlDao().upsert(
            VaultPearlEntity(
                id = pearlId,
                ownerUuid = testOwnerUuid,
                title = "Custom Passphrase Vault",
                secret = encSecret,
                username = "user1",
                createdAt = "2026-10-04T12:00:00Z"
            )
        )

        val customPass = "MySuperSecretBackupPassword2026!"

        val exportResult = backupEngine.exportVault(
            ownerUuid = testOwnerUuid,
            protectionMode = BackupProtectionMode.CUSTOM_PASSPHRASE,
            customPassphrase = customPass
        ).getOrThrow()

        assertTrue(exportResult.isEncrypted)
        assertEquals(BackupProtectionMode.CUSTOM_PASSPHRASE, exportResult.protectionMode)
        assertTrue(exportResult.jsonString.contains("\"kdf\":\"pbkdf2\""))

        // Decrypt with correct passphrase
        val decrypted = backupEngine.decryptBackupEnvelope(exportResult.jsonString, customPass).getOrThrow()
        
        val pearlItems = decrypted.allItems().filter { it.type == "password" }
        assertEquals(1, pearlItems.size)
        assertEquals("PassphraseProtectedKey", pearlItems[0].secret)

        // Attempt decrypt with wrong passphrase fails
        val failResult = backupEngine.decryptBackupEnvelope(exportResult.jsonString, "WrongPassword!")
        assertTrue(failResult.isFailure)
    }

    @Test
    fun testExportPlaintext() = runTest {
        val pearlId = UUID.randomUUID().toString()
        val encSecret = ShellCryptionEngine.encryptField("PlainSecret", testShellKey, ShellCryptionEngine.AadNamespace.pearlSecret(pearlId))
        database.vaultPearlDao().upsert(
            VaultPearlEntity(
                id = pearlId,
                ownerUuid = testOwnerUuid,
                title = "Plain Item",
                secret = encSecret,
                createdAt = "2026-10-04T12:00:00Z"
            )
        )

        val exportResult = backupEngine.exportVault(
            ownerUuid = testOwnerUuid,
            protectionMode = BackupProtectionMode.PLAINTEXT
        ).getOrThrow()

        assertFalse(exportResult.isEncrypted)
        assertEquals(BackupProtectionMode.PLAINTEXT, exportResult.protectionMode)
        assertTrue(exportResult.jsonString.contains("\"items\":"))
        assertTrue(exportResult.jsonString.contains("PlainSecret"))
    }

    @Test
    fun testImportBitwardenJson() = runTest {
        val bwJson = """
            {
              "encrypted": false,
              "folders": [
                {"id": "folder-1", "name": "Work Logins"}
              ],
              "items": [
                {
                  "id": "bw-item-1",
                  "type": 1,
                  "name": "Bitwarden GitHub",
                  "notes": "Bitwarden note",
                  "favorite": true,
                  "folderId": "folder-1",
                  "login": {
                    "username": "bw-user",
                    "password": "bw-password-999",
                    "totp": "JBSWY3DPEHPK3PXP",
                    "uris": [{"uri": "https://github.com"}]
                  },
                  "fields": []
                },
                {
                  "id": "bw-item-2",
                  "type": 2,
                  "name": "Bitwarden Secure Note",
                  "notes": "Top secret note content",
                  "favorite": false,
                  "folderId": "folder-1",
                  "fields": []
                }
              ]
            }
        """.trimIndent()

        val importResult = backupEngine.importBitwardenJson(bwJson, testOwnerUuid).getOrThrow()
        assertEquals(1, importResult.pearlsCount)
        assertEquals(1, importResult.notesCount)

        // Verify inserted into Room
        val activePearls = database.vaultPearlDao().getAllActivePearls(testOwnerUuid)
        assertEquals(1, activePearls.size)
        assertEquals("Bitwarden GitHub", activePearls[0].title)
        assertEquals("bw-user", activePearls[0].username)
        assertEquals("Work Logins", activePearls[0].category)
        assertEquals("PENDING_SYNC", activePearls[0].syncState)

        // Decrypt password from Room
        val decryptedSecret = ShellCryptionEngine.decryptField(
            activePearls[0].secret,
            testShellKey,
            ShellCryptionEngine.AadNamespace.pearlSecret(activePearls[0].id)
        )
        assertEquals("bw-password-999", decryptedSecret)

        val activeNotes = database.secureNoteDao().getAllActiveNotes(testOwnerUuid)
        assertEquals(1, activeNotes.size)
        assertEquals("Bitwarden Secure Note", activeNotes[0].title)
        val decryptedNote = ShellCryptionEngine.decryptField(
            activeNotes[0].content,
            testShellKey,
            ShellCryptionEngine.AadNamespace.secureNoteContent(activeNotes[0].id)
        )
        assertEquals("Top secret note content", decryptedNote)
    }
}
