package com.clawstack.shellguard.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clawstack.shellguard.data.repository.SyncStatus
import com.clawstack.shellguard.data.repository.VaultItemDomain
import com.clawstack.shellguard.di.AppContainer
import com.clawstack.shellguard.domain.models.CustomField
import com.clawstack.shellguard.domain.models.IdentityDetail
import com.clawstack.shellguard.domain.models.PasswordHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ItemDetailUiState {
    object Loading : ItemDetailUiState

    data class Success(
        val domain: VaultItemDomain,
        val id: String,
        val title: String,
        val category: String? = null,
        val secret: String = "",
        val username: String? = null,
        val url: String? = null,
        val notes: String? = null,
        val totpSecret: String? = null,
        val customFields: List<CustomField> = emptyList(),
        val tags: List<String> = emptyList(),
        val passwordHistory: List<PasswordHistoryEntry> = emptyList(),
        val uris: List<String> = emptyList(),
        val attachments: List<AttachmentItemDetail> = emptyList(),
        val identityDetail: IdentityDetail? = null,
        val reprompt: Boolean = false,
        val isOffline: Boolean = false,
        val isSecretRevealed: Boolean = false
    ) : ItemDetailUiState

    data class Error(val message: String) : ItemDetailUiState
    object Deleted : ItemDetailUiState
}

data class AttachmentItemDetail(
    val id: String,
    val fileName: String,
    val sizeBytes: Long,
    val mimeType: String
)

class ItemDetailViewModel(
    private val appContainer: AppContainer,
    private val domainStr: String,
    private val itemId: String
) : ViewModel() {

    private val domain: VaultItemDomain = try {
        VaultItemDomain.valueOf(domainStr.uppercase())
    } catch (_: Exception) {
        VaultItemDomain.PASSWORD
    }

    private val _rawState = MutableStateFlow<ItemDetailUiState>(ItemDetailUiState.Loading)
    private val syncStatus = appContainer.syncRepository.syncStatus

    val uiState: StateFlow<ItemDetailUiState> = combine(_rawState, syncStatus) { state, status ->
        if (state is ItemDetailUiState.Success) {
            state.copy(isOffline = status == SyncStatus.OFFLINE_READ_ONLY)
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ItemDetailUiState.Loading)

    init {
        loadItem()
    }

    fun loadItem() {
        viewModelScope.launch {
            _rawState.value = ItemDetailUiState.Loading
            when (domain) {
                VaultItemDomain.PASSWORD -> {
                    val result = appContainer.syncRepository.getPearlDetail(itemId)
                    result.fold(
                        onSuccess = { pearl ->
                            val ownerUuid = appContainer.deviceVault.getOwnerUuid().orEmpty()
                            val loadedAttachments = pearl.attachments.mapNotNull { attId ->
                                appContainer.syncRepository.getAttachment(ownerUuid, attId).getOrNull()?.let { entity ->
                                    AttachmentItemDetail(entity.id, entity.fileName, entity.sizeBytes, entity.mimeType)
                                }
                            }
                            _rawState.value = ItemDetailUiState.Success(
                                domain = VaultItemDomain.PASSWORD,
                                id = pearl.id,
                                title = pearl.title,
                                category = pearl.category,
                                secret = pearl.secret,
                                username = pearl.username,
                                url = pearl.url,
                                notes = pearl.notes,
                                totpSecret = pearl.totpSecret,
                                customFields = pearl.customFields,
                                tags = pearl.tags,
                                passwordHistory = pearl.passwordHistory,
                                uris = pearl.uris,
                                attachments = loadedAttachments,
                                reprompt = pearl.reprompt,
                                isOffline = syncStatus.value == SyncStatus.OFFLINE_READ_ONLY
                            )
                        },
                        onFailure = { e ->
                            _rawState.value = ItemDetailUiState.Error(e.message ?: "Failed to load password item")
                        }
                    )
                }
                VaultItemDomain.IDENTITY -> {
                    val result = appContainer.syncRepository.getIdentityDetail(itemId)
                    result.fold(
                        onSuccess = { identity ->
                            val ownerUuid = appContainer.deviceVault.getOwnerUuid().orEmpty()
                            val loadedAttachments = identity.attachments.mapNotNull { attId ->
                                appContainer.syncRepository.getAttachment(ownerUuid, attId).getOrNull()?.let { entity ->
                                    AttachmentItemDetail(entity.id, entity.fileName, entity.sizeBytes, entity.mimeType)
                                }
                            }
                            _rawState.value = ItemDetailUiState.Success(
                                domain = VaultItemDomain.IDENTITY,
                                id = identity.id,
                                title = identity.title,
                                category = identity.category,
                                username = identity.username.ifBlank { identity.fullName },
                                notes = identity.notes,
                                customFields = identity.customFields,
                                tags = identity.tags,
                                attachments = loadedAttachments,
                                identityDetail = identity,
                                reprompt = identity.reprompt,
                                isOffline = syncStatus.value == SyncStatus.OFFLINE_READ_ONLY
                            )
                        },
                        onFailure = { e ->
                            _rawState.value = ItemDetailUiState.Error(e.message ?: "Failed to load identity item")
                        }
                    )
                }
                VaultItemDomain.NOTE -> {
                    val result = appContainer.syncRepository.getNoteDetail(itemId)
                    result.fold(
                        onSuccess = { note ->
                            val ownerUuid = appContainer.deviceVault.getOwnerUuid().orEmpty()
                            val loadedAttachments = note.attachments.mapNotNull { attId ->
                                appContainer.syncRepository.getAttachment(ownerUuid, attId).getOrNull()?.let { entity ->
                                    AttachmentItemDetail(entity.id, entity.fileName, entity.sizeBytes, entity.mimeType)
                                }
                            }
                            _rawState.value = ItemDetailUiState.Success(
                                domain = VaultItemDomain.NOTE,
                                id = note.id,
                                title = note.title,
                                category = note.category,
                                secret = note.content,
                                customFields = note.customFields,
                                tags = note.tags,
                                attachments = loadedAttachments,
                                reprompt = note.reprompt,
                                isOffline = syncStatus.value == SyncStatus.OFFLINE_READ_ONLY
                            )
                        },
                        onFailure = { e ->
                            _rawState.value = ItemDetailUiState.Error(e.message ?: "Failed to load note item")
                        }
                    )
                }
                VaultItemDomain.SSH_KEY -> {
                    val result = appContainer.syncRepository.getSshKeyDetail(itemId)
                    result.fold(
                        onSuccess = { key ->
                            _rawState.value = ItemDetailUiState.Success(
                                domain = VaultItemDomain.SSH_KEY,
                                id = key.id,
                                title = key.title,
                                category = key.category,
                                secret = key.keyValue,
                                username = key.username,
                                customFields = key.customFields,
                                tags = key.tags,
                                reprompt = key.reprompt,
                                isOffline = syncStatus.value == SyncStatus.OFFLINE_READ_ONLY
                            )
                        },
                        onFailure = { e ->
                            _rawState.value = ItemDetailUiState.Error(e.message ?: "Failed to load SSH key item")
                        }
                    )
                }
            }
        }
    }

    fun toggleSecretVisibility() {
        val current = _rawState.value
        if (current is ItemDetailUiState.Success) {
            _rawState.value = current.copy(isSecretRevealed = !current.isSecretRevealed)
        }
    }

    fun downloadAttachmentToUri(
        context: android.content.Context,
        attachmentId: String,
        destinationUri: android.net.Uri,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): Job = viewModelScope.launch {
        val res = appContainer.syncRepository.decryptAttachmentBytes(attachmentId)
        res.fold(
            onSuccess = { bytes ->
                val writeResult = withContext(Dispatchers.IO) {
                    runCatching {
                        val out = context.contentResolver.openOutputStream(destinationUri)
                            ?: throw IllegalStateException("Unable to open destination file.")
                        out.use { stream ->
                            stream.write(bytes)
                            stream.flush()
                        }
                    }
                }
                writeResult.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { e ->
                        onError("Failed to save attachment: ${e.message ?: "unknown error"}")
                    }
                )
            },
            onFailure = { e ->
                onError("Unable to download attachment: ${e.message ?: "decryption failed"}")
            }
        )
    }

    fun deleteItem(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val res = appContainer.syncRepository.deleteItem(domain, itemId)
            res.fold(
                onSuccess = {
                    _rawState.value = ItemDetailUiState.Deleted
                    onDeleted()
                },
                onFailure = { e ->
                    _rawState.value = ItemDetailUiState.Error(e.message ?: "Delete failed")
                }
            )
        }
    }
}
