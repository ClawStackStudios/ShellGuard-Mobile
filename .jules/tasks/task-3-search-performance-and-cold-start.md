# 📋 Task 3: Cold-Start Routing, Reconnect Recovery & Sub-16ms Search Performance

> **Target Objective**: Optimize in-memory search across the vault to guarantee sub-16ms frame times across 150+ items. Harden cold-start routing to transition immediately to cached Room data in offline mode when disconnected, and ensure seamless automated reconnect synchronization when connectivity resumes without UI stutter or modal errors.

---

## 🏛️ Invariants & Rules
1. **Zero Crypto Seam Mutation**: Plaintext secrets and cryptographic envelopes (`AES-GCM-256`) remain untouched. Do NOT edit `ShellCryptionEngine.kt`.
2. **Zero Database Migrations**: Room schema version remains at version 1. In-memory search index is constructed during Room Flow mapping, not persisted to disk.
3. **Bitwarden Offline Mutation Guard**: If offline (`SyncStatus.OFFLINE_READ_ONLY`), mutations remain disabled. Do not bypass the offline read-only guard.
4. **⚠️ Invariant**: NEVER tag `@jules` in PR comments or commit messages. Plain text `Hey Jules` or `@google-labs-jules[bot]` only.
5. **Attribution Format**: All commits must follow the two-layer format (`User: ...` / `AI: ...`).

---

## 📁 Files in Scope (Strictly Orthogonal to Task 4)
- `app/src/main/java/com/clawstack/shellguard/data/repository/SyncRepository.kt`
- `app/src/main/java/com/clawstack/shellguard/ui/screens/dashboard/VaultDashboardViewModel.kt`
- `app/src/main/java/com/clawstack/shellguard/ui/screens/gateway/GatewayViewModel.kt`
- `app/src/test/java/com/clawstack/shellguard/ui/screens/dashboard/VaultDashboardViewModelTest.kt`

---

## 🛠️ Step-by-Step Implementation Guide

### 1. Pre-Normalized Search Index (`SyncRepository.kt`)
In `UnifiedVaultItem`:
```kotlin
data class UnifiedVaultItem(
    val id: String,
    val ownerUuid: String,
    val title: String,
    val subtitle: String,
    val domain: VaultItemDomain,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val reprompt: Boolean = false,
    val hasAttachments: Boolean = false,
    val localUpdatedAt: Long = 0L,
    val remoteUpdatedAt: Long = 0L,
    val normalizedSearchIndex: String = buildSearchIndex(title, subtitle, category, tags)
) {
    companion object {
        fun buildSearchIndex(title: String, subtitle: String, category: String?, tags: List<String>): String {
            return buildString {
                append(title.lowercase().trim())
                append(" ")
                append(subtitle.lowercase().trim())
                if (!category.isNullOrBlank()) {
                    append(" ")
                    append(category.lowercase().trim())
                }
                if (tags.isNotEmpty()) {
                    append(" ")
                    append(tags.joinToString(" ") { it.lowercase().trim() })
                }
            }
        }
    }
}
```
In `observeUnifiedItems(ownerUuid)`:
Construct `UnifiedVaultItem` using the helper or let the default parameter calculate `normalizedSearchIndex` once upon entity mapping.

---

### 2. Debounced Sub-16ms Filtering & Offline Error Suppression (`VaultDashboardViewModel.kt`)
1. **Debounced Query & Dispatchers.Default**:
   ```kotlin
   @OptIn(kotlinx.coroutines.FlowPreview::class)
   private val debouncedSearchQuery = _uiState
       .map { it.searchQuery.trim().lowercase() }
       .debounce(80)
       .distinctUntilChanged()

   val filteredItems: StateFlow<List<UnifiedVaultItem>> = combine(
       allItems,
       _uiState.map { it.selectedPod }.distinctUntilChanged(),
       debouncedSearchQuery
   ) { items, selectedPod, query ->
       val tokens = if (query.isEmpty()) emptyList() else query.split("\\s+".toRegex()).filter { it.isNotBlank() }
       items.filter { item ->
           val matchesPod = when (selectedPod) {
               PodFilter.ALL -> true
               PodFilter.PASSWORDS -> item.domain == VaultItemDomain.PASSWORD
               PodFilter.NOTES -> item.domain == VaultItemDomain.NOTE
               PodFilter.SSH_KEYS -> item.domain == VaultItemDomain.SSH_KEY
               PodFilter.ATTACHMENTS -> item.hasAttachments
           }
           val matchesSearch = tokens.isEmpty() || tokens.all { token ->
               item.normalizedSearchIndex.contains(token)
           }
           matchesPod && matchesSearch
       }
   }
   .flowOn(kotlinx.coroutines.Dispatchers.Default)
   .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
   ```

2. **Graceful Offline Error Handling**:
   In `triggerSync()`:
   ```kotlin
   fun triggerSync() {
       if (ownerUuid.isBlank()) return
       viewModelScope.launch {
           _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
           val result = appContainer.syncRepository.syncAll(ownerUuid)
           _uiState.update { current ->
               val hasCachedItems = allItems.value.isNotEmpty()
               val isOfflineProbeFailure = result.isFailure && result.exceptionOrNull()?.message?.contains("health probe") == true
               current.copy(
                   isSyncing = false,
                   errorMessage = if (isOfflineProbeFailure && hasCachedItems) null else result.exceptionOrNull()?.message
               )
           }
       }
   }
   ```

---

### 3. Gateway Cold-Start Auto-Fill & Key Validation (`GatewayViewModel.kt`)
Harden `GatewayViewModel` so that when initialized, if a saved server URL or Base62 key exists in `EncryptedDeviceVault`, it pre-populates host, port, and protocol, and validates the key format using `ClawCrypto.isValidClawKey()`.

---

### 4. Search Benchmark Test Suite (`VaultDashboardViewModelTest.kt`)
Create `app/src/test/java/com/clawstack/shellguard/ui/screens/dashboard/VaultDashboardViewModelTest.kt`:
1. Use Robolectric `@Config(sdk = [34])`.
2. Seed 150 dummy `UnifiedVaultItem` entities.
3. Benchmark search query execution time across 10 iterations; assert average filtering duration is `< 16ms`.
4. Verify tokenized multi-word search matches items containing tokens in any order.
5. Verify offline health probe error is suppressed when cached items are present.

---

## 🚦 Verification Gate
Run the following checks to confirm everything is green:
```bash
python3 scripts/audit_docs_drift.py
./gradlew testDebugUnitTest --tests "com.clawstack.shellguard.ui.screens.dashboard.*"
```
Ensure all tests pass 100% green before submitting your PR!
