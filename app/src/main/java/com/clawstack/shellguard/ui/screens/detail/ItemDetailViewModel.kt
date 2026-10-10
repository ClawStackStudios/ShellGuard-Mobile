package com.clawstack.shellguard.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clawstack.shellguard.data.repository.SyncStatus
import com.clawstack.shellguard.data.repository.VaultItemDomain
import com.clawstack.shellguard.di.AppContainer
import com.clawstack.shellguard.domain.models.CustomField
import com.clawstack.shellguard.domain.models.PasswordHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun openAttachment(context: android.content.Context, attachmentId: String, onReady: (android.net.Uri, String) -> Unit) {
        viewModelScope.launch {
            val res = appContainer.syncRepository.decryptAttachment(attachmentId)
            res.onSuccess { plaintextBase64 ->
                try {
                    val bytes = java.util.Base64.getDecoder().decode(plaintextBase64)
                    val ownerUuid = appContainer.deviceVault.getOwnerUuid().orEmpty()
                    val entity = appContainer.syncRepository.getAttachment(ownerUuid, attachmentId).getOrNull()
                    if (entity != null) {
                        val cacheDir = java.io.File(context.cacheDir, "decrypted_attachments")
                        if (!cacheDir.exists()) cacheDir.mkdirs()
                        val targetFile = java.io.File(cacheDir, entity.fileName)
                        targetFile.writeBytes(bytes)
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
                        onReady(uri, entity.mimeType)
                    }
                } catch (e: Exception) {
                    _rawState.value = ItemDetailUiState.Error("Failed to decode and open attachment: ${e.message}")
                }
            }.onFailure {
                _rawState.value = ItemDetailUiState.Error("Failed to decrypt attachment: ${it.message}")
            }
        }
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
