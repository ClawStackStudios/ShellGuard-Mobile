package com.clawstack.shellguard.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.data.local.entities.AgentKeyEntity
import com.clawstack.shellguard.data.local.entities.AuditLogEntity
import com.clawstack.shellguard.data.local.entities.SecureAttachmentEntity
import com.clawstack.shellguard.data.local.entities.SecureNoteEntity
import com.clawstack.shellguard.data.local.entities.SshKeyEntity
import com.clawstack.shellguard.data.local.entities.SyncMetadataEntity
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
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

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseTest {

    private lateinit var db: ShellGuardDatabase
    private val ownerUuid = "owner_test_12345"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = ShellGuardDatabase.getInMemoryDatabase(context)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testVaultPearlEntityCrud() = runBlocking {
        val dao = db.vaultPearlDao()
        val pearl = VaultPearlEntity(
            id = "pearl_1",
            ownerUuid = ownerUuid,
            title = "Proton Mail Login",
            secret = "{\"v\":1,\"alg\":\"AES-GCM-256\",\"ct\":\"enc_secret\"}",
            username = "admin@clawstack.org",
            url = "https://mail.proton.me",
            createdAt = "2026-09-25T00:00:00Z"
        )

        dao.upsert(pearl)

        val retrieved = dao.getById(ownerUuid, "pearl_1")
        assertNotNull(retrieved)
        assertEquals("Proton Mail Login", retrieved?.title)
        assertEquals("admin@clawstack.org", retrieved?.username)

        val searchResults = dao.search(ownerUuid, "proton").first()
        assertEquals(1, searchResults.size)
        assertEquals("pearl_1", searchResults[0].id)

        dao.deleteById(ownerUuid, "pearl_1")
        val deleted = dao.getById(ownerUuid, "pearl_1")
        assertNull(deleted)
    }

    @Test
    fun testSecureNoteEntityCrud() = runBlocking {
        val dao = db.secureNoteDao()
        val note = SecureNoteEntity(
            id = "note_1",
            ownerUuid = ownerUuid,
            title = "Recovery Seed Phrase",
            content = "{\"v\":1,\"alg\":\"AES-GCM-256\",\"ct\":\"enc_content\"}",
            createdAt = "2026-09-25T00:00:00Z"
        )

        dao.upsert(note)

        val notes = dao.observeAll(ownerUuid).first()
        assertEquals(1, notes.size)
        assertEquals("Recovery Seed Phrase", notes[0].title)

        dao.deleteById(ownerUuid, "note_1")
        val emptyNotes = dao.observeAll(ownerUuid).first()
        assertTrue(emptyNotes.isEmpty())
    }

    @Test
    fun testSshKeyEntityCrud() = runBlocking {
        val dao = db.sshKeyDao()
        val sshKey = SshKeyEntity(
            id = "ssh_1",
            ownerUuid = ownerUuid,
            title = "Production Bastion Key",
            keyValue = "{\"v\":1,\"alg\":\"AES-GCM-256\",\"ct\":\"enc_key\"}",
            username = "root",
            createdAt = "2026-09-25T00:00:00Z"
        )

        dao.upsert(sshKey)

        val keys = dao.observeAll(ownerUuid).first()
        assertEquals(1, keys.size)
        assertEquals("Production Bastion Key", keys[0].title)
    }

    @Test
    fun testSecureAttachmentMetadataCrud() = runBlocking {
        val dao = db.secureAttachmentDao()
        val attachment = SecureAttachmentEntity(
            id = "att_1",
            ownerUuid = ownerUuid,
            title = "Passport Scan",
            fileName = "passport.enc",
            sizeBytes = 2048576L,
            mimeType = "application/pdf",
            localFilePath = "/data/user/0/com.clawstack.shellguard/files/vault_attachments/att_1.enc",
            createdAt = "2026-09-25T00:00:00Z"
        )

        dao.upsert(attachment)

        val retrieved = dao.getById(ownerUuid, "att_1")
        assertNotNull(retrieved)
        assertEquals(2048576L, retrieved?.sizeBytes)
        assertEquals("passport.enc", retrieved?.fileName)
    }

    @Test
    fun testSyncMetadataAndAuditLog() = runBlocking {
        val syncDao = db.syncMetadataDao()
        val auditDao = db.auditLogDao()

        val meta = SyncMetadataEntity(ownerUuid = ownerUuid, lastSyncTimestamp = 1000L)
        syncDao.upsert(meta)

        syncDao.recordSuccessfulSync(ownerUuid, 2000L)
        val updatedMeta = syncDao.getMetadata(ownerUuid)
        assertEquals(2000L, updatedMeta?.lastSyncTimestamp)

        val logEntry = AuditLogEntity(
            id = "audit_1",
            eventType = "LOGIN",
            actorType = "HUMAN",
            details = "Successful biometric authentication"
        )
        auditDao.insert(logEntry)

        val logs = auditDao.observeAll().first()
        assertEquals(1, logs.size)
        assertEquals("LOGIN", logs[0].eventType)
    }

    @Test
    fun testAgentKeyRevocation() = runBlocking {
        val dao = db.agentKeyDao()
        val agentKey = AgentKeyEntity(
            id = "agent_1",
            ownerUuid = ownerUuid,
            label = "ClawCoder Agent Key",
            createdAt = "2026-09-25T00:00:00Z"
        )

        dao.upsert(agentKey)
        val activeKeys = dao.observeActiveKeys(ownerUuid).first()
        assertEquals(1, activeKeys.size)

        dao.revokeKey(ownerUuid, "agent_1")
        val keysAfterRevoke = dao.observeActiveKeys(ownerUuid).first()
        assertTrue(keysAfterRevoke.isEmpty())
    }
}
