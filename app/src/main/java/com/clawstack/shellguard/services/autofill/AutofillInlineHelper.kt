package com.clawstack.shellguard.services.autofill

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.autofill.InlinePresentation
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.v1.InlineSuggestionUi
import com.clawstack.shellguard.R
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Helper to construct Android 11+ (API 30+) Keyboard Inline Suggestion chips
 * for Gboard, SwiftKey, and modern IME keyboards, plus zero-knowledge subtitle
 * disambiguation and zero-copy Binder resource iconography.
 *
 * Utilizes the official AndroidX Autofill Inline Suggestion Slice Protocol
 * to guarantee that suggestion chips are parsed and rendered by IMEs.
 */
object AutofillInlineHelper {

    private val BROWSER_PACKAGES = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.fenix",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "com.sec.android.app.sbrowser"
    )

    @RequiresApi(Build.VERSION_CODES.R)
    fun createInlinePresentation(
        context: Context,
        spec: InlinePresentationSpec,
        title: String,
        subtitle: String = "",
        icon: Icon? = null,
        pinned: Boolean = false,
        attributionIntent: PendingIntent? = null
    ): InlinePresentation {
        val safeAttribution = attributionIntent ?: PendingIntent.getActivity(
            context,
            0,
            Intent(),
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = InlineSuggestionUi.newContentBuilder(safeAttribution)
            .setTitle(title)
            .setContentDescription(title)

        if (subtitle.isNotBlank()) {
            builder.setSubtitle(subtitle)
        }

        if (icon != null) {
            builder.setStartIcon(icon)
        }

        val slice = builder.build().slice

        return InlinePresentation(slice, spec, pinned)
    }

    /**
     * Formats a privacy-preserving subtitle for an unlocked Vault Pearl chip (Option B).
     * Never exposes the raw plaintext username on the keyboard chip:
     * - Combines non-default category or primary tag with a partially masked username hint
     *   (e.g., "Work · lu***@gmail.com" or "lu***@gmail.com").
     * - Falls back to "Password" when neither category/tag nor username is present.
     */
    fun formatUnlockedChipSubtitle(pearl: VaultPearlEntity): String {
        val badge = extractCategoryOrTag(pearl)
        val maskedUser = maskUsername(pearl.username)

        return when {
            badge != null && maskedUser.isNotBlank() -> "$badge · $maskedUser"
            badge != null -> badge
            maskedUser.isNotBlank() -> maskedUser
            else -> "Password"
        }
    }

    /**
     * Partially masks a username or email address so raw credentials are never
     * exposed to shoulder-surfing on the IME strip while remaining recognizable
     * to the owner across multiple accounts.
     */
    fun maskUsername(rawUsername: String): String {
        val trimmed = rawUsername.trim()
        if (trimmed.isEmpty()) return ""

        val atIndex = trimmed.indexOf('@')
        if (atIndex > 0 && atIndex < trimmed.length - 1) {
            val local = trimmed.substring(0, atIndex)
            val domain = trimmed.substring(atIndex + 1)
            val visiblePrefix = if (local.length <= 2) local.take(1) else local.take(2)
            return "$visiblePrefix***@$domain"
        }

        return when {
            trimmed.length <= 2 -> "${trimmed.take(1)}***"
            trimmed.length in 3..4 -> "${trimmed.take(1)}***${trimmed.last()}"
            else -> "${trimmed.take(2)}***${trimmed.last()}"
        }
    }

    /**
     * Extracts a non-default category name (excluding "General") or the first tag
     * from the Pearl's JSON tag array for visual disambiguation.
     */
    fun extractCategoryOrTag(pearl: VaultPearlEntity): String? {
        val category = pearl.category.trim()
        if (category.isNotEmpty() && !category.equals("General", ignoreCase = true)) {
            return category
        }

        val rawTags = pearl.tags.trim()
        if (rawTags.isNotEmpty() && rawTags != "[]") {
            try {
                val array = Json.parseToJsonElement(rawTags).jsonArray
                val firstTag = array.firstOrNull()?.jsonPrimitive?.content?.trim()
                if (!firstTag.isNullOrEmpty()) {
                    return firstTag
                }
            } catch (_: Exception) {
                // Ignore malformed tag JSON and fall back cleanly
            }
        }

        return null
    }

    /**
     * Resolves a lightweight, Binder-safe [Icon] for an inline autofill chip.
     * Uses zero-copy resource references ([Icon.createWithResource]) to prevent
     * Bitmap allocation overhead or TransactionTooLargeException across Binder IPC.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun resolveChipIcon(
        context: Context,
        targetPackage: String?,
        isFromWebView: Boolean,
        isLockedOrReprompt: Boolean
    ): Icon {
        if (isLockedOrReprompt) {
            return Icon.createWithResource(context, R.drawable.ic_locked_shell)
        }

        if (!isFromWebView && !targetPackage.isNullOrBlank() && targetPackage !in BROWSER_PACKAGES) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(targetPackage, 0)
                if (appInfo.icon != 0) {
                    return Icon.createWithResource(targetPackage, appInfo.icon)
                }
            } catch (_: Exception) {
                // Target package not queryable or icon resource unavailable; fall back to ShellGuard emblem
            }
        }

        return Icon.createWithResource(context, R.drawable.ic_locked_shell)
    }
}

