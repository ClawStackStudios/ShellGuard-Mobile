package com.clawstack.shellguard.data.remote

import com.clawstack.shellguard.data.remote.models.CreateNoteRequest
import com.clawstack.shellguard.data.remote.models.CreateSshKeyRequest
import com.clawstack.shellguard.data.remote.models.CreateVaultItemRequest
import com.clawstack.shellguard.data.remote.models.KeyItemResponse
import com.clawstack.shellguard.data.remote.models.KeysResponse
import com.clawstack.shellguard.data.remote.models.NoteItemResponse
import com.clawstack.shellguard.data.remote.models.NotesResponse
import com.clawstack.shellguard.data.remote.models.PearlDto
import com.clawstack.shellguard.data.remote.models.SecureNoteDto
import com.clawstack.shellguard.data.remote.models.SessionData
import com.clawstack.shellguard.data.remote.models.SshKeyDto
import com.clawstack.shellguard.data.remote.models.TokenRequest
import com.clawstack.shellguard.data.remote.models.TokenResponse
import com.clawstack.shellguard.data.remote.models.VaultItemResponse
import com.clawstack.shellguard.data.remote.models.VaultResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShellGuardClient(
    val baseUrl: String,
    val onUnauthorized: (() -> Unit)? = null,
    injectedClient: HttpClient? = null
) {
    val client: HttpClient = injectedClient ?: KtorClientProvider.createClient(
        baseUrl = baseUrl,
        onUnauthorized = onUnauthorized
    )

    /**
     * 1. Public Health Check (GET /api/health)
     */
    suspend fun getHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.get("api/health")
            response.status == HttpStatusCode.OK
        }
    }

    /**
     * 2. Authentication Handshake (POST /api/auth/token)
     */
    suspend fun authenticate(keyHash: String, uuid: String? = null): Result<SessionData> = withContext(Dispatchers.IO) {
        runCatching {
            val requestBody = TokenRequest(type = "human", keyHash = keyHash, uuid = uuid)
            val response: HttpResponse = client.post("api/auth/token") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                val res = response.body<TokenResponse>()
                if (res.success && res.data != null) {
                    res.data
                } else {
                    throw Exception(res.error ?: "Authentication failed")
                }
            } else {
                throw Exception("Auth request failed: ${response.status.value}")
            }
        }
    }

    /**
     * 3. Fetch Vault Items / Pearls (GET /api/vault)
     */
    suspend fun fetchVault(sessionToken: String): Result<List<PearlDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.get("api/vault") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.OK) {
                val res = response.body<VaultResponse>()
                res.data
            } else {
                throw Exception("Fetch vault failed with status: ${response.status.value}")
            }
        }
    }

    /**
     * 4. Create Vault Item (POST /api/vault)
     */
    suspend fun createVaultItem(sessionToken: String, request: CreateVaultItemRequest): Result<PearlDto> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.post("api/vault") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                val res = response.body<VaultItemResponse>()
                if (res.success && res.data != null) {
                    res.data
                } else {
                    throw Exception(res.error ?: "Create vault item failed")
                }
            } else {
                throw Exception("Create vault item failed with status: ${response.status.value}")
            }
        }
    }

    /**
     * 5. Update Vault Item (PUT /api/vault/:id)
     */
    suspend fun updateVaultItem(sessionToken: String, id: String, request: CreateVaultItemRequest): Result<PearlDto> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.put("api/vault/$id") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.OK) {
                val res = response.body<VaultItemResponse>()
                if (res.success && res.data != null) {
                    res.data
                } else {
                    throw Exception(res.error ?: "Update vault item failed")
                }
            } else {
                throw Exception("Update vault item failed with status: ${response.status.value}")
            }
        }
    }

    /**
     * 6. Delete Vault Item (DELETE /api/vault/:id)
     */
    suspend fun deleteVaultItem(sessionToken: String, id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.delete("api/vault/$id") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
            }
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent
        }
    }

    /**
     * 7. Fetch Secure Notes (GET /api/notes)
     */
    suspend fun fetchNotes(sessionToken: String): Result<List<SecureNoteDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.get("api/notes") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.OK) {
                val res = response.body<NotesResponse>()
                res.data
            } else {
                throw Exception("Fetch notes failed with status: ${response.status.value}")
            }
        }
    }

    /**
     * 8. Fetch SSH Keys (GET /api/keys)
     */
    suspend fun fetchKeys(sessionToken: String): Result<List<SshKeyDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.get("api/keys") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.OK) {
                val res = response.body<KeysResponse>()
                res.data
            } else {
                throw Exception("Fetch SSH keys failed with status: ${response.status.value}")
            }
        }
    }

    /**
     * 9. Create Note (POST /api/notes)
     */
    suspend fun createNote(sessionToken: String, request: CreateNoteRequest): Result<SecureNoteDto> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.post("api/notes") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                val res = response.body<NoteItemResponse>()
                if (res.success && res.data != null) {
                    res.data
                } else {
                    throw Exception(res.error ?: "Create note failed")
                }
            } else {
                throw Exception("Create note failed with status: ${response.status.value}")
            }
        }
    }

    /**
     * 10. Update Note (PUT /api/notes/:id)
     */
    suspend fun updateNote(sessionToken: String, id: String, request: CreateNoteRequest): Result<SecureNoteDto> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.put("api/notes/$id") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.OK) {
                val res = response.body<NoteItemResponse>()
                if (res.success && res.data != null) {
                    res.data
                } else {
                    throw Exception(res.error ?: "Update note failed")
                }
            } else {
                throw Exception("Update note failed with status: ${response.status.value}")
            }
        }
    }

    /**
     * 11. Delete Note (DELETE /api/notes/:id)
     */
    suspend fun deleteNote(sessionToken: String, id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.delete("api/notes/$id") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
            }
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent
        }
    }

    /**
     * 12. Create SSH Key (POST /api/keys)
     */
    suspend fun createSshKey(sessionToken: String, request: CreateSshKeyRequest): Result<SshKeyDto> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.post("api/keys") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                val res = response.body<KeyItemResponse>()
                if (res.success && res.data != null) {
                    res.data
                } else {
                    throw Exception(res.error ?: "Create SSH key failed")
                }
            } else {
                throw Exception("Create SSH key failed with status: ${response.status.value}")
            }
        }
    }

    /**
     * 13. Update SSH Key (PUT /api/keys/:id)
     */
    suspend fun updateSshKey(sessionToken: String, id: String, request: CreateSshKeyRequest): Result<SshKeyDto> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.put("api/keys/$id") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.OK) {
                val res = response.body<KeyItemResponse>()
                if (res.success && res.data != null) {
                    res.data
                } else {
                    throw Exception(res.error ?: "Update SSH key failed")
                }
            } else {
                throw Exception("Update SSH key failed with status: ${response.status.value}")
            }
        }
    }

    /**
     * 14. Delete SSH Key (DELETE /api/keys/:id)
     */
    suspend fun deleteSshKey(sessionToken: String, id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.delete("api/keys/$id") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
            }
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent
        }
    }
}

