package com.clawstack.shellguard.services.autofill

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutofillStructureParserTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun nextAutofillId(): AutofillId {
        return View(context).autofillId!!
    }

    private class FakeAutofillNode(
        override val autofillId: AutofillId? = null,
        override val visibility: Int = View.VISIBLE,
        override val webDomain: String? = null,
        override val idPackage: String? = null,
        override val className: String? = "android.view.View",
        override val autofillType: Int = View.AUTOFILL_TYPE_NONE,
        override val autofillHints: Array<String>? = null,
        override val htmlTag: String? = null,
        override val htmlAttributes: List<Pair<String, String?>>? = null,
        override val inputType: Int = InputType.TYPE_NULL,
        override val idEntry: String? = null,
        override val hint: CharSequence? = null,
        override val contentDescription: CharSequence? = null,
        val children: List<FakeAutofillNode> = emptyList()
    ) : AutofillNode {
        override val childCount: Int get() = children.size
        override fun getChildAt(index: Int): AutofillNode? = children.getOrNull(index)
    }

    @Test
    fun testContainerHijackingDefense_nonInputFormAndDivDoNotStealUsernameId() {
        val formContainerId = nextAutofillId()
        val divWrapperId = nextAutofillId()
        val realUsernameInputId = nextAutofillId()
        val realPasswordInputId = nextAutofillId()

        val root = FakeAutofillNode(
            autofillId = formContainerId,
            webDomain = "auth.example.com",
            htmlTag = "form",
            idEntry = "login-form",
            children = listOf(
                FakeAutofillNode(
                    autofillId = divWrapperId,
                    htmlTag = "div",
                    idEntry = "user-login-box",
                    hint = "Username",
                    children = listOf(
                        FakeAutofillNode(
                            autofillId = realUsernameInputId,
                            htmlTag = "input",
                            htmlAttributes = listOf("type" to "text", "name" to "username")
                        ),
                        FakeAutofillNode(
                            autofillId = realPasswordInputId,
                            htmlTag = "input",
                            htmlAttributes = listOf("type" to "password", "name" to "password")
                        )
                    )
                )
            )
        )

        val parsed = AutofillStructureParser.parseNodes(listOf(root), "com.android.chrome")
        assertEquals(realUsernameInputId, parsed.usernameId)
        assertEquals(realPasswordInputId, parsed.passwordId)
        assertTrue(parsed.isFromWebView)
    }

    @Test
    fun testMutualExclusion_passwordFieldWithLoginOrUserPrefixDoesNotOverwriteUsernameId() {
        val passId = nextAutofillId()
        val userId = nextAutofillId()

        // Even if password node has id="login_password" and hint="User password",
        // it must never match as usernameId.
        val root = FakeAutofillNode(
            className = "android.widget.LinearLayout",
            idEntry = "login_root",
            children = listOf(
                FakeAutofillNode(
                    autofillId = passId,
                    className = "android.widget.EditText",
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    idEntry = "login_password",
                    hint = "User Password"
                ),
                FakeAutofillNode(
                    autofillId = userId,
                    className = "android.widget.EditText",
                    inputType = InputType.TYPE_CLASS_TEXT,
                    idEntry = "login_username",
                    hint = "Username"
                )
            )
        )

        val parsed = AutofillStructureParser.parseNodes(listOf(root), "com.example.app")
        assertEquals(userId, parsed.usernameId)
        assertEquals(passId, parsed.passwordId)
    }

    @Test
    fun testHtmlEmailAndPlaceholderDetection_withoutAutocompleteAttribute() {
        val emailId = nextAutofillId()
        val passId = nextAutofillId()

        val root = FakeAutofillNode(
            webDomain = "reef.local",
            htmlTag = "div",
            children = listOf(
                FakeAutofillNode(
                    autofillId = emailId,
                    htmlTag = "input",
                    htmlAttributes = listOf(
                        "type" to "email",
                        "placeholder" to "Enter your email"
                    )
                ),
                FakeAutofillNode(
                    autofillId = passId,
                    htmlTag = "input",
                    htmlAttributes = listOf("type" to "password")
                )
            )
        )

        val parsed = AutofillStructureParser.parseNodes(listOf(root), "com.android.chrome")
        assertEquals(emailId, parsed.usernameId)
        assertEquals(passId, parsed.passwordId)
        assertEquals(AutofillStructureParser.RANK_HTML_PRIMARY, parsed.usernameConfidenceRank)
    }

    @Test
    fun testAndroidInputTypeEmailVariation_detectsUsernameField() {
        val emailId = nextAutofillId()
        val passId = nextAutofillId()

        val root = FakeAutofillNode(
            className = "android.widget.LinearLayout",
            children = listOf(
                FakeAutofillNode(
                    autofillId = emailId,
                    className = "android.widget.EditText",
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                ),
                FakeAutofillNode(
                    autofillId = passId,
                    className = "android.widget.EditText",
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                )
            )
        )

        val parsed = AutofillStructureParser.parseNodes(listOf(root), "com.example.nativeapp")
        assertEquals(emailId, parsed.usernameId)
        assertEquals(passId, parsed.passwordId)
        assertEquals(AutofillStructureParser.RANK_INPUT_TYPE, parsed.usernameConfidenceRank)
    }

    @Test
    fun testConfidenceRankOverride_explicitUsernameOverridesEarlierGenericLoginField() {
        val tenantFieldId = nextAutofillId()
        val usernameFieldId = nextAutofillId()
        val passwordFieldId = nextAutofillId()

        val root = FakeAutofillNode(
            className = "android.widget.LinearLayout",
            children = listOf(
                FakeAutofillNode(
                    autofillId = tenantFieldId,
                    className = "android.widget.EditText",
                    inputType = InputType.TYPE_CLASS_TEXT,
                    idEntry = "login_tenant"
                ),
                FakeAutofillNode(
                    autofillId = usernameFieldId,
                    className = "android.widget.EditText",
                    inputType = InputType.TYPE_CLASS_TEXT,
                    idEntry = "login_username"
                ),
                FakeAutofillNode(
                    autofillId = passwordFieldId,
                    className = "android.widget.EditText",
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    idEntry = "login_password"
                )
            )
        )

        val parsed = AutofillStructureParser.parseNodes(listOf(root), "com.example.enterprise")
        assertEquals(usernameFieldId, parsed.usernameId)
        assertEquals(passwordFieldId, parsed.passwordId)
        assertEquals(AutofillStructureParser.RANK_INPUT_TYPE, parsed.usernameConfidenceRank)
    }

    @Test
    fun testRank5ProximityFallback_bindsUnlabeledEditableInputImmediatelyPrecedingPassword() {
        val searchBoxId = nextAutofillId()
        val unlabeledHandleId = nextAutofillId()
        val passwordId = nextAutofillId()

        val root = FakeAutofillNode(
            webDomain = "custom.portal.internal",
            htmlTag = "div",
            children = listOf(
                FakeAutofillNode(
                    autofillId = searchBoxId,
                    htmlTag = "input",
                    htmlAttributes = listOf("type" to "search", "name" to "site_search")
                ),
                FakeAutofillNode(
                    autofillId = unlabeledHandleId,
                    htmlTag = "input",
                    htmlAttributes = listOf("type" to "text", "id" to "field_alpha_01")
                ),
                FakeAutofillNode(
                    autofillId = passwordId,
                    htmlTag = "input",
                    htmlAttributes = listOf("type" to "password", "id" to "field_alpha_02")
                )
            )
        )

        val parsed = AutofillStructureParser.parseNodes(listOf(root), "com.android.chrome")
        assertEquals(unlabeledHandleId, parsed.usernameId)
        assertEquals(passwordId, parsed.passwordId)
        assertEquals(AutofillStructureParser.RANK_PROXIMITY, parsed.usernameConfidenceRank)
    }

    @Test
    fun testCoPresenceGate_rejectsWeakHeuristicWhenNoPasswordFieldOnScreen() {
        val genericAccountNoteId = nextAutofillId()

        val root = FakeAutofillNode(
            className = "android.widget.LinearLayout",
            children = listOf(
                FakeAutofillNode(
                    autofillId = genericAccountNoteId,
                    className = "android.widget.EditText",
                    inputType = InputType.TYPE_CLASS_TEXT,
                    idEntry = "account_nickname",
                    hint = "Enter account display label"
                )
            )
        )

        val parsed = AutofillStructureParser.parseNodes(listOf(root), "com.example.profile")
        assertNull(parsed.passwordId)
        assertNull(parsed.usernameId)
    }

    @Test
    fun testMultiStepLoginPage1_retainsHighConfidenceEmailOrUsernameWhenPasswordAbsent() {
        val step1EmailId = nextAutofillId()

        val root = FakeAutofillNode(
            webDomain = "accounts.google.com",
            htmlTag = "form",
            children = listOf(
                FakeAutofillNode(
                    autofillId = step1EmailId,
                    htmlTag = "input",
                    htmlAttributes = listOf("type" to "email", "autocomplete" to "username")
                )
            )
        )

        val parsed = AutofillStructureParser.parseNodes(listOf(root), "com.android.chrome")
        assertNull(parsed.passwordId)
        assertEquals(step1EmailId, parsed.usernameId)
        assertEquals(AutofillStructureParser.RANK_HTML_PRIMARY, parsed.usernameConfidenceRank)
    }

    @Test
    fun testAutoSpillDefense_enteringWebViewClearsOuterNativeHostFields() {
        val outerOmniboxId = nextAutofillId()
        val outerNativeFakeUserId = nextAutofillId()
        val webPasswordId = nextAutofillId()

        val root = FakeAutofillNode(
            className = "android.widget.FrameLayout",
            children = listOf(
                FakeAutofillNode(
                    autofillId = outerOmniboxId,
                    className = "android.widget.EditText",
                    inputType = InputType.TYPE_CLASS_TEXT,
                    idEntry = "url_bar"
                ),
                FakeAutofillNode(
                    autofillId = outerNativeFakeUserId,
                    className = "android.widget.EditText",
                    inputType = InputType.TYPE_CLASS_TEXT,
                    idEntry = "username"
                ),
                FakeAutofillNode(
                    webDomain = "vault.clawstack.io",
                    htmlTag = "div",
                    children = listOf(
                        FakeAutofillNode(
                            autofillId = webPasswordId,
                            htmlTag = "input",
                            htmlAttributes = listOf("type" to "password")
                        )
                    )
                )
            )
        )

        val parsed = AutofillStructureParser.parseNodes(listOf(root), "com.malicious.host")
        assertEquals("vault.clawstack.io", parsed.webDomain)
        assertNull(parsed.usernameId)
        assertEquals(webPasswordId, parsed.passwordId)
        assertTrue(parsed.isFromWebView)
    }

    @Test
    fun testOptionBMaskUsername_masksEmailAndHandlesWithoutExposingPlaintext() {
        assertEquals("lu***@gmail.com", AutofillInlineHelper.maskUsername("lucas@gmail.com"))
        assertEquals("a***@x.io", AutofillInlineHelper.maskUsername("ab@x.io"))
        assertEquals("lu***s", AutofillInlineHelper.maskUsername("lucas"))
        assertEquals("r***t", AutofillInlineHelper.maskUsername("root"))
        assertEquals("a***", AutofillInlineHelper.maskUsername("ab"))
        assertEquals("", AutofillInlineHelper.maskUsername("   "))
    }

    @Test
    fun testFormatUnlockedChipSubtitle_combinesCategoryOrTagWithMaskedUsername() {
        val workPearl = com.clawstack.shellguard.data.local.entities.VaultPearlEntity(
            id = "p1",
            ownerUuid = "u1",
            title = "Google",
            secret = "{}",
            username = "lucas@company.com",
            category = "Work",
            tags = "[]",
            createdAt = "2026-10-08T00:00:00Z"
        )
        assertEquals("Work · lu***@company.com", AutofillInlineHelper.formatUnlockedChipSubtitle(workPearl))

        val taggedPearl = workPearl.copy(
            category = "General",
            tags = "[\"Personal\",\"Cloud\"]",
            username = "lucas@gmail.com"
        )
        assertEquals("Personal · lu***@gmail.com", AutofillInlineHelper.formatUnlockedChipSubtitle(taggedPearl))

        val defaultCategoryPearl = workPearl.copy(
            category = "General",
            tags = "[]",
            username = "admin_user"
        )
        assertEquals("ad***r", AutofillInlineHelper.formatUnlockedChipSubtitle(defaultCategoryPearl))

        val passwordOnlyPearl = workPearl.copy(
            category = "General",
            tags = "[]",
            username = ""
        )
        assertEquals("Password", AutofillInlineHelper.formatUnlockedChipSubtitle(passwordOnlyPearl))
    }

    @Test
    fun testResolveChipIcon_returnsValidResourceIconInAllModes() {
        val lockedIcon = AutofillInlineHelper.resolveChipIcon(
            context = context,
            targetPackage = "com.android.chrome",
            isFromWebView = true,
            isLockedOrReprompt = true
        )
        assertNotNull(lockedIcon)

        val unlockedNativeIcon = AutofillInlineHelper.resolveChipIcon(
            context = context,
            targetPackage = context.packageName,
            isFromWebView = false,
            isLockedOrReprompt = false
        )
        assertNotNull(unlockedNativeIcon)
    }
}


