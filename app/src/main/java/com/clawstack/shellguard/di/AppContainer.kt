package com.clawstack.shellguard.di

import android.content.Context
import com.clawstack.shellguard.crypto.AndroidKeyStoreHelper
import com.clawstack.shellguard.crypto.ClawCrypto

/**
 * Top-level dependency container scoped to the Application lifecycle.
 * Provides frameworkless, sub-100ms startup and zero-reflection auditability.
 */
interface AppContainer {
    val keyStoreHelper: AndroidKeyStoreHelper
    val clawCrypto: ClawCrypto
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
}
