# 📋 Task 1: Full Server Parity — Encrypted Attachments & Multi-URI Bridge

> **Target Objective**: Achieve 100% data layer and synchronization parity with the ShellGuard server database across all 4 core tables (`vault_pearls`, `vault_secure_notes`, `vault_ssh_keys`, and `vault_secure_attachments`). Uphold the CWE-400 2MB CursorWindow defense by streaming ciphertext files directly to internal storage while maintaining reactive Room metadata.

---

## 🔍 Exact Web Server Contracts (Source of Truth)

The server route is `/api/attachments` (`src/server/routes/attachments.ts`):
1. **`GET /api/attachments`**:
   - Returns metadata only:
     ```json
     {
       "success": true,
       "data": [
         {
           "id": "att-uuid-1",
           "title": "Document Title",
           "file_name": "contract.pdf",
           "size_bytes": 1048576,
           "mime_type": "application/pdf",
           "category": "",
           "created_at": "2026-10-09T20:00:00.000Z"
         }
       ]
     }
     ```
2. **`GET /api/attachments/:id/file`**:
   - Streams raw binary ciphertext with `Content-Type: application/octet-stream`.
   - The ciphertext is the raw UTF-8 bytes of the ShellCryption envelope (`{"v":1,"alg":"AES-GCM-256","iv":"...","ct":"...","aad":"vault_secure_attachments:<id>"}`).
3. **`POST /api/attachments`**:
   - Requires `multipart/form-data`.
   - Fields: `id` (string <= 64 chars), `title` (string <= 255 chars), `file_name` (string <= 512 chars), `mime_type` (string <= 255 chars), `category` (string <= 64 chars).
   - Binary file part: `file_data` (containing the encrypted ShellCryption envelope UTF-8 bytes). Max 500MB per file.
   - Response: `{"success": true, "data": {"id": "...", "title": "...", "category": "...", "size_bytes": 12345}}`.
4. **`DELETE /api/attachments/:id`**:
   - Deletes the attachment. Response: `{"success": true, "data": {"message": "Attachment removed."}}`.

---

## 🛠️ Step-by-Step Implementation Requirements

### 1. Network Models (`app/src/main/java/com/clawstack/shellguard/data/remote/models/ShellResponse.kt`)
Add the following models:
```kotlin
@Serializable
data class AttachmentDto(
    val id: String,
    val owner_uuid: String = "",
    val title: String,
    val size_bytes: Long = 0L,
    val file_name: String = "",
    val mime_type: String = "application/octet-stream",
    val category: String? = null,
    val created_at: String = ""
)

@Serializable
data class AttachmentUploadData(
    val id: String,
    val title: String,
    val category: String? = null,
    val size_bytes: Long = 0L
)

@Serializable
data class CreateAttachmentResponse(
    val success: Boolean,
    val data: AttachmentUploadData? = null,
    val error: String? = null
)

@Serializable
data class AttachmentsResponse(
    val success: Boolean,
    val data: List<AttachmentDto> = emptyList(),
    val error: String? = null
)

@Serializable
data class AttachmentItemResponse(
    val success: Boolean,
    val data: AttachmentDto? = null,
    val error: String? = null
)
```
Also update existing requests:
- In `CreateVaultItemRequest`: add `val attachments: String? = null`
- In `CreateNoteRequest`: add `val attachments: String? = null`

---

### 2. Ktor Client Methods (`app/src/main/java/com/clawstack/shellguard/data/remote/ShellGuardClient.kt`)
Implement the following methods on `ShellGuardClient`:
```kotlin
open suspend fun fetchAttachments(sessionToken: String): Result<List<AttachmentDto>>
open suspend fun downloadAttachmentFile(sessionToken: String, id: String, destinationFile: java.io.File): Result<java.io.File>
open suspend fun uploadAttachmentMultipart(
    sessionToken: String,
    id: String,
    title: String,
    fileName: String,
    mimeType: String,
    category: String,
    ciphertextBytes: ByteArray
): Result<AttachmentUploadData>
open suspend fun deleteAttachment(sessionToken: String, id: String): Result<Boolean>
```
*Note on Ktor Multipart Upload*:
Use `client.submitFormWithBinaryData` to send `id`, `title`, `file_name`, `mime_type`, `category` and binary `file_data` (with Content-Disposition filename).

---

### 3. Hybrid Storage Manager (`app/src/main/java/com/clawstack/shellguard/data/local/AttachmentVaultManager.kt`)
Create a helper class to manage ciphertext files in `context.filesDir/vault_attachments`:
- Encrypted file location: `File(context.filesDir, "vault_attachments/${id}.enc")`
- `fun writeEncryptedBytes(id: String, bytes: ByteArray): File`
- `fun readEncryptedBytes(id: String): ByteArray?`
- `fun deleteEncryptedFile(id: String): Boolean`
- `fun clearAll(): Unit`

---

### 4. SyncRepository Integration (`app/src/main/java/com/clawstack/shellguard/data/repository/SyncRepository.kt`)
1. **Pass `attachments` & `uris` on Push**:
   - In `pushPendingChanges`:
     - When constructing `CreateVaultItemRequest`: include `attachments = item.attachments.ifBlank { null }` and `uris = item.uris.ifBlank { null }`.
     - When constructing `CreateNoteRequest`: include `attachments = item.attachments.ifBlank { null }`.
2. **Sync Pending Attachments**:
   - In `pushPendingChanges`:
     - Drain pending deletes: for items in `database.secureAttachmentDao().getPendingSyncItems(ownerUuid)` or pending deletes, if marked for delete, invoke `client.deleteAttachment(sessionToken, id)`.
     - Drain pending uploads: for items with `syncState == "PENDING_SYNC"`, read their encrypted bytes from disk via `AttachmentVaultManager`, call `client.uploadAttachmentMultipart`, and update Room `syncState = "SYNCED"`.
3. **Downstream Delta Pull for Attachments**:
   - In `syncAll`:
     - Call `client.fetchAttachments(sessionToken)`.
     - Upsert remote `AttachmentDto`s to Room `SecureAttachmentDao` as `SecureAttachmentEntity`.
     - Prune deleted remote attachments: remove obsolete records from Room, and delete their `.enc` files from disk!
4. **Decrypted Attachment Access Helpers**:
   - `suspend fun getAttachment(id: String): Result<SecureAttachmentEntity>`
   - `suspend fun stageAttachment(title: String, fileName: String, mimeType: String, category: String, rawDataUrlOrBytes: String): Result<SecureAttachmentEntity>`
   - `suspend fun decryptAttachment(id: String): Result<String>` (decrypts envelope using `AadNamespace.secureAttachment(id)` with `shellKey`).

---

### 5. Multi-URI & DomainMatcher Support
1. In `app/src/main/java/com/clawstack/shellguard/domain/models/VaultDomainModels.kt`:
   - In `PearlDetail`: add `val uris: List<String> = emptyList()` and `val attachments: List<String> = emptyList()`.
   - In `SecureNoteDetail`: add `val attachments: List<String> = emptyList()`.
2. In `app/src/main/java/com/clawstack/shellguard/domain/matcher/DomainMatcher.kt`:
   - Add `matchesAnyUri(primaryUrl: String, urisJson: String?, requestedUrlOrPackage: String, matchMode: UriMatchMode = UriMatchMode.BASE_DOMAIN): Boolean`.
   - If `primaryUrl` matches, return true. Otherwise parse `urisJson` (array of URL strings) and test each URL.

---

### 6. Verification Gate
Run and ensure 100% green:
```bash
./gradlew testDebugUnitTest --no-daemon
./gradlew assembleDebug --no-daemon
```
