# 📋 Task 1: Multi-URI & Attachment UI/UX + Autofill Hook

> **Target Objective**: Connect the completed Data/Sync layer for Multi-URIs and Encrypted Attachments to user-facing UI screens and the Android Autofill framework. Implement dynamic Multi-URI builders, attachment picking/viewing cards, dashboard category filtering, and secondary-URI autofill matching.

---

## 🏛️ Invariants & Rules
1. **Zero Raw BLOBs in Room**: Attachment files stream directly to `context.filesDir/vault_attachments/{id}.enc`. Room stores metadata only (`size_bytes`, `mime_type`, `file_name`).
2. **Compose Modularity**: Follow the UI modularity standard in [JULES.md](../JULES.md). Decompose complex screen sections into dedicated sub-composables (`MultiUriEditorSection`, `AttachmentPickerSection`, etc.) to keep files below the 500-line ceiling.
3. **⚠️ Invariant**: NEVER tag `@jules` in PR comments or commit messages. Plain text `Hey Jules` or `@google-labs-jules[bot]` only.
4. **Attribution Format**: All commits must follow the two-layer format (`User: ...` / `AI: ...`).

---

## 📁 Files in Scope (Strictly Orthogonal)
- `app/src/main/java/com/clawstack/shellguard/ui/screens/form/ItemFormViewModel.kt`
- `app/src/main/java/com/clawstack/shellguard/ui/screens/form/ItemFormScreen.kt`
- `app/src/main/java/com/clawstack/shellguard/ui/screens/detail/ItemDetailViewModel.kt`
- `app/src/main/java/com/clawstack/shellguard/ui/screens/detail/ItemDetailScreen.kt`
- `app/src/main/java/com/clawstack/shellguard/ui/screens/dashboard/VaultDashboardViewModel.kt`
- `app/src/main/java/com/clawstack/shellguard/ui/screens/dashboard/VaultDashboardScreen.kt`
- `app/src/main/java/com/clawstack/shellguard/services/autofill/ShellGuardAutofillService.kt`
- `app/src/test/java/com/clawstack/shellguard/ui/screens/form/ItemFormViewModelTest.kt`

---

## 🛠️ Step-by-Step Implementation Guide

### 1. Item Form State & Logic (`ItemFormViewModel.kt`)
1. **Extend `ItemFormUiState`**:
   ```kotlin
   data class ItemFormUiState(
       // existing fields...
       val uris: List<String> = emptyList(),
       val stagedAttachments: List<AttachmentSummary> = emptyList(), // or attachment IDs / metadata
       // ...
   )
   ```
2. **Multi-URI Handlers**:
   - `fun addUri(uri: String = "")`: Appends a blank or new URI string to `uris`.
   - `fun updateUri(index: Int, uri: String)`: Updates the URI at `index`.
   - `fun removeUri(index: Int)`: Removes the URI at `index`.
3. **Attachment Handlers**:
   - `fun stageAttachment(contentUri: Uri, context: Context)`:
     Reads the input stream via `context.contentResolver`, extracts display name and size, encrypts & stages via `appContainer.syncRepository.stageAttachment`, and adds the resulting attachment record/ID to `stagedAttachments`.
   - `fun removeAttachment(attachmentId: String)`: Removes from `stagedAttachments`.
4. **Save Integration**:
   - When calling `syncRepository.stageOrUpdatePearl(...)`:
     Serialize `uris` to JSON array string (e.g. `Json.encodeToString(uiState.value.uris)` or empty string) and pass as `uris`.
     Serialize attachment IDs to JSON array string and pass as `attachments`.
   - When calling `syncRepository.stageOrUpdateNote(...)`:
     Serialize attachment IDs to JSON array string and pass as `attachments`.

---

### 2. Item Form UI (`ItemFormScreen.kt`)
1. **Multi-URI Section** (Only visible for `VaultItemDomain.PASSWORD`):
   - Render a card or block titled **"Secondary URLs & Domains"**.
   - List each URI with an OutlinedTextField and a delete icon button (`Icons.Default.Delete` or `Icons.Default.Close`).
   - Provide an **"Add URL"** button (`+ Add URL`) with dashed border or secondary action button.
2. **Encrypted Attachments Section** (Visible for Passwords and Notes):
   - Render a card or block titled **"Encrypted Attachments"**.
   - Use `rememberLauncherForActivityResult(ActivityResultContracts.GetContent())` to open the system file picker (*/*).
   - Display a list of attached files: icon (`Icons.Default.AttachFile`), file name, formatted size (KB/MB), and a remove icon button.
   - Provide an **"Add File Attachment"** button that launches the file picker.

---

### 3. Item Detail State & Logic (`ItemDetailViewModel.kt`)
1. **Extend `ItemDetailUiState.Success`**:
   ```kotlin
   data class Success(
       // existing fields...
       val uris: List<String> = emptyList(),
       val attachments: List<AttachmentItemDetail> = emptyList(), // ID, fileName, sizeBytes, mimeType
       // ...
   ) : ItemDetailUiState
   ```
2. **Load Item Enhancement**:
   - When loading `PearlDetail`: populate `uris = pearl.uris`. For each ID in `pearl.attachments`, fetch attachment metadata from `syncRepository.getAttachment(id)` and populate `attachments`.
   - When loading `SecureNoteDetail`: for each ID in `note.attachments`, fetch attachment metadata and populate `attachments`.
3. **Open / Decrypt Attachment Action**:
   - `fun openAttachment(context: Context, attachmentId: String, onReady: (Uri, String) -> Unit)`:
     Decrypts the attachment ciphertext, writes the decrypted bytes to a secure temporary file in `File(context.cacheDir, "decrypted_attachments/${fileName}")`, creates a content URI using `FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)`, and calls `onReady(contentUri, mimeType)`.

---

### 4. Item Detail UI (`ItemDetailScreen.kt`)
1. **Multi-URI Section**:
   - Display secondary URIs in styled cards or chip rows.
   - For each URI, provide an **"Open"** action (launches browser intent `Intent(Intent.ACTION_VIEW, Uri.parse(uri))`) and a **"Copy"** action (copies to clipboard with snackbar confirmation).
2. **Attachments Section**:
   - Display each linked attachment in a card:
     - Document/attachment icon.
     - File name (bold).
     - Size in KB/MB and MIME type pill/badge.
     - **"View / Open"** action button: calls `viewModel.openAttachment`, which triggers an `Intent.ACTION_VIEW` intent with `FLAG_GRANT_READ_URI_PERMISSION`.

---

### 5. Vault Dashboard Category Pod Chip (`VaultDashboardViewModel.kt` & `VaultDashboardScreen.kt`)
1. In `VaultDashboardViewModel.kt`:
   - Add `ATTACHMENTS("Attachments")` to the `PodFilter` enum:
     ```kotlin
     enum class PodFilter(val label: String) {
         ALL("All"),
         PASSWORDS("Passwords"),
         NOTES("Notes"),
         SSH_KEYS("SSH Keys"),
         ATTACHMENTS("Attachments")
     }
     ```
   - In category counts calculation, calculate total attachments count from `database.secureAttachmentDao()`.
   - When `ATTACHMENTS` pod filter is selected, filter dashboard items to display items that contain attachments (or display attachments directly).

---

### 6. System Autofill Multi-URI Matching (`ShellGuardAutofillService.kt`)
In `ShellGuardAutofillService.kt`, update the URL candidate filter loop:
```kotlin
val matchedPearls = allPearls.filter { pearl ->
    val primaryMatch = DomainMatcher.matchesAnyUri(pearl.url, pearl.uris, target)
    val packageMatch = targetPackage != null && DomainMatcher.matchesAnyUri(pearl.url, pearl.uris, "androidapp://$targetPackage")
    primaryMatch || packageMatch
}.take(5)
```
This enables any secondary URL configured on a Pearl to automatically trigger Android Autofill suggestions!

---

## 🧪 Verification Gate
Run and ensure 100% green:
```bash
./gradlew testDebugUnitTest --no-daemon
./gradlew assembleDebug --no-daemon
```
