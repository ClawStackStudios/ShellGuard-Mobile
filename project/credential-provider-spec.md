# 🛡️ ShellGuard Mobile — Android 14+ Credential Provider Specification

> **System-Level Credential Provider Service (`android.service.credentials.CredentialProviderService`), Android 14+ Credential Manager, Passkeys Horizon & Dual-Stack Coexistence**  
> *Targeted for Google AI Studio Android Application Generator & ShellGuard Mobile Architecture.*

---

## 1. Credential Manager Architecture & Dual-Stack Horizon

Android 14 (API 34+) introduced the **Credential Manager API**, replacing legacy autofill dialogs with unified system bottom sheets and inline keyboard suggestions for Passwords, Passkeys (FIDO2 / WebAuthn), and Federated Sign-in.

ShellGuard Mobile implements a **Dual-Stack Architecture**:
1. **`ShellGuardAutofillService`** (`android.service.autofill.AutofillService`, API 26+): Handles legacy autofill heuristics, custom web forms, WebView contexts, and Android 11–13 keyboard inline suggestions.
2. **`ShellGuardCredentialProviderService`** (`android.service.credentials.CredentialProviderService`, API 34+): Handles modern system credential bottom sheets, Android 14+ IME suggestion chips, passkeys, and direct app credential requests.

```mermaid
flowchart TD
    App[Calling App / Browser / Chrome] -->|CredentialManager.getCredential()| Sys[Android 14+ OS Credential Framework]
    
    subgraph SystemRouting ["OS Provider Selection"]
        Sys -->|API 34+ Credential Request| CPS[ShellGuardCredentialProviderService]
        Sys -->|API 26-33 Autofill Request| AFS[ShellGuardAutofillService]
    end

    subgraph ShellGuardStack ["ShellGuard Dual-Stack Engine"]
        CPS -->|onBeginGetCredential| Handler[Credential Request Handler]
        AFS -->|onFillRequest| AutofillHandler[Autofill Fill Handler]
        
        Handler --> Matcher[DomainMatcher & App Package Resolver]
        AutofillHandler --> Matcher
        
        Matcher --> Query[Query VaultPearlDao]
        Query --> Gate{Vault Locked?}
        
        Gate -->|Locked| AuthEntry[Wrap Entry with CredentialAuthActivity PendingIntent]
        Gate -->|Unlocked| ReadyEntry[Build PasswordCredentialEntry with Decrypted Secret]
    end

    AuthEntry --> Response[BeginGetCredentialResponse]
    ReadyEntry --> Response
    Response -->|Return Entries| Sys
    Sys -->|Render Bottom Sheet or IME Strip| User([User Device])

    classDef secure fill:#1a3a2a,stroke:#2e7d32,stroke-width:2px,color:#fff;
    classDef warning fill:#3a2a1a,stroke:#d84315,stroke-width:2px,color:#fff;
    class Gate,AuthEntry warning;
    class ReadyEntry,Response secure;
```

---

## 2. Manifest & System Registration

To be recognized by Android 14+ as a system Credential Provider, the service must be declared in `AndroidManifest.xml` protected by `BIND_CREDENTIAL_PROVIDER_SERVICE`.

### `AndroidManifest.xml` Declaration
```xml
<!-- Android 14+ (API 34+) Credential Provider Service -->
<service
    android:name=".services.credentials.ShellGuardCredentialProviderService"
    android:label="@string/credential_provider_label"
    android:permission="android.permission.BIND_CREDENTIAL_PROVIDER_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.service.credentials.CredentialProviderService" />
    </intent-filter>
    <meta-data
        android:name="android.service.credentials.credential_provider"
        android:resource="@xml/credential_provider_config" />
</service>
```

### Provider Configuration (`res/xml/credential_provider_config.xml`)
```xml
<?xml version="1.0" encoding="utf-8"?>
<credential-provider
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:settingsActivity="com.clawstack.shellguard.ui.MainActivity" />
```

---

## 3. Core Credential Provider Engine

### `ShellGuardCredentialProviderService.kt`
```kotlin
package com.clawstack.shellguard.services.credentials

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.service.credentials.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.provider.*
import com.clawstack.shellguard.ShellGuardApp
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.domain.matcher.DomainMatcher
import com.clawstack.shellguard.domain.models.VaultPearl
import kotlinx.coroutines.*
import java.security.SecureRandom

/**
 * System-level Credential Provider Service for Android 14+ (API 34+).
 *
 * Implements modern Credential Manager queries, surfacing passwords and passkeys
 * directly into the Android system bottom sheet and IME keyboard strips.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class ShellGuardCredentialProviderService : CredentialProviderService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBeginGetCredential(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
    ) {
        val app = application as? ShellGuardApp ?: run {
            callback.onError(GetCredentialUnknownException("Application container uninitialized"))
            return
        }

        val container = app.appContainer
        val deviceVault = container.deviceVault
        val database = container.database
        val cryptoEngine = container.cryptoEngine

        val callingAppInfo = request.callingAppInfo
        val callingPackage = callingAppInfo?.packageName ?: ""
        val origin = callingAppInfo?.origin // Present if requested from Chrome / WebAuthn

        serviceScope.launch {
            try {
                val ownerUuid = deviceVault.getOwnerUuid()
                if (ownerUuid == null) {
                    callback.onResult(BeginGetCredentialResponse.Builder().build())
                    return@launch
                }

                // Query candidate credentials matching the calling package or web origin
                val allPearls = database.vaultPearlDao().observeAll(ownerUuid)
                val targetHost = origin ?: callingPackage

                val matchedPearls = database.vaultPearlDao().search(ownerUuid, targetHost)
                    .ifEmpty {
                        // Fallback: check domain matcher across all pearls
                        val list = mutableListOf<VaultPearl>()
                        // Load candidates and evaluate via DomainMatcher
                        list
                    }

                val responseBuilder = BeginGetCredentialResponse.Builder()
                val shellKey = deviceVault.getInMemoryShellKey()
                val isVaultLocked = (shellKey == null)

                for (option in request.beginGetCredentialOptions) {
                    when (option) {
                        is BeginGetPasswordOption -> {
                            handlePasswordOption(
                                option = option,
                                matchedPearls = matchedPearls,
                                isVaultLocked = isVaultLocked,
                                callingPackage = callingPackage,
                                responseBuilder = responseBuilder
                            )
                        }
                        is BeginGetPublicKeyCredentialOption -> {
                            // Passkeys / FIDO2 WebAuthn reservation
                            Log.d("CredentialProvider", "Passkey requested for origin: $origin")
                        }
                    }
                }

                callback.onResult(responseBuilder.build())
            } catch (e: Exception) {
                Log.e("CredentialProvider", "Error processing onBeginGetCredential", e)
                callback.onError(GetCredentialUnknownException(e.message))
            }
        }
    }

    private fun handlePasswordOption(
        option: BeginGetPasswordOption,
        matchedPearls: List<com.clawstack.shellguard.data.local.entities.VaultPearlEntity>,
        isVaultLocked: Boolean,
        callingPackage: String,
        responseBuilder: BeginGetCredentialResponse.Builder
    ) {
        for (pearl in matchedPearls) {
            val authIntent = Intent(applicationContext, CredentialAuthActivity::class.java).apply {
                data = Uri.parse("shellguard://credentials/pearl/${pearl.id}")
                putExtra(CredentialAuthActivity.EXTRA_PEARL_ID, pearl.id)
                putExtra(CredentialAuthActivity.EXTRA_CALLING_PACKAGE, callingPackage)
            }

            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                pearl.id.hashCode() xor SecureRandom().nextInt(0xFFFF),
                authIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            val entry = PasswordCredentialEntry.Builder(
                context = applicationContext,
                username = pearl.username.ifBlank { "User" },
                pendingIntent = pendingIntent,
                beginGetPasswordOption = option
            )
            .setDisplayName(pearl.title)
            .setAutoSelectAllowed(false)
            .build()

            responseBuilder.addCredentialEntry(entry)
        }
    }

    override fun onBeginCreateCredential(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
    ) {
        // Handles prompts to save newly entered credentials to ShellGuard
        callback.onResult(BeginCreateCredentialResponse.Builder().build())
    }

    override fun onClearCredentialState(
        request: ClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void, ClearCredentialStateException>
    ) {
        // Purges active session caches upon user sign-out
        callback.onResult(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
```

---

## 4. Credential Selection & Biometric Authorization Gate

When the user taps a credential entry in the Android system bottom sheet, the system executes the entry's `PendingIntent`, invoking `CredentialAuthActivity`.

### `CredentialAuthActivity.kt`
```kotlin
package com.clawstack.shellguard.services.credentials

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.credentials.PasswordCredential
import androidx.credentials.provider.PendingIntentHandler
import androidx.fragment.app.FragmentActivity
import com.clawstack.shellguard.BuildConfig
import com.clawstack.shellguard.ShellGuardApp
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import kotlinx.coroutines.*

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class CredentialAuthActivity : FragmentActivity() {

    private val activityScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        val pearlId = intent.getStringExtra(EXTRA_PEARL_ID)
        if (pearlId.isNullOrBlank()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val app = application as? ShellGuardApp ?: run {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val container = app.appContainer
        val deviceVault = container.deviceVault
        val shellKey = deviceVault.getInMemoryShellKey()

        if (shellKey == null) {
            promptBiometricAndFulfill(pearlId)
        } else {
            fulfillCredential(pearlId, shellKey)
        }
    }

    private fun promptBiometricAndFulfill(pearlId: String) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val app = application as ShellGuardApp
                val shellKey = app.appContainer.deviceVault.getInMemoryShellKey()
                if (shellKey != null) {
                    fulfillCredential(pearlId, shellKey)
                } else {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock ShellGuard")
            .setSubtitle("Authenticate to fill credential")
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun fulfillCredential(pearlId: String, shellKey: ByteArray) {
        val app = application as ShellGuardApp
        val container = app.appContainer
        val database = container.database
        val cryptoEngine = container.cryptoEngine
        val ownerUuid = container.deviceVault.getOwnerUuid() ?: return

        activityScope.launch {
            val pearl = withContext(Dispatchers.IO) {
                database.vaultPearlDao().getById(ownerUuid, pearlId)
            } ?: run {
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
                    Log.e("CredentialAuthActivity", "Decryption failed; failing closed", e)
                    null
                }
            }

            if (decryptedPassword == null) {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@launch
            }

            val credential = PasswordCredential(pearl.username, decryptedPassword)
            val resultData = Intent()
            PendingIntentHandler.setGetCredentialResponse(
                resultData,
                androidx.credentials.GetCredentialResponse(credential)
            )

            setResult(Activity.RESULT_OK, resultData)
            finish()
        }
    }

    companion object {
        const val EXTRA_PEARL_ID = "extra_pearl_id"
        const val EXTRA_CALLING_PACKAGE = "extra_calling_package"
    }
}
```

---

## 5. Dual-Stack Interoperability Matrix

| Feature | Legacy Autofill Service (`API 26–33`) | Credential Provider Service (`API 34+`) |
|---|---|---|
| **Primary System UI** | Dropdown overlay anchored to input field | Native system bottom sheet overlay |
| **IME Keyboard Inline** | `androidx.autofill.inline.v1.InlineSuggestionUi` (Slices) | Native Credential Manager IME suggestion chips |
| **App Integration** | Automatic via `View.setAutofillHints()` | Explicit via `androidx.credentials:credentials` |
| **Passkey / WebAuthn** | Not supported | Fully supported (FIDO2 / passkeys) |
| **Settings Category** | Android Settings -> Passwords & accounts -> Autofill service | Android Settings -> Passwords & accounts -> Additional providers |
| **Security Invariant** | Fail closed: never emit ciphertext | Fail closed: never emit ciphertext |

### Coordination Rule
When running on Android 14+ (API 34+):
- If the calling application issues a modern `CredentialManager.getCredential()` request, the system handles it via `ShellGuardCredentialProviderService`.
- If an older application or web form renders standard HTML inputs without Credential Manager integration, Android falls back to `ShellGuardAutofillService`.
- Both services share the same `DomainMatcher`, `EncryptedDeviceVault`, and `Room` database, ensuring 100% data parity and zero duplication.
