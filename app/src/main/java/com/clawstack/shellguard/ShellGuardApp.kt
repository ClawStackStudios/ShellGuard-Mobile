package com.clawstack.shellguard

import android.app.Application
import android.util.Log
import com.clawstack.shellguard.di.AppContainer
import com.clawstack.shellguard.di.DefaultAppContainer

/**
 * ShellGuard Mobile Application Class
 * Responsible for core security initialization, SQLCipher loading, and AppContainer setup.
 */
class ShellGuardApp : Application() {

    val container: AppContainer by lazy {
        DefaultAppContainer(this)
    }

    val appContainer: AppContainer
        get() = container

    override fun onCreate() {
        super.onCreate()
        initializeSecurityFoundation()
    }

    private fun initializeSecurityFoundation() {
        if (!isRobolectric()) {
            try {
                System.loadLibrary("sqlcipher")
                Log.d("ShellGuardApp", "SQLCipher native libraries initialized successfully.")
            } catch (t: Throwable) {
                Log.e("ShellGuardApp", "Failed to load SQLCipher native binaries: ${t.message}")
            }
        }
    }

    private fun isRobolectric(): Boolean {
        return try {
            Class.forName("org.robolectric.Robolectric")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
