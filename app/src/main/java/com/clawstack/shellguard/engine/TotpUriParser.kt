package com.clawstack.shellguard.engine

import java.net.URLDecoder

data class ParsedTotpData(
    val title: String,
    val secret: String,
    val issuer: String,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Long = 30L
)

object TotpUriParser {

    /**
     * Parses standard otpauth://totp/... and otpauth://hotp/... URIs.
     * Falls back to raw Base32 secret string handling.
     */
    fun parse(uriString: String): ParsedTotpData? {
        val trimmed = uriString.trim()
        if (trimmed.isEmpty()) return null

        if (!trimmed.startsWith("otpauth://", ignoreCase = true)) {
            // Assume raw Base32 secret string if >= 8 chars
            val sanitized = trimmed.replace(" ", "").replace("-", "").uppercase()
            return if (sanitized.length >= 8 && sanitized.all { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567=" }) {
                ParsedTotpData(
                    title = "Verification Code",
                    secret = sanitized,
                    issuer = ""
                )
            } else null
        }

        return try {
            val questionMarkIndex = trimmed.indexOf('?')
            val pathPart = if (questionMarkIndex != -1) {
                trimmed.substring(0, questionMarkIndex)
            } else {
                trimmed
            }
            val queryPart = if (questionMarkIndex != -1 && questionMarkIndex < trimmed.length - 1) {
                trimmed.substring(questionMarkIndex + 1)
            } else {
                ""
            }

            // Path format: otpauth://totp/Issuer:Account or otpauth://totp/Account
            val prefixEnd = pathPart.indexOf("://") + 3
            val slashAfterType = pathPart.indexOf('/', prefixEnd)
            val rawLabel = if (slashAfterType != -1 && slashAfterType < pathPart.length - 1) {
                pathPart.substring(slashAfterType + 1)
            } else {
                "Verification Code"
            }

            val decodedLabel = try {
                URLDecoder.decode(rawLabel, "UTF-8")
            } catch (_: Exception) {
                rawLabel
            }

            // Parse query parameters manually for headless portability
            val queryParams = mutableMapOf<String, String>()
            if (queryPart.isNotBlank()) {
                queryPart.split('&').forEach { param ->
                    val eqIndex = param.indexOf('=')
                    if (eqIndex != -1) {
                        val key = param.substring(0, eqIndex).trim().lowercase()
                        val value = param.substring(eqIndex + 1).trim()
                        val decodedVal = try {
                            URLDecoder.decode(value, "UTF-8")
                        } catch (_: Exception) {
                            value
                        }
                        queryParams[key] = decodedVal
                    }
                }
            }

            val secret = queryParams["secret"] ?: return null
            val issuerParam = queryParams["issuer"] ?: ""
            val algorithm = queryParams["algorithm"]?.uppercase() ?: "SHA1"
            val digits = queryParams["digits"]?.toIntOrNull() ?: 6
            val period = queryParams["period"]?.toLongOrNull() ?: 30L

            var label = decodedLabel
            var issuer = issuerParam

            if (label.contains(":")) {
                val parts = label.split(":", limit = 2)
                if (issuer.isBlank()) {
                    issuer = parts[0].trim()
                }
                label = parts[1].trim()
            }

            ParsedTotpData(
                title = if (label.isNotBlank()) label else "Verification Code",
                secret = secret.replace("=", "").replace(" ", "").replace("-", "").uppercase(),
                issuer = issuer,
                algorithm = algorithm,
                digits = digits,
                period = period
            )
        } catch (e: Exception) {
            null
        }
    }
}
