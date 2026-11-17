package com.clawstack.shellguard.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawstack.shellguard.data.repository.SyncStatus
import com.clawstack.shellguard.data.repository.UnifiedVaultItem
import com.clawstack.shellguard.data.repository.VaultItemDomain
import com.clawstack.shellguard.ui.theme.BorderSubtle
import com.clawstack.shellguard.ui.theme.BrandClawCyan
import com.clawstack.shellguard.ui.theme.OceanDark
import com.clawstack.shellguard.ui.theme.ReefPink
import com.clawstack.shellguard.ui.theme.StatusError
import com.clawstack.shellguard.ui.theme.StatusSuccess
import com.clawstack.shellguard.ui.theme.StatusWarning
import com.clawstack.shellguard.ui.theme.SurfaceContainerDark
import com.clawstack.shellguard.ui.theme.SurfaceDark
import com.clawstack.shellguard.ui.theme.TextMuted
import com.clawstack.shellguard.ui.theme.TextPrimary
import com.clawstack.shellguard.ui.theme.TextSecondary
import com.clawstack.shellguard.ui.theme.WarningBoxBg
import com.clawstack.shellguard.ui.theme.WarningText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(
    viewModel: VaultDashboardViewModel,
    modifier: Modifier = Modifier,
    onItemClick: (UnifiedVaultItem) -> Unit = {},
    onAddItemClick: (VaultItemDomain) -> Unit = {},
    onLockClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val pullToRefreshEnabled by viewModel.pullToRefreshEnabled.collectAsState()
    val filteredItems by viewModel.filteredItems.collectAsState()
    val podCounts by viewModel.podCounts.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var isFabExpanded by remember { mutableStateOf(false) }
    var showExitLockDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isFabExpanded) {
        isFabExpanded = false
    }

    BackHandler(enabled = !isFabExpanded && !showExitLockDialog) {
        showExitLockDialog = true
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(OceanDark),
        color = OceanDark
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // ── Top Bar with Brand and Server Connection Status ─────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(listOf(ReefPink, BrandClawCyan))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🐚", fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = ReefPink, fontWeight = FontWeight.Bold)) { append("Shell") }
                                    withStyle(SpanStyle(color = BrandClawCyan, fontWeight = FontWeight.Bold)) { append("Guard") }
                                    withStyle(SpanStyle(color = TextMuted, fontSize = 10.sp)) { append(" ©™") }
                                },
                                fontSize = 18.sp
                            )

                            // Server connection badge pill
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val statusColor = when (syncStatus) {
                                    SyncStatus.ONLINE_SYNCED -> StatusSuccess
                                    SyncStatus.OFFLINE_READ_ONLY -> StatusWarning
                                    SyncStatus.SYNCING -> BrandClawCyan
                                    SyncStatus.SYNC_ERROR -> StatusError
                                }
                                val statusLabel = when (syncStatus) {
                                    SyncStatus.ONLINE_SYNCED -> "Online Synced"
                                    SyncStatus.OFFLINE_READ_ONLY -> "Offline (Read-Only)"
                                    SyncStatus.SYNCING -> "Syncing..."
                                    SyncStatus.SYNC_ERROR -> "Sync Error"
                                }

                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = statusLabel,
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.lockVault(onLockClick) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = ReefPink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Search Bar ──────────────────────────────────────────────
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search pearls, notes, SSH keys...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("dashboard_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrect = false, capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ReefPink,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Horizontal Pod Category Filter Chips ────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PodFilter.entries.forEach { pod ->
                        val isSelected = uiState.selectedPod == pod
                        val count = podCounts[pod] ?: 0

                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ReefPink else SurfaceDark)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ReefPink else BorderSubtle,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.selectPod(pod) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${pod.label} ($count)",
                                color = if (isSelected) Color.White else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // ── Offline Read-Only Banner (Bitwarden Invariant) ───────────
                AnimatedVisibility(
                    visible = syncStatus == SyncStatus.OFFLINE_READ_ONLY,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = WarningBoxBg),
                            border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = WarningText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Offline Mode — Vault is read-only to prevent split-brain conflicts.",
                                    color = WarningText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // ── Sync Error Banner (Sanitized + Retryable) ────────────────
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.errorMessage.orEmpty(),
                                    color = StatusError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.triggerSync() },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(
                                        text = "Retry",
                                        color = BrandClawCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.dismissError() },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Dismiss error",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Vault Items List (Pull-to-Refresh) ──────────────────────
                val pullToRefreshState = rememberPullToRefreshState()
                val listContent: @Composable () -> Unit = {
                    if (filteredItems.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🐚", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) "No matching vault items" else "Your vault is empty",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) "Try adjusting your search query or pod filter" else "Pull down to sync with your server",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            if (uiState.searchQuery.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Text("Clear Search Filter", color = BrandClawCyan, fontSize = 14.sp)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                VaultItemCard(
                                    item = item,
                                    onClick = { onItemClick(item) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(72.dp)) // padding for FAB
                            }
                        }
                    }
                }

                if (pullToRefreshEnabled) {
                    PullToRefreshBox(
                        isRefreshing = uiState.isSyncing,
                        onRefresh = { viewModel.triggerSync() },
                        state = pullToRefreshState,
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                state = pullToRefreshState,
                                isRefreshing = uiState.isSyncing,
                                containerColor = SurfaceContainerDark,
                                color = ReefPink,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        listContent()
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        listContent()
                    }
                }
            }

            // ── Speed-Dial Scrim ────────────────────────────────────────────
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OceanDark.copy(alpha = 0.72f))
                        .clickable { isFabExpanded = false }
                )
            }

            // ── Speed-Dial Floating Action Button (Mutation-Guarded) ────────
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpeedDialOption(
                            label = "SSH Key",
                            icon = Icons.Default.Terminal,
                            accentColor = Color(0xFFC77DFF),
                            onClick = {
                                isFabExpanded = false
                                onAddItemClick(VaultItemDomain.SSH_KEY)
                            }
                        )
                        SpeedDialOption(
                            label = "Secure Note",
                            icon = Icons.Default.Description,
                            accentColor = BrandClawCyan,
                            onClick = {
                                isFabExpanded = false
                                onAddItemClick(VaultItemDomain.NOTE)
                            }
                        )
                        SpeedDialOption(
                            label = "Password",
                            icon = Icons.Default.Key,
                            accentColor = ReefPink,
                            onClick = {
                                isFabExpanded = false
                                onAddItemClick(VaultItemDomain.PASSWORD)
                            }
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        if (syncStatus == SyncStatus.OFFLINE_READ_ONLY) {
                            Toast.makeText(
                                context,
                                "Creating items is disabled while offline (Bitwarden mutation guard).",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            isFabExpanded = !isFabExpanded
                        }
                    },
                    containerColor = if (syncStatus == SyncStatus.OFFLINE_READ_ONLY) ReefPink.copy(alpha = 0.4f) else ReefPink,
                    contentColor = TextPrimary,
                    modifier = Modifier.testTag("dashboard_add_item_fab")
                ) {
                    Icon(
                        imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (isFabExpanded) "Close Add Menu" else "Add Item",
                        tint = if (syncStatus == SyncStatus.OFFLINE_READ_ONLY) TextPrimary.copy(alpha = 0.5f) else TextPrimary
                    )
                }
            }

            if (showExitLockDialog) {
                AlertDialog(
                    onDismissRequest = { showExitLockDialog = false },
                    containerColor = SurfaceDark,
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Text(
                            text = "Lock & Exit ShellGuard?",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = "Your vault keys will be zeroized from memory and ShellGuard will close.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExitLockDialog = false
                                viewModel.lockVault {
                                    onLockClick()
                                    val activity = context as? Activity
                                    activity?.finishAndRemoveTask() ?: activity?.finish()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ReefPink),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Lock & Exit", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showExitLockDialog = false },
                            border = BorderStroke(1.dp, BorderSubtle),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Stay", color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SpeedDialOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 0.dp,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = label,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun VaultItemCard(
    item: UnifiedVaultItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Domain Icon Badge
            val (badgeBg, iconVector) = when (item.domain) {
                VaultItemDomain.PASSWORD -> Pair(ReefPink.copy(alpha = 0.15f), Icons.Default.Key)
                VaultItemDomain.NOTE -> Pair(BrandClawCyan.copy(alpha = 0.15f), Icons.Default.Description)
                VaultItemDomain.SSH_KEY -> Pair(Color(0xFF9D4EDD).copy(alpha = 0.15f), Icons.Default.Terminal)
            }
            val badgeTint = when (item.domain) {
                VaultItemDomain.PASSWORD -> ReefPink
                VaultItemDomain.NOTE -> BrandClawCyan
                VaultItemDomain.SSH_KEY -> Color(0xFFC77DFF)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = badgeTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (item.reprompt) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Claw Re-Prompt",
                            tint = ReefPink,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.subtitle,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!item.category.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceContainerDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.category,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
