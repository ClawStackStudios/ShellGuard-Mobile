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
    var isFromWebView: Boolean = false,
    var usernameConfidenceRank: Int = Int.MAX_VALUE,
    var passwordConfidenceRank: Int = Int.MAX_VALUE
)

/**
 * Lightweight, testable abstraction over [AssistStructure.ViewNode].
 */
interface AutofillNode {
    val autofillId: AutofillId?
    val visibility: Int
    val webDomain: String?
    val idPackage: String?
    val className: String?
    val autofillType: Int
    val autofillHints: Array<String>?
    val htmlTag: String?
    val htmlAttributes: List<Pair<String, String?>>?
    val inputType: Int
    val idEntry: String?
    val hint: CharSequence?
    val contentDescription: CharSequence?
    val childCount: Int
    fun getChildAt(index: Int): AutofillNode?
}

/**
 * Traverses an AssistStructure view hierarchy to identify credential input fields,
 * target web domains, and package names.
 *
 * Implements a 5-tier confidence-ranked detection heuristic with blast-radius containment:
 * 1. Standard Autofill Hints (AUTOFILL_HINT_USERNAME, AUTOFILL_HINT_PASSWORD, etc.)
 * 2. Primary HTML Input Attributes (type="password", type="email", autocomplete="username|email|*-password")
 * 3. Android Input Type Variations (TYPE_TEXT_VARIATION_PASSWORD, TYPE_TEXT_VARIATION_EMAIL_ADDRESS, etc.)
 *    and explicit HTML input name/id attributes
 * 4. Editable-Input Resource ID, Hint, Placeholder & Content Description Heuristics
 * 5. Preceding Editable Text Input Proximity Fallback (only when a password field is confirmed on screen)
 *
 * Enforces:
 * - Strict Editable-Input Gating: Non-input containers (<form>, <div>, <label>, LinearLayout) can never hijack IDs.
 * - Password/Username Mutual Exclusion: A password node (e.g. id="login_password") can never also match as usernameId.
 * - Negative Exclusion Filter: Rejects browser URL/omnibox bars, search boxes, OTP/2FA fields, and chat inputs.
 * - AutoSpill Defense: WebView domain context clears any outer native host fields and isolates binding to the web tree.
 */
@RequiresApi(Build.VERSION_CODES.O)
object AutofillStructureParser {

    private const val MAX_DEPTH = 64

    const val RANK_EXPLICIT_HINT = 1
    const val RANK_HTML_PRIMARY = 2
    const val RANK_INPUT_TYPE = 3
    const val RANK_HEURISTIC = 4
    const val RANK_PROXIMITY = 5

    private val NON_TEXT_HTML_INPUT_TYPES = setOf(
        "hidden", "submit", "button", "reset", "checkbox", "radio",
        "file", "image", "range", "color", "search", "date",
        "time", "month", "week", "datetime-local"
    )

    private class TraverseContext(
        var lastEditableTextId: AutofillId? = null,
        var lastEditableTextIsWeb: Boolean = false,
        var precedingTextBeforePasswordId: AutofillId? = null,
        var precedingTextBeforePasswordIsWeb: Boolean = false
    )

    private class ViewNodeAdapter(private val node: AssistStructure.ViewNode) : AutofillNode {
        override val autofillId: AutofillId? get() = node.autofillId
        override val visibility: Int get() = node.visibility
        override val webDomain: String? get() = node.webDomain
        override val idPackage: String? get() = node.idPackage
        override val className: String? get() = node.className
        override val autofillType: Int get() = node.autofillType
        override val autofillHints: Array<String>? get() = node.autofillHints
        override val htmlTag: String? get() = node.htmlInfo?.tag
        override val htmlAttributes: List<Pair<String, String?>>?
            get() = node.htmlInfo?.attributes?.map { Pair(it.first ?: "", it.second) }
        override val inputType: Int get() = node.inputType
        override val idEntry: String? get() = node.idEntry
        override val hint: CharSequence? get() = node.hint
        override val contentDescription: CharSequence? get() = node.contentDescription
        override val childCount: Int get() = node.childCount
        override fun getChildAt(index: Int): AutofillNode? =
            node.getChildAt(index)?.let { ViewNodeAdapter(it) }
    }

    fun parse(structure: AssistStructure): ParsedAutofillFields {
        val roots = mutableListOf<AutofillNode>()
        val nodeCount = structure.windowNodeCount
        for (i in 0 until nodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            windowNode?.rootViewNode?.let { roots.add(ViewNodeAdapter(it)) }
        }
        return parseNodes(roots, structure.activityComponent?.packageName)
    }

    fun parseNodes(
        roots: List<AutofillNode>,
        activityPackageName: String? = null
    ): ParsedAutofillFields {
        val result = ParsedAutofillFields()
        val context = TraverseContext()

        for (root in roots) {
            traverseNode(root, result, context, depth = 0, activeWebDomain = null)
        }

        // Rank 5: Positional Proximity Fallback
        // Only activates when a password field is confirmed on screen (passwordId != null)
        // and no higher-rank username field (Ranks 1-4) was discovered.
        if (result.passwordId != null && result.usernameId == null) {
            val candidateId = context.precedingTextBeforePasswordId
            val candidateIsWeb = context.precedingTextBeforePasswordIsWeb
            val scopeValid = if (result.webDomain != null) candidateIsWeb else true
            if (candidateId != null && candidateId != result.passwordId && scopeValid) {
                result.usernameId = candidateId
                result.usernameConfidenceRank = RANK_PROXIMITY
                if (candidateIsWeb) {
                    result.isFromWebView = true
                }
            }
        }

        // Co-Presence Gate: If no password field exists on the screen (passwordId == null),
        // reject weak Rank 4/5 heuristic username matches (e.g., generic "account" or "user" substrings
        // on non-login screens). Only high-confidence signals (Ranks 1-3: autofillHints, HTML email/autocomplete,
        // email InputType, or explicit "username"/"email" field labels) survive without a password field.
        if (result.passwordId == null && result.usernameConfidenceRank > RANK_INPUT_TYPE) {
            result.usernameId = null
            result.usernameConfidenceRank = Int.MAX_VALUE
        }

        if (result.packageName.isNullOrBlank()) {
            result.packageName = activityPackageName
        }

        return result
    }

    private fun traverseNode(
        node: AutofillNode?,
        result: ParsedAutofillFields,
        context: TraverseContext,
        depth: Int,
        activeWebDomain: String?
    ) {
        if (node == null || depth > MAX_DEPTH) return
        if (node.visibility != View.VISIBLE) return

        // Check if this node or an ancestor defines a webDomain (WebView or browser tab)
        val currentWebDomain = node.webDomain?.ifBlank { null } ?: activeWebDomain
        if (currentWebDomain != null && result.webDomain == null) {
            result.webDomain = currentWebDomain
            // AutoSpill & Host-Hijack Defense: Clear any fields captured from outer native host views
            // before entering the WebView hierarchy.
            if (!result.isFromWebView) {
                result.usernameId = null
                result.passwordId = null
                result.usernameConfidenceRank = Int.MAX_VALUE
                result.passwordConfidenceRank = Int.MAX_VALUE
                context.lastEditableTextId = null
                context.precedingTextBeforePasswordId = null
            }
        }

        // Extract package name from view node
        node.idPackage?.let { pkg ->
            if (result.packageName == null && pkg.isNotBlank()) {
                result.packageName = pkg
            }
        }

        val autofillId = node.autofillId
        val isWebNode = (currentWebDomain != null || node.htmlTag != null)

        if (autofillId != null) {
            val allowBinding = if (result.webDomain != null) {
                isWebNode
            } else {
                true
            }

            if (allowBinding) {
                evaluateNode(node, autofillId, isWebNode, result, context)
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            traverseNode(node.getChildAt(i), result, context, depth + 1, currentWebDomain)
        }
    }

    private fun evaluateNode(
        node: AutofillNode,
        autofillId: AutofillId,
        isWebNode: Boolean,
        result: ParsedAutofillFields,
        context: TraverseContext
    ) {
        val editableInput = isEditableInputNode(node)
        val idEntry = node.idEntry?.lowercase(Locale.ROOT) ?: ""
        val hintText = node.hint?.toString()?.lowercase(Locale.ROOT) ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase(Locale.ROOT) ?: ""

        var htmlType = ""
        var htmlAutocomplete = ""
        var htmlName = ""
        var htmlId = ""
        var htmlPlaceholder = ""
        var htmlAriaLabel = ""

        if (node.htmlTag?.lowercase(Locale.ROOT) == "input") {
            node.htmlAttributes?.forEach { (rawName, rawVal) ->
                val attrName = rawName.lowercase(Locale.ROOT)
                val attrVal = rawVal?.lowercase(Locale.ROOT) ?: ""
                when (attrName) {
                    "type" -> htmlType = attrVal
                    "autocomplete" -> htmlAutocomplete = attrVal
                    "name" -> htmlName = attrVal
                    "id" -> htmlId = attrVal
                    "placeholder" -> htmlPlaceholder = attrVal
                    "aria-label", "label" -> htmlAriaLabel = attrVal
                }
            }
        }

        // -----------------------------------------------------------------
        // STEP 1: Evaluate Password Signals First (Mutual Exclusion Guard)
        // A node identified as a password field at ANY rank is strictly a
        // password node and must NEVER be classified as a username field.
        // -----------------------------------------------------------------
        var matchedPasswordRank: Int? = null

        // Rank 1: Explicit Password Autofill Hints
        val hints = node.autofillHints
        if (hints != null) {
            for (rawHint in hints) {
                val h = rawHint.lowercase(Locale.ROOT)
                if (h == View.AUTOFILL_HINT_PASSWORD.lowercase(Locale.ROOT) ||
                    h == "password" ||
                    h == "current-password" ||
                    h == "new-password"
                ) {
                    matchedPasswordRank = RANK_EXPLICIT_HINT
                    break
                }
            }
        }

        // Rank 2: Primary HTML Password Attributes
        if (matchedPasswordRank == null && node.htmlTag?.lowercase(Locale.ROOT) == "input") {
            if (htmlType == "password" || htmlAutocomplete.contains("password")) {
                matchedPasswordRank = RANK_HTML_PRIMARY
            }
        }

        // Rank 3: Android Password InputType Variations
        if (matchedPasswordRank == null && isPasswordInputType(node.inputType)) {
            matchedPasswordRank = RANK_INPUT_TYPE
        }

        // Rank 4: Editable Input Password Heuristics
        if (matchedPasswordRank == null && editableInput && htmlType != "email") {
            if (isPasswordHeuristic(idEntry, hintText, contentDesc, htmlName, htmlId, htmlPlaceholder, htmlAriaLabel)) {
                matchedPasswordRank = RANK_HEURISTIC
            }
        }

        if (matchedPasswordRank != null) {
            if (matchedPasswordRank < result.passwordConfidenceRank) {
                result.passwordId = autofillId
                result.passwordConfidenceRank = matchedPasswordRank
                if (isWebNode) result.isFromWebView = true
                // Snapshot the most recent editable text input seen prior to this password field
                if (context.lastEditableTextId != null && context.lastEditableTextId != autofillId) {
                    context.precedingTextBeforePasswordId = context.lastEditableTextId
                    context.precedingTextBeforePasswordIsWeb = context.lastEditableTextIsWeb
                }
            }
            // Mutual Exclusion: Never evaluate a password node as a username candidate
            // And if this node had previously been assigned to usernameId, clear it.
            if (result.usernameId == autofillId) {
                result.usernameId = null
                result.usernameConfidenceRank = Int.MAX_VALUE
            }
            return
        }

        // -----------------------------------------------------------------
        // STEP 2: Negative Exclusion & Editable Input Gate for Usernames
        // Non-editable containers (<form id="login">, <div id="user-box">)
        // and search/URL/OTP inputs are strictly rejected.
        // -----------------------------------------------------------------
        if (isExcludedNonCredentialInput(
                idEntry = idEntry,
                hint = hintText,
                desc = contentDesc,
                htmlType = htmlType,
                htmlAutocomplete = htmlAutocomplete,
                htmlName = htmlName,
                htmlId = htmlId,
                htmlPlaceholder = htmlPlaceholder,
                htmlAriaLabel = htmlAriaLabel
            )
        ) {
            return
        }

        var matchedUsernameRank: Int? = null

        // Rank 1: Explicit Username / Email Autofill Hints
        if (hints != null) {
            for (rawHint in hints) {
                val h = rawHint.lowercase(Locale.ROOT)
                if (h == View.AUTOFILL_HINT_USERNAME.lowercase(Locale.ROOT) ||
                    h == View.AUTOFILL_HINT_EMAIL_ADDRESS.lowercase(Locale.ROOT) ||
                    h == "username" ||
                    h == "email" ||
                    h == "emailaddress" ||
                    h == "new-username" ||
                    h == "newusername"
                ) {
                    matchedUsernameRank = RANK_EXPLICIT_HINT
                    break
                }
            }
        }

        // All lower ranks (Ranks 2-5) strictly require an editable input node
        if (!editableInput && matchedUsernameRank == null) {
            return
        }

        // Rank 2: Primary HTML Username / Email Attributes
        if (matchedUsernameRank == null && node.htmlTag?.lowercase(Locale.ROOT) == "input") {
            if (htmlType == "email" ||
                htmlAutocomplete.contains("username") ||
                htmlAutocomplete.contains("email")
            ) {
                matchedUsernameRank = RANK_HTML_PRIMARY
            }
        }

        // Rank 3: Android Email InputType Variations OR Explicit Username/Email Identifiers
        if (matchedUsernameRank == null) {
            if (isEmailInputType(node.inputType) ||
                isExplicitUsernameOrEmailLabel(idEntry, hintText, contentDesc, htmlName, htmlId, htmlPlaceholder, htmlAriaLabel)
            ) {
                matchedUsernameRank = RANK_INPUT_TYPE
            }
        }

        // Rank 4: Editable Input Heuristic Matching (Generic login/account/user identifiers)
        if (matchedUsernameRank == null) {
            if (isUsernameHeuristic(idEntry, hintText, contentDesc, htmlName, htmlId, htmlPlaceholder, htmlAriaLabel)) {
                matchedUsernameRank = RANK_HEURISTIC
            }
        }

        if (matchedUsernameRank != null && autofillId != result.passwordId) {
            if (matchedUsernameRank < result.usernameConfidenceRank) {
                result.usernameId = autofillId
                result.usernameConfidenceRank = matchedUsernameRank
                if (isWebNode) result.isFromWebView = true
            }
        }

        // Track eligible editable text input for Rank 5 proximity fallback
        if (editableInput && autofillId != result.passwordId) {
            context.lastEditableTextId = autofillId
            context.lastEditableTextIsWeb = isWebNode
            // If passwordId was not yet found, keep updating precedingTextBeforePasswordId
            if (result.passwordId == null) {
                context.precedingTextBeforePasswordId = autofillId
                context.precedingTextBeforePasswordIsWeb = isWebNode
            }
        }
    }

    private fun isEditableInputNode(node: AutofillNode): Boolean {
        val tag = node.htmlTag?.lowercase(Locale.ROOT)
        if (tag != null) {
            if (tag != "input") return false
            val typeAttr = node.htmlAttributes
                ?.firstOrNull { it.first.equals("type", ignoreCase = true) }
                ?.second
                ?.lowercase(Locale.ROOT)
                ?.trim()
                ?: "text"
            return typeAttr !in NON_TEXT_HTML_INPUT_TYPES
        }

        val cls = node.className?.lowercase(Locale.ROOT) ?: ""
        if (cls.contains("edittext") || cls.contains("autocompletetextview")) {
            return true
        }

        // Reject known non-editable container or static widget classes
        if (cls.contains("layout") ||
            cls.contains("viewgroup") ||
            cls.contains("button") ||
            cls.contains("imageview") ||
            cls.contains("checkbox") ||
            cls.contains("radiobutton") ||
            cls.contains("switch") ||
            cls.contains("recyclerview") ||
            cls.contains("listview") ||
            cls.contains("scrollview") ||
            cls.contains("webview")
        ) {
            return false
        }

        if (node.inputType != InputType.TYPE_NULL) {
            return true
        }

        // Allow Compose or custom text inputs that explicitly declare AUTOFILL_TYPE_TEXT
        // and are not static TextViews without inputType
        if (node.autofillType == View.AUTOFILL_TYPE_TEXT && !cls.endsWith("textview")) {
            return true
        }

        return false
    }

    private fun isPasswordInputType(inputType: Int): Boolean {
        if (inputType == InputType.TYPE_NULL) return false
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return when (inputClass) {
            InputType.TYPE_CLASS_TEXT -> {
                variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            InputType.TYPE_CLASS_NUMBER -> {
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            }
            else -> false
        }
    }

    private fun isEmailInputType(inputType: Int): Boolean {
        if (inputType == InputType.TYPE_NULL) return false
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return inputClass == InputType.TYPE_CLASS_TEXT && (
                variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                )
    }

    private fun isExcludedNonCredentialInput(
        idEntry: String,
        hint: String,
        desc: String,
        htmlType: String,
        htmlAutocomplete: String,
        htmlName: String,
        htmlId: String,
        htmlPlaceholder: String,
        htmlAriaLabel: String
    ): Boolean {
        if (htmlType in NON_TEXT_HTML_INPUT_TYPES) return true
        if (htmlAutocomplete == "off" && (idEntry.contains("search") || htmlName.contains("search"))) return true
        if (htmlAutocomplete.contains("one-time-code")) return true

        val combined = "$idEntry $hint $desc $htmlName $htmlId $htmlPlaceholder $htmlAriaLabel"
        return combined.contains("url_bar") ||
                combined.contains("omnibox") ||
                combined.contains("autocompletetextview") ||
                combined.contains("location_bar") ||
                combined.contains("address_bar") ||
                combined.contains("search") ||
                combined.contains("query") ||
                combined.contains("filter") ||
                combined.contains("totp") ||
                combined.contains("otp") ||
                combined.contains("2fa") ||
                combined.contains("mfa") ||
                combined.contains("verification") ||
                combined.contains("security_code") ||
                combined.contains("captcha") ||
                combined.contains("comment") ||
                combined.contains("coupon") ||
                combined.contains("promo") ||
                combined.contains("server_url") ||
                combined.contains("host_url")
    }

    private fun isExplicitUsernameOrEmailLabel(
        id: String,
        hint: String,
        desc: String,
        htmlName: String,
        htmlId: String,
        htmlPlaceholder: String,
        htmlAriaLabel: String
    ): Boolean {
        val exactTokens = setOf(
            "username", "user", "user_name", "userid", "user_id",
            "login", "login_id", "email", "email_address"
        )
        if (htmlName in exactTokens || htmlId in exactTokens || id in exactTokens) {
            return true
        }
        val combined = "$id $hint $desc $htmlName $htmlId $htmlPlaceholder $htmlAriaLabel"
        return combined.contains("username") ||
                combined.contains("user_name") ||
                combined.contains("email") ||
                combined.contains("e-mail")
    }

    private fun isPasswordHeuristic(
        id: String,
        hint: String,
        desc: String,
        htmlName: String,
        htmlId: String,
        htmlPlaceholder: String,
        htmlAriaLabel: String
    ): Boolean {
        val combined = "$id $hint $desc $htmlName $htmlId $htmlPlaceholder $htmlAriaLabel"
        return combined.contains("password") ||
                combined.contains("passcode") ||
                combined.contains("passphrase") ||
                id.contains("pwd") ||
                hint.contains("pwd") ||
                htmlName.contains("pwd") ||
                htmlId.contains("pwd")
    }

    private fun isUsernameHeuristic(
        id: String,
        hint: String,
        desc: String,
        htmlName: String,
        htmlId: String,
        htmlPlaceholder: String,
        htmlAriaLabel: String
    ): Boolean {
        val combined = "$id $hint $desc $htmlName $htmlId $htmlPlaceholder $htmlAriaLabel"
        if (combined.contains("server") || combined.contains("host") || combined.contains("port")) {
            return false
        }
        return combined.contains("user_id") ||
                combined.contains("userid") ||
                combined.contains("login") ||
                combined.contains("account") ||
                combined.contains("identifier") ||
                hint.contains("user") ||
                htmlPlaceholder.contains("user") ||
                htmlAriaLabel.contains("user")
    }
}
