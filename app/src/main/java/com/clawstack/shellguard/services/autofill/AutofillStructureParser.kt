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
    var packageName: String? = null,
    var isFromWebView: Boolean = false
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
 * Enforces:
 * - A maximum recursion depth limit (64) to prevent StackOverflowError on deeply nested DOMs.
 * - AutoSpill Defense: When webDomain is discovered inside a WebView/HTML branch, credentials
 *   are strictly bound to that web branch and cannot be spilled or mixed into outer native host fields.
 */
@RequiresApi(Build.VERSION_CODES.O)
object AutofillStructureParser {

    private const val MAX_DEPTH = 64

    fun parse(structure: AssistStructure): ParsedAutofillFields {
        val result = ParsedAutofillFields()
        val nodeCount = structure.windowNodeCount

        for (i in 0 until nodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            traverseNode(windowNode.rootViewNode, result, 0, activeWebDomain = null)
        }

        return result
    }

    private fun traverseNode(
        node: AssistStructure.ViewNode?,
        result: ParsedAutofillFields,
        depth: Int,
        activeWebDomain: String?
    ) {
        if (node == null || depth > MAX_DEPTH) return
        if (node.visibility != View.VISIBLE) return

        // Check if this node or an ancestor defines a webDomain (WebView or browser tab)
        val currentWebDomain = node.webDomain?.ifBlank { null } ?: activeWebDomain
        if (currentWebDomain != null && result.webDomain == null) {
            result.webDomain = currentWebDomain
        }

        // Extract package name from view node
        node.idPackage?.let { pkg ->
            if (result.packageName == null && pkg.isNotBlank()) {
                result.packageName = pkg
            }
        }

        val autofillId = node.autofillId
        val isWebNode = (currentWebDomain != null || node.htmlInfo != null)

        if (autofillId != null) {
            // AutoSpill Defense: If we have identified a web domain context (WebView or browser),
            // ONLY accept credential fields that originate from this web hierarchy.
            // Reject native host input fields that could be attempting to harvest web credentials.
            val allowBinding = if (result.webDomain != null) {
                isWebNode
            } else {
                true
            }

            if (allowBinding) {
                // Rank 1: Standard Android Autofill Hints
                val hints = node.autofillHints
                if (hints != null) {
                    for (hint in hints) {
                        when (hint.lowercase(Locale.ROOT)) {
                            View.AUTOFILL_HINT_USERNAME,
                            View.AUTOFILL_HINT_EMAIL_ADDRESS,
                            "email",
                            "username" -> {
                                if (result.usernameId == null) {
                                    result.usernameId = autofillId
                                    if (isWebNode) result.isFromWebView = true
                                }
                            }
                            View.AUTOFILL_HINT_PASSWORD,
                            "password",
                            "current-password",
                            "new-password" -> {
                                if (result.passwordId == null) {
                                    result.passwordId = autofillId
                                    if (isWebNode) result.isFromWebView = true
                                }
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
                                    if (result.passwordId == null) {
                                        result.passwordId = autofillId
                                        result.isFromWebView = true
                                    }
                                } else if (attrName == "autocomplete") {
                                    if (attrVal.contains("password") && result.passwordId == null) {
                                        result.passwordId = autofillId
                                        result.isFromWebView = true
                                    } else if ((attrVal.contains("username") || attrVal.contains("email")) && result.usernameId == null) {
                                        result.usernameId = autofillId
                                        result.isFromWebView = true
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
                    if (isWebNode) result.isFromWebView = true
                }

                // Rank 4: Heuristic Matching on View ID, Hint, or Content Description
                val idEntry = node.idEntry?.lowercase(Locale.ROOT) ?: ""
                val hintText = node.hint?.toString()?.lowercase(Locale.ROOT) ?: ""
                val contentDesc = node.contentDescription?.toString()?.lowercase(Locale.ROOT) ?: ""

                if (result.passwordId == null && isPasswordHeuristic(idEntry, hintText, contentDesc)) {
                    result.passwordId = autofillId
                    if (isWebNode) result.isFromWebView = true
                } else if (result.usernameId == null && isUsernameHeuristic(idEntry, hintText, contentDesc)) {
                    result.usernameId = autofillId
                    if (isWebNode) result.isFromWebView = true
                }
            }
        }

        // Recursively traverse child nodes with activeWebDomain context
        val childCount = node.childCount
        for (i in 0 until childCount) {
            traverseNode(node.getChildAt(i), result, depth + 1, currentWebDomain)
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
