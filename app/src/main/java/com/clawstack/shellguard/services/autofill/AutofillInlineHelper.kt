package com.clawstack.shellguard.services.autofill

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.service.autofill.InlinePresentation
import android.util.Size
import android.view.View
import android.view.autofill.AutofillId
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi

/**
 * Helper to construct Android 11+ (API 30+) Keyboard Inline Suggestion chips
 * for Gboard, SwiftKey, and modern IME keyboards.
 *
 * All invocations are guarded by Build.VERSION.SDK_INT >= Build.VERSION_CODES.R.
 */
@RequiresApi(Build.VERSION_CODES.R)
object AutofillInlineHelper {

    fun createInlinePresentation(
        spec: InlinePresentationSpec,
        pinned: Boolean = false
    ): InlinePresentation {
        val slice = android.app.slice.Slice.Builder(
            android.net.Uri.parse("content://com.clawstack.shellguard.autofill/inline"),
            android.app.slice.SliceSpec("inline_suggestion", 1)
        ).build()

        return InlinePresentation(slice, spec, pinned)
    }
}
