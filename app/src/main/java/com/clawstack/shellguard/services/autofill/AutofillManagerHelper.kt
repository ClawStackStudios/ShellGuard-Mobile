package com.clawstack.shellguard.services.autofill

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.autofill.AutofillManager

/**
 * System integration helper for Android Autofill status inspection and settings navigation.
 *
 * Provides checks for whether ShellGuard is active as the OS autofill provider,
 * and launches the system prompt (Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
 * to select ShellGuard directly.
 */
object AutofillManagerHelper {

    /**
     * Checks if the device supports the Android Autofill Framework (API 26+)
     */
    fun isAutofillSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = context.getSystemService(AutofillManager::class.java) ?: return false
        return manager.isAutofillSupported
    }

    /**
     * Checks if ShellGuard is currently selected as the active autofill provider in Android Settings.
     */
    fun isAutofillServiceEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = context.getSystemService(AutofillManager::class.java) ?: return false
        return manager.isAutofillSupported && manager.hasEnabledAutofillServices()
    }

    /**
     * Creates an Intent to prompt the user to select ShellGuard as their preferred Autofill provider.
     * Launches the system confirmation dialog or directs to the Autofill service picker.
     */
    fun createSetAutofillServiceIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /**
     * Creates an Intent to open the general system autofill / password settings.
     */
    fun createOpenAutofillSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }
}
