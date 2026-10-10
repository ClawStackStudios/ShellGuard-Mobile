package com.clawstack.shellguard.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clawstack.shellguard.data.repository.SyncStatus
import com.clawstack.shellguard.data.repository.UnifiedVaultItem
import com.clawstack.shellguard.data.repository.VaultItemDomain
import com.clawstack.shellguard.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PodFilter(val label: String) {
    ALL("All"),
    PASSWORDS("Passwords"),
    NOTES("Notes"),
    SSH_KEYS("SSH Keys"),
    ATTACHMENTS("Attachments")
}

data class DashboardUiState(
    val searchQuery: String = "",
    val selectedPod: PodFilter = PodFilter.ALL,
    val isSearching: Boolean = false,
    val isSyncing: Boolean = false,
    val username: String = "",
    val serverUrl: String = "",
    val errorMessage: String? = null
)

class VaultDashboardViewModel(
    private val appContainer: AppContainer
) : ViewModel() {

    private val ownerUuid = appContainer.deviceVault.getOwnerUuid().orEmpty()
    private val _uiState = MutableStateFlow(
        DashboardUiState(
            username = appContainer.deviceVault.getUsername().orEmpty(),
            serverUrl = appContainer.deviceVault.getServerUrl().orEmpty()
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = appContainer.syncRepository.syncStatus

    private val allItems: StateFlow<List<UnifiedVaultItem>> =
        appContainer.syncRepository.observeUnifiedItems(ownerUuid)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredItems: StateFlow<List<UnifiedVaultItem>> = combine(
        allItems,
        _uiState
    ) { items, state ->
        items.filter { item ->
            val matchesPod = when (state.selectedPod) {
                PodFilter.ALL -> true
                PodFilter.PASSWORDS -> item.domain == VaultItemDomain.PASSWORD
                PodFilter.NOTES -> item.domain == VaultItemDomain.NOTE
                PodFilter.SSH_KEYS -> item.domain == VaultItemDomain.SSH_KEY
                PodFilter.ATTACHMENTS -> item.tags.contains("has_attachment") // Future proofing tag filtering
            }

            val query = state.searchQuery.trim().lowercase()
            val matchesSearch = query.isEmpty() ||
                item.title.lowercase().contains(query) ||
                item.subtitle.lowercase().contains(query) ||
                item.category?.lowercase()?.contains(query) == true

            matchesPod && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val attachmentCount: StateFlow<Int> = appContainer.database.secureAttachmentDao().observeItemCount(ownerUuid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val podCounts: StateFlow<Map<PodFilter, Int>> = combine(allItems, attachmentCount) { items, attCount ->
        mapOf(
            PodFilter.ALL to items.size,
            PodFilter.PASSWORDS to items.count { it.domain == VaultItemDomain.PASSWORD },
            PodFilter.NOTES to items.count { it.domain == VaultItemDomain.NOTE },
            PodFilter.SSH_KEYS to items.count { it.domain == VaultItemDomain.SSH_KEY },
            PodFilter.ATTACHMENTS to attCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        triggerSync()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectPod(pod: PodFilter) {
        _uiState.update { it.copy(selectedPod = pod) }
    }

    fun triggerSync() {
        if (ownerUuid.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            val result = appContainer.syncRepository.syncAll(ownerUuid)
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun lockVault(onLocked: () -> Unit) {
        appContainer.deviceVault.zeroizeMemory()
        appContainer.vaultLockManager.lockVaultNow()
        onLocked()
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            appContainer.deviceVault.clearSession()
            onLoggedOut()
        }
    }
}
