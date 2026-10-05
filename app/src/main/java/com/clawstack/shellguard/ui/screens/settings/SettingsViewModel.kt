package com.clawstack.shellguard.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clawstack.shellguard.crypto.LockTimeout
import com.clawstack.shellguard.data.local.AppSettings
import com.clawstack.shellguard.data.local.ThemeMode
import com.clawstack.shellguard.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import com.clawstack.shellguard.data.backup.BackupFormatType
import com.clawstack.shellguard.data.backup.BackupProtectionMode
import com.clawstack.shellguard.data.backup.ExportResult
import com.clawstack.shellguard.data.backup.ImportResult

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val serverUrl: String = "",
    val username: String = "",
    val isSyncing: Boolean = false,
    val isWiping: Boolean = false,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val lastExportResult: ExportResult? = null,
    val lastImportResult: ImportResult? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val appContainer: AppContainer
) : ViewModel() {

    private val settingsRepo = appContainer.settingsRepository
    private val deviceVault = appContainer.deviceVault
    private val vaultLockManager = appContainer.vaultLockManager

    private val _extraState = MutableStateFlow(
        SettingsUiState(
            serverUrl = deviceVault.getServerUrl().orEmpty(),
            username = deviceVault.getUsername().orEmpty()
        )
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepo.settingsFlow,
        _extraState
    ) { appSettings, extra ->
        extra.copy(settings = appSettings)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState(
            serverUrl = deviceVault.getServerUrl().orEmpty(),
            username = deviceVault.getUsername().orEmpty()
        )
    )

    fun updateThemeMode(mode: ThemeMode): Job = viewModelScope.launch {
        settingsRepo.setThemeMode(mode)
    }

    fun updateDynamicColors(enabled: Boolean): Job = viewModelScope.launch {
        settingsRepo.setDynamicColors(enabled)
    }

    fun updateShowFavicons(enabled: Boolean): Job = viewModelScope.launch {
        settingsRepo.setShowFavicons(enabled)
    }

    fun updateCompactView(enabled: Boolean): Job = viewModelScope.launch {
        settingsRepo.setCompactView(enabled)
    }

    fun updateLockTimeout(timeout: LockTimeout): Job = viewModelScope.launch {
        vaultLockManager.setLockTimeout(timeout)
        settingsRepo.setLockTimeout(timeout)
    }

    fun updateAllowScreenCapture(enabled: Boolean): Job = viewModelScope.launch {
        settingsRepo.setAllowScreenCapture(enabled)
    }

    fun updateClipboardClearSeconds(seconds: Int): Job = viewModelScope.launch {
        settingsRepo.setClipboardClearSeconds(seconds)
    }

    fun updatePanicWipeCountdownSeconds(seconds: Int): Job = viewModelScope.launch {
        settingsRepo.setPanicWipeCountdownSeconds(seconds)
    }

    fun updateAutofillInlineChips(enabled: Boolean): Job = viewModelScope.launch {
        settingsRepo.setAutofillInlineChips(enabled)
    }

    fun updateSyncOverCellular(enabled: Boolean): Job = viewModelScope.launch {
        settingsRepo.setSyncOverCellular(enabled)
    }

    fun updatePullToRefreshEnabled(enabled: Boolean): Job = viewModelScope.launch {
        settingsRepo.setPullToRefreshEnabled(enabled)
    }

    fun triggerManualSync(): Job = viewModelScope.launch {
        val ownerUuid = deviceVault.getOwnerUuid().orEmpty()
        if (ownerUuid.isBlank()) {
            _extraState.update { it.copy(errorMessage = "No active session to sync.") }
            return@launch
        }
        _extraState.update { it.copy(isSyncing = true, infoMessage = "Sync in progress...") }
        val result = appContainer.syncRepository.syncAll(ownerUuid)
        if (result.isSuccess) {
            _extraState.update { it.copy(isSyncing = false, infoMessage = "Vault synchronized successfully.") }
        } else {
            val err = result.exceptionOrNull()?.message ?: "Sync failed"
            _extraState.update { it.copy(isSyncing = false, errorMessage = err) }
        }
    }

    fun clearMessages() {
        _extraState.update { it.copy(infoMessage = null, errorMessage = null) }
    }

    fun executePanicPurge(onCompleted: () -> Unit): Job = viewModelScope.launch {
        _extraState.update { it.copy(isWiping = true) }
        try {
            // 1. Wipe Room Database tables
            appContainer.database.clearAllTables()
            // 2. Zeroize in-memory secrets and clear EncryptedSharedPreferences session
            appContainer.deviceVault.clearSession()
            // 3. Clear settings repository preferences
            appContainer.settingsRepository.clearAll()
            // 4. Reset vault lock manager
            appContainer.vaultLockManager.unlockVault()
        } catch (t: Throwable) {
            // Ensure session is cleared even if database wipe throws
            appContainer.deviceVault.clearSession()
        } finally {
            _extraState.update { it.copy(isWiping = false) }
            onCompleted()
        }
    }

    fun exportVaultBackup(
        protectionMode: BackupProtectionMode,
        customPassphrase: String? = null,
        activeClawKey: String? = null,
        onCompleted: (Result<ExportResult>) -> Unit = {}
    ): Job = viewModelScope.launch {
        val ownerUuid = deviceVault.getOwnerUuid().orEmpty()
        if (ownerUuid.isBlank()) {
            val err = Result.failure<ExportResult>(IllegalStateException("No active session to export."))
            _extraState.update { it.copy(errorMessage = "No active session to export.") }
            onCompleted(err)
            return@launch
        }
        _extraState.update { it.copy(isExporting = true) }
        val result = appContainer.backupEngine.exportVault(
            ownerUuid = ownerUuid,
            protectionMode = protectionMode,
            customPassphrase = customPassphrase,
            activeClawKey = activeClawKey
        )
        _extraState.update {
            it.copy(
                isExporting = false,
                lastExportResult = result.getOrNull(),
                errorMessage = result.exceptionOrNull()?.message,
                infoMessage = if (result.isSuccess) "Vault exported successfully." else null
            )
        }
        onCompleted(result)
    }

    fun importVaultBackup(
        rawContent: String,
        passwordOrKey: String? = null,
        onCompleted: (Result<ImportResult>) -> Unit = {}
    ): Job = viewModelScope.launch {
        val ownerUuid = deviceVault.getOwnerUuid().orEmpty()
        if (ownerUuid.isBlank()) {
            val err = Result.failure<ImportResult>(IllegalStateException("No active session to import into."))
            _extraState.update { it.copy(errorMessage = "No active session to import into.") }
            onCompleted(err)
            return@launch
        }
        _extraState.update { it.copy(isImporting = true) }
        val format = appContainer.backupEngine.detectBackupFormat(rawContent)
        val result = when (format) {
            BackupFormatType.BITWARDEN_JSON -> {
                appContainer.backupEngine.importBitwardenJson(rawContent, ownerUuid)
            }
            BackupFormatType.BITWARDEN_ENCRYPTED -> {
                Result.failure(IllegalArgumentException("Encrypted Bitwarden backups are not supported. Please export an unencrypted JSON from Bitwarden."))
            }
            BackupFormatType.SHELLGUARD_ENCRYPTED -> {
                val secretKey = passwordOrKey ?: ""
                val decryptResult = appContainer.backupEngine.decryptBackupEnvelope(rawContent, secretKey)
                if (decryptResult.isSuccess) {
                    appContainer.backupEngine.importPayload(decryptResult.getOrThrow(), ownerUuid)
                } else {
                    Result.failure(decryptResult.exceptionOrNull() ?: Exception("Decryption failed"))
                }
            }
            BackupFormatType.SHELLGUARD_PLAIN -> {
                try {
                    val payload = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<com.clawstack.shellguard.data.backup.VaultBackupPayload>(rawContent)
                    appContainer.backupEngine.importPayload(payload, ownerUuid)
                } catch (t: Throwable) {
                    Result.failure(t)
                }
            }
            BackupFormatType.UNKNOWN -> {
                Result.failure(IllegalArgumentException("Unrecognized backup file format."))
            }
        }

        _extraState.update {
            it.copy(
                isImporting = false,
                lastImportResult = result.getOrNull(),
                errorMessage = result.exceptionOrNull()?.message,
                infoMessage = if (result.isSuccess) {
                    val res = result.getOrNull()!!
                    "Imported ${res.pearlsCount} logins, ${res.notesCount} notes, ${res.sshKeysCount} SSH keys."
                } else null
            )
        }
        onCompleted(result)
    }

    fun detectBackupFormat(raw: String): BackupFormatType = appContainer.backupEngine.detectBackupFormat(raw)

    fun clearBackupResults() {
        _extraState.update { it.copy(lastExportResult = null, lastImportResult = null) }
    }

    public override fun onCleared() {
        super.onCleared()
    }
}
