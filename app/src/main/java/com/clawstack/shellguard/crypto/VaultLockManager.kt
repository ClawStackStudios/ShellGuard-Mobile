package com.clawstack.shellguard.crypto

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LockTimeout(val label: String, val millis: Long) {
    IMMEDIATE("Immediately", 0L),
    ONE_MINUTE("1 minute", 60_000L),
    FIVE_MINUTES("5 minutes", 300_000L),
    FIFTEEN_MINUTES("15 minutes", 900_000L),
    NEVER("Never", Long.MAX_VALUE);

    companion object {
        fun fromName(name: String?): LockTimeout {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: FIVE_MINUTES
        }
    }
}

class VaultLockManager(
    private val context: Context,
    private val deviceVault: EncryptedDeviceVault
) {
    private val prefs by lazy {
        context.getSharedPreferences("shellguard_lock_prefs", Context.MODE_PRIVATE)
    }

    private val _isVaultLocked = MutableStateFlow(
        deviceVault.hasActiveSession() && prefs.getBoolean(KEY_IS_LOCKED, false)
    )
    val isVaultLocked: StateFlow<Boolean> = _isVaultLocked.asStateFlow()

    private var backgroundTimestamp: Long
        get() = prefs.getLong(KEY_BG_TIMESTAMP, 0L)
        set(value) {
            prefs.edit().putLong(KEY_BG_TIMESTAMP, value).commit()
        }

    fun getLockTimeout(): LockTimeout {
        val name = prefs.getString(KEY_LOCK_TIMEOUT, LockTimeout.FIVE_MINUTES.name)
        return LockTimeout.fromName(name)
    }

    fun setLockTimeout(timeout: LockTimeout) {
        prefs.edit().putString(KEY_LOCK_TIMEOUT, timeout.name).commit()
    }

    fun lockVaultNow() {
        if (deviceVault.hasActiveSession()) {
            _isVaultLocked.value = true
            prefs.edit().putBoolean(KEY_IS_LOCKED, true).commit()
        }
    }

    fun unlockVault() {
        _isVaultLocked.value = false
        prefs.edit()
            .putBoolean(KEY_IS_LOCKED, false)
            .putLong(KEY_BG_TIMESTAMP, 0L)
            .commit()
    }

    fun onAppBackgrounded(timestamp: Long = System.currentTimeMillis()) {
        if (!deviceVault.hasActiveSession()) return
        backgroundTimestamp = timestamp
        if (getLockTimeout() == LockTimeout.IMMEDIATE) {
            lockVaultNow()
        }
    }

    fun onAppForegrounded(timestamp: Long = System.currentTimeMillis()) {
        if (!deviceVault.hasActiveSession()) return
        if (_isVaultLocked.value) return

        val timeout = getLockTimeout()
        if (timeout == LockTimeout.NEVER) return

        val bgTs = backgroundTimestamp
        if (bgTs > 0L) {
            val elapsed = timestamp - bgTs
            if (elapsed >= timeout.millis) {
                lockVaultNow()
            }
        }
        backgroundTimestamp = 0L
    }

    companion object {
        private const val KEY_LOCK_TIMEOUT = "lock_timeout_mode"
        private const val KEY_IS_LOCKED = "is_vault_locked"
        private const val KEY_BG_TIMESTAMP = "background_timestamp"
    }
}
