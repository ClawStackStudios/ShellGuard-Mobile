package com.clawstack.shellguard.remote

import com.clawstack.shellguard.data.remote.KtorClientProvider
import com.clawstack.shellguard.data.remote.models.CreateVaultItemRequest
import com.clawstack.shellguard.data.remote.models.PearlDto
import com.clawstack.shellguard.data.remote.models.SessionData
import com.clawstack.shellguard.data.remote.models.ShellResponse
import com.clawstack.shellguard.data.remote.models.TokenRequest
import com.clawstack.shellguard.data.remote.models.TokenResponse
import com.clawstack.shellguard.data.remote.models.UserProfileDto
import com.clawstack.shellguard.data.remote.models.VaultResponse
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellGuardClientTest {

    private val json = KtorClientProvider.jsonConfig

    @Test
    fun testTokenRequestSerialization() {
        val request = TokenRequest(type = "human", keyHash = "a1b2c3d4e5f6")
        val serialized = json.encodeToString(request)
        assertTrue(serialized.contains("\"type\":\"human\""))
        assertTrue(serialized.contains("\"keyHash\":\"a1b2c3d4e5f6\""))
    }

    @Test
    fun testTokenResponseDeserialization() {
        val jsonPayload = """
            {
                "success": true,
                "data": {
                    "token": "mock.jwt.token",
                    "type": "human",
                    "createdAt": "2026-09-26T12:00:00Z",
                    "expiresAt": "2026-09-27T12:00:00Z",
                    "user": {
                        "uuid": "user-uuid-1234",
                        "username": "lucas",
                        "displayName": "Lucas Reef"
                    }
                }
            }
        """.trimIndent()

        val response = json.decodeFromString<TokenResponse>(jsonPayload)
        assertTrue(response.success)
        assertNotNull(response.data)
        assertEquals("mock.jwt.token", response.data?.token)
        assertEquals("user-uuid-1234", response.data?.user?.uuid)
        assertEquals("lucas", response.data?.user?.username)
    }

    @Test
    fun testVaultResponseDeserialization() {
        val jsonPayload = """
            {
                "success": true,
                "data": [
                    {
                        "id": "item-1",
                        "owner_uuid": "user-1234",
                        "title": "GitHub Login",
                        "username": "octocat",
                        "url": "https://github.com",
                        "secret": "{\"v\":1,\"alg\":\"AES-GCM-256\",\"iv\":\"iv\",\"ct\":\"ct\",\"aad\":\"aad\"}",
                        "type": "password",
                        "category": "Development",
                        "reprompt": true,
                        "created_at": "2026-09-26T00:00:00Z"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<VaultResponse>(jsonPayload)
        assertTrue(response.success)
        assertEquals(1, response.data.size)
        val pearl = response.data[0]
        assertEquals("item-1", pearl.id)
        assertEquals("GitHub Login", pearl.title)
        assertEquals("octocat", pearl.username)
        assertTrue(pearl.reprompt)
    }

    @Test
    fun testUniformShellResponseErrorDeserialization() {
        val jsonError = """
            {
                "success": false,
                "error": "Invalid authentication credentials",
                "code": "AUTH_INVALID_KEY",
                "details": [
                    {
                        "path": "keyHash",
                        "message": "Key hash does not match registered identity"
                    }
                ],
                "suggestion": "Check your hu- key or identity file."
            }
        """.trimIndent()

        val response = json.decodeFromString<ShellResponse<Unit>>(jsonError)
        assertFalse(response.success)
        assertEquals("Invalid authentication credentials", response.error)
        assertEquals("AUTH_INVALID_KEY", response.code)
        assertEquals(1, response.details?.size)
        assertEquals("keyHash", response.details?.get(0)?.path)
        assertEquals("Check your hu- key or identity file.", response.suggestion)
    }

    @Test
    fun testCreateVaultItemRequestSerialization() {
        val request = CreateVaultItemRequest(
            title = "Proton Mail",
            username = "admin@proton.me",
            url = "https://mail.proton.me",
            secret = "{\"v\":1}",
            totp_secret = "{\"v\":1}",
            category = "Email",
            reprompt = false
        )
        val encoded = json.encodeToString(request)
        assertTrue(encoded.contains("\"title\":\"Proton Mail\""))
        assertTrue(encoded.contains("\"username\":\"admin@proton.me\""))
        assertTrue(encoded.contains("\"category\":\"Email\""))
    }
}
