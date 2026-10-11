package com.clawstack.shellguard.ui.screens.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clawstack.shellguard.data.repository.VaultItemDomain
import com.clawstack.shellguard.di.AppContainer
import com.clawstack.shellguard.domain.models.CustomField
import com.clawstack.shellguard.domain.models.CustomFieldType
import com.clawstack.shellguard.domain.models.PasswordHistoryEntry
import com.clawstack.shellguard.domain.models.PearlDetail
import com.clawstack.shellguard.domain.models.SecureNoteDetail
import com.clawstack.shellguard.domain.models.SshKeyDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class FormMode {
    NEW, EDIT
}

data class ItemFormUiState(
    val mode: FormMode = FormMode.NEW,
    val domain: VaultItemDomain = VaultItemDomain.PASSWORD,
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val username: String = "",
    val url: String = "",
    val secret: String = "",
    val notes: String = "",
    val totpSecret: String = "",
    val reprompt: Boolean = false,
    val tags: List<String> = emptyList(),
    val customFields: List<CustomField> = emptyList(),
    val passwordHistory: List<PasswordHistoryEntry> = emptyList(),
    val uris: List<String> = emptyList(),
    val attachments: List<com.clawstack.shellguard.ui.screens.detail.AttachmentItemDetail> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSecretVisible: Boolean = false,
    val errorMessage: String? = null,
    val isSaveSuccess: Boolean = false
)

class ItemFormViewModel(
    private val appContainer: AppContainer,
    private val modeStr: String,
    private val domainStr: String,
    private val initialItemId: String?
) : ViewModel() {

    private val parsedMode = if (modeStr.equals("EDIT", ignoreCase = true)) FormMode.EDIT else FormMode.NEW
    private val parsedDomain = try {
        VaultItemDomain.valueOf(domainStr.uppercase())
    } catch (_: Exception) {
        VaultItemDomain.PASSWORD
    }

    private val _uiState = MutableStateFlow(
        ItemFormUiState(
            mode = parsedMode,
            domain = parsedDomain,
            id = if (parsedMode == FormMode.EDIT && !initialItemId.isNullOrBlank()) initialItemId else UUID.randomUUID().toString()
        )
    )
    val uiState: StateFlow<ItemFormUiState> = _uiState.asStateFlow()

    init {
        if (parsedMode == FormMode.EDIT && !initialItemId.isNullOrBlank()) {
            loadExistingItem(initialItemId, parsedDomain)
        }
    }

    private fun loadExistingItem(id: String, domain: VaultItemDomain) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (domain) {
                VaultItemDomain.PASSWORD -> {
                    val result = appContainer.syncRepository.getPearlDetail(id)
                    result.fold(
                        onSuccess = { pearl ->
                            val ownerUuid = appContainer.deviceVault.getOwnerUuid().orEmpty()
                            val loadedAttachments = pearl.attachments.mapNotNull { attId ->
                                appContainer.syncRepository.getAttachment(ownerUuid, attId).getOrNull()?.let { entity ->
                                    com.clawstack.shellguard.ui.screens.detail.AttachmentItemDetail(entity.id, entity.fileName, entity.sizeBytes, entity.mimeType)
                                }
                            }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    title = pearl.title,
                                    category = pearl.category.orEmpty(),
                                    username = pearl.username.orEmpty(),
                                    url = pearl.url.orEmpty(),
                                    secret = pearl.secret,
                                    notes = pearl.notes.orEmpty(),
                                    totpSecret = pearl.totpSecret.orEmpty(),
                                    reprompt = pearl.reprompt,
                                    tags = pearl.tags,
                                    customFields = pearl.customFields,
                                    passwordHistory = pearl.passwordHistory,
                                    uris = pearl.uris,
                                    attachments = loadedAttachments
                                )
                            }
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load item") }
                        }
                    )
                }
                VaultItemDomain.NOTE -> {
                    val result = appContainer.syncRepository.getNoteDetail(id)
                    result.fold(
                        onSuccess = { note ->
                            val ownerUuid = appContainer.deviceVault.getOwnerUuid().orEmpty()
                            val loadedAttachments = note.attachments.mapNotNull { attId ->
                                appContainer.syncRepository.getAttachment(ownerUuid, attId).getOrNull()?.let { entity ->
                                    com.clawstack.shellguard.ui.screens.detail.AttachmentItemDetail(entity.id, entity.fileName, entity.sizeBytes, entity.mimeType)
                                }
                            }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    title = note.title,
                                    category = note.category.orEmpty(),
                                    secret = note.content,
                                    reprompt = note.reprompt,
                                    tags = note.tags,
                                    customFields = note.customFields,
                                    attachments = loadedAttachments
                                )
                            }
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load item") }
                        }
                    )
                }
                VaultItemDomain.SSH_KEY -> {
                    val result = appContainer.syncRepository.getSshKeyDetail(id)
                    result.fold(
                        onSuccess = { key ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    title = key.title,
                                    category = key.category.orEmpty(),
                                    username = key.username.orEmpty(),
                                    secret = key.keyValue,
                                    reprompt = key.reprompt,
                                    tags = key.tags,
                                    customFields = key.customFields
                                )
                            }
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load item") }
                        }
                    )
                }
            }
        }
    }

    fun setDomain(newDomain: VaultItemDomain) {
        if (_uiState.value.mode == FormMode.NEW) {
            _uiState.update { it.copy(domain = newDomain) }
        }
    }

    fun updateTitle(v: String) = _uiState.update { it.copy(title = v, errorMessage = null) }
    fun updateCategory(v: String) = _uiState.update { it.copy(category = v) }
    fun updateUsername(v: String) = _uiState.update { it.copy(username = v) }
    fun updateUrl(v: String) = _uiState.update { it.copy(url = v) }
    fun updateSecret(v: String) = _uiState.update { it.copy(secret = v) }
    fun updateNotes(v: String) = _uiState.update { it.copy(notes = v) }
    fun updateTotpSecret(v: String) = _uiState.update { it.copy(totpSecret = v) }
    fun toggleReprompt(v: Boolean) = _uiState.update { it.copy(reprompt = v) }
    fun toggleSecretVisibility() = _uiState.update { it.copy(isSecretVisible = !it.isSecretVisible) }

    fun addTag(tag: String) {
        val trimmed = tag.trim().trimStart('#')
        if (trimmed.isNotBlank() && !_uiState.value.tags.contains(trimmed)) {
            _uiState.update { it.copy(tags = it.tags + trimmed) }
        }
    }

    fun removeTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags.filter { t -> t != tag }) }
    }

    fun addCustomField(type: CustomFieldType, label: String, value: String) {
        val field = CustomField(
            id = UUID.randomUUID().toString(),
            label = label.ifBlank { "Field" },
            value = value,
            type = type
        )
        _uiState.update { it.copy(customFields = it.customFields + field) }
    }

    fun updateCustomField(id: String, label: String, value: String) {
        _uiState.update { state ->
            state.copy(
                customFields = state.customFields.map {
                    if (it.id == id) it.copy(label = label, value = value) else it
                }
            )
        }
    }

    fun removeCustomField(id: String) {
        _uiState.update { state ->
            state.copy(customFields = state.customFields.filter { it.id != id })
        }
    }

    fun addUri(uri: String = "") {
        _uiState.update { it.copy(uris = it.uris + uri) }
    }

    fun updateUri(index: Int, uri: String) {
        _uiState.update { state ->
            val mutable = state.uris.toMutableList()
            if (index in mutable.indices) {
                mutable[index] = uri
                state.copy(uris = mutable)
            } else state
        }
    }

    fun removeUri(index: Int) {
        _uiState.update { state ->
            val mutable = state.uris.toMutableList()
            if (index in mutable.indices) {
                mutable.removeAt(index)
                state.copy(uris = mutable)
            } else state
        }
    }

    fun stageAttachment(contentUri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val cursor = context.contentResolver.query(contentUri, null, null, null, null)
                var fileName = "attachment"
                var sizeBytes = 0L
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) fileName = it.getString(nameIndex)
                        val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1) sizeBytes = it.getLong(sizeIndex)
                    }
                }

                val mimeType = context.contentResolver.getType(contentUri) ?: "application/octet-stream"
                val rawBytes = context.contentResolver.openInputStream(contentUri)?.use { it.readBytes() }

                if (rawBytes != null && rawBytes.isNotEmpty()) {
                    val ownerUuid = appContainer.deviceVault.getOwnerUuid().orEmpty()
                    val result = appContainer.syncRepository.stageAttachment(
                        ownerUuid = ownerUuid,
                        title = fileName,
                        fileName = fileName,
                        mimeType = mimeType,
                        category = "",
                        rawPlaintextBytes = rawBytes
                    )

                    result.onSuccess { entity ->
                        val draft = com.clawstack.shellguard.ui.screens.detail.AttachmentItemDetail(entity.id, entity.fileName, entity.sizeBytes, entity.mimeType)
                        _uiState.update { it.copy(attachments = it.attachments + draft) }
                    }.onFailure { e ->
                        _uiState.update { it.copy(errorMessage = "Failed to stage attachment: ${e.message}") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error staging attachment: ${e.message}") }
            }
        }
    }

    fun removeAttachment(attachmentId: String) {
        _uiState.update { state ->
            state.copy(attachments = state.attachments.filter { it.id != attachmentId })
        }
    }

    fun save(onSuccess: (domain: String, id: String) -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val ownerUuid = appContainer.deviceVault.getOwnerUuid().orEmpty()

            val saveResult = when (state.domain) {
                VaultItemDomain.PASSWORD -> {
                    val pearl = PearlDetail(
                        id = state.id,
                        ownerUuid = ownerUuid,
                        title = state.title.trim(),
                        secret = state.secret,
                        username = state.username.trim(),
                        url = state.url.trim(),
                        category = state.category.trim(),
                        notes = state.notes.trim(),
                        totpSecret = state.totpSecret.trim(),
                        customFields = state.customFields,
                        tags = state.tags,
                        uris = state.uris.filter { it.isNotBlank() },
                        attachments = state.attachments.map { it.id },
                        passwordHistory = state.passwordHistory,
                        reprompt = state.reprompt
                    )
                    appContainer.syncRepository.savePearlDetail(pearl)
                }
                VaultItemDomain.NOTE -> {
                    val note = SecureNoteDetail(
                        id = state.id,
                        ownerUuid = ownerUuid,
                        title = state.title.trim(),
                        content = state.secret,
                        category = state.category.trim(),
                        customFields = state.customFields,
                        tags = state.tags,
                        attachments = state.attachments.map { it.id },
                        reprompt = state.reprompt
                    )
                    appContainer.syncRepository.saveNoteDetail(note)
                }
                VaultItemDomain.SSH_KEY -> {
                    val key = SshKeyDetail(
                        id = state.id,
                        ownerUuid = ownerUuid,
                        title = state.title.trim(),
                        keyValue = state.secret,
                        username = state.username.trim(),
                        category = state.category.trim(),
                        customFields = state.customFields,
                        tags = state.tags,
                        reprompt = state.reprompt
                    )
                    appContainer.syncRepository.saveSshKeyDetail(key)
                }
            }

            saveResult.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, isSaveSuccess = true) }
                    onSuccess(state.domain.name, state.id)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Save failed") }
                }
            )
        }
    }
}
