package com.clawstack.shellguard.ui.screens.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clawstack.shellguard.data.repository.VaultItemDomain
import com.clawstack.shellguard.di.AppContainer
import com.clawstack.shellguard.domain.models.CustomField
import com.clawstack.shellguard.domain.models.CustomFieldType
import com.clawstack.shellguard.domain.models.IdentityDetail
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
    val honorificTitle: String = "",
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val company: String = "",
    val email: String = "",
    val phone: String = "",
    val address1: String = "",
    val address2: String = "",
    val address3: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val country: String = "",
    val ssn: String = "",
    val passportNumber: String = "",
    val licenseNumber: String = "",
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
                VaultItemDomain.IDENTITY -> {
                    val result = appContainer.syncRepository.getIdentityDetail(id)
                    result.fold(
                        onSuccess = { identity ->
                            val ownerUuid = appContainer.deviceVault.getOwnerUuid().orEmpty()
                            val loadedAttachments = identity.attachments.mapNotNull { attId ->
                                appContainer.syncRepository.getAttachment(ownerUuid, attId).getOrNull()?.let { entity ->
                                    com.clawstack.shellguard.ui.screens.detail.AttachmentItemDetail(entity.id, entity.fileName, entity.sizeBytes, entity.mimeType)
                                }
                            }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    title = identity.title,
                                    category = identity.category,
                                    username = identity.username,
                                    notes = identity.notes,
                                    honorificTitle = identity.honorificTitle,
                                    firstName = identity.firstName,
                                    middleName = identity.middleName,
                                    lastName = identity.lastName,
                                    company = identity.company,
                                    email = identity.email,
                                    phone = identity.phone,
                                    address1 = identity.address1,
                                    address2 = identity.address2,
                                    address3 = identity.address3,
                                    city = identity.city,
                                    state = identity.state,
                                    postalCode = identity.postalCode,
                                    country = identity.country,
                                    ssn = identity.ssn,
                                    passportNumber = identity.passportNumber,
                                    licenseNumber = identity.licenseNumber,
                                    reprompt = identity.reprompt,
                                    tags = identity.tags,
                                    customFields = identity.customFields,
                                    attachments = loadedAttachments
                                )
                            }
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load identity") }
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
    fun updateHonorificTitle(v: String) = _uiState.update { it.copy(honorificTitle = v) }
    fun updateFirstName(v: String) = _uiState.update { it.copy(firstName = v) }
    fun updateMiddleName(v: String) = _uiState.update { it.copy(middleName = v) }
    fun updateLastName(v: String) = _uiState.update { it.copy(lastName = v) }
    fun updateCompany(v: String) = _uiState.update { it.copy(company = v) }
    fun updateEmail(v: String) = _uiState.update { it.copy(email = v) }
    fun updatePhone(v: String) = _uiState.update { it.copy(phone = v) }
    fun updateAddress1(v: String) = _uiState.update { it.copy(address1 = v) }
    fun updateAddress2(v: String) = _uiState.update { it.copy(address2 = v) }
    fun updateAddress3(v: String) = _uiState.update { it.copy(address3 = v) }
    fun updateCity(v: String) = _uiState.update { it.copy(city = v) }
    fun updateState(v: String) = _uiState.update { it.copy(state = v) }
    fun updatePostalCode(v: String) = _uiState.update { it.copy(postalCode = v) }
    fun updateCountry(v: String) = _uiState.update { it.copy(country = v) }
    fun updateSsn(v: String) = _uiState.update { it.copy(ssn = v) }
    fun updatePassportNumber(v: String) = _uiState.update { it.copy(passportNumber = v) }
    fun updateLicenseNumber(v: String) = _uiState.update { it.copy(licenseNumber = v) }
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
                VaultItemDomain.IDENTITY -> {
                    val identity = IdentityDetail(
                        id = state.id,
                        ownerUuid = ownerUuid,
                        title = state.title.trim(),
                        honorificTitle = state.honorificTitle.trim(),
                        firstName = state.firstName.trim(),
                        middleName = state.middleName.trim(),
                        lastName = state.lastName.trim(),
                        username = state.username.trim(),
                        company = state.company.trim(),
                        email = state.email.trim(),
                        phone = state.phone.trim(),
                        address1 = state.address1.trim(),
                        address2 = state.address2.trim(),
                        address3 = state.address3.trim(),
                        city = state.city.trim(),
                        state = state.state.trim(),
                        postalCode = state.postalCode.trim(),
                        country = state.country.trim(),
                        ssn = state.ssn.trim(),
                        passportNumber = state.passportNumber.trim(),
                        licenseNumber = state.licenseNumber.trim(),
                        category = state.category.trim(),
                        notes = state.notes.trim(),
                        customFields = state.customFields,
                        attachments = state.attachments.map { it.id },
                        tags = state.tags,
                        reprompt = state.reprompt
                    )
                    appContainer.syncRepository.saveIdentityDetail(identity)
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
