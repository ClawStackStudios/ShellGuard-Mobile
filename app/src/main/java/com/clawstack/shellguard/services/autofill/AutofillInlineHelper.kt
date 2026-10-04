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

/**
 * Helper to construct Android 11+ (API 30+) Keyboard Inline Suggestion chips
 * for Gboard, SwiftKey, and modern IME keyboards.
 *
 * Utilizes the official AndroidX Autofill Inline Suggestion Slice Protocol
 * to guarantee that suggestion chips are parsed and rendered by IMEs.
 */
@RequiresApi(Build.VERSION_CODES.R)
object AutofillInlineHelper {

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
}
