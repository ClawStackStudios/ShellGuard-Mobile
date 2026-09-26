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
                                    passwordHistory = pearl.passwordHistory
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
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    title = note.title,
                                    category = note.category.orEmpty(),
                                    secret = note.content,
                                    reprompt = note.reprompt,
                                    tags = note.tags,
                                    customFields = note.customFields
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
