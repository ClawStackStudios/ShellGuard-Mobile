package com.clawstack.shellguard.ui.screens.lock

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clawstack.shellguard.crypto.ClawCrypto
import com.clawstack.shellguard.data.local.AppSettings
import com.clawstack.shellguard.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LockViewModel(private val appContainer: AppContainer) : ViewModel() {

    val username: String = appContainer.deviceVault.getUsername() ?: "Vault Owner"
    val serverUrl: String = appContainer.deviceVault.getServerUrl() ?: ""

    val settings: StateFlow<AppSettings> = appContainer.settingsRepository.settingsFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AppSettings()
        )

    fun unlock() {
        appContainer.vaultLockManager.unlockVault()
    }

    fun logout() {
        appContainer.deviceVault.clearSession()
        appContainer.vaultLockManager.unlockVault()
    }

    fun verifyPin(pin: String): Boolean {
        val success = appContainer.pinAuthManager.verifyPin(pin)
        if (success) {
            appContainer.deviceVault.getInMemoryShellKey()
            unlock()
        }
        return success
    }

    fun isPinLockedOut(): Boolean {
        val maxAttempts = settings.value.maxPinAttempts
        return appContainer.pinAuthManager.isLockedOut(maxAttempts)
    }

    fun getRemainingPinAttempts(): Int {
        val maxAttempts = settings.value.maxPinAttempts
        return appContainer.pinAuthManager.getRemainingAttempts(maxAttempts)
    }

    fun verifyMasterKey(huKey: String): Boolean {
        val cleanKey = huKey.trim()
        if (!ClawCrypto.isValidClawKey(cleanKey)) return false

        val isValid = appContainer.deviceVault.verifyMasterKey(cleanKey)
        if (isValid) {
            val ownerUuid = appContainer.deviceVault.getOwnerUuid()
            if (!ownerUuid.isNullOrBlank()) {
                val derivedShellKey = appContainer.cryptoEngine.deriveShellKey(cleanKey, ownerUuid)
                appContainer.deviceVault.setInMemoryShellKey(derivedShellKey)
            }
            appContainer.pinAuthManager.resetAttempts()
            unlock()
            return true
        }
        return false
    }

    fun verifyClawKeyContent(content: String): Boolean {
        val extractedKey = ClawCrypto.extractKey(content) ?: return false
        return verifyMasterKey(extractedKey)
    }

    fun readClawKeyFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            }
        } catch (_: Exception) {
            null
        }
    }
}
