# ShellGuard Mobile: Routes and API Contracts Specification
**Targeted for Google AI Studio Android Application Generator**

This document specifies the network layer, API contracts, and sync mechanisms for the complete ShellGuard Mobile client.
**Package:** `com.clawstack.shellguard`

---

## 1. Uniform Response Envelope

All API responses follow a uniform envelope format.

```kotlin
package com.clawstack.shellguard.data.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ShellResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val code: String? = null,
    val details: List<ValidationIssue>? = null,
    val suggestion: String? = null
)

@Serializable
data class ValidationIssue(
    val path: String,
    val message: String
)
```

---

## 2. API Endpoints & Request/Response Models

### A. Authentication: POST /api/auth/token

Exchanges credentials for an access token and user profile.

**Request JSON:**
```json
{
  "username": "admin",
  "password": "securepassword",
  "device_id": "android-uuid-1234"
}
```

**Response JSON:**
```json
{
  "success": true,
  "data": {
    "token": "jwt.token.string",
    "user": {
      "uuid": "user-uuid",
      "username": "admin",
      "email": "admin@example.com"
    },
    "session": {
      "expires_at": "2026-12-31T23:59:59Z"
    }
  }
}
```

**Kotlin DTOs:**
```kotlin
package com.clawstack.shellguard.data.api.models.auth

import kotlinx.serialization.Serializable

@Serializable
data class TokenRequest(
    val username: String,
    val password: String,
    val device_id: String? = null
)

@Serializable
data class UserProfileDto(
    val uuid: String,
    val username: String,
    val email: String? = null
)

@Serializable
data class SessionData(
    val expires_at: String
)

@Serializable
data class TokenResponse(
    val token: String,
    val user: UserProfileDto,
    val session: SessionData
)
```

### B. Authentication: POST /api/auth/register

Registers a new user (if the server allows).

**Kotlin DTO:**
```kotlin
package com.clawstack.shellguard.data.api.models.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null
)
```

### C. Vault Pearls: GET/POST/PUT/DELETE /api/vault

Pearls represent core vault items like logins, cards, and identities.

**Kotlin DTOs:**
```kotlin
package com.clawstack.shellguard.data.api.models.vault

import kotlinx.serialization.Serializable

@Serializable
data class PearlDto(
    val id: String,
    val owner_uuid: String,
    val title: String,
    val secret: String, // Encrypted payload or cleartext password based on config
    val username: String? = null,
    val url: String? = null,
    val type: String,
    val category: String? = null,
    val notes: String? = null,
    val totp_secret: String? = null,
    val attachments: List<String> = emptyList(),
    val custom_fields: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
    val uris: List<String> = emptyList(),
    val password_history: List<String> = emptyList(),
    val created_at: String,
    val updated_at: String
)

@Serializable
data class CreatePearlDto(
    val title: String,
    val secret: String,
    val username: String? = null,
    val url: String? = null,
    val type: String,
    val category: String? = null,
    val notes: String? = null,
    val totp_secret: String? = null,
    val attachments: List<String> = emptyList(),
    val custom_fields: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
    val uris: List<String> = emptyList()
)

@Serializable
data class UpdatePearlDto(
    val title: String? = null,
    val secret: String? = null,
    val username: String? = null,
    val url: String? = null,
    val type: String? = null,
    val category: String? = null,
    val notes: String? = null,
    val totp_secret: String? = null,
    val attachments: List<String>? = null,
    val custom_fields: Map<String, String>? = null,
    val tags: List<String>? = null,
    val uris: List<String>? = null
)
```

### D. Secure Notes: GET/POST/PUT/DELETE /api/notes

**Kotlin DTOs:**
```kotlin
package com.clawstack.shellguard.data.api.models.notes

import kotlinx.serialization.Serializable

@Serializable
data class SecureNoteDto(
    val id: String,
    val owner_uuid: String,
    val title: String,
    val content: String,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class CreateNoteDto(
    val title: String,
    val content: String
)

@Serializable
data class UpdateNoteDto(
    val title: String? = null,
    val content: String? = null
)
```

### E. SSH Keys: GET/POST/PUT/DELETE /api/keys

**Kotlin DTOs:**
```kotlin
package com.clawstack.shellguard.data.api.models.keys

import kotlinx.serialization.Serializable

@Serializable
data class SshKeyDto(
    val id: String,
    val owner_uuid: String,
    val name: String,
    val public_key: String,
    val private_key: String,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class CreateSshKeyDto(
    val name: String,
    val public_key: String,
    val private_key: String
)

@Serializable
data class UpdateSshKeyDto(
    val name: String? = null,
    val public_key: String? = null,
    val private_key: String? = null
)
```

### F. Attachments: POST /api/attachments, GET /api/attachments/:id/file

Handles streaming upload via multipart. Limit is 500MB per file, 1GB quota per user.

**Kotlin DTO:**
```kotlin
package com.clawstack.shellguard.data.api.models.attachments

import kotlinx.serialization.Serializable

@Serializable
data class AttachmentDto(
    val id: String,
    val filename: String,
    val mime_type: String,
    val size: Long,
    val created_at: String
)
```

### G. Agent Keys: GET/POST/PATCH/DELETE /api/agent-keys

Agent keys are for M2M communication but managed by humans.

**Kotlin DTOs:**
```kotlin
package com.clawstack.shellguard.data.api.models.agentkeys

import kotlinx.serialization.Serializable

@Serializable
data class AgentKeyDto(
    val id: String,
    val owner_uuid: String,
    val name: String,
    val prefix: String,
    val created_at: String,
    val expires_at: String? = null
)

@Serializable
data class CreateAgentKeyDto(
    val name: String,
    val expires_at: String? = null
)
```

### H. Settings: GET/PUT /api/settings/:key

Settings values endpoints.

### I. Health: GET /api/health

Health check endpoint.

### J. Bulk Operations: POST /api/vault/bulk-import, DELETE /api/vault/bulk

Bulk import or delete of vault pearls.

---

## 3. ShellGuard API Client (Ktor Engine)

The main API client using Ktor OkHttp engine. Handles dynamic base URL, client version header, and 401/403 errors.

```kotlin
package com.clawstack.shellguard.data.api

import com.clawstack.shellguard.data.api.models.*
import com.clawstack.shellguard.data.api.models.auth.*
import com.clawstack.shellguard.data.api.models.vault.*
import com.clawstack.shellguard.data.api.models.notes.*
import com.clawstack.shellguard.data.api.models.keys.*
import com.clawstack.shellguard.data.api.models.attachments.*
import com.clawstack.shellguard.data.api.models.agentkeys.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.ConnectionSpec
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import java.util.concurrent.TimeUnit

class ShellGuardClient(
    private var baseUrl: String,
    private var tokenProvider: () -> String?
) {

    private val httpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = getUnsafeOkHttpClient()
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(DefaultRequest) {
            header("X-Client-Version", "ShellGuard-Mobile/1.0.0")
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
        }
    }

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<X509TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        return OkHttpClient.Builder()
            // 1. Explicitly allow Plaintext HTTP alongside Modern & Compatible TLS
            .connectionSpecs(
                listOf(
                    ConnectionSpec.MODERN_TLS,
                    ConnectionSpec.COMPATIBLE_TLS,
                    ConnectionSpec.CLEARTEXT
                )
            )
            // 2. Relaxed TrustManager for self-signed certificates on local LAN IPs and Tailscale nodes
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0])
            .hostnameVerifier { _, _ -> true }
            // 3. Follow redirects across VPN tunnels (Tailscale / WireGuard)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun updateBaseUrl(newUrl: String) {
        baseUrl = newUrl
    }

    private fun HttpRequestBuilder.authHeader() {
        tokenProvider()?.let { token ->
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    private fun buildUrl(path: String) = baseUrl.trimEnd('/') + path

    suspend fun login(request: TokenRequest): ShellResponse<TokenResponse> =
        httpClient.post(buildUrl("/api/auth/token")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun register(request: RegisterRequest): ShellResponse<Unit> =
        httpClient.post(buildUrl("/api/auth/register")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getVaultPearls(): ShellResponse<List<PearlDto>> =
        httpClient.get(buildUrl("/api/vault")) { authHeader() }.body()

    suspend fun createPearl(dto: CreatePearlDto): ShellResponse<PearlDto> =
        httpClient.post(buildUrl("/api/vault")) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.body()

    suspend fun updatePearl(id: String, dto: UpdatePearlDto): ShellResponse<PearlDto> =
        httpClient.put(buildUrl("/api/vault/$id")) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.body()

    suspend fun deletePearl(id: String): ShellResponse<Unit> =
        httpClient.delete(buildUrl("/api/vault/$id")) { authHeader() }.body()

    suspend fun getNotes(): ShellResponse<List<SecureNoteDto>> =
        httpClient.get(buildUrl("/api/notes")) { authHeader() }.body()

    suspend fun createNote(dto: CreateNoteDto): ShellResponse<SecureNoteDto> =
        httpClient.post(buildUrl("/api/notes")) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.body()

    suspend fun getSshKeys(): ShellResponse<List<SshKeyDto>> =
        httpClient.get(buildUrl("/api/keys")) { authHeader() }.body()

    suspend fun createSshKey(dto: CreateSshKeyDto): ShellResponse<SshKeyDto> =
        httpClient.post(buildUrl("/api/keys")) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.body()

    // Add remaining methods (updates, deletes, agent keys, attachments, health)...
}
```

---

## 4. ApiClient Dynamic Provider

A singleton to manage instances and configuration.

```kotlin
package com.clawstack.shellguard.data.api

object ApiClientProvider {
    private var client: ShellGuardClient? = null
    var currentBaseUrl: String = "http://localhost:8080"
    var currentToken: String? = null

    fun getClient(): ShellGuardClient {
        if (client == null) {
            client = ShellGuardClient(currentBaseUrl) { currentToken }
        }
        return client!!
    }

    fun updateConfig(baseUrl: String, token: String?) {
        currentBaseUrl = baseUrl
        currentToken = token
        client?.updateBaseUrl(baseUrl)
    }
}
```

---

## 5. Bidirectional Delta Reconciliation Algorithm

Synchronizes local changes with the server. Server wins conflicts.

```kotlin
package com.clawstack.shellguard.data.sync

import com.clawstack.shellguard.data.api.ShellGuardClient
import com.clawstack.shellguard.data.api.models.vault.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncRepository(
    private val apiClient: ShellGuardClient,
    private val localDatabase: Any // Placeholder for Room DAO
) {
    suspend fun syncVault() = withContext(Dispatchers.IO) {
        // 1. Push local changes
        // Example: Find local items marked as 'dirty' and POST/PUT to server
        // Example: Find local items marked as 'deleted' and DELETE on server

        // 2. Pull server changes
        val response = apiClient.getVaultPearls()
        if (response.success && response.data != null) {
            val serverItems = response.data
            // 3. Reconcile
            // Example: For each server item, if local timestamp < server timestamp, upsert local
            // Server wins by default in this implementation.
            serverItems.forEach { serverPearl ->
                // decrypt payload using ShellCryption here if needed
                // localDatabase.upsert(serverPearl)
            }

            // 4. Prune local items not on server (unless they are new and pending push)
        }
    }
}
```

---

## 6. Transport Security, LAN, Tailscale VPN & Cleartext HTTP Configuration

Since the ShellGuard server often runs on local networks (LAN) or via Tailscale, cleartext HTTP and self-signed certificates must be permitted.

**`network_security_config.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

**`AndroidManifest.xml` Excerpt**
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    android:usesCleartextTraffic="true"
    ...>
    <!-- Activities -->
</application>
```

**Ktor OkHttp Engine configuration (refer to Section 3 for Kotlin code):**
The `getUnsafeOkHttpClient` in `ShellGuardClient` configures OkHttp to trust all certificates and bypass hostname verification, which is necessary for connections to local LAN IPs or self-signed Tailscale endpoints.

---
