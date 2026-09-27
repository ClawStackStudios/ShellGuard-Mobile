package com.clawstack.shellguard.ui.screens.lock

import androidx.lifecycle.ViewModel
import com.clawstack.shellguard.di.AppContainer

class LockViewModel(private val appContainer: AppContainer) : ViewModel() {

    val username: String = appContainer.deviceVault.getUsername() ?: "Vault Owner"
    val serverUrl: String = appContainer.deviceVault.getServerUrl() ?: ""

    fun unlock() {
        appContainer.vaultLockManager.unlockVault()
    }

    fun logout() {
        appContainer.deviceVault.clearSession()
        appContainer.vaultLockManager.unlockVault()
    }
}
