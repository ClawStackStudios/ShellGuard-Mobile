package com.clawstack.shellguard.services.autofill

import android.app.assist.AssistStructure
import android.os.Build
import android.view.View
import android.view.autofill.AutofillId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowView

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutofillStructureParserTest {

    @Test
    fun testParsedAutofillFields_dataClassInitialization() {
        val fields = ParsedAutofillFields(
            usernameId = null,
            passwordId = null,
            webDomain = "example.com",
            packageName = "com.example.app"
        )

        assertEquals("example.com", fields.webDomain)
        assertEquals("com.example.app", fields.packageName)
        assertNull(fields.usernameId)
        assertNull(fields.passwordId)
    }

    @Test
    fun testParsedAutofillFields_assignment() {
        val fields = ParsedAutofillFields()
        fields.webDomain = "accounts.google.com"
        fields.packageName = "com.android.chrome"

        assertEquals("accounts.google.com", fields.webDomain)
        assertEquals("com.android.chrome", fields.packageName)
    }
}
