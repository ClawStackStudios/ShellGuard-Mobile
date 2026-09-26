package com.clawstack.shellguard.ui.screens.gateway

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clawstack.shellguard.crypto.ClawCrypto
import com.clawstack.shellguard.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class KeyInputMode {
    PASTE,
    FILE
}

data class GatewayUiState(
    val protocol: String = "https",
    val host: String = "",
    val port: String = "",
    val clawKey: String = "",
    val uploadedKey: String? = null,
    val uploadedFileName: String? = null,
    val uploadedUuid: String? = null,
    val isKeyVisible: Boolean = false,
    val inputMode: KeyInputMode = KeyInputMode.FILE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUrlValid: Boolean = true,
    val urlErrorMessage: String? = null,
    val isConnectionSuccess: Boolean = false
) {
    val effectiveKey: String
        get() = if (inputMode == KeyInputMode.FILE) uploadedKey.orEmpty() else clawKey

    val isKeyValid: Boolean
        get() = ClawCrypto.isValidClawKey(effectiveKey)

    val serverUrl: String
        get() {
            val cleanHost = host.trim()
                .removePrefix("https://")
                .removePrefix("http://")
            return buildString {
                append(if (protocol == "https") "https://" else "http://")
                append(cleanHost)
                if (port.isNotBlank()) {
                    append(":")
                    append(port.trim())
                }
            }
        }

    val isFormValid: Boolean
        get() = host.isNotBlank() && isKeyValid
}

class GatewayViewModel(
    private val appContainer: AppContainer? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GatewayUiState())
    val uiState: StateFlow<GatewayUiState> = _uiState.asStateFlow()

    fun parseAndSetUrl(input: String) {
        var raw = input.trim()
        if (raw.isEmpty()) {
            _uiState.update { it.copy(host = "", isUrlValid = true, urlErrorMessage = null) }
            return
        }

        var detectedProtocol = _uiState.value.protocol
        if (raw.startsWith("https://", ignoreCase = true)) {
            detectedProtocol = "https"
            raw = raw.substring("https://".length)
        } else if (raw.startsWith("http://", ignoreCase = true)) {
            detectedProtocol = "http"
            raw = raw.substring("http://".length)
        }

        var detectedHost = raw
        var detectedPort = _uiState.value.port

        if (raw.contains(":")) {
            val parts = raw.split(":")
            detectedHost = parts[0]
            if (parts.size > 1) {
                val portAndPath = parts[1]
                val slashIndex = portAndPath.indexOf('/')
                if (slashIndex != -1) {
                    detectedPort = portAndPath.substring(0, slashIndex).filter { it.isDigit() }
                    detectedHost += portAndPath.substring(slashIndex)
                } else {
                    detectedPort = portAndPath.filter { it.isDigit() }
                }
            }
        }

        _uiState.update {
            it.copy(
                protocol = detectedProtocol,
                host = detectedHost,
                port = detectedPort,
                isUrlValid = true,
                urlErrorMessage = null,
                errorMessage = null
            )
        }
    }

    fun updateProtocol(protocol: String) {
        _uiState.update { it.copy(protocol = protocol, errorMessage = null) }
    }

    fun updateHost(host: String) {
        parseAndSetUrl(host)
    }

    fun updatePort(port: String) {
        _uiState.update { it.copy(port = port.filter { c -> c.isDigit() }, errorMessage = null) }
    }

    fun updateClawKey(rawInput: String) {
        val extracted = cleanAndExtractKey(rawInput)
        _uiState.update { it.copy(clawKey = extracted, errorMessage = null) }
    }

    fun handleUploadedFile(fileName: String, content: String) {
        val extractedKey = cleanAndExtractKey(content)
        val extractedUuid = extractUuid(content)
        if (extractedKey.isNotBlank()) {
            _uiState.update {
                it.copy(
                    uploadedKey = extractedKey,
                    uploadedFileName = fileName,
                    uploadedUuid = extractedUuid,
                    errorMessage = null
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    errorMessage = "Could not find a valid identity key in $fileName. Make sure it contains a 'token' or 'hu-' key."
                )
            }
        }
    }

    fun extractUuid(rawInput: String): String? {
        val uuidMatch = """"uuid"\s*:\s*"([^"]+)"""".toRegex().find(rawInput)
            ?: """"id"\s*:\s*"([^"]+)"""".toRegex().find(rawInput)
        return uuidMatch?.groupValues?.get(1)?.trim()
    }

    fun clearUploadedFile() {
        _uiState.update {
            it.copy(uploadedKey = null, uploadedFileName = null, uploadedUuid = null, errorMessage = null)
        }
    }

    fun toggleKeyVisibility() {
        _uiState.update { it.copy(isKeyVisible = !it.isKeyVisible) }
    }

    fun setInputMode(mode: KeyInputMode) {
        _uiState.update { it.copy(inputMode = mode, errorMessage = null) }
    }

    fun pasteClawKey(pastedText: String) {
        val extracted = cleanAndExtractKey(pastedText)
        if (ClawCrypto.isValidClawKey(extracted)) {
            _uiState.update { it.copy(clawKey = extracted, errorMessage = null) }
        } else {
            _uiState.update {
                it.copy(
                    clawKey = extracted,
                    errorMessage = "Invalid ClawKey format. Must begin with 'hu-' followed by 64 hexadecimal characters."
                )
            }
        }
    }

    fun cleanAndExtractKey(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ""

        val tokenJsonMatch = """"token"\s*:\s*"([^"]+)"""".toRegex().find(trimmed)
            ?: """"key"\s*:\s*"([^"]+)"""".toRegex().find(trimmed)
            ?: """"secret"\s*:\s*"([^"]+)"""".toRegex().find(trimmed)
            ?: """"identityKey"\s*:\s*"([^"]+)"""".toRegex().find(trimmed)
        if (tokenJsonMatch != null) {
            val extracted = tokenJsonMatch.groupValues[1].trim()
            if (extracted.isNotEmpty()) return extracted
        }

        val keyPatternMatch = """(hu-|lb-)[a-zA-Z0-9_-]+""".toRegex().find(trimmed)
        if (keyPatternMatch != null) {
            return keyPatternMatch.value.trim()
        }

        return trimmed.removeSurrounding("\"").removeSurrounding("'").trim()
    }

    fun connectToServer(onSuccess: (serverUrl: String, hashedKey: String) -> Unit) {
        val state = _uiState.value
        val keyToUse = state.effectiveKey

        if (!ClawCrypto.isValidClawKey(keyToUse)) {
            _uiState.update {
                it.copy(errorMessage = "Please enter or upload a valid 67-character ClawKey (hu-...)")
            }
            return
        }

        if (state.host.isBlank()) {
            _uiState.update {
                it.copy(isUrlValid = false, urlErrorMessage = "Server host address cannot be blank")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val hashedKey = ClawCrypto.hashHumanKey(keyToUse)
                val targetUrl = state.serverUrl

                if (appContainer != null) {
                    val client = appContainer.getClient(targetUrl)
                    val authResult = client.authenticate(keyHash = hashedKey, uuid = state.uploadedUuid)

                    if (authResult.isSuccess) {
                        val sessionData = authResult.getOrThrow()
                        val derivedShellKey = appContainer.cryptoEngine.deriveShellKey(
                            huKey = keyToUse,
                            userUuid = sessionData.user.uuid
                        )
                        appContainer.deviceVault.saveSession(
                            token = sessionData.token,
                            serverUrl = targetUrl,
                            ownerUuid = sessionData.user.uuid,
                            username = sessionData.user.username,
                            hashedKey = hashedKey,
                            shellKey = derivedShellKey
                        )

                        // Trigger initial sync in background
                        try {
                            appContainer.syncRepository.syncAll(sessionData.user.uuid)
                        } catch (_: Exception) {}

                        _uiState.update { it.copy(isLoading = false, isConnectionSuccess = true) }
                        onSuccess(targetUrl, hashedKey)
                    } else {
                        val errorMsg = authResult.exceptionOrNull()?.message ?: "Authentication rejected by server"
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = errorMsg
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, isConnectionSuccess = true) }
                    onSuccess(targetUrl, hashedKey)
                }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Connection error: ${e.message ?: "Failed to connect to server"}"
                    )
                }
            }
        }
    }
}
