package com.clawstack.shellguard.crypto

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthManager {

    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun hasBiometricEnrolled(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock ShellGuard Vault",
        subtitle: String = "Authenticate to access your encrypted credentials",
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .apply {
                if (cryptoObject == null) {
                    // When no CryptoObject is passed, we can allow device credentials (PIN/Pattern/Password)
                    setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    setNegativeButtonText("Use Password / PIN")
                }
            }
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        })

        if (cryptoObject != null) {
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}
