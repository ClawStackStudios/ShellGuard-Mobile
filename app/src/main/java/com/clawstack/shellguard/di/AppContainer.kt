package com.clawstack.shellguard.di

import android.content.Context
import com.clawstack.shellguard.crypto.AndroidKeyStoreHelper
import com.clawstack.shellguard.crypto.ClawCrypto
import com.clawstack.shellguard.crypto.EncryptedDeviceVault
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.data.local.ShellGuardDatabase
import com.clawstack.shellguard.data.remote.ShellGuardClient
import com.clawstack.shellguard.data.repository.ConnectivityMonitor
import com.clawstack.shellguard.data.repository.SyncRepository

/**
 * Top-level dependency container scoped to the Application lifecycle.
 * Provides frameworkless, sub-100ms startup and zero-reflection auditability.
 */
interface AppContainer {
    val keyStoreHelper: AndroidKeyStoreHelper
    val clawCrypto: ClawCrypto
    val cryptoEngine: ShellCryptionEngine
    val database: ShellGuardDatabase
    val deviceVault: EncryptedDeviceVault
    val connectivityMonitor: ConnectivityMonitor
    val syncRepository: SyncRepository
    val vaultLockManager: com.clawstack.shellguard.crypto.VaultLockManager
    fun getClient(baseUrl: String): ShellGuardClient
}

/**
 * Thread-safe lazy implementation of AppContainer.
 * Dependencies instantiate strictly upon first access.
 */
class DefaultAppContainer(private val context: Context) : AppContainer {

    override val keyStoreHelper: AndroidKeyStoreHelper by lazy {
        AndroidKeyStoreHelper
    }

    override val clawCrypto: ClawCrypto by lazy {
        ClawCrypto
    }

    override val cryptoEngine: ShellCryptionEngine by lazy {
        ShellCryptionEngine
    }

    override val database: ShellGuardDatabase by lazy {
        ShellGuardDatabase.getInstance(context)
    }

    override val deviceVault: EncryptedDeviceVault by lazy {
        EncryptedDeviceVault(context)
    }

    override val connectivityMonitor: ConnectivityMonitor by lazy {
        ConnectivityMonitor(context)
    }

    override val vaultLockManager: com.clawstack.shellguard.crypto.VaultLockManager by lazy {
        com.clawstack.shellguard.crypto.VaultLockManager(context, deviceVault)
    }

    override val syncRepository: SyncRepository by lazy {
        SyncRepository(
            database = database,
            clientProvider = { baseUrl -> getClient(baseUrl) },
            deviceVault = deviceVault,
            connectivityMonitor = connectivityMonitor
        )
    }

    override fun getClient(baseUrl: String): ShellGuardClient {
        return ShellGuardClient(
            baseUrl = baseUrl,
            onUnauthorized = {
                deviceVault.clearSession()
            }
        )
    }
}

