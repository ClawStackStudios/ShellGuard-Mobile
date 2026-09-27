package com.clawstack.shellguard.engine

/**
 * RFC 4648 compliant Base32 Decoder for ShellGuard Mobile.
 *
 * Sanitizes Base32 secret keys by stripping spaces, hyphens, and padding ('='),
 * converting 5-bit characters to raw byte arrays for HMAC key derivation.
 */
object Base32Decoder {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val DECODE_TABLE = IntArray(128) { -1 }.apply {
        for (i in ALPHABET.indices) {
            this[ALPHABET[i].code] = i
        }
    }

    fun decode(encoded: String): ByteArray {
        val cleanInput = encoded.trim().replace("=", "").replace(" ", "").replace("-", "").uppercase()
        if (cleanInput.isEmpty()) return ByteArray(0)

        val output = ArrayList<Byte>()
        var buffer = 0
        var bitsLeft = 0

        for (ch in cleanInput) {
            val charCode = ch.code
            if (charCode >= DECODE_TABLE.size || DECODE_TABLE[charCode] < 0) {
                continue
            }
            val value = DECODE_TABLE[charCode]
            buffer = (buffer shl 5) or value
            bitsLeft += 5

            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return output.toByteArray()
    }
}
