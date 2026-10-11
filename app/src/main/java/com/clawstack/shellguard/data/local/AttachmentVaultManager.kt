package com.clawstack.shellguard.data.local

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class AttachmentVaultManager(private val context: Context) {
    private val attachmentDir = File(context.filesDir, "vault_attachments").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    fun getAttachmentFile(id: String): File {
        return File(attachmentDir, "$id.enc")
    }

    fun writeEncryptedBytes(id: String, bytes: ByteArray): File {
        val file = getAttachmentFile(id)
        file.writeBytes(bytes)
        return file
    }

    fun readEncryptedBytes(id: String): ByteArray? {
        val file = getAttachmentFile(id)
        return if (file.exists() && file.isFile) {
            file.readBytes()
        } else null
    }

    fun getEncryptedInputStream(id: String): InputStream? {
        val file = getAttachmentFile(id)
        return if (file.exists() && file.isFile) {
            file.inputStream()
        } else null
    }

    fun getEncryptedOutputStream(id: String): OutputStream {
        val file = getAttachmentFile(id)
        return file.outputStream()
    }

    fun deleteEncryptedFile(id: String): Boolean {
        val file = getAttachmentFile(id)
        return if (file.exists()) {
            file.delete()
        } else false
    }

    fun clearAll() {
        if (attachmentDir.exists() && attachmentDir.isDirectory) {
            attachmentDir.listFiles()?.forEach { it.delete() }
        }
    }
}
