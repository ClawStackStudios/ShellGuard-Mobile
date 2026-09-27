package com.clawstack.shellguard.services.autofill

import android.app.assist.AssistStructure
import android.os.Build
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId
import androidx.annotation.RequiresApi
import java.util.Locale

data class ParsedAutofillFields(
    var usernameId: AutofillId? = null,
    var passwordId: AutofillId? = null,
    var webDomain: String? = null,
    var packageName: String? = null
)

/**
 * Traverses an AssistStructure view hierarchy to identify credential input fields,
 * target web domains, and package names.
 *
 * Implements a 4-tier detection heuristic:
 * 1. Standard Autofill Hints (AUTOFILL_HINT_USERNAME, AUTOFILL_HINT_PASSWORD, etc.)
 * 2. HTML Input Attributes (WebViews / Chrome / Firefox)
 * 3. Android Input Type Variations (TYPE_TEXT_VARIATION_PASSWORD, etc.)
 * 4. Resource ID, Hint & Content Description Heuristics
 *
 * Enforces a maximum recursion depth limit (64) to prevent StackOverflowError
 * on deeply nested web DOMs.
 */
@RequiresApi(Build.VERSION_CODES.O)
object AutofillStructureParser {

    private const val MAX_DEPTH = 64

    fun parse(structure: AssistStructure): ParsedAutofillFields {
        val result = ParsedAutofillFields()
        val nodeCount = structure.windowNodeCount

        for (i in 0 until nodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            traverseNode(windowNode.rootViewNode, result, 0)
        }

        return result
    }

    private fun traverseNode(
        node: AssistStructure.ViewNode?,
        result: ParsedAutofillFields,
        depth: Int
    ) {
        if (node == null || depth > MAX_DEPTH) return
        if (node.visibility != View.VISIBLE) return

        // Extract target web domain from browser view node
        node.webDomain?.let { domain ->
            if (result.webDomain == null && domain.isNotBlank()) {
                result.webDomain = domain
            }
        }

        // Extract package name from view node
        node.idPackage?.let { pkg ->
            if (result.packageName == null && pkg.isNotBlank()) {
                result.packageName = pkg
            }
        }

        val autofillId = node.autofillId

        if (autofillId != null) {
            // Rank 1: Standard Android Autofill Hints
            val hints = node.autofillHints
            if (hints != null) {
                for (hint in hints) {
                    when (hint.lowercase(Locale.ROOT)) {
                        View.AUTOFILL_HINT_USERNAME,
                        View.AUTOFILL_HINT_EMAIL_ADDRESS,
                        "email",
                        "username" -> {
                            if (result.usernameId == null) result.usernameId = autofillId
                        }
                        View.AUTOFILL_HINT_PASSWORD,
                        "password",
                        "current-password",
                        "new-password" -> {
                            if (result.passwordId == null) result.passwordId = autofillId
                        }
                    }
                }
            }

            // Rank 2: HTML Info Attributes (for browsers and WebViews)
            val htmlInfo = node.htmlInfo
            if (htmlInfo != null) {
                val tag = htmlInfo.tag?.lowercase(Locale.ROOT)
                if (tag == "input") {
                    val attributes = htmlInfo.attributes
                    if (attributes != null) {
                        for (pair in attributes) {
                            val attrName = pair.first.lowercase(Locale.ROOT)
                            val attrVal = pair.second.lowercase(Locale.ROOT)

                            if (attrName == "type" && (attrVal == "password")) {
                                if (result.passwordId == null) result.passwordId = autofillId
                            } else if (attrName == "autocomplete") {
                                if (attrVal.contains("password") && result.passwordId == null) {
                                    result.passwordId = autofillId
                                } else if ((attrVal.contains("username") || attrVal.contains("email")) && result.usernameId == null) {
                                    result.usernameId = autofillId
                                }
                            }
                        }
                    }
                }
            }

            // Rank 3: Android Input Type Variations
            val inputType = node.inputType
            val isPasswordType = (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                    (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

            if (isPasswordType && result.passwordId == null) {
                result.passwordId = autofillId
            }

            // Rank 4: Heuristic Matching on View ID, Hint, or Content Description
            val idEntry = node.idEntry?.lowercase(Locale.ROOT) ?: ""
            val hintText = node.hint?.toString()?.lowercase(Locale.ROOT) ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase(Locale.ROOT) ?: ""

            if (result.passwordId == null && isPasswordHeuristic(idEntry, hintText, contentDesc)) {
                result.passwordId = autofillId
            } else if (result.usernameId == null && isUsernameHeuristic(idEntry, hintText, contentDesc)) {
                result.usernameId = autofillId
            }
        }

        // Recursively traverse child nodes
        val childCount = node.childCount
        for (i in 0 until childCount) {
            traverseNode(node.getChildAt(i), result, depth + 1)
        }
    }

    private fun isPasswordHeuristic(id: String, hint: String, desc: String): Boolean {
        return id.contains("password") || id.contains("pwd") || id.contains("passcode") ||
                hint.contains("password") || hint.contains("pwd") ||
                desc.contains("password") || desc.contains("pwd")
    }

    private fun isUsernameHeuristic(id: String, hint: String, desc: String): Boolean {
        return id.contains("username") || id.contains("login") || id.contains("email") || id.contains("user_id") ||
                hint.contains("username") || hint.contains("email") || hint.contains("user") ||
                desc.contains("username") || desc.contains("email")
    }
}
