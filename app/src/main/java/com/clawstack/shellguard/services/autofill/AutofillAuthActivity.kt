package com.clawstack.shellguard.services.autofill

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.autofill.Dataset
import android.util.Log
import android.view.WindowManager
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.clawstack.shellguard.BuildConfig
import com.clawstack.shellguard.ShellGuardApp
import com.clawstack.shellguard.crypto.AndroidKeyStoreHelper
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.engine.TotpEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Transparent authorization gate for Android Autofill.
 *
 * When an autofill item requires authentication (Claw Re-Prompt or locked vault),
 * the Android Autofill framework invokes this Activity.
 *
 * Extends FragmentActivity to host androidx.biometric.BiometricPrompt.
 * Upon successful authentication, it decrypts the requested Pearl's credentials,
 * auto-copies the TOTP verification code to the sensitive clipboard (Bitwarden parity),
 * attaches the Dataset to AutofillManager.EXTRA_AUTHENTICATION_RESULT, and finishes.
 */
class AutofillAuthActivity : FragmentActivity() {

    private val activityScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enforce FLAG_SECURE on release builds
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        val pearlId = intent.getStringExtra(EXTRA_PEARL_ID)
        val usernameId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_USERNAME_ID, AutofillId::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_USERNAME_ID)
        }

        val passwordId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PASSWORD_ID, AutofillId::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PASSWORD_ID)
        }

        val app = application as? ShellGuardApp
        val deviceVault = app?.appContainer?.deviceVault
        val isLocked = deviceVault?.getInMemoryShellKey() == null

        if (isLocked) {
            // Vault is completely locked/zeroized. Biometric auth won't help us decrypt anything.
            // Route directly to MainActivity to prompt full ClawKey login.
            val openIntent = Intent(this, com.clawstack.shellguard.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(openIntent)
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        if (pearlId.isNullOrBlank()) {
            promptGeneralUnlockAuthentication()
            return
        }

        promptBiometricAuthentication(pearlId, usernameId, passwordId)
    }

    private fun promptGeneralUnlockAuthentication() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val openIntent = Intent(this@AutofillAuthActivity, com.clawstack.shellguard.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(openIntent)
                setResult(Activity.RESULT_OK)
                finish()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                setResult(Activity.RESULT_CANCELED)
                finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock ShellGuard")
            .setSubtitle("Confirm identity to open vault")
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun promptBiometricAuthentication(
        pearlId: String,
        usernameId: AutofillId?,
        passwordId: AutofillId?
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                fulfillAutofillDataset(pearlId, usernameId, passwordId)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.w("AutofillAuthActivity", "Biometric error: $errString ($errorCode)")
                setResult(Activity.RESULT_CANCELED)
                finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Transient fail, user can retry
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock ShellGuard")
            .setSubtitle("Confirm identity to autofill credentials")
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun fulfillAutofillDataset(
        pearlId: String,
        usernameId: AutofillId?,
        passwordId: AutofillId?
    ) {
        val app = application as? ShellGuardApp ?: run {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val container = app.appContainer
        val deviceVault = container.deviceVault
        val database = container.database
        val cryptoEngine = container.cryptoEngine

        activityScope.launch {
            val ownerUuid = deviceVault.getOwnerUuid()
            val shellKey = deviceVault.getInMemoryShellKey()

            if (ownerUuid == null || shellKey == null) {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@launch
            }

            val pearl = withContext(Dispatchers.IO) {
                database.vaultPearlDao().getById(ownerUuid, pearlId)
            }

            if (pearl == null) {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@launch
            }

            val decryptedPassword = withContext(Dispatchers.IO) {
                try {
                    cryptoEngine.decryptField(
                        pearl.secret,
                        shellKey,
                        ShellCryptionEngine.AadNamespace.pearlSecret(pearlId)
                    )
                } catch (e: Exception) {
                    Log.e("AutofillAuthActivity", "Decryption failed for pearl $pearlId; failing closed", e)
                    null
                }
            }

            if (decryptedPassword == null) {
                // Fail CLOSED: Do NOT emit raw ciphertext into user form
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@launch
            }

            // Bitwarden Parity: Auto-Copy TOTP to Sensitive Clipboard if present
            if (pearl.totpSecret.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    try {
                        val plainTotpSecret = cryptoEngine.decryptField(
                            pearl.totpSecret,
                            shellKey,
                            ShellCryptionEngine.AadNamespace.pearlTotp(pearlId)
                        )
                        val totpCode = TotpEngine.generateTotp(plainTotpSecret)
                        copyTotpToClipboard(totpCode)
                    } catch (_: Exception) {}
                }
            }

            // Build completed Dataset
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val datasetBuilder = Dataset.Builder()

                if (usernameId != null && pearl.username.isNotBlank()) {
                    datasetBuilder.setValue(usernameId, AutofillValue.forText(pearl.username))
                }

                if (passwordId != null) {
                    datasetBuilder.setValue(passwordId, AutofillValue.forText(decryptedPassword))
                }

                val replyIntent = Intent().apply {
                    putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, datasetBuilder.build())
                }
                setResult(Activity.RESULT_OK, replyIntent)
            } else {
                setResult(Activity.RESULT_OK)
            }

            finish()
        }
    }

    private fun copyTotpToClipboard(code: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText("TOTP Code", code).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                description.extras = android.os.PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
        }
        clipboard.setPrimaryClip(clip)

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Verification code copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // Schedule 30-second automated clipboard scrub
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                if (clipboard.hasPrimaryClip()) {
                    val currentClip = clipboard.primaryClip
                    if (currentClip != null && currentClip.itemCount > 0) {
                        val text = currentClip.getItemAt(0).text?.toString()
                        if (text == code) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                clipboard.clearPrimaryClip()
                            } else {
                                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }, 30_000L)
    }

    companion object {
        const val EXTRA_PEARL_ID = "extra_pearl_id"
        const val EXTRA_USERNAME_ID = "extra_username_id"
        const val EXTRA_PASSWORD_ID = "extra_password_id"
    }
}
