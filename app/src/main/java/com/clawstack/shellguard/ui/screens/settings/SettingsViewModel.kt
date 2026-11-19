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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        settingsRepo.settingsFlow.onStart { emit(AppSettings()) },
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

    fun updateUnlockMethod(method: com.clawstack.shellguard.data.local.UnlockMethod): Job = viewModelScope.launch {
        settingsRepo.setUnlockMethod(method)
    }

    fun setupPin(pin: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}): Job = viewModelScope.launch {
        try {
            appContainer.pinAuthManager.setupPin(pin)
            settingsRepo.setUnlockMethod(com.clawstack.shellguard.data.local.UnlockMethod.PIN)
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Failed to set up PIN")
        }
    }

    fun clearPin(onSuccess: () -> Unit = {}): Job = viewModelScope.launch {
        appContainer.pinAuthManager.clearPin()
        settingsRepo.setUnlockMethod(com.clawstack.shellguard.data.local.UnlockMethod.MASTER_KEY)
        onSuccess()
    }

    fun updateMaxPinAttempts(attempts: Int): Job = viewModelScope.launch {
        settingsRepo.setMaxPinAttempts(attempts)
    }

    fun updateSyncOverCellular(enabled: Boolean): Job = viewModelScope.launch {
        settingsRepo.setSyncOverCellular(enabled)
    }

    fun updatePullToRefreshEnabled(enabled: Boolean): Job = viewModelScope.launch {
        settingsRepo.setPullToRefreshEnabled(enabled)
    }

    fun updateAllowSelfSignedCerts(enabled: Boolean): Job = viewModelScope.launch {
        settingsRepo.setAllowSelfSignedCerts(enabled)
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

    fun updateDefaultUriMatchMode(mode: com.clawstack.shellguard.domain.matcher.UriMatchMode) {
        viewModelScope.launch {
            appContainer.settingsRepository.setDefaultUriMatchMode(mode)
        }
    }

    fun executePanicPurge(onCompleted: () -> Unit): Job = viewModelScope.launch {
        _extraState.update { it.copy(isWiping = true) }
        try {
            withContext(Dispatchers.IO) {
                // 1. Wipe Room Database tables
                appContainer.database.clearAllTables()
                // 2. Zeroize in-memory secrets and clear EncryptedSharedPreferences session
                appContainer.deviceVault.clearSession()
                // 3. Wipe disk-stored encrypted attachment payloads
                appContainer.attachmentVaultManager.clearAll()
                // 4. Clear settings repository preferences & evict cached network client
                appContainer.settingsRepository.clearAll()
                appContainer.clearCachedClient()
                // 5. Reset vault lock manager
                appContainer.vaultLockManager.unlockVault()
            }
        } catch (_: Throwable) {
            // Fail-closed: ensure session, attachments, and client are cleared even if DB wipe throws
            withContext(Dispatchers.IO) {
                appContainer.deviceVault.clearSession()
                try {
                    appContainer.attachmentVaultManager.clearAll()
                } catch (_: Throwable) {}
                appContainer.clearCachedClient()
            }
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
                    val payload = com.clawstack.shellguard.data.remote.KtorClientProvider.jsonConfig.decodeFromString<com.clawstack.shellguard.data.backup.VaultBackupPayload>(rawContent)
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

    fun importVaultBackupFromStream(
        inputStreamProvider: () -> java.io.InputStream?,
        passwordOrKey: String? = null,
        maxBytes: Int = 25 * 1024 * 1024,
        onCompleted: (Result<ImportResult>) -> Unit = {}
    ): Job = viewModelScope.launch {
        _extraState.update { it.copy(isImporting = true, errorMessage = null) }
        val readResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val stream = inputStreamProvider() ?: throw IllegalStateException("Unable to open selected backup file.")
                stream.use { input ->
                    val buffer = ByteArray(8192)
                    val out = java.io.ByteArrayOutputStream()
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        total += read
                        if (total > maxBytes) {
                            throw IllegalArgumentException("Backup file exceeds maximum supported size (25 MB).")
                        }
                        out.write(buffer, 0, read)
                    }
                    out.toString(Charsets.UTF_8.name())
                }
            }
        }

        if (readResult.isFailure) {
            val err = Result.failure<ImportResult>(readResult.exceptionOrNull() ?: Exception("Failed to read backup file."))
            _extraState.update {
                it.copy(
                    isImporting = false,
                    errorMessage = readResult.exceptionOrNull()?.message ?: "Failed to read backup file."
                )
            }
            onCompleted(err)
            return@launch
        }

        val rawContent = readResult.getOrThrow()
        importVaultBackup(
            rawContent = rawContent,
            passwordOrKey = passwordOrKey,
            onCompleted = onCompleted
        ).join()
    }

    fun detectBackupFormat(raw: String): BackupFormatType = appContainer.backupEngine.detectBackupFormat(raw)

    fun clearBackupResults() {
        _extraState.update { it.copy(lastExportResult = null, lastImportResult = null) }
    }

    fun lockVault(onLocked: () -> Unit = {}) {
        appContainer.vaultLockManager.lockVaultNow()
        appContainer.deviceVault.zeroizeMemory()
        try {
            appContainer.attachmentVaultManager.clearDecryptedCache()
        } catch (_: Throwable) {}
        onLocked()
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            appContainer.deviceVault.clearSession()
            try {
                appContainer.attachmentVaultManager.clearDecryptedCache()
            } catch (_: Throwable) {}
            onLoggedOut()
        }
    }

    public override fun onCleared() {
        super.onCleared()
    }
}
