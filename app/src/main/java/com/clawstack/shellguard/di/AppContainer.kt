package com.clawstack.shellguard.di

import android.content.Context
import com.clawstack.shellguard.crypto.AndroidKeyStoreHelper
import com.clawstack.shellguard.crypto.ClawCrypto
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.data.local.ShellGuardDatabase

/**
 * Top-level dependency container scoped to the Application lifecycle.
 * Provides frameworkless, sub-100ms startup and zero-reflection auditability.
 */
interface AppContainer {
    val keyStoreHelper: AndroidKeyStoreHelper
    val clawCrypto: ClawCrypto
    val cryptoEngine: ShellCryptionEngine
    val database: ShellGuardDatabase
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
}
