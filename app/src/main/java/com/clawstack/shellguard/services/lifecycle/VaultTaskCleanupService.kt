package com.clawstack.shellguard.services.lifecycle

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.clawstack.shellguard.ShellGuardApp

/**
 * Lightweight task-lifecycle service that intercepts Android Recent Apps swipe-away
 * (`onTaskRemoved`) to immediately lock the vault, persist the locked state, and
 * zeroize volatile RAM secrets before process termination.
 */
class VaultTaskCleanupService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            val appContainer = (application as? ShellGuardApp)?.appContainer
            appContainer?.vaultLockManager?.lockVaultNow()
            appContainer?.deviceVault?.zeroizeMemory()
            appContainer?.attachmentVaultManager?.clearDecryptedCache()
        } catch (_: Throwable) {
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}
