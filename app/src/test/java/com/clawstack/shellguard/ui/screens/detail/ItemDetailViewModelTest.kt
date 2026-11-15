package com.clawstack.shellguard.ui.screens.detail

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.di.DefaultAppContainer
import com.clawstack.shellguard.domain.models.PearlDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ItemDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var appContainer: DefaultAppContainer
    private val testOwnerUuid = "detail-test-owner-uuid"
    private lateinit var pearlId: String

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        appContainer = DefaultAppContainer(context)

        val testHuKey = "hu-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val shellKey = ShellCryptionEngine.deriveShellKey(testHuKey, testOwnerUuid)
        appContainer.deviceVault.saveSession(
            token = "detail-test-token",
            serverUrl = "http://localhost:6565",
            ownerUuid = testOwnerUuid,
            username = "lucas",
            hashedKey = "hash",
            shellKey = shellKey
        )

        val saved = appContainer.syncRepository.savePearl(
            PearlDetail(
                id = "",
                ownerUuid = testOwnerUuid,
                title = "Attachment Test Item",
                secret = "super-secret"
            )
        ).getOrThrow()
        pearlId = saved.id
    }

    @After
    fun tearDown() {
        appContainer.syncRepository.cancelScope()
        Dispatchers.resetMain()
    }

    @Test
    fun testDownloadAttachmentFailurePreservesSuccessState() = runTest(testDispatcher) {
        val viewModel = ItemDetailViewModel(appContainer, "PASSWORD", pearlId)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        val loaded = viewModel.uiState.first { it is ItemDetailUiState.Success }
        assertTrue(loaded is ItemDetailUiState.Success)

        val destFile = File(context.cacheDir, "unused_download_target.txt")
        var errorMessage: String? = null
        var succeeded = false

        viewModel.downloadAttachmentToUri(
            context = context,
            attachmentId = "missing-attachment-id",
            destinationUri = Uri.fromFile(destFile),
            onSuccess = { succeeded = true },
            onError = { errorMessage = it }
        ).join()

        assertFalse(succeeded)
        assertTrue("Expected non-null error callback", !errorMessage.isNullOrBlank())
        assertTrue(
            "Detail screen must remain in Success state on attachment failure",
            viewModel.uiState.value is ItemDetailUiState.Success
        )
    }

    @Test
    fun testDownloadAttachmentToUriWritesToDestinationWithoutCacheSpill() = runTest(testDispatcher) {
        val plaintext = """{"key":"value","format":"json"}""".toByteArray(Charsets.UTF_8)
        val staged = appContainer.syncRepository.stageAttachment(
            ownerUuid = testOwnerUuid,
            title = "Config",
            fileName = "config.json",
            mimeType = "application/json",
            category = "Config",
            rawPlaintextBytes = plaintext,
            parentId = pearlId,
            parentType = "pearl"
        ).getOrThrow()

        val decryptedCacheDir = File(context.cacheDir, "decrypted_attachments")
        decryptedCacheDir.deleteRecursively()

        val viewModel = ItemDetailViewModel(appContainer, "PASSWORD", pearlId)
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it is ItemDetailUiState.Success }

        val destFile = File(context.cacheDir, "exported_config.json")
        destFile.delete()
        var succeeded = false
        var errorMessage: String? = null

        viewModel.downloadAttachmentToUri(
            context = context,
            attachmentId = staged.id,
            destinationUri = Uri.fromFile(destFile),
            onSuccess = { succeeded = true },
            onError = { errorMessage = it }
        ).join()

        assertTrue("Expected download success, got error: $errorMessage", succeeded)
        assertEquals(String(plaintext, Charsets.UTF_8), destFile.readText(Charsets.UTF_8))
        assertFalse(
            "Plaintext must never be written to decrypted_attachments cache",
            decryptedCacheDir.exists() && (decryptedCacheDir.listFiles()?.isNotEmpty() == true)
        )
    }
}
