package com.clawstack.shellguard.services.autofill

import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.util.Log
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.clawstack.shellguard.R
import com.clawstack.shellguard.ShellGuardApp
import com.clawstack.shellguard.crypto.ShellCryptionEngine
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
import com.clawstack.shellguard.domain.matcher.DomainMatcher
import com.clawstack.shellguard.engine.TotpEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * System-level Android Autofill Service for ShellGuard Mobile.
 *
 * Implements Android Autofill Framework API 26+:
 * - Inspects incoming AssistStructure via AutofillStructureParser
 * - Matches target web domain or app package name against local Room vault items
 * - Supports Claw Re-Prompt & Locked Vault via AutofillAuthActivity IntentSender
 * - Performs Bitwarden-parity auto-copy of TOTP codes to sensitive clipboard
 * - Provides RemoteViews dropdown suggestions & Android 11+ inline chips
 */
@RequiresApi(Build.VERSION_CODES.O)
class ShellGuardAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess(null)
            return
        }

        val parsedFields = AutofillStructureParser.parse(structure)
        if (parsedFields.usernameId == null && parsedFields.passwordId == null) {
            callback.onSuccess(null)
            return
        }

        val app = application as? ShellGuardApp ?: run {
            callback.onSuccess(null)
            return
        }

        val container = app.appContainer
        val deviceVault = container.deviceVault
        val database = container.database
        val cryptoEngine = container.cryptoEngine

        serviceScope.launch {
            if (cancellationSignal.isCanceled) return@launch

            val ownerUuid = deviceVault.getOwnerUuid()
            if (ownerUuid.isNullOrBlank()) {
                callback.onSuccess(null)
                return@launch
            }

            val targetDomain = parsedFields.webDomain
            val targetPackage = parsedFields.packageName
            val target = targetDomain?.ifBlank { null } ?: targetPackage?.let { "androidapp://$it" }

            if (target.isNullOrBlank()) {
                callback.onSuccess(null)
                return@launch
            }

            val allPearls = database.vaultPearlDao().getAllActivePearls(ownerUuid)
            if (cancellationSignal.isCanceled) return@launch

            val matchedPearls = allPearls.filter { pearl ->
                val primaryMatch = pearl.url.isNotBlank() && DomainMatcher.isMatch(pearl.url, target)
                val packageMatch = targetPackage != null && DomainMatcher.isMatch(pearl.url, "androidapp://$targetPackage")
                primaryMatch || packageMatch
            }.take(5) // Limit to top 5 candidates

            if (cancellationSignal.isCanceled) return@launch

            val responseBuilder = FillResponse.Builder()
            val packageName = applicationContext.packageName

            // Configure defensive SaveInfo for password/username saving when passwordId is present
            parsedFields.passwordId?.let { passId ->
                val saveType = SaveInfo.SAVE_DATA_TYPE_PASSWORD
                val requiredIds = arrayOf(passId)
                val saveInfoBuilder = SaveInfo.Builder(saveType, requiredIds)
                parsedFields.usernameId?.let { userId ->
                    saveInfoBuilder.setOptionalIds(arrayOf(userId))
                }
                responseBuilder.setSaveInfo(saveInfoBuilder.build())
            }

            val app = applicationContext as? ShellGuardApp
            val lockManager = app?.appContainer?.vaultLockManager
            val shellKey = deviceVault.getInMemoryShellKey()
            val isVaultLocked = (shellKey == null) || (lockManager?.isVaultLocked?.value == true)

            val inlineRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                request.inlineSuggestionsRequest
            } else null

            val fillUrl = when {
                !targetDomain.isNullOrBlank() -> if (targetDomain.startsWith("http://") || targetDomain.startsWith("https://")) targetDomain else "https://$targetDomain"
                !targetPackage.isNullOrBlank() -> "androidapp://$targetPackage"
                else -> ""
            }

            // Case A: No matched items -> Show "Add Item" option chip when a confirmed password field OR
            // a high-confidence Rank 1-3 username/email field (e.g., 2-step login like accounts.google.com) is on screen.
            // (Weak Rank 4/5 username heuristics are already stripped by AutofillStructureParser's Co-Presence Gate when passwordId == null)
            if (matchedPearls.isEmpty()) {
                val userFieldId = parsedFields.usernameId
                val passFieldId = parsedFields.passwordId
                if (userFieldId != null || passFieldId != null) {
                    val fallbackDatasetBuilder = Dataset.Builder()
                    val displaySub = targetDomain ?: targetPackage ?: "ShellGuard"

                    val fallbackPresentation = RemoteViews(packageName, R.layout.autofill_suggestion_item).apply {
                        setTextViewText(R.id.autofill_title, "Add Item")
                        setTextViewText(R.id.autofill_subtitle, displaySub)
                    }

                    val fallbackInlinePresentation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && inlineRequest != null) {
                        val spec = inlineRequest.inlinePresentationSpecs.firstOrNull()
                        if (spec != null && inlineRequest.maxSuggestionCount > 0) {
                            AutofillInlineHelper.createInlinePresentation(
                                context = applicationContext,
                                spec = spec,
                                title = "Add Item",
                                subtitle = displaySub,
                                icon = Icon.createWithResource(applicationContext, R.drawable.ic_locked_shell)
                            )
                        } else null
                    } else null

                    val addItemIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("shellguard://app/form/NEW/PASSWORD/new?url=${Uri.encode(fillUrl)}")
                    ).apply {
                        setPackage(packageName)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }

                    val intentSender = PendingIntent.getActivity(
                        applicationContext,
                        8888,
                        addItemIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    ).intentSender

                    fallbackDatasetBuilder.setAuthentication(intentSender)

                    // Bind usernameId (if present) and passwordId (if present) so focusing either input shows "Add Item"
                    if (userFieldId != null) {
                        if (fallbackInlinePresentation != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            fallbackDatasetBuilder.setValue(
                                userFieldId,
                                null,
                                fallbackPresentation,
                                fallbackInlinePresentation
                            )
                        } else {
                            fallbackDatasetBuilder.setValue(
                                userFieldId,
                                null,
                                fallbackPresentation
                            )
                        }
                    }

                    if (passFieldId != null) {
                        if (fallbackInlinePresentation != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            fallbackDatasetBuilder.setValue(
                                passFieldId,
                                null,
                                fallbackPresentation,
                                fallbackInlinePresentation
                            )
                        } else {
                            fallbackDatasetBuilder.setValue(
                                passFieldId,
                                null,
                                fallbackPresentation
                            )
                        }
                    }

                    responseBuilder.addDataset(fallbackDatasetBuilder.build())
                    callback.onSuccess(responseBuilder.build())
                } else {
                    callback.onSuccess(null)
                }
                return@launch
            }

            // Case B: Matches found -> Display matched URI / credentials inline
            var datasetAdded = false
            var datasetIndex = 0
            for (pearl in matchedPearls) {
                // If this screen has NO password field (username-only step 1) and this pearl has no username, skip it
                if (parsedFields.passwordId == null && pearl.username.isBlank()) {
                    continue
                }

                val datasetBuilder = Dataset.Builder()

                // If vault is locked, display the matched URI string inline so the user sees site parity without leaking secret titles
                val chipTitle = if (isVaultLocked) {
                    val uriCandidate = pearl.url.ifBlank { targetDomain ?: targetPackage ?: "ShellGuard" }
                    uriCandidate
                        .removePrefix("https://")
                        .removePrefix("http://")
                        .removePrefix("androidapp://")
                        .substringBefore("/")
                } else {
                    pearl.title.ifBlank { targetDomain ?: targetPackage ?: "ShellGuard" }
                }

                val chipSubtitle = if (isVaultLocked || pearl.reprompt) {
                    "Unlock Vault"
                } else {
                    AutofillInlineHelper.formatUnlockedChipSubtitle(pearl)
                }

                val chipIcon = AutofillInlineHelper.resolveChipIcon(
                    context = applicationContext,
                    targetPackage = targetPackage,
                    isFromWebView = parsedFields.isFromWebView,
                    isLockedOrReprompt = isVaultLocked || pearl.reprompt
                )

                // RemoteViews Dropdown Presentation
                val presentation = RemoteViews(packageName, R.layout.autofill_suggestion_item).apply {
                    setTextViewText(R.id.autofill_title, chipTitle)
                    setTextViewText(R.id.autofill_subtitle, chipSubtitle)
                }

                // Android 11+ (API 30+) Keyboard Inline Suggestion Chip
                val inlinePresentation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && inlineRequest != null) {
                    val specs = inlineRequest.inlinePresentationSpecs
                    if (specs.isNotEmpty() && datasetIndex < inlineRequest.maxSuggestionCount) {
                        val spec = specs.getOrNull(datasetIndex) ?: specs.last()
                        AutofillInlineHelper.createInlinePresentation(
                            context = applicationContext,
                            spec = spec,
                            title = chipTitle,
                            subtitle = chipSubtitle,
                            icon = chipIcon
                        )
                    } else null
                } else null

                var fieldBound = false

                // If vault is locked or Claw Re-Prompt is enabled, require authentication
                if (isVaultLocked || pearl.reprompt) {
                    val authIntent = Intent(applicationContext, AutofillAuthActivity::class.java).apply {
                        data = Uri.parse("shellguard://autofill/pearl/${pearl.id}")
                        putExtra(AutofillAuthActivity.EXTRA_PEARL_ID, pearl.id)
                        putExtra(AutofillAuthActivity.EXTRA_USERNAME_ID, parsedFields.usernameId)
                        putExtra(AutofillAuthActivity.EXTRA_PASSWORD_ID, parsedFields.passwordId)
                    }
                    val intentSender = PendingIntent.getActivity(
                        applicationContext,
                        pearl.id.hashCode(),
                        authIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    ).intentSender

                    datasetBuilder.setAuthentication(intentSender)

                    if (parsedFields.usernameId != null && pearl.username.isNotBlank()) {
                        val userFieldId = parsedFields.usernameId!!
                        if (inlinePresentation != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            datasetBuilder.setValue(
                                userFieldId,
                                null,
                                presentation,
                                inlinePresentation
                            )
                        } else {
                            datasetBuilder.setValue(
                                userFieldId,
                                null,
                                presentation
                            )
                        }
                        fieldBound = true
                    }

                    parsedFields.passwordId?.let { passFieldId ->
                        if (inlinePresentation != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            datasetBuilder.setValue(
                                passFieldId,
                                null,
                                presentation,
                                inlinePresentation
                            )
                        } else {
                            datasetBuilder.setValue(
                                passFieldId,
                                null,
                                presentation
                            )
                        }
                        fieldBound = true
                    }
                } else {
                    // Decrypt credentials directly in memory (Fail CLOSED on decryption error)
                    val decryptedPassword = try {
                        cryptoEngine.decryptField(
                            pearl.secret,
                            shellKey!!,
                            ShellCryptionEngine.AadNamespace.pearlSecret(pearl.id)
                        )
                    } catch (e: Exception) {
                        Log.e("AutofillService", "Decryption failed for pearl ${pearl.id}; failing closed", e)
                        null
                    }

                    if (decryptedPassword == null) {
                        // Fail closed: Do NOT emit raw ciphertext to third-party forms
                        continue
                    }

                    // Auto-copy TOTP if present (Bitwarden parity)
                    if (pearl.totpSecret.isNotBlank()) {
                        try {
                            val plainTotpSecret = cryptoEngine.decryptField(
                                pearl.totpSecret,
                                shellKey!!,
                                ShellCryptionEngine.AadNamespace.pearlTotp(pearl.id)
                            )
                            val totpCode = TotpEngine.generateTotp(plainTotpSecret)
                            copyTotpToClipboard(totpCode)
                        } catch (_: Exception) {}
                    }

                    // Blast-Radius Guard: Only bind usernameId when pearl.username is non-blank
                    // so we never overwrite a user's manually typed username with an empty string "".
                    if (parsedFields.usernameId != null && pearl.username.isNotBlank()) {
                        val userFieldId = parsedFields.usernameId!!
                        if (inlinePresentation != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            datasetBuilder.setValue(
                                userFieldId,
                                AutofillValue.forText(pearl.username),
                                presentation,
                                inlinePresentation
                            )
                        } else {
                            datasetBuilder.setValue(
                                userFieldId,
                                AutofillValue.forText(pearl.username),
                                presentation
                            )
                        }
                        fieldBound = true
                    }

                    parsedFields.passwordId?.let { passFieldId ->
                        if (inlinePresentation != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            datasetBuilder.setValue(
                                passFieldId,
                                AutofillValue.forText(decryptedPassword),
                                presentation,
                                inlinePresentation
                            )
                        } else {
                            datasetBuilder.setValue(
                                passFieldId,
                                AutofillValue.forText(decryptedPassword),
                                presentation
                            )
                        }
                        fieldBound = true
                    }
                }

                if (fieldBound) {
                    responseBuilder.addDataset(datasetBuilder.build())
                    datasetAdded = true
                    datasetIndex++
                }
            }

            if (!datasetAdded) {
                callback.onSuccess(null)
            } else {
                callback.onSuccess(responseBuilder.build())
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
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

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
