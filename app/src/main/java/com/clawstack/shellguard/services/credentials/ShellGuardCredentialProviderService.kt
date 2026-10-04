package com.clawstack.shellguard.services.credentials

import android.app.PendingIntent
import android.app.slice.Slice
import android.app.slice.SliceSpec
import android.content.Intent
import android.credentials.ClearCredentialStateException
import android.credentials.CreateCredentialException
import android.credentials.GetCredentialException
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.service.credentials.Action
import android.service.credentials.BeginCreateCredentialRequest
import android.service.credentials.BeginCreateCredentialResponse
import android.service.credentials.BeginGetCredentialOption
import android.service.credentials.BeginGetCredentialRequest
import android.service.credentials.BeginGetCredentialResponse
import android.service.credentials.ClearCredentialStateRequest
import android.service.credentials.CredentialEntry
import android.service.credentials.CredentialProviderService
import android.util.Log
import androidx.annotation.RequiresApi
import com.clawstack.shellguard.ShellGuardApp
import com.clawstack.shellguard.domain.matcher.DomainMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android 14+ (API 34+) Credential Provider Service.
 *
 * Implements the system Credential Manager provider interface, allowing ShellGuard
 * to appear in Android Settings ("Preferred service" / "Passwords, passkeys & autofill")
 * and surface credentials directly into system bottom sheets and IME keyboard strips.
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
            callback.onError(GetCredentialException(GetCredentialException.TYPE_UNKNOWN, "Application container uninitialized"))
            return
        }

        val container = app.appContainer
        val deviceVault = container.deviceVault
        val database = container.database

        val callingAppInfo = request.callingAppInfo
        val callingPackage = callingAppInfo?.packageName ?: ""
        val origin = callingAppInfo?.origin // Present if requested from Chrome / Web

        serviceScope.launch {
            try {
                if (cancellationSignal.isCanceled) return@launch

                val ownerUuid = deviceVault.getOwnerUuid()
                if (ownerUuid.isNullOrBlank()) {
                    callback.onResult(BeginGetCredentialResponse.Builder().build())
                    return@launch
                }

                val allPearls = database.vaultPearlDao().getAllActivePearls(ownerUuid)
                val target = origin?.ifBlank { null } ?: "androidapp://$callingPackage"

                val matchedPearls = allPearls.filter { pearl ->
                    val primaryMatch = pearl.url.isNotBlank() && DomainMatcher.isMatch(pearl.url, target)
                    val packageMatch = callingPackage.isNotBlank() && DomainMatcher.isMatch(pearl.url, "androidapp://$callingPackage")
                    primaryMatch || packageMatch
                }.take(5)

                val responseBuilder = BeginGetCredentialResponse.Builder()
                val shellKey = deviceVault.getInMemoryShellKey()
                val isVaultLocked = (shellKey == null)

                // 1. If matching pearls exist, populate CredentialEntry items
                for (pearl in matchedPearls) {
                    val authIntent = Intent(applicationContext, CredentialAuthActivity::class.java).apply {
                        data = Uri.parse("shellguard://credentials/pearl/${pearl.id}")
                        putExtra(CredentialAuthActivity.EXTRA_PEARL_ID, pearl.id)
                        putExtra(CredentialAuthActivity.EXTRA_CALLING_PACKAGE, callingPackage)
                    }

                    val pendingIntent = PendingIntent.getActivity(
                        applicationContext,
                        pearl.id.hashCode(),
                        authIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )

                    val subSlice = Slice.Builder(
                        Uri.parse("content://com.clawstack.shellguard.credentials/pearl/${pearl.id}/sub"),
                        SliceSpec("sub", 1)
                    ).build()

                    val slice = Slice.Builder(
                        Uri.parse("content://com.clawstack.shellguard.credentials/pearl/${pearl.id}"),
                        SliceSpec("credential", 1)
                    )
                        .addAction(pendingIntent, subSlice, null)
                        .addText(pearl.title, null, listOf(Slice.HINT_TITLE))
                        .addText(pearl.username.ifBlank { "Password" }, null, listOf(Slice.HINT_SUMMARY))
                        .build()

                    val entry = CredentialEntry("android.credentials.TYPE_PASSWORD_CREDENTIAL", slice)
                    responseBuilder.addCredentialEntry(entry)
                }

                // 2. If locked or no matches found, provide an Authentication / Action entry to unlock ShellGuard
                if (isVaultLocked || matchedPearls.isEmpty()) {
                    val unlockIntent = Intent(applicationContext, CredentialAuthActivity::class.java).apply {
                        data = Uri.parse("shellguard://credentials/unlock")
                        putExtra(CredentialAuthActivity.EXTRA_CALLING_PACKAGE, callingPackage)
                    }

                    val unlockPendingIntent = PendingIntent.getActivity(
                        applicationContext,
                        1001,
                        unlockIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )

                    val unlockSubSlice = Slice.Builder(
                        Uri.parse("content://com.clawstack.shellguard.credentials/action/unlock/sub"),
                        SliceSpec("sub", 1)
                    ).build()

                    val unlockSlice = Slice.Builder(
                        Uri.parse("content://com.clawstack.shellguard.credentials/action/unlock"),
                        SliceSpec("action", 1)
                    )
                        .addAction(unlockPendingIntent, unlockSubSlice, null)
                        .addText("Unlock ShellGuard", null, listOf(Slice.HINT_TITLE))
                        .addText("Confirm identity to autofill credentials", null, listOf(Slice.HINT_SUMMARY))
                        .build()

                    val authAction = Action(unlockSlice)
                    responseBuilder.addAuthenticationAction(authAction)
                }

                callback.onResult(responseBuilder.build())
            } catch (e: Exception) {
                Log.e("CredentialProvider", "Error processing onBeginGetCredential", e)
                callback.onError(GetCredentialException(GetCredentialException.TYPE_UNKNOWN, e.message))
            }
        }
    }

    override fun onBeginCreateCredential(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
    ) {
        callback.onResult(BeginCreateCredentialResponse.Builder().build())
    }

    override fun onClearCredentialState(
        request: ClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void, ClearCredentialStateException>
    ) {
        callback.onResult(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
