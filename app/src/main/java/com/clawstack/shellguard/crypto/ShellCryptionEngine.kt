package com.clawstack.shellguard.crypto

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Standardized ShellCryption Envelope Schema
 */
@Serializable
data class ShellCryptionEnvelope(
    val v: Int = 1,
    val alg: String = "AES-GCM-256",
    val iv: String,
    val ct: String,
    val aad: String
)

/**
 * ShellCryption Cryptographic Engine
 *
 * Implements HKDF-SHA-256 key derivation and AES-GCM-256 encryption/decryption
 * with strict Additional Authenticated Data (AAD) namespace enforcement.
 * Maintains 100% cryptographic parity with the ShellGuard Web and TOTP clients.
 */
object ShellCryptionEngine {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val HKDF_INFO = "clawchives-shellcryption-v1"
    private const val GCM_IV_LENGTH_BYTES = 12

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Canonical AAD Namespaces
    object AadNamespace {
        fun pearlSecret(id: String) = "vault_pearls:$id"
        fun pearlTotp(id: String) = "vault_pearls_totp:$id"
        fun pearlCustomFields(id: String) = "vault_pearls_custom:$id"
        fun pearlPasswordHistory(id: String) = "vault_pearls_history:$id"
        fun secureNoteContent(id: String) = "vault_secure_notes:$id"
        fun secureNoteCustomFields(id: String) = "vault_secure_notes_custom:$id"
        fun sshKeyPrivate(id: String) = "vault_ssh_keys:$id"
        fun sshKeyCustomFields(id: String) = "vault_ssh_keys_custom:$id"
        fun secureAttachment(id: String) = "vault_secure_attachments:$id"
        fun totpBackup(ownerUuid: String) = "totp_backup:$ownerUuid"
    }

    /**
     * Derives a 32-byte (256-bit) Shell Key from a raw human ClawKey (`hu-`) and user UUID salt.
     * Uses HKDF-SHA-256 with info = "clawchives-shellcryption-v1".
     */
    fun deriveShellKey(huKey: String, userUuid: String): ByteArray {
        return hkdf(
            ikm = huKey.trim().toByteArray(StandardCharsets.UTF_8),
            salt = userUuid.trim().toByteArray(StandardCharsets.UTF_8),
            info = HKDF_INFO.toByteArray(StandardCharsets.UTF_8),
            length = 32
        )
    }

    /**
     * Encrypts plaintext string using AES-GCM-256 with a random 12-byte IV and binds the given AAD.
     * Returns a standardized ShellCryption JSON envelope string.
     */
    fun encryptField(plainText: String, shellKey: ByteArray, aad: String): String {
        require(shellKey.size == 32) { "Shell Key must be exactly 32 bytes (256 bits)" }
        require(aad.isNotBlank()) { "AAD must not be blank" }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKeySpec = SecretKeySpec(shellKey, "AES")
        val gcmParameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec)
        cipher.updateAAD(aad.toByteArray(StandardCharsets.UTF_8))

        val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        val envelope = ShellCryptionEnvelope(
            v = 1,
            alg = "AES-GCM-256",
            iv = base64Encode(iv),
            ct = base64Encode(cipherText),
            aad = aad
        )

        return json.encodeToString(envelope)
    }

    /**
     * Determines whether a raw string represents an encrypted ShellCryption envelope object.
     * Prevents deserialization crashes on plain JSON arrays (e.g. "[]") or empty strings.
     */
    fun isEncryptedEnvelope(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val trimmed = value.trim()
        return trimmed.startsWith("{") && trimmed.endsWith("}") &&
               trimmed.contains("\"v\":") && trimmed.contains("\"ct\":")
    }

    /**
     * Decrypts a ShellCryption JSON envelope string using AES-GCM-256 and asserts AAD equality.
     * Throws SecurityException if AAD mismatches or envelope tampering is detected.
     */
    fun decryptField(envelopeJson: String, shellKey: ByteArray, expectedAad: String): String {
        require(shellKey.size == 32) { "Shell Key must be exactly 32 bytes (256 bits)" }

        if (!isEncryptedEnvelope(envelopeJson)) {
            throw IllegalArgumentException("Invalid ShellCryption envelope: not a valid JSON envelope object")
        }

        val envelope = json.decodeFromString<ShellCryptionEnvelope>(envelopeJson)

        if (envelope.v != 1 || envelope.alg != "AES-GCM-256") {
            throw IllegalArgumentException("Unsupported envelope format: v=${envelope.v}, alg=${envelope.alg}")
        }

        if (envelope.aad != expectedAad) {
            throw SecurityException("AAD mismatch: Expected '$expectedAad', but envelope declared '${envelope.aad}'")
        }

        val iv = base64Decode(envelope.iv)
        val ct = base64Decode(envelope.ct)

        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKeySpec = SecretKeySpec(shellKey, "AES")
        val gcmParameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec)
        cipher.updateAAD(envelope.aad.toByteArray(StandardCharsets.UTF_8))

        val decrypted = cipher.doFinal(ct)
        return String(decrypted, StandardCharsets.UTF_8)
    }

    // HKDF Implementation (RFC 5869)
    private fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val saltKey = if (salt.isEmpty()) ByteArray(32) else salt
        mac.init(SecretKeySpec(saltKey, "HmacSHA256"))
        return mac.doFinal(ikm)
    }

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))

        var t = ByteArray(0)
        var okm = ByteArray(0)
        var i = 1

        while (okm.size < length) {
            mac.update(t)
            mac.update(info)
            mac.update(i.toByte())
            t = mac.doFinal()
            okm += t
            i++
        }

        return okm.copyOfRange(0, length)
    }

    private fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val prk = hkdfExtract(salt, ikm)
        return hkdfExpand(prk, info, length)
    }

    // Platform-agnostic Base64 helpers for JVM and Android compatibility
    private fun base64Encode(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (e: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    private fun base64Decode(str: String): ByteArray {
        return try {
            java.util.Base64.getDecoder().decode(str.trim())
        } catch (e: Throwable) {
            android.util.Base64.decode(str.trim(), android.util.Base64.DEFAULT)
        }
    }
}
